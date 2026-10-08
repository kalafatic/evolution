package eu.kalafatic.evolution.view.editors.pages.tools;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import eu.kalafatic.evolution.controller.orchestration.OrchestratorResponse;
import eu.kalafatic.evolution.controller.orchestration.OrchestratorServiceImpl;
import eu.kalafatic.evolution.controller.orchestration.ResultType;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.editors.pages.AEvoGroup;

/**
 * Abstract superclass for tool-specific UI groups with status feedback.
 */
public abstract class AToolGroup extends AEvoGroup {
    protected Color successColor;

    public AToolGroup(MultiPageEditor editor, Orchestrator orchestrator, Color successColor) {
        super(editor, orchestrator);
        this.successColor = successColor;
    }

    /**
     * Updates the group background color based on the test status.
     */
    public void updateGroupStatus() {
        if (group == null || group.isDisposed()) return;

        String status = getTestStatus();
        if ("SUCCESS".equals(status)) {
            group.setBackground(successColor);
        } else if ("FAILED".equals(status)) {
            group.setBackground(group.getDisplay().getSystemColor(SWT.COLOR_RED));
        } else {
            group.setBackground(group.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
        }
    }

    /**
     * Resets the test status in the model and updates the UI.
     */
    public void resetStatus() {
        clearTestStatus();
        updateGroupStatus();
    }

    /**
     * Gets the test status from the model.
     */
    protected abstract String getTestStatus();

    /**
     * Clears the test status in the model.
     */
    protected abstract void clearTestStatus();

    protected void executeCommand(String command, String type) {
        if (orchestrator == null) return;

        eu.kalafatic.evolution.model.orchestration.Task task = eu.kalafatic.evolution.model.orchestration.OrchestrationFactory.eINSTANCE.createTask();
        task.setName(command);
        task.setType(type);
        task.setStatus(eu.kalafatic.evolution.model.orchestration.TaskStatus.PENDING);
        orchestrator.getTasks().add(task);

        org.eclipse.core.runtime.jobs.Job job = new org.eclipse.core.runtime.jobs.Job("Tool Action: " + command) {
            @Override
            protected org.eclipse.core.runtime.IStatus run(org.eclipse.core.runtime.IProgressMonitor monitor) {
                long startTime = System.currentTimeMillis();
                String orchId = orchestrator != null ? orchestrator.getName() : "ToolAction";
                System.out.println("[TOOL][START] Action=" + command + " Type=" + type);
                eu.kalafatic.evolution.controller.manager.OrchestrationStatusManager.getInstance().updateStatus(orchId, 0.2, "Executing " + type + " command: " + command);

                try {
                    task.setStatus(eu.kalafatic.evolution.model.orchestration.TaskStatus.RUNNING);
                    java.io.File workingDir = getProjectDir();

                    eu.kalafatic.evolution.controller.orchestration.TaskRequest request = new eu.kalafatic.evolution.controller.orchestration.TaskRequest(command, workingDir);
                    request.getContext().put("orchestrator", orchestrator);

                    OrchestratorResponse response =
                        OrchestratorServiceImpl.getInstance().handle(request);

                    long duration = System.currentTimeMillis() - startTime;
                    if (response.getResultType() == ResultType.ERROR) {
                        task.setStatus(eu.kalafatic.evolution.model.orchestration.TaskStatus.FAILED);
                        System.out.println("[TOOL][END] Action=" + command + " FAILED durationMs=" + duration + " Summary=" + response.getSummary());
                        eu.kalafatic.evolution.controller.manager.OrchestrationStatusManager.getInstance().updateStatus(orchId, 0.0, "Tool command failed: " + command);
                        return new org.eclipse.core.runtime.Status(org.eclipse.core.runtime.IStatus.ERROR, "eu.kalafatic.evolution.view", response.getSummary());
                    }

                    task.setStatus(eu.kalafatic.evolution.model.orchestration.TaskStatus.DONE);
                    task.setResponse(response.getSummary());
                    System.out.println("[TOOL][END] Action=" + command + " SUCCESS durationMs=" + duration);
                    eu.kalafatic.evolution.controller.manager.OrchestrationStatusManager.getInstance().updateStatus(orchId, 1.0, "Tool command finished: " + command);

                    return org.eclipse.core.runtime.Status.OK_STATUS;
                } catch (Exception e) {
                    long duration = System.currentTimeMillis() - startTime;
                    task.setStatus(eu.kalafatic.evolution.model.orchestration.TaskStatus.FAILED);
                    System.out.println("[TOOL][END] Action=" + command + " EXCEPTION durationMs=" + duration + " Error=" + e.getMessage());
                    eu.kalafatic.evolution.controller.manager.OrchestrationStatusManager.getInstance().updateStatus(orchId, 0.0, "Tool command error: " + e.getMessage());
                    return new org.eclipse.core.runtime.Status(org.eclipse.core.runtime.IStatus.ERROR, "eu.kalafatic.evolution.view", e.getMessage(), e);
                }
            }
        };
        job.schedule();
    }

    protected java.io.File getProjectDir() {
        if (editor.getEditorInput() instanceof org.eclipse.ui.IFileEditorInput) {
            return ((org.eclipse.ui.IFileEditorInput) editor.getEditorInput()).getFile().getProject().getLocation().toFile();
        }
        return new java.io.File(System.getProperty("java.io.tmpdir"));
    }
}
