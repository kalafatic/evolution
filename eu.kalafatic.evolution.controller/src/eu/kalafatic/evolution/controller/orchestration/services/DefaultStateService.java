package eu.kalafatic.evolution.controller.orchestration.services;

import java.util.List;
import eu.kalafatic.evolution.controller.orchestration.IterationManager;
import eu.kalafatic.evolution.controller.orchestration.SystemState;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.TransitionToken;
import eu.kalafatic.evolution.controller.orchestration.OrchestrationState;
import eu.kalafatic.evolution.controller.orchestration.diagnostics.CausalNode;
import eu.kalafatic.evolution.controller.workflow.RuntimeEventBus;
import eu.kalafatic.evolution.controller.workflow.RuntimeEventType;
import eu.kalafatic.evolution.controller.workflow.StepModeController;
import eu.kalafatic.evolution.controller.workflow.WorkflowStatus;
import eu.kalafatic.evolution.controller.workflow.WorkflowStep;
import eu.kalafatic.evolution.controller.orchestration.SessionContext;
import eu.kalafatic.evolution.model.orchestration.IterationStatus;

public class DefaultStateService implements StateService {

    @Override
    public void initializeState(TaskContext context, IterationManager manager) {
        transition(SystemState.INIT, context, manager);
    }

    @Override
    public void transition(SystemState to, TaskContext ctx, IterationManager manager) {
        if (manager.getSessionContainer() == null) {
            throw new IllegalStateException("IterationManager: sessionContainer is null. Cannot transition state.");
        }
        manager.getSessionContainer().getStatusManager().updateStatus(ctx.getSessionId(), 0.0, to.toString());

        SystemState current = ctx.getStateHolder().getState();
        if (current == to) return;

        if (current == SystemState.DONE || current == SystemState.FAILED) {
            if (to != SystemState.INIT && to != SystemState.RECOVERING) {
                ctx.log("[KERNEL] Illegal state transition attempt: " + current + " -> " + to + ". Terminal states can only transition to INIT or RECOVERING.");
                return;
            }
        }

        TransitionToken token = new TransitionToken();
        SystemState from = ctx.getStateHolder().getState();
        ctx.getStateHolder().applyTransition(token, to);

        if (to == SystemState.INIT || to == SystemState.RECOVERING) {
            if (manager.getGitManager() != null) {
                manager.getGitManager().cleanupLocks();
            }
        }

        if (manager.getCurrentIterationModel() != null) {
            switch (to) {
                case DONE: manager.getCurrentIterationModel().setStatus(IterationStatus.DONE); break;
                case FAILED: manager.getCurrentIterationModel().setStatus(IterationStatus.FAILED); break;
                default: manager.getCurrentIterationModel().setStatus(IterationStatus.RUNNING); break;
            }
            String existingJustification = manager.getCurrentIterationModel().getJustification() != null ? manager.getCurrentIterationModel().getJustification() : "";
            if (!existingJustification.contains(to.toString())) {
                manager.getCurrentIterationModel().setJustification(existingJustification + "\n[STATE_TRANSITION] Reached phase: " + to);
            }
        }

        RuntimeEventBus bus = manager.getSessionContainer().getEventBus();
        bus.publish(
            new eu.kalafatic.evolution.controller.workflow.RuntimeEvent(
                eu.kalafatic.evolution.controller.workflow.RuntimeEventType.SUPERVISOR_STATUS_CHANGED,
                ctx.getSessionId(), "Kernel", to.toString())
                .withMetadata("execId", ctx.getDeterministicExecutionId())
                .withMetadata("fromState", from != null ? from.toString() : "NONE")
                .withMetadata("toState", to.toString()));

        String logMsg = String.format("[KERNEL] [%s] [%d] [%d] Transition: %s -> %s", ctx.getDeterministicExecutionId(), System.currentTimeMillis(), Thread.currentThread().getId(), (current != null ? current : "NONE"), to);
        ctx.log(logMsg);
        ctx.getOrchestrationState().addDiagnostic("[OrchestrationTrace] " + logMsg);

        ctx.getOrchestrationState().getCognitiveTrace().addNode(new CausalNode(
            "state-transition-" + System.currentTimeMillis(),
            "STATE_TRANSITION",
            "IterationManager",
            List.of(current != null ? current.toString() : "NONE"),
            List.of(to.toString()),
            1.0,
            "Transition to " + to
        ));
    }

    @Override
    public void checkStep(String entityId, String type, String description, TaskContext context, IterationManager manager) throws Exception {
        if (context.getOrchestrator().getAiChat() != null &&
            context.getOrchestrator().getAiChat().getPromptInstructions() != null &&
            context.getOrchestrator().getAiChat().getPromptInstructions().isStepMode()) {

            WorkflowStep step = new WorkflowStep("step-" + System.currentTimeMillis(), entityId, type);
            step.setDescription(description);
            if (manager.getSessionContainer() == null) {
                throw new IllegalStateException("IterationManager: sessionContainer is null. Cannot wait for step.");
            }
            StepModeController smc = (manager.getSessionContainer() instanceof SessionContext) ? ((SessionContext)manager.getSessionContainer()).getStepModeController() : null;
            if (smc == null) {
                throw new IllegalStateException("IterationManager: StepModeController is null.");
            }
            WorkflowStatus result = smc.waitForStep(context.getSessionId(), step, context);
            if (result == WorkflowStatus.FAILED) {
                throw new Exception("Step failed or rejected by user: " + description);
            }
        }
    }
}
