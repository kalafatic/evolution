package eu.kalafatic.evolution.controller.orchestration;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.json.JSONArray;
import org.json.JSONObject;

import eu.kalafatic.evolution.controller.workflow.RuntimeEvent;
import eu.kalafatic.evolution.controller.workflow.RuntimeEventType;

/**
 * Helper to publish evolutionary progress events and update the UI progress monitor.
 */
public class EvolutionProgressPublisher {
    private static final Map<String, EvolutionProgressEvent> activeEvents = new ConcurrentHashMap<>();

    public static void startIteration(TaskContext context, int iterationCount, int generation, String lineage) {
        EvolutionProgressEvent event = new EvolutionProgressEvent();
        event.setSessionId(context.getSessionId());
        event.setIterationCount(iterationCount);
        event.setGeneration(generation);
        event.setLineage(lineage);
        event.setStage(EvolutionStage.ITERATION_START);
        event.setStartTime(System.currentTimeMillis());
        event.setTotalSteps(9); // Standard stages count
        event.setCompletedSteps(0);

        // Populate explicit state if available
        OrchestrationState state = context.getOrchestrationState();
        if (state != null) {
            event.setGoal(state.getRawInput());
            event.setCurrentDimension((String) state.getMetadata().get("current_dimension"));
            event.setCurrentDimensionDescription((String) state.getMetadata().get("current_dimension_description"));

            eu.kalafatic.evolution.controller.orchestration.selfdev.EvolutionTree tree = context.getKernelContext().getMemoryService().getEvolutionTree();
            if (tree != null) {
                event.setParentId(tree.getCurrentWinnerId());
            }

            Object genomeObj = state.getMetadata().get("semanticGenome");
            eu.kalafatic.evolution.controller.orchestration.selfdev.SemanticGenome genome =
                eu.kalafatic.evolution.controller.parsers.JsonUtils.restoreFromMetadata(genomeObj,
                    eu.kalafatic.evolution.controller.orchestration.selfdev.SemanticGenome.class, "semanticGenome", context);
            if (genome != null) {
                event.setLockedDecisionCount(genome.getLockedDimensions().size());
            }
        }

        activeEvents.put(context.getSessionId(), event);
        publish(context, event);
    }

    public static void updateStage(TaskContext context, EvolutionStage stage) {
        EvolutionProgressEvent event = activeEvents.get(context.getSessionId());
        if (event == null) return;

        event.setStage(stage);
        event.setCompletedSteps(stage.ordinal());
        event.setTimestamp(System.currentTimeMillis());

        publish(context, event);
    }

    public static void updateBranchStatus(TaskContext context, String branchId, String strategy, String status, Double score) {
        EvolutionProgressEvent event = activeEvents.get(context.getSessionId());
        if (event == null) return;

        EvolutionProgressEvent.BranchStatus bs = event.getBranchStatuses().stream()
                .filter(b -> b.getId().equals(branchId))
                .findFirst()
                .orElse(null);

        if (bs == null) {
            bs = new EvolutionProgressEvent.BranchStatus();
            bs.setId(branchId);
            bs.setStrategy(strategy);
            event.getBranchStatuses().add(bs);
        }

        bs.setStatus(status);
        if (score != null) bs.setScore(score);
        event.setTimestamp(System.currentTimeMillis());

        publish(context, event);
    }

    public static void updateActiveModel(TaskContext context, String model, String task) {
        EvolutionProgressEvent event = activeEvents.get(context.getSessionId());
        if (event == null) return;

        event.setCurrentModel(model);
        event.setCurrentTask(task);
        event.setTimestamp(System.currentTimeMillis());

        publish(context, event);
    }

    public static void setWinnerId(TaskContext context, String winnerId) {
        EvolutionProgressEvent event = activeEvents.get(context.getSessionId());
        if (event == null) return;

        event.setWinnerId(winnerId);
        event.setTimestamp(System.currentTimeMillis());

        publish(context, event);
    }

    public static void completeIteration(TaskContext context) {
        updateStage(context, EvolutionStage.ITERATION_COMPLETE);
        activeEvents.remove(context.getSessionId());
    }

    private static void publish(TaskContext context, EvolutionProgressEvent event) {
        populateSessionProperties(context, event);
        JSONObject payload = toJson(event);

        // 1. Publish to RuntimeEventBus for real-time UI updates
        RuntimeEvent runtimeEvent = new RuntimeEvent(
                RuntimeEventType.EVOLUTION_PROGRESS,
                context.getSessionId(),
                "DarwinFlow",
                payload.toString()
        );

        if (context.getKernelContext() != null && context.getKernelContext().getEventBus() != null) {
             context.getKernelContext().getEventBus().publish(runtimeEvent);
        }

        // 2. Also send as a progress message to ConversationOutputController
        // We use a special agentType 'evolution-progress' which the JS renderer will recognize
        // Use a consistent turnId based on iteration to allow the UI to update the same block
        String turnId = context.getSessionId() + "_iter_" + event.getIterationCount();
        ConversationOutputController.getInstance().submitMessage(
                context.getSessionId(),
                turnId,
                "Evolution Monitor",
                payload.toString(),
                "evolution-progress",
                MessagePriority.PROGRESS,
                event.getStage() == EvolutionStage.ITERATION_COMPLETE
        );
    }

    private static void populateSessionProperties(TaskContext context, EvolutionProgressEvent event) {
        event.setAutoApprove(context.isAutoApprove());
        if (context.getOrchestrator() != null && context.getOrchestrator().getAiChat() != null) {
            String sid = context.getSessionId();
            context.getOrchestrator().getAiChat().getSessions().stream()
                .filter(s -> sid.equals(s.getId()))
                .findFirst()
                .ifPresent(s -> {
                    event.setGitAutomation(s.isGitAutomation());
                    event.setStepMode(s.isStepMode());
                    event.setMaxIterations(s.getMaxIterations());
                });
        }
    }

    private static JSONObject toJson(EvolutionProgressEvent event) {
        JSONObject json = new JSONObject();
        json.put("sessionId", event.getSessionId());
        json.put("iterationCount", event.getIterationCount());
        json.put("generation", event.getGeneration());
        json.put("lineage", event.getLineage());
        json.put("stage", event.getStage().name());
        json.put("completedSteps", event.getCompletedSteps());
        json.put("totalSteps", event.getTotalSteps());
        json.put("currentBranch", event.getCurrentBranch());
        json.put("currentModel", event.getCurrentModel());
        json.put("currentTask", event.getCurrentTask());
        json.put("goal", event.getGoal());
        json.put("winnerId", event.getWinnerId());
        json.put("parentId", event.getParentId());
        json.put("currentDimension", event.getCurrentDimension());
        json.put("currentDimensionDescription", event.getCurrentDimensionDescription());
        json.put("lockedDecisionCount", event.getLockedDecisionCount());
        json.put("autoApprove", event.isAutoApprove());
        json.put("gitAutomation", event.isGitAutomation());
        json.put("stepMode", event.isStepMode());
        json.put("maxIterations", event.getMaxIterations());
        json.put("timestamp", event.getTimestamp());
        json.put("startTime", event.getStartTime());

        JSONArray branches = new JSONArray();
        for (EvolutionProgressEvent.BranchStatus bs : event.getBranchStatuses()) {
            JSONObject bJson = new JSONObject();
            bJson.put("id", bs.getId());
            bJson.put("strategy", bs.getStrategy());
            bJson.put("status", bs.getStatus());
            bJson.put("score", bs.getScore());
            branches.put(bJson);
        }
        json.put("branches", branches);
        return json;
    }
}
