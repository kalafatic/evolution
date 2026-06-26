package eu.kalafatic.evolution.controller.orchestration.services;

import java.util.List;
import eu.kalafatic.evolution.controller.orchestration.IterationManager;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.OrchestrationState;
import eu.kalafatic.evolution.controller.orchestration.SystemState;
import eu.kalafatic.evolution.controller.orchestration.ChangeUnit;
import eu.kalafatic.evolution.controller.orchestration.util.EvolutionConstants;
import eu.kalafatic.evolution.controller.workflow.RuntimeEventBus;
import eu.kalafatic.evolution.controller.workflow.RuntimeEventType;
import eu.kalafatic.evolution.model.orchestration.Task;
import eu.kalafatic.evolution.model.orchestration.TaskStatus;
import org.json.JSONObject;

public class DefaultTaskExecutionService implements TaskExecutionService {

    @Override
    public boolean executeTasksWithRetries(List<Task> tasks, TaskContext context, IterationManager manager) throws Exception {
        return executeTasksWithRetries(tasks, null, context, manager);
    }

    @Override
    public boolean executeTasksWithRetries(List<Task> tasks, Runnable onStepComplete, TaskContext context, IterationManager manager) throws Exception {
        OrchestrationState state = context.getOrchestrationState();
        for (Task task : tasks) {
            if (task.getStatus() == TaskStatus.DONE) continue;
            boolean success = false;
            for (int retry = 1; retry <= EvolutionConstants.MAX_TASK_RETRIES; retry++) {
                state.addDiagnostic("[OrchestrationTrace] Executing task: " + task.getName() + " (Attempt " + retry + ")");
                context.checkPause();
                manager.transition(SystemState.EXECUTING, context);
                task.setStatus(TaskStatus.RUNNING);

                if (manager.getSessionContainer() == null) {
                    throw new IllegalStateException("IterationManager: sessionContainer is null. Cannot publish task started event.");
                }
                RuntimeEventBus bus = manager.getSessionContainer().getEventBus();
                bus.publish(
                    new eu.kalafatic.evolution.controller.workflow.RuntimeEvent(
                        RuntimeEventType.TASK_STARTED,
                        context.getSessionId(), "Kernel", task.getId()));

                manager.checkStep(task.getId(), "TASK_EXECUTION", "Executing task: " + task.getName());

                String result = manager.getTaskExecutor().getOrchestrator().executeTask(task, context);
                manager.transition(SystemState.VERIFYING, context);
                task.setStatus(TaskStatus.VERIFYING);
                ChangeUnit change = new ChangeUnit();
                change.setPatch(task.getResponse());

                manager.checkStep(task.getId(), "PATCH_GENERATION", "Patch generated for task: " + task.getName());

                JSONObject evaluation = manager.getValidator().evaluate(change, task.getName(), context);
                if (evaluation.optBoolean("success", false)) {
                    task.setStatus(TaskStatus.DONE);
                    state.addDiagnostic("[OrchestrationTrace] Task " + task.getName() + " succeeded.");

                    bus.publish(
                        new eu.kalafatic.evolution.controller.workflow.RuntimeEvent(
                            RuntimeEventType.TASK_COMPLETED,
                            context.getSessionId(), "Kernel", task.getId()));

                    success = true;
                    if (onStepComplete != null) {
                        onStepComplete.run();
                    }
                    break;
                } else if (retry < EvolutionConstants.MAX_TASK_RETRIES) {
                    state.addDiagnostic("[OrchestrationTrace] Task " + task.getName() + " failed attempt " + retry + ". Diagnosing...");
                    manager.transition(SystemState.ANALYZING, context);
                    manager.getAnalyticAgent().diagnose(result, evaluation.optString("feedback"), context);
                    manager.transition(SystemState.MUTATING, context);
                } else {
                    state.addDiagnostic("[OrchestrationTrace] Task " + task.getName() + " failed after max retries.");
                    task.setStatus(TaskStatus.FAILED);
                }
            }
            if (!success) return false;
        }
        return true;
    }
}
