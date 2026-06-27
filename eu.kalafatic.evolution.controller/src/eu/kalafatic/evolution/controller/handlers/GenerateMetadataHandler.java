package eu.kalafatic.evolution.controller.handlers;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;

import eu.kalafatic.evolution.controller.agents.MetadataAgent;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

/**
 * Command handler to trigger AI Metadata generation for a project.
 */
public class GenerateMetadataHandler extends AbstractOrchestratorHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        Orchestrator orchestrator = getOrchestrator(event);
        if (orchestrator != null) {
            IProject project = getProject(orchestrator);
            if (project != null) {
                Job job = new Job("Generating AI Metadata") {
                    @Override
                    protected IStatus run(IProgressMonitor monitor) {
                        try {
                            MetadataAgent generator = new MetadataAgent();
                            generator.generate(project.getLocation().toFile(), monitor);

                            Display.getDefault().asyncExec(() -> {
                                MessageDialog.openInformation(null, "Metadata Generation",
                                    "AI Metadata generation completed for: " + project.getName());
                            });
                            return Status.OK_STATUS;
                        } catch (Exception e) {
                            return new Status(IStatus.ERROR, "eu.kalafatic.evolution.controller",
                                "Failed to generate metadata", e);
                        }
                    }
                };
                job.schedule();
            }
        }
        return null;
    }
}
