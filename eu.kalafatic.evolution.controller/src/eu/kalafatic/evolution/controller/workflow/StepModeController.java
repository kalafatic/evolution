package eu.kalafatic.evolution.controller.workflow;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;

public class StepModeController {
    private static final StepModeController instance = new StepModeController();
    private final Map<String, CompletableFuture<WorkflowStatus>> stepGates = new ConcurrentHashMap<>();

    private StepModeController() {}

    public static StepModeController getInstance() { return instance; }

    public WorkflowStatus waitForStep(String sessionId, WorkflowStep step, TaskContext context) {
        if (context.getOrchestrator().getAiChat() != null &&
            context.getOrchestrator().getAiChat().getPromptInstructions() != null &&
            !context.getOrchestrator().getAiChat().getPromptInstructions().isStepMode()) {
            return WorkflowStatus.COMPLETED;
        }

        step.setStatus(WorkflowStatus.WAITING_USER);
        WorkflowStepRegistry.getInstance().registerStep(sessionId, step);

        RuntimeEventBus.getInstance().publish(new RuntimeEvent(
            RuntimeEventType.STEP_WAITING, sessionId, "Kernel", step.getId()));

        CompletableFuture<WorkflowStatus> gate = new CompletableFuture<>();
        stepGates.put(step.getId(), gate);

        try {
            context.log("[STEP_MODE] Execution paused at step: " + step.getDescription() + " (ID: " + step.getId() + ")");
            return gate.get(); // Blocks orchestration
        } catch (Exception e) {
            context.log("[STEP_MODE] Step gate interrupted: " + e.getMessage());
            return WorkflowStatus.FAILED;
        } finally {
            stepGates.remove(step.getId());
        }
    }

    public void resumeStep(String stepId, WorkflowStatus result) {
        CompletableFuture<WorkflowStatus> gate = stepGates.get(stepId);
        if (gate != null) {
            WorkflowStep step = WorkflowStepRegistry.getInstance().getStep(stepId);
            if (step != null) {
                step.setStatus(result);
                String sessionId = "unknown";
                for (Map.Entry<String, java.util.List<String>> entry : WorkflowStepRegistry.getInstance().getSessionStepsMap().entrySet()) {
                    if (entry.getValue().contains(stepId)) {
                        sessionId = entry.getKey();
                        break;
                    }
                }
                RuntimeEventBus.getInstance().publish(new RuntimeEvent(
                    RuntimeEventType.STEP_RESUMED, sessionId, "UI", stepId));
            }
            gate.complete(result);
        }
    }
}
