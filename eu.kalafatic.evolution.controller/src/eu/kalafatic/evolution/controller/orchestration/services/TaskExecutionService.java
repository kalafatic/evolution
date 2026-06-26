package eu.kalafatic.evolution.controller.orchestration.services;

import java.util.List;
import eu.kalafatic.evolution.controller.orchestration.IterationManager;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.model.orchestration.Task;

public interface TaskExecutionService {
    boolean executeTasksWithRetries(List<Task> tasks, TaskContext context, IterationManager manager) throws Exception;
    boolean executeTasksWithRetries(List<Task> tasks, Runnable onStepComplete, TaskContext context, IterationManager manager) throws Exception;
}
