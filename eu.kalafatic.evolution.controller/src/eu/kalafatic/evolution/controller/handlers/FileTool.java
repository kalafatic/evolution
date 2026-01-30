package eu.kalafatic.evolution.controller.handlers;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Path;
import eu.kalafatic.evolution.model.orchestration.Agent;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.Task;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

public class FileTool implements ITaskTool {
    @Override
    public boolean canHandle(String taskType) {
        return "file".equalsIgnoreCase(taskType);
    }

    @Override
    public String execute(Orchestrator orchestrator, IProject project, Agent agent, Task task, String context, String lastFeedback, OrchestrationCommandHandler handler) throws Exception {
        String taskName = task.getName();
        // Heuristic: try to find a path in the task name
        String filePath = extractFilePath(taskName);
        if (filePath == null) {
            return "Error: Could not extract file path from task name: " + taskName;
        }

        String prompt = "Generate content for the file: " + filePath + "\n" +
                "Context: " + context + "\n" +
                "Requirements: " + taskName + "\n" +
                "Provide ONLY the file content, no markdown blocks or explanations.";

        String content = handler.sendRequest(orchestrator, prompt, (orchestrator.getAiChat() != null) ? orchestrator.getAiChat().getProxyUrl() : null);

        if (project != null) {
            IFile file = project.getFile(new Path(filePath));
            if (file.exists()) {
                file.setContents(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)), IResource.FORCE, null);
            } else {
                // Ensure parent directories exist
                createParentFolders(file);
                file.create(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)), true, null);
            }
            project.refreshLocal(IResource.DEPTH_INFINITE, null);
            return "Successfully wrote content to " + filePath;
        }

        return "Error: Project not available to save file: " + filePath;
    }

    private String extractFilePath(String taskName) {
        // Simple heuristic: look for something that looks like a path (contains . and /)
        String[] parts = taskName.split("\\s+");
        for (String part : parts) {
            if (part.contains(".") && (part.contains("/") || part.contains("\\"))) {
                return part.replaceAll("['\"]", "");
            }
        }
        return null;
    }

    private void createParentFolders(IFile file) throws Exception {
        org.eclipse.core.resources.IContainer parent = file.getParent();
        if (parent instanceof org.eclipse.core.resources.IFolder && !parent.exists()) {
            createFolder((org.eclipse.core.resources.IFolder) parent);
        }
    }

    private void createFolder(org.eclipse.core.resources.IFolder folder) throws Exception {
        org.eclipse.core.resources.IContainer parent = folder.getParent();
        if (parent instanceof org.eclipse.core.resources.IFolder && !parent.exists()) {
            createFolder((org.eclipse.core.resources.IFolder) parent);
        }
        if (!folder.exists()) {
            folder.create(true, true, null);
        }
    }
}
