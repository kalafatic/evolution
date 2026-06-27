package eu.kalafatic.evolution.controller.workflow;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;

public class StepModeController {
    private final Map<String, CompletableFuture<WorkflowStatus>> stepGates = new ConcurrentHashMap<>();
    private final RuntimeEventBus eventBus;
    private final WorkflowStepRegistry workflowRegistry;

    public StepModeController(RuntimeEventBus eventBus, WorkflowStepRegistry workflowRegistry) {
        this.eventBus = eventBus;
        this.workflowRegistry = workflowRegistry;
    }

    public WorkflowStatus waitForStep(String sessionId, WorkflowStep step, TaskContext context) {
        if (context.isAutoApprove()) {
            return WorkflowStatus.COMPLETED;
        }

        if (context.getOrchestrator().getAiChat() != null &&
            context.getOrchestrator().getAiChat().getPromptInstructions() != null &&
            !context.getOrchestrator().getAiChat().getPromptInstructions().isStepMode()) {
            return WorkflowStatus.COMPLETED;
        }

        step.setStatus(WorkflowStatus.WAITING_USER);
        workflowRegistry.registerStep(sessionId, step);

        eventBus.publish(new RuntimeEvent(
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
            WorkflowStep step = workflowRegistry.getStep(stepId);
            if (step != null) {
                step.setStatus(result);
                String sessionId = "unknown";
                for (Map.Entry<String, java.util.List<String>> entry : workflowRegistry.getSessionStepsMap().entrySet()) {
                    if (entry.getValue().contains(stepId)) {
                        sessionId = entry.getKey();
                        break;
                    }
                }
                eventBus.publish(new RuntimeEvent(
                    RuntimeEventType.STEP_RESUMED, sessionId, "UI", stepId));
            }
            gate.complete(result);
        }
    }
}
