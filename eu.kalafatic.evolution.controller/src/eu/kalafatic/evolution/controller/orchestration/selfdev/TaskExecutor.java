package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.util.List;
import eu.kalafatic.evolution.controller.orchestration.EvolutionOrchestrator;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.model.orchestration.Task;
import eu.kalafatic.evolution.model.orchestration.TaskStatus;

public class TaskExecutor {
    private final EvolutionOrchestrator orchestrator = new EvolutionOrchestrator();
    private final TaskContext context;

    public TaskExecutor(TaskContext context) {
        this.context = context;
    }

    public boolean executeTasks(List<Task> tasks) {
        context.log("[EXECUTOR] Starting execution of " + tasks.size() + " tasks.");
        for (Task task : tasks) {
            try {
                context.log("[EXECUTOR] Executing task: " + task.getName());
                task.setStatus(TaskStatus.RUNNING);

                // We use the existing orchestrator's execute method.
                // However, the orchestrator normally plans its own tasks.
                // For Self-Development, we already have atomic tasks.
                // Let's call execute with the task name as the request.
                String result = orchestrator.execute(task.getName(), context);

                task.setResponse(result);
                task.setStatus(TaskStatus.DONE);
                context.log("[EXECUTOR] Task completed: " + task.getName());
            } catch (Exception e) {
                task.setStatus(TaskStatus.FAILED);
                task.setFeedback("Execution error: " + e.getMessage());
                context.log("[EXECUTOR] Task failed: " + task.getName() + ". Error: " + e.getMessage());
                return false; // Stop execution on first failure
            }
        }
        return true;
    }
}
