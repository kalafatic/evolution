package eu.kalafatic.evolution.view.editors.pages.tools;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
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
                try {
                    task.setStatus(eu.kalafatic.evolution.model.orchestration.TaskStatus.RUNNING);
                    java.io.File workingDir = getProjectDir();
                    eu.kalafatic.evolution.controller.orchestration.TaskContext context = new eu.kalafatic.evolution.controller.orchestration.TaskContext(orchestrator, workingDir);
                    eu.kalafatic.evolution.controller.orchestration.EvolutionOrchestrator evo = new eu.kalafatic.evolution.controller.orchestration.EvolutionOrchestrator();
                    evo.executeTask(task, context);
                    return org.eclipse.core.runtime.Status.OK_STATUS;
                } catch (Exception e) {
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
