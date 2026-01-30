package eu.kalafatic.evolution.view.wizards;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import org.eclipse.swt.layout.GridData;

public class SetupOllamaModelWizard extends Wizard implements INewWizard {
    private Orchestrator orchestrator;
    private SetupOllamaModelPage page;

    public SetupOllamaModelWizard() {
        setWindowTitle("Setup Ollama Model");
        setNeedsProgressMonitor(true);
    }

    public SetupOllamaModelWizard(Orchestrator orchestrator) {
        this();
        this.orchestrator = orchestrator;
    }

    @Override
    public void init(IWorkbench workbench, IStructuredSelection selection) {
        if (orchestrator == null) {
            IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
            if (window != null) {
                IWorkbenchPage page = window.getActivePage();
                if (page != null) {
                    org.eclipse.ui.IViewPart view = page.findView(eu.kalafatic.evolution.view.views.PropertiesView.ID);
                    if (view instanceof eu.kalafatic.evolution.view.views.PropertiesView) {
                        orchestrator = (Orchestrator) ((eu.kalafatic.evolution.view.views.PropertiesView) view).getRootObject();
                    }
                }
            }
        }
    }

    @Override
    public void addPages() {
        page = new SetupOllamaModelPage();
        addPage(page);
    }

    @Override
    public boolean performFinish() {
        String modelName = page.getModelName();
        if (orchestrator.getOllama() != null) {
            orchestrator.getOllama().setModel(modelName);
        }

        Job job = new Job("Pulling Ollama Model: " + modelName) {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                monitor.beginTask("Pulling model " + modelName, IProgressMonitor.UNKNOWN);
                try {
                    ProcessBuilder pb = new ProcessBuilder("ollama", "pull", modelName);
                    pb.redirectErrorStream(true);
                    Process process = pb.start();
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            monitor.subTask(line);
                            if (monitor.isCanceled()) {
                                process.destroy();
                                return Status.CANCEL_STATUS;
                            }
                        }
                    }
                    int exitCode = process.waitFor();
                    if (exitCode != 0) {
                        return new Status(IStatus.ERROR, "eu.kalafatic.evolution.view", "Failed to pull model. Exit code: " + exitCode);
                    }
                } catch (Exception e) {
                    return new Status(IStatus.ERROR, "eu.kalafatic.evolution.view", "Failed to pull model", e);
                } finally {
                    monitor.done();
                }
                return Status.OK_STATUS;
            }
        };
        job.setUser(true);
        job.schedule();
        return true;
    }

    private class SetupOllamaModelPage extends WizardPage {
        private Text modelText;

        protected SetupOllamaModelPage() {
            super("SetupOllamaModelPage");
            setTitle("Setup Ollama Model");
            setDescription("Enter the name of the model to pull (e.g., llama3).");
        }

        @Override
        public void createControl(Composite parent) {
            Composite container = new Composite(parent, SWT.NONE);
            container.setLayout(new GridLayout(2, false));

            new Label(container, SWT.NONE).setText("Model Name:");
            modelText = new Text(container, SWT.BORDER);
            modelText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            String currentModel = (orchestrator.getOllama() != null) ? orchestrator.getOllama().getModel() : null;
            modelText.setText(currentModel != null ? currentModel : "llama3");

            setControl(container);
        }

        public String getModelName() { return modelText.getText(); }
    }
}
