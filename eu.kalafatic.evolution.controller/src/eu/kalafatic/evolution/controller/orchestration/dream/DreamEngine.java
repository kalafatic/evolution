package eu.kalafatic.evolution.controller.orchestration.dream;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;

import eu.kalafatic.evolution.controller.memory.MemoryEntry;
import eu.kalafatic.evolution.controller.memory.MemoryImportance;
import eu.kalafatic.evolution.controller.memory.MemoryScope;
import eu.kalafatic.evolution.controller.memory.MemoryService;
import eu.kalafatic.evolution.controller.memory.MemorySource;
import eu.kalafatic.evolution.controller.memory.MemoryType;
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
import eu.kalafatic.evolution.controller.orchestration.selfdev.IterationRecord;
import eu.kalafatic.evolution.controller.orchestration.selfdev.SystemStateSignalProvider;

/**
 * Specialized Darwin Engine implementation for DARWIN_DREAM_MODE.
 * Executes bounded, low-priority background cognitive analysis over existing platform experiences.
 */
public class DreamEngine extends ADarwinEngine {

    private final WakeDreamCoordinator coordinator = WakeDreamCoordinator.getInstance();
    private DreamCheckpoint checkpoint;

    public DreamEngine(TaskContext context, IterationMemoryService memoryService, SystemStateSignalProvider stateProvider) {
        super(context, memoryService, stateProvider, PlatformType.DREAM_MODE);
    }

    @Override
    public OrchestratorResponse orchestrateEvolution(TaskRequest taskRequest, IterationManager iterationManager) throws Exception {
        String cycleId = "dream-" + System.currentTimeMillis();

        if (!coordinator.requestDreamStart(cycleId)) {
            context.log("[DREAM] Unable to start DREAM cycle: " + coordinator.getBlockedReason());
            OrchestratorResponse response = new OrchestratorResponse();
            response.setResultType(ResultType.CHAT);
            response.setSummary("DREAM cycle skipped: " + coordinator.getBlockedReason());
            return response;
        }

        try {
            checkpoint = new DreamCheckpoint(cycleId);
            context.getOrchestrationState().getMetadata().put("dreamCheckpoint", checkpoint);

            context.log("[DREAM] Starting DREAM background analysis cycle: " + cycleId);

            boolean success = executeDreamLifecycle(iterationManager);

            coordinator.notifyDreamComplete(cycleId, success);

            OrchestratorResponse response = new OrchestratorResponse();
            response.setResultType(ResultType.CHAT);
            response.setSummary(success ? "DREAM analysis cycle completed successfully." : "DREAM analysis cycle interrupted/failed.");
            return response;

        } catch (Exception e) {
            context.log("[DREAM] CRITICAL error during DREAM cycle: " + e.getMessage());
            coordinator.notifyDreamComplete(cycleId, false);
            throw e;
        }
    }

    /**
     * Bounded 6-stage analytical lifecycle.
     */
    public boolean executeDreamLifecycle(IterationManager iterationManager) throws Exception {
        if (shouldPauseOrYield()) {
            handlePause(checkpoint.getStage());
            return false;
        }

        // STAGE 1: SELECT_EXPERIENCES
        checkpoint.setStage("SELECT_EXPERIENCES");
        context.log("[DREAM] Stage 1/6: SELECT_EXPERIENCES");
        selectExperiences();

        if (shouldPauseOrYield()) {
            handlePause("ANALYZE_RELATIONSHIPS");
            return false;
        }

        // STAGE 2: ANALYZE_RELATIONSHIPS
        checkpoint.setStage("ANALYZE_RELATIONSHIPS");
        context.log("[DREAM] Stage 2/6: ANALYZE_RELATIONSHIPS");
        analyzeRelationships();

        if (shouldPauseOrYield()) {
            handlePause("GENERATE_HYPOTHESES");
            return false;
        }

        // STAGE 3: GENERATE_HYPOTHESES
        checkpoint.setStage("GENERATE_HYPOTHESES");
        context.log("[DREAM] Stage 3/6: GENERATE_HYPOTHESES");
        generateHypotheses();

        if (shouldPauseOrYield()) {
            handlePause("VERIFY_HYPOTHESES");
            return false;
        }

        // STAGE 4: VERIFY_HYPOTHESES
        checkpoint.setStage("VERIFY_HYPOTHESES");
        context.log("[DREAM] Stage 4/6: VERIFY_HYPOTHESES");
        verifyHypotheses();

        if (shouldPauseOrYield()) {
            handlePause("CONSOLIDATE_RESULTS");
            return false;
        }

        // STAGE 5: CONSOLIDATE_RESULTS
        checkpoint.setStage("CONSOLIDATE_RESULTS");
        context.log("[DREAM] Stage 5/6: CONSOLIDATE_RESULTS");
        consolidateResults();

        // STAGE 6: COMPLETE
        checkpoint.setStage("COMPLETE");
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
        context.log("[DREAM] Safe suspension point reached. Checkpoint saved for stage: " + nextStage);
        coordinator.confirmDreamPaused(checkpoint.getCycleId());
    }

    private void selectExperiences() {
        FailureMemory failureMemory = memoryService != null ? memoryService.getFailureMemory() : null;
        if (failureMemory != null && !failureMemory.getFingerprints().isEmpty()) {
            failureMemory.getFingerprints().forEach((fingerprint, count) -> {
                checkpoint.getObservedFacts().add("Recurring failure (" + count + "x): " + fingerprint);
            });
        }

        if (memoryService != null && memoryService.getRecords() != null) {
            List<IterationRecord> failedRecords = memoryService.getRecords().stream()
                .filter(r -> "FAILED".equalsIgnoreCase(r.getResult()) || (r.getRejectionReasons() != null && !r.getRejectionReasons().isEmpty()))
                .limit(5)
                .collect(Collectors.toList());

            for (IterationRecord rec : failedRecords) {
                String id = "record-" + rec.getIteration() + "-" + rec.getBranchId();
                checkpoint.getSelectedExperienceIds().add(id);
                checkpoint.getObservedFacts().add("Failed iteration " + rec.getIteration() + " [Goal: " + rec.getGoal() + " | Strategy: " + rec.getStrategy() + "]");
            }
        }

        List<MemoryEntry> recentMemories = MemoryService.getInstance().getAllEntries();
        for (MemoryEntry entry : recentMemories) {
            if (entry.getScope() == MemoryScope.PROJECT || entry.getType() == MemoryType.OBSERVATION) {
                checkpoint.getObservedFacts().add("Memory Fact: " + entry.getContent());
            }
        }

        context.log("[DREAM] Selected " + checkpoint.getObservedFacts().size() + " observed facts for analysis.");
    }

    private void analyzeRelationships() throws Exception {
        if (checkpoint.getObservedFacts().isEmpty()) {
            context.log("[DREAM] No facts available to analyze relationships.");
            return;
        }

        StringBuilder prompt = new StringBuilder();
        prompt.append("Analyze the following observed historical EVO platform events/failures and identify potential underlying relationships or recurring root causes:\n\n");
        for (String fact : checkpoint.getObservedFacts()) {
            prompt.append("- ").append(fact).append("\n");
        }
        prompt.append("\nReturn concise bullet points of potential relationships between these failures/events. Do NOT include code.");

        LlmResponse response = aiService.sendLlmRequest(context.getOrchestrator(), prompt.toString(), 0.3f, null, context, null);
        if (response != null && response.getFinalContent() != null) {
            String content = response.getFinalContent();
            String[] lines = content.split("\n");
            for (String line : lines) {
                String trimmed = line.trim().replaceAll("^[\\-*#\\d\\.]+\\s*", "");
                if (!trimmed.isEmpty()) {
                    checkpoint.getCandidateRelationships().add(trimmed);
                }
            }
        }
    }

    private void generateHypotheses() throws Exception {
        if (checkpoint.getCandidateRelationships().isEmpty()) {
            context.log("[DREAM] No candidate relationships to generate hypotheses.");
            return;
        }

        StringBuilder prompt = new StringBuilder();
        prompt.append("Based on these identified relationships in historical EVO failures:\n");
        for (String rel : checkpoint.getCandidateRelationships()) {
            prompt.append("- ").append(rel).append("\n");
        }
        prompt.append("\nFormulate up to 3 concrete analytical hypotheses explaining why these failures occur and how they can be prevented. Format each hypothesis as a clear statement starting with 'Hypothesis: '.");

        LlmResponse response = aiService.sendLlmRequest(context.getOrchestrator(), prompt.toString(), 0.3f, null, context, null);
        if (response != null && response.getFinalContent() != null) {
            String content = response.getFinalContent();
            String[] lines = content.split("\n");
            for (String line : lines) {
                if (line.toLowerCase().contains("hypothesis:")) {
                    String hyp = line.substring(line.toLowerCase().indexOf("hypothesis:") + 11).trim();
                    if (!isDuplicateHypothesis(hyp)) {
                        checkpoint.getGeneratedHypotheses().add(hyp);
                    }
                }
            }
        }
    }

    private boolean isDuplicateHypothesis(String hyp) {
        String norm = hyp.toLowerCase().trim();
        for (String existing : checkpoint.getGeneratedHypotheses()) {
            if (existing.toLowerCase().trim().equals(norm)) return true;
        }
        return false;
    }

    private void verifyHypotheses() {
        for (String hyp : checkpoint.getGeneratedHypotheses()) {
            boolean hasFactSupport = false;
            for (String fact : checkpoint.getObservedFacts()) {
                if (containsCommonKeywords(hyp, fact)) {
                    checkpoint.getSupportingEvidence().add("Hypothesis [" + hyp + "] supported by fact: " + fact);
                    hasFactSupport = true;
                }
            }
            if (hasFactSupport) {
                checkpoint.getVerifiedResults().add("Verified Insight: " + hyp);
            } else {
                checkpoint.getUnresolvedQuestions().add("Unresolved Hypothesis (Lacks direct fact correlation): " + hyp);
            }
        }
    }

    private boolean containsCommonKeywords(String a, String b) {
        String[] wordsA = a.toLowerCase().split("\\W+");
        String[] wordsB = b.toLowerCase().split("\\W+");
        int matches = 0;
        for (String wa : wordsA) {
            if (wa.length() < 4) continue;
            for (String wb : wordsB) {
                if (wa.equals(wb)) {
                    matches++;
                    if (matches >= 2) return true;
                }
            }
        }
        return false;
    }

    private void consolidateResults() {
        if (checkpoint.getVerifiedResults().isEmpty()) {
            context.log("[DREAM] No new verified insights to persist.");
            return;
        }

        MemoryService memoryService = MemoryService.getInstance();
        for (String verified : checkpoint.getVerifiedResults()) {
            MemoryEntry entry = new MemoryEntry();
            entry.setId(UUID.randomUUID().toString());
            entry.setContent(verified);
            entry.setScope(MemoryScope.PROJECT);
            entry.setType(MemoryType.FACT);
            entry.setSource(MemorySource.INFERENCE);
            entry.setImportance(MemoryImportance.MEDIUM);
            entry.setConfidence(0.85);
            entry.setAssociatedId(checkpoint.getCycleId());
            memoryService.addEntry(entry);
            context.log("[DREAM] Persisted verified DREAM insight into MemoryService: " + verified);
        }
    }

    public DreamCheckpoint getCheckpoint() {
        return checkpoint;
    }
}
