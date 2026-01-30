package eu.kalafatic.evolution.controller.handlers;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import eu.kalafatic.evolution.model.orchestration.Agent;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.Task;

public class MavenTool implements ITaskTool {
    @Override
    public boolean canHandle(String taskType) {
        return "maven".equalsIgnoreCase(taskType);
    }

    @Override
    public String execute(Orchestrator orchestrator, IProject project, Agent agent, Task task, String context, String lastFeedback, OrchestrationCommandHandler handler) throws Exception {
        java.io.File workingDir = (project != null) ? project.getLocation().toFile() : null;
        List<String> mavenArgs = new ArrayList<>();
        mavenArgs.add("mvn");
        if (orchestrator.getMaven() != null) {
            mavenArgs.addAll(orchestrator.getMaven().getGoals());
        } else {
            mavenArgs.add("clean");
            mavenArgs.add("install");
        }
        String result = handler.executeCommandExternal(workingDir, mavenArgs.toArray(new String[0]));
        if (project != null) project.refreshLocal(IResource.DEPTH_INFINITE, null);
        return result;
    }
}
