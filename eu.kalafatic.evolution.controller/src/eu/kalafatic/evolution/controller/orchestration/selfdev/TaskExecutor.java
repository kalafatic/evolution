package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.util.List;
import eu.kalafatic.evolution.controller.orchestration.EvolutionOrchestrator;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.TaskRequest;
import eu.kalafatic.evolution.model.orchestration.Task;
import eu.kalafatic.evolution.model.orchestration.TaskStatus;

public class TaskExecutor {
    private final EvolutionOrchestrator orchestrator;
    private final TaskContext context;

    public TaskExecutor(TaskContext context) {
        this.context = context;
        this.orchestrator = new EvolutionOrchestrator();
    }

    public TaskExecutor(TaskContext context, eu.kalafatic.evolution.model.orchestration.Orchestrator orchestrator) {
        this.context = context;
        this.orchestrator = (orchestrator instanceof EvolutionOrchestrator) ? (EvolutionOrchestrator) orchestrator : new EvolutionOrchestrator();
    }

    public EvolutionOrchestrator getOrchestrator() {
        return orchestrator;
    }

    public boolean executeTasks(List<Task> tasks) {
        return executeTasks(tasks, null);
    }

    public boolean executeTasks(List<Task> tasks, eu.kalafatic.evolution.controller.orchestration.AiService aiService) {
        context.log("[EXECUTOR] Routing " + tasks.size() + " tasks to the Kernel Control Plane.");
        try {
            // TaskExecutor is a helper that MUST route back through the IterationManager
            // to preserve the "Single Transition Authority" and PEV lifecycle.
            eu.kalafatic.evolution.controller.orchestration.IterationManager kernel = (aiService != null) ?
                eu.kalafatic.evolution.controller.orchestration.KernelFactory.create(context, aiService) :
                eu.kalafatic.evolution.controller.orchestration.KernelFactory.create(context);
            return kernel.executeTasksWithRetries(tasks);
        } catch (Exception e) {
            context.log("[EXECUTOR] Kernel execution failed: " + e.getMessage());
            return false;
        }
    }
}
