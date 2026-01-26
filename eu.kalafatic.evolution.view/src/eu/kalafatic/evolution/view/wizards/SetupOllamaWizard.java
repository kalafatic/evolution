package eu.kalafatic.evolution.view.wizards;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Button;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.Ollama;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import org.eclipse.swt.layout.GridData;

public class SetupOllamaWizard extends Wizard implements INewWizard {
    private Orchestrator orchestrator;
    private SetupOllamaPage page;

    public SetupOllamaWizard() {
        setWindowTitle("Setup Ollama");
        setNeedsProgressMonitor(true);
    }

    public SetupOllamaWizard(Orchestrator orchestrator) {
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
                    org.eclipse.ui.IViewPart view = page.findView(eu.kalafatic.evolution.view.PropertiesView.ID);
                    if (view instanceof eu.kalafatic.evolution.view.PropertiesView) {
                        orchestrator = (Orchestrator) ((eu.kalafatic.evolution.view.PropertiesView) view).getRootObject();
                    }
                }
            }
        }
    }

    @Override
    public void addPages() {
        page = new SetupOllamaPage();
        addPage(page);
    }

    @Override
    public boolean performFinish() {
        Ollama ollama = orchestrator.getOllama();
        if (ollama == null) {
            return false;
        }
        ollama.setUrl(page.getUrl());
        ollama.setPath(page.getPath());

        if (page.isDownloadRequested()) {
            Job job = new Job("Downloading and Installing Ollama") {
                @Override
                protected IStatus run(IProgressMonitor monitor) {
                    monitor.beginTask("Installing Ollama", IProgressMonitor.UNKNOWN);
                    try {
                        ProcessBuilder pb = new ProcessBuilder("sh", "-c", "curl -fsSL https://ollama.com/install.sh | sh");
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
                            return new Status(IStatus.ERROR, "eu.kalafatic.evolution.view", "Installation failed with exit code " + exitCode);
                        }
                    } catch (Exception e) {
                        return new Status(IStatus.ERROR, "eu.kalafatic.evolution.view", "Installation failed", e);
                    } finally {
                        monitor.done();
                    }
                    return Status.OK_STATUS;
                }
            };
            job.setUser(true);
            job.schedule();
        }
        return true;
    }

    private class SetupOllamaPage extends WizardPage {
        private Text urlText;
        private Text pathText;
        private Button downloadBtn;

        protected SetupOllamaPage() {
            super("SetupOllamaPage");
            setTitle("Setup Ollama");
            setDescription("Enter Ollama URL and executable path.");
        }

        @Override
        public void createControl(Composite parent) {
            Composite container = new Composite(parent, SWT.NONE);
            container.setLayout(new GridLayout(2, false));

            new Label(container, SWT.NONE).setText("URL:");
            urlText = new Text(container, SWT.BORDER);
            urlText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            String currentUrl = orchestrator.getOllama() != null ? orchestrator.getOllama().getUrl() : null;
            urlText.setText(currentUrl != null ? currentUrl : "http://localhost:11434");

            new Label(container, SWT.NONE).setText("Executable Path:");
            pathText = new Text(container, SWT.BORDER);
            pathText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            String currentPath = orchestrator.getOllama() != null ? orchestrator.getOllama().getPath() : null;
            pathText.setText(currentPath != null ? currentPath : "");

            downloadBtn = new Button(container, SWT.CHECK);
            downloadBtn.setText("Download and Install Ollama (Linux)");
            downloadBtn.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, true, false, 2, 1));

            setControl(container);
        }

        public String getUrl() { return urlText.getText(); }
        public String getPath() { return pathText.getText(); }
        public boolean isDownloadRequested() { return downloadBtn.getSelection(); }
    }
}
