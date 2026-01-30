package eu.kalafatic.evolution.controller.handlers;

import org.eclipse.core.resources.IProject;
import eu.kalafatic.evolution.model.orchestration.Agent;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.Task;

public interface ITaskTool {
    boolean canHandle(String taskType);
    String execute(Orchestrator orchestrator, IProject project, Agent agent, Task task, String context, String lastFeedback, OrchestrationCommandHandler handler) throws Exception;
}
