package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.util.List;

import eu.kalafatic.evolution.controller.orchestration.EvolutionOrchestrator;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.workflow.RuntimeEvent;
import eu.kalafatic.evolution.controller.workflow.RuntimeEventType;
import eu.kalafatic.evolution.model.orchestration.Task;

public class TaskExecutor {
    private final EvolutionOrchestrator orchestrator;
    private final TaskContext context;

    public TaskExecutor(TaskContext context) {
        this(context, context.getOrchestrator());
    }

    public TaskExecutor(TaskContext context, eu.kalafatic.evolution.model.orchestration.Orchestrator orchestrator) {
        this.context = context;
        eu.kalafatic.evolution.controller.orchestration.SessionContainer session = eu.kalafatic.evolution.controller.orchestration.SessionManager.getInstance().getSession(context.getSessionId());
        this.orchestrator = (orchestrator instanceof EvolutionOrchestrator) ? (EvolutionOrchestrator) orchestrator : new EvolutionOrchestrator(session);
    }

    public EvolutionOrchestrator getOrchestrator() {
        return orchestrator;
    }

    public boolean executeTasks(List<Task> tasks) {
        return executeTasks(tasks, null);
    }

    public boolean executeTasks(List<Task> tasks, eu.kalafatic.evolution.controller.orchestration.AiService aiService) {
        context.log("[EXECUTOR] Routing " + tasks.size() + " tasks to the Kernel Control Plane.");
        // If this is called, it might be from a legacy path or a recursive variant evaluation.
        // We SHOULD NOT create a new IterationManager here if we are already in one,
        // but TaskExecutor currently doesn't have a direct reference to its parent IterationManager
        // to avoid circular dependencies in constructors.

        // HOWEVER, EvolutionOrchestrator (which this class owns) can execute tasks directly.
        try {
            for (Task task : tasks) {
                context.getKernelContext().getEventBus().publish(new RuntimeEvent(RuntimeEventType.TASK_STARTED, context.getSessionId(), "TaskExecutor", task.getId()));
                orchestrator.executeTask(task, context);
                context.getKernelContext().getEventBus().publish(new RuntimeEvent(RuntimeEventType.TASK_COMPLETED, context.getSessionId(), "TaskExecutor", task.getId()));
            }
            return true;
        } catch (Exception e) {
            context.getKernelContext().getEventBus().publish(new RuntimeEvent(RuntimeEventType.TASK_FAILED, context.getSessionId(), "TaskExecutor", e.getMessage()));
            context.log("[EXECUTOR] Direct execution failed: " + e.getMessage());
            return false;
        }
    }
}
