package eu.kalafatic.evolution.controller.handlers;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import eu.kalafatic.evolution.model.orchestration.Agent;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.Task;

public class GitTool implements ITaskTool {
    @Override
    public boolean canHandle(String taskType) {
        return "git".equalsIgnoreCase(taskType);
    }

    @Override
    public String execute(Orchestrator orchestrator, IProject project, Agent agent, Task task, String context, String lastFeedback, OrchestrationCommandHandler handler) throws Exception {
        java.io.File workingDir = (project != null) ? project.getLocation().toFile() : null;
        String taskName = task.getName();
        if (taskName.toLowerCase().contains("add") || taskName.toLowerCase().contains("commit")) {
            handler.executeCommandExternal(workingDir, "git", "add", ".");
            String result = handler.executeCommandExternal(workingDir, "git", "commit", "-m", "AI Evolution step: " + taskName);
            if (project != null) project.refreshLocal(IResource.DEPTH_INFINITE, null);
            return result;
        }
        return "No git action mapped for: " + taskName;
    }
}
