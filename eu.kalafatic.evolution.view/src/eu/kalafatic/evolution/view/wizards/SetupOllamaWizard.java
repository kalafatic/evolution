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
import java.io.InputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.eclipse.swt.layout.GridData;
import org.eclipse.jface.viewers.ISelection;

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

    private IProject getProject() {
        if (orchestrator != null && orchestrator.eResource() != null) {
            org.eclipse.emf.common.util.URI uri = orchestrator.eResource().getURI();
            if (uri.isPlatformResource()) {
                String platformString = uri.toPlatformString(true);
                org.eclipse.core.resources.IResource res = ResourcesPlugin.getWorkspace().getRoot().findMember(platformString);
                if (res != null) {
                    return res.getProject();
                }
            }
        }
        return ResourcesPlugin.getWorkspace().getRoot().getProject("TestProject");
    }

    @Override
    public boolean performFinish() {
        Ollama ollama = orchestrator.getOllama();
        if (ollama == null) {
            return false;
        }
        ollama.setUrl(page.getUrl());

        final String os = System.getProperty("os.name").toLowerCase();
        final boolean isWin = os.contains("win");

        String path = page.getPath();
        if (page.isDownloadToWorkspaceRequested()) {
            IProject project = getProject();
            String binName = isWin ? "ollama.exe" : "ollama_bin";
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
                        if (toWorkspace) {
                            IProject project = getProject();
                            File binDir = project.getLocation().toFile();
                            if (!binDir.exists()) binDir.mkdirs();

                            if (isWin) {
                                monitor.subTask("Downloading Ollama for Windows ZIP...");
                                URL downloadUrl = new URL("https://github.com/ollama/ollama/releases/latest/download/ollama-windows-amd64.zip");
                                try (InputStream in = downloadUrl.openStream();
                                     ZipInputStream zis = new ZipInputStream(in)) {
                                    ZipEntry entry;
                                    while ((entry = zis.getNextEntry()) != null) {
                                        if (entry.getName().equals("ollama.exe")) {
                                            File outputFile = new File(binDir, "ollama.exe");
                                            try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                                                byte[] buffer = new byte[8192];
                                                int len;
                                                while ((len = zis.read(buffer)) > 0) {
                                                    fos.write(buffer, 0, len);
                                                }
                                            }
                                            outputFile.setExecutable(true);
                                            break;
                                        }
                                        zis.closeEntry();
                                    }
                                }
                            } else {
                                monitor.subTask("Downloading Ollama for Linux...");
                                File outputFile = new File(binDir, "ollama_bin");
                                URL downloadUrl = new URL("https://ollama.com/download/ollama-linux-amd64");
                                try (ReadableByteChannel rbc = Channels.newChannel(downloadUrl.openStream());
                                     FileOutputStream fos = new FileOutputStream(outputFile)) {
                                    fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
                                }
                                outputFile.setExecutable(true);
                            }
                            monitor.subTask("Download complete to workspace.");
                        } else if (page.isDownloadRequested()) {
                            if (isWin) {
                                monitor.subTask("Opening Ollama download page for Windows...");
                                PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL("https://ollama.com/download/windows"));
                            } else {
                                monitor.subTask("Installing Ollama globally (Linux)...");
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
            pathText.setText(currentPath != null ? currentPath : "");

            workspaceBtn = new Button(container, SWT.CHECK);
            workspaceBtn.setText("Download to Project Workspace");
            workspaceBtn.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, true, false, 2, 1));

            downloadBtn = new Button(container, SWT.CHECK);
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                downloadBtn.setText("Download/Install Ollama (Windows)");
            } else {
                downloadBtn.setText("Install Ollama Globally (Linux)");
            }
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
