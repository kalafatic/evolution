package eu.kalafatic.evolution.view.wizards;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Button;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.emf.common.util.URI;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Path;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.Ollama;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import org.eclipse.swt.layout.GridData;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.core.runtime.IPath;

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
            orchestrator = findOrchestrator(selection);
        }
        if (orchestrator == null) {
            IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
            if (window != null) {
                ISelection serviceSelection = window.getSelectionService().getSelection("eu.kalafatic.evolution.view.propertiesView");
                orchestrator = findOrchestrator(serviceSelection);
            }
        }
    }

    private Orchestrator findOrchestrator(ISelection selection) {
        if (selection instanceof IStructuredSelection) {
            Object first = ((IStructuredSelection) selection).getFirstElement();
            if (first instanceof Orchestrator) {
                return (Orchestrator) first;
            }
        }
        return null;
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

        String path = page.getPath();
        if (page.isDownloadToWorkspaceRequested()) {
            IProject project = getProject();
            String binName = System.getProperty("os.name").toLowerCase().contains("win") ? "ollama.exe" : "ollama";
            File workspaceOllama = project.getLocation().append(binName).toFile();
            path = workspaceOllama.getAbsolutePath();
        }
        ollama.setPath(path);

        if (page.isDownloadRequested() || page.isDownloadToWorkspaceRequested() || page.isRunRequested()) {
            final boolean toWorkspace = page.isDownloadToWorkspaceRequested();
            final boolean runAfter = page.isRunRequested();
            final String finalPath = path;

            Job job = new Job("Ollama Task") {
                @Override
                protected IStatus run(IProgressMonitor monitor) {
                    monitor.beginTask("Ollama Setup", IProgressMonitor.UNKNOWN);
                    try {
                        if (toWorkspace && (page.isDownloadToWorkspaceRequested())) {
                            IProject project = getProject();
                            File binDir = project.getLocation().toFile();
                            if (!binDir.exists()) binDir.mkdirs();

                            String os = System.getProperty("os.name").toLowerCase();
                            String binName = os.contains("win") ? "ollama-windows-amd64.zip" : "ollama";
                            File outputFile = new File(binDir, binName);

                            monitor.subTask("Downloading from ollama.com...");
                            String urlStr = "https://ollama.com/download/ollama-linux-amd64";
                            if (os.contains("win")) {
                                urlStr = "https://ollama.com/download/ollama-windows-amd64.zip";
                            } else if (os.contains("mac")) {
                                urlStr = "https://ollama.com/download/ollama-darwin-amd64";
                            }

                            URL downloadUrl = new URL(urlStr);
                            try (ReadableByteChannel rbc = Channels.newChannel(downloadUrl.openStream());
                                 FileOutputStream fos = new FileOutputStream(outputFile)) {
                                fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
                            }
                            outputFile.setExecutable(true);
                            monitor.subTask("Download complete: " + outputFile.getAbsolutePath());
                        } else if (page.isDownloadRequested()) {
                            monitor.subTask("Installing Ollama globally...");
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
                            process.waitFor();
                        }

                        if (runAfter) {
                            monitor.subTask("Starting Ollama...");
                            ProcessBuilder pbRun = new ProcessBuilder(finalPath, "serve");
                            pbRun.environment().put("OLLAMA_HOST", page.getUrl());
                            pbRun.start();
                            monitor.subTask("Ollama started.");
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

    private IProject getProject() {
        if (orchestrator != null && orchestrator.eResource() != null) {
            URI uri = orchestrator.eResource().getURI();
            if (uri.isPlatformResource()) {
                IPath path = new Path(uri.toPlatformString(true));
                return ResourcesPlugin.getWorkspace().getRoot().getFile(path).getProject();
            }
        }
        // If no project found, look for first project in workspace
        IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
        if (projects.length > 0) {
            return projects[0];
        }
        return ResourcesPlugin.getWorkspace().getRoot().getProject("TestProject");
    }

    private class SetupOllamaPage extends WizardPage {
        private Text urlText;
        private Text pathText;
        private Button downloadBtn;
        private Button workspaceBtn;
        private Button runBtn;

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
            String currentUrl = orchestrator != null && orchestrator.getOllama() != null ? orchestrator.getOllama().getUrl() : null;
            urlText.setText(currentUrl != null ? currentUrl : "http://localhost:11434");

            new Label(container, SWT.NONE).setText("Executable Path:");
            pathText = new Text(container, SWT.BORDER);
            pathText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            String currentPath = orchestrator != null && orchestrator.getOllama() != null ? orchestrator.getOllama().getPath() : null;
            if (currentPath == null || currentPath.isEmpty()) {
                String os = System.getProperty("os.name").toLowerCase();
                if (os.contains("win")) {
                    String localAppData = System.getenv("LOCALAPPDATA");
                    if (localAppData != null) {
                        currentPath = localAppData + "\\Programs\\Ollama\\ollama.exe";
                    } else {
                        currentPath = "C:\\Users\\" + System.getProperty("user.name") + "\\AppData\\Local\\Programs\\Ollama\\ollama.exe";
                    }
                } else if (os.contains("mac")) {
                    currentPath = "/usr/local/bin/ollama";
                } else {
                    currentPath = "/usr/bin/ollama";
                }
            }
            pathText.setText(currentPath != null ? currentPath : "");

            workspaceBtn = new Button(container, SWT.CHECK);
            workspaceBtn.setText("Download to Project Workspace");
            workspaceBtn.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, true, false, 2, 1));

            downloadBtn = new Button(container, SWT.CHECK);
            downloadBtn.setText("Install Ollama Globally (Linux)");
            downloadBtn.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, true, false, 2, 1));

            runBtn = new Button(container, SWT.CHECK);
            runBtn.setText("Run Ollama after finish");
            runBtn.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, true, false, 2, 1));

            setControl(container);
        }

        public String getUrl() { return urlText.getText(); }
        public String getPath() { return pathText.getText(); }
        public boolean isDownloadRequested() { return downloadBtn.getSelection(); }
        public boolean isDownloadToWorkspaceRequested() { return workspaceBtn.getSelection(); }
        public boolean isRunRequested() { return runBtn.getSelection(); }
    }
}
