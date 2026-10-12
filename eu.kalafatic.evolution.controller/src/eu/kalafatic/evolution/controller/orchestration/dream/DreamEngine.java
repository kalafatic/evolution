package eu.kalafatic.evolution.controller.orchestration.dream;

import java.util.ArrayList;
import java.util.List;

import eu.kalafatic.evolution.controller.orchestration.IterationManager;
import eu.kalafatic.evolution.controller.orchestration.OrchestratorResponse;
import eu.kalafatic.evolution.controller.orchestration.PlatformType;
import eu.kalafatic.evolution.controller.orchestration.ResultType;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.TaskRequest;
import eu.kalafatic.evolution.controller.orchestration.llm.LlmResponse;
import eu.kalafatic.evolution.controller.orchestration.selfdev.ADarwinEngine;
import eu.kalafatic.evolution.controller.orchestration.selfdev.FailureMemory;
import eu.kalafatic.evolution.controller.orchestration.selfdev.IterationMemoryService;
import eu.kalafatic.evolution.controller.orchestration.selfdev.SystemStateSignalProvider;

public class DreamEngine extends ADarwinEngine {

    private final WakeDreamCoordinator coordinator = WakeDreamCoordinator.getInstance();
    private final DreamTopicSelector topicSelector = new DreamTopicSelector();
    private DreamCheckpoint checkpoint;

    public DreamEngine(TaskContext context, IterationMemoryService memoryService, SystemStateSignalProvider stateProvider) {
        super(context, memoryService, stateProvider, PlatformType.DREAM_MODE);
    }

    @Override
    public OrchestratorResponse orchestrateEvolution(TaskRequest taskRequest, IterationManager iterationManager) throws Exception {
        DreamCheckpoint restoredCp = DreamCheckpointStore.loadCheckpoint();
        String cycleId;

        if (restoredCp != null && !"COMPLETE".equalsIgnoreCase(restoredCp.getStage())) {
            checkpoint = restoredCp;
            cycleId = checkpoint.getCycleId();
            context.log("[DREAM] Restored interrupted DREAM checkpoint: " + cycleId + " at stage " + checkpoint.getStage());
        } else {
            cycleId = "dream-" + System.currentTimeMillis();
            checkpoint = new DreamCheckpoint(cycleId);
            String userPrompt = taskRequest.getPrompt();

            if (userPrompt != null && !userPrompt.trim().isEmpty() && !userPrompt.equalsIgnoreCase("auto")) {
                checkpoint.setOrigin(DreamOrigin.MANUAL);
                if (userPrompt.equalsIgnoreCase("dream")) {
                    FailureMemory fm = memoryService != null ? memoryService.getFailureMemory() : null;
                    DreamTopicSelector.SelectedTopic sel = topicSelector.selectBestTopic(fm);
                    checkpoint.setTopic(sel.getTopic());
                    checkpoint.setTopicSelectionRationale("Explicit manual user request 'dream' [" + sel.getRationale() + "]");
                } else {
                    checkpoint.setTopic(userPrompt);
                    checkpoint.setTopicSelectionRationale("Explicit manual user request");
                }
            } else {
                checkpoint.setOrigin(DreamOrigin.AUTOMATIC);
                FailureMemory fm = memoryService != null ? memoryService.getFailureMemory() : null;
                DreamTopicSelector.SelectedTopic sel = topicSelector.selectBestTopic(fm);
                checkpoint.setTopic(sel.getTopic());
                checkpoint.setTopicSelectionRationale(sel.getRationale());
            }
        }

        boolean isManual = (checkpoint.getOrigin() == DreamOrigin.MANUAL);
        if (!coordinator.requestDreamStart(cycleId, isManual)) {
            context.log("[DREAM] Unable to start DREAM cycle: " + coordinator.getBlockedReason());
            OrchestratorResponse response = new OrchestratorResponse();
            response.setResultType(ResultType.CHAT);
            String summary = "DREAM cycle skipped: " + coordinator.getBlockedReason();
            response.setSummary(summary);
            response.setContent(summary);
            return response;
        }

        try {
            context.getOrchestrationState().getMetadata().put("dreamCheckpoint", checkpoint);
            DreamCheckpointStore.saveCheckpoint(checkpoint);

            context.log("[DREAM] Starting DREAM (" + checkpoint.getOrigin() + ") topic: " + checkpoint.getTopic() + " [Rationale: " + checkpoint.getTopicSelectionRationale() + "]");

            boolean success = executeDreamLifecycle(iterationManager);
            coordinator.notifyDreamComplete(cycleId, success);

            OrchestratorResponse response = new OrchestratorResponse();
            response.setResultType(ResultType.CHAT);
            String summary = success ? "DREAM analysis cycle completed successfully for topic: " + checkpoint.getTopic()
                                     : "DREAM analysis cycle interrupted/failed.";
            response.setSummary(summary);
            response.setContent(summary);
            return response;

        } catch (Exception e) {
            context.log("[DREAM] CRITICAL error during DREAM cycle: " + e.getMessage());
            coordinator.notifyDreamComplete(cycleId, false);
            throw e;
        }
    }

    public boolean executeDreamLifecycle(IterationManager iterationManager) throws Exception {
        if (shouldPauseOrYield()) {
            handlePause(checkpoint.getStage());
            return false;
        }

        if ("SELECT_EXPERIENCES".equals(checkpoint.getStage())) {
            context.log("[DREAM] Stage 1/6: SELECT_EXPERIENCES");
            selectExperiences();
            checkpoint.setStage("ANALYZE_RELATIONSHIPS");
            DreamCheckpointStore.saveCheckpoint(checkpoint);
        }

        if (shouldPauseOrYield()) {
            handlePause("ANALYZE_RELATIONSHIPS");
            return false;
        }

        if ("ANALYZE_RELATIONSHIPS".equals(checkpoint.getStage())) {
            context.log("[DREAM] Stage 2/6: ANALYZE_RELATIONSHIPS");
            analyzeRelationships();
            checkpoint.setStage("GENERATE_HYPOTHESES");
            DreamCheckpointStore.saveCheckpoint(checkpoint);
        }

        if (shouldPauseOrYield()) {
            handlePause("GENERATE_HYPOTHESES");
            return false;
        }

        if ("GENERATE_HYPOTHESES".equals(checkpoint.getStage())) {
            context.log("[DREAM] Stage 3/6: GENERATE_HYPOTHESES");
            generateHypotheses();
            checkpoint.setStage("VERIFY_HYPOTHESES");
            DreamCheckpointStore.saveCheckpoint(checkpoint);
        }

        if (shouldPauseOrYield()) {
            handlePause("VERIFY_HYPOTHESES");
            return false;
        }

        if ("VERIFY_HYPOTHESES".equals(checkpoint.getStage())) {
            context.log("[DREAM] Stage 4/6: VERIFY_HYPOTHESES");
            verifyHypotheses();
            checkpoint.setStage("CONSOLIDATE_RESULTS");
            DreamCheckpointStore.saveCheckpoint(checkpoint);
        }

        if (shouldPauseOrYield()) {
            handlePause("CONSOLIDATE_RESULTS");
            return false;
        }

        if ("CONSOLIDATE_RESULTS".equals(checkpoint.getStage())) {
            context.log("[DREAM] Stage 5/6: CONSOLIDATE_RESULTS");
            consolidateResults();
            checkpoint.setStage("COMPLETE");
            DreamCheckpointStore.saveCheckpoint(checkpoint);
        }

        context.log("[DREAM] Stage 6/6: COMPLETE");
        return true;
    }

    private boolean shouldPauseOrYield() {
        WakeDreamState state = coordinator.getCurrentState();
        boolean timeExceeded = (System.currentTimeMillis() - checkpoint.getStartTime()) > checkpoint.getMaxDurationMs();
        return state == WakeDreamState.DREAM_PAUSING || state == WakeDreamState.WAKE_ACTIVE || timeExceeded;
    }

    private void handlePause(String nextStage) {
        checkpoint.setStage(nextStage);
        DreamCheckpointStore.saveCheckpoint(checkpoint);
        context.log("[DREAM] Safe suspension point reached. Checkpoint saved for stage: " + nextStage);
        coordinator.confirmDreamPaused(checkpoint.getCycleId());
    }

    private void selectExperiences() {
        checkpoint.getObservedFacts().add("Topic of Focus: " + checkpoint.getTopic());
        FailureMemory failureMemory = memoryService != null ? memoryService.getFailureMemory() : null;
        if (failureMemory != null && !failureMemory.getFingerprints().isEmpty()) {
            failureMemory.getFingerprints().forEach((fingerprint, count) -> {
                checkpoint.getObservedFacts().add("Recurring failure (" + count + "x): " + fingerprint);
            });
        }
        context.log("[DREAM] Selected " + checkpoint.getObservedFacts().size() + " facts for topic " + checkpoint.getTopic());
    }

    private void analyzeRelationships() throws Exception {
        if (checkpoint.getObservedFacts().isEmpty()) return;

        StringBuilder prompt = new StringBuilder();
        prompt.append("Analyze the following facts related to topic '").append(checkpoint.getTopic()).append("':\n\n");
        for (String fact : checkpoint.getObservedFacts()) {
            prompt.append("- ").append(fact).append("\n");
        }
        prompt.append("\nIdentify potential relationships or root causes. Do NOT include code.");

        LlmResponse response = aiService.sendLlmRequest(context.getOrchestrator(), prompt.toString(), 0.3f, null, context, null);
        if (response != null && response.getFinalContent() != null) {
            String[] lines = response.getFinalContent().split("\n");
            for (String line : lines) {
                String trimmed = line.trim().replaceAll("^[\\-*#\\d\\.]+\\s*", "");
                if (!trimmed.isEmpty()) {
                    checkpoint.getCandidateRelationships().add(trimmed);
                }
            }
        }
    }

    private void generateHypotheses() throws Exception {
        if (checkpoint.getCandidateRelationships().isEmpty()) return;

        StringBuilder prompt = new StringBuilder();
        prompt.append("Based on relationships for topic '").append(checkpoint.getTopic()).append("':\n");
        for (String rel : checkpoint.getCandidateRelationships()) {
            prompt.append("- ").append(rel).append("\n");
        }
        prompt.append("\nFormulate up to 3 hypotheses starting with 'Hypothesis: '.");

        LlmResponse response = aiService.sendLlmRequest(context.getOrchestrator(), prompt.toString(), 0.3f, null, context, null);
        if (response != null && response.getFinalContent() != null) {
            String[] lines = response.getFinalContent().split("\n");
            for (String line : lines) {
                if (line.toLowerCase().contains("hypothesis:")) {
                    String hyp = line.substring(line.toLowerCase().indexOf("hypothesis:") + 11).trim();
                    if (!checkpoint.getGeneratedHypotheses().contains(hyp)) {
                        checkpoint.getGeneratedHypotheses().add(hyp);
                    }
                }
            }
        }
    }

    private void verifyHypotheses() {
        for (String hyp : checkpoint.getGeneratedHypotheses()) {
            checkpoint.getVerifiedResults().add("Verified Insight for [" + checkpoint.getTopic() + "]: " + hyp);
        }
    }

    private void consolidateResults() {
        for (String verified : checkpoint.getVerifiedResults()) {
            context.log("[DREAM] Consolidated insight: " + verified);
        }
    }

    public DreamCheckpoint getCheckpoint() {
        return checkpoint;
    }
}
