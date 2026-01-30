package eu.kalafatic.evolution.view.wizards;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.http.HttpClient;

import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.fieldassist.SimpleContentProposalProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.json.JSONArray;
import org.json.JSONObject;

import eu.kalafatic.evolution.model.orchestration.Ollama;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

public class OllamaSettingsPage extends WizardPage {
    private Text urlText, modelText, pathText;
    private Button skipCheck;
    private ControlDecoration pathDecorator, modelDecorator;
    private SimpleContentProposalProvider proposalProvider;
    private Job validationJob;

    public OllamaSettingsPage() {
        super("OllamaSettingsPage");
        setTitle("Ollama Settings");
        setDescription("Configure Ollama API settings.");
    }

    @Override
    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);
        container.setLayout(new GridLayout(2, false));

        new Label(container, SWT.NONE).setText("Ollama URL:");
        urlText = new Text(container, SWT.BORDER);
        urlText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        urlText.setText("http://localhost:11434");

        new Label(container, SWT.NONE).setText("Model Name:");
        modelText = new Text(container, SWT.BORDER);
        modelText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        modelText.setText("llama3");

        new Label(container, SWT.NONE).setText("Executable Path:");
        pathText = new Text(container, SWT.BORDER);
        pathText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        String defaultPath = "/usr/bin/ollama";
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            String localAppData = System.getenv("LOCALAPPDATA");
            if (localAppData != null) {
                defaultPath = localAppData + "\\Programs\\Ollama\\ollama.exe";
            } else {
                defaultPath = "C:\\Users\\" + System.getProperty("user.name") + "\\AppData\\Local\\Programs\\Ollama\\ollama.exe";
            }
        } else if (os.contains("mac")) {
            defaultPath = "/usr/local/bin/ollama";
        }
        pathText.setText(defaultPath);

        pathDecorator = new ControlDecoration(pathText, SWT.TOP | SWT.LEFT);
        pathDecorator.setImage(FieldDecorationRegistry.getDefault()
                .getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
        pathDecorator.hide();

        modelDecorator = new ControlDecoration(modelText, SWT.TOP | SWT.LEFT);
        modelDecorator.setImage(FieldDecorationRegistry.getDefault()
                .getFieldDecoration(FieldDecorationRegistry.DEC_WARNING).getImage());
        modelDecorator.hide();

        proposalProvider = new SimpleContentProposalProvider(new String[0]);
        proposalProvider.setFiltering(true);
        new ContentProposalAdapter(modelText, new TextContentAdapter(), proposalProvider, null, null);

        Link setupLink = new Link(container, SWT.NONE);
        setupLink.setText("<a>Setup/Download Ollama...</a>");
        setupLink.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false, 2, 1));
        setupLink.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                OrchestrationFactory factory = OrchestrationFactory.eINSTANCE;
                Orchestrator tempOrch = factory.createOrchestrator();
                Ollama ollama = factory.createOllama();
                ollama.setUrl(urlText.getText());
                ollama.setPath(pathText.getText());
                tempOrch.setOllama(ollama);

                SetupOllamaWizard wizard = new SetupOllamaWizard(tempOrch);
                WizardDialog dialog = new WizardDialog(getShell(), wizard);
                if (dialog.open() == WizardDialog.OK) {
                    urlText.setText(ollama.getUrl());
                    pathText.setText(ollama.getPath());
                    validateOllama();
                }
            }
        });

        Link modelsLink = new Link(container, SWT.NONE);
        modelsLink.setText("<a>Browse Ollama Models...</a>");
        modelsLink.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false, 2, 1));
        modelsLink.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                openUrl("https://ollama.com/library");
            }
        });

        Button downloadLatestBtn = new Button(container, SWT.PUSH);
        downloadLatestBtn.setText("Download Latest Ollama Headless");
        downloadLatestBtn.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false, 2, 1));
        downloadLatestBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                downloadLatestOllama();
            }
        });

        pathText.addModifyListener(e -> validateOllama());
        urlText.addModifyListener(e -> validateOllama());
        modelText.addModifyListener(e -> validateOllama());

        skipCheck = new Button(container, SWT.CHECK);
        skipCheck.setText("Skip this step and setup later");
        skipCheck.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, true, false, 2, 1));
        skipCheck.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                validateOllama();
            }
        });

        setControl(container);
        validateOllama();
    }

    private void validateOllama() {
        if (skipCheck.getSelection()) {
            if (validationJob != null) validationJob.cancel();
            pathDecorator.hide();
            modelDecorator.hide();
            setPageComplete(true);
            setErrorMessage(null);
            return;
        }
        String path = pathText.getText();
        File file = new File(path);
        if (!file.exists()) {
            pathDecorator.setDescriptionText("Ollama executable not found at specified path.");
            pathDecorator.show();
            setPageComplete(false);
            setErrorMessage("Ollama executable not found.");
        } else {
            pathDecorator.hide();
            setErrorMessage(null);
            setPageComplete(true);
        }

        // Debounced async check for models
        if (validationJob != null) validationJob.cancel();
        final String url = urlText.getText();
        final String model = modelText.getText();
        validationJob = new Job("Validate Ollama") {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                checkOllamaCommunication(url, model);
                return Status.OK_STATUS;
            }
        };
        validationJob.setSystem(true);
        validationJob.schedule(800);
    }

    private void checkOllamaCommunication(String url, String model) {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(java.time.Duration.ofSeconds(2))
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(java.net.URI.create(url + "/api/tags"))
                    .GET()
                    .build();
            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        if (response.statusCode() == 200) {
                            JSONObject json = new JSONObject(response.body());
                            JSONArray models = json.getJSONArray("models");
                            List<String> modelNames = new ArrayList<>();
                            boolean found = false;
                            for (int i = 0; i < models.length(); i++) {
                                String name = models.getJSONObject(i).getString("name");
                                modelNames.add(name);
                                if (name.equals(model) || name.split(":")[0].equals(model)) {
                                    found = true;
                                }
                            }
                            final boolean modelFound = found;
                            final String[] names = modelNames.toArray(new String[0]);
                            Display.getDefault().asyncExec(() -> {
                                proposalProvider.setProposals(names);
                                if (modelFound) {
                                    modelDecorator.hide();
                                } else {
                                    modelDecorator.setDescriptionText("Model not found on Ollama server.");
                                    modelDecorator.show();
                                }
                            });
                        }
                    }).exceptionally(ex -> {
                        Display.getDefault().asyncExec(() -> {
                            modelDecorator.setDescriptionText("Could not connect to Ollama server.");
                            modelDecorator.show();
                        });
                        return null;
                    });
        } catch (Exception e) {
            // ignore
        }
    }

    private void downloadLatestOllama() {
        Job job = new Job("Download Ollama") {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                monitor.beginTask("Downloading Ollama", IProgressMonitor.UNKNOWN);
                try {
                    String os = System.getProperty("os.name").toLowerCase();
                    String urlStr = "https://ollama.com/download/ollama-linux-amd64";
                    String binName = "ollama";
                    if (os.contains("win")) {
                        urlStr = "https://ollama.com/download/ollama-windows-amd64.zip";
                        binName = "ollama.zip";
                    } else if (os.contains("mac")) {
                        urlStr = "https://ollama.com/download/ollama-darwin-amd64";
                    }

                    URL url = new URL(urlStr);
                    File targetFile = new File(System.getProperty("user.home"), binName);

                    // Try to find a better place in workspace if possible
                    try {
                        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
                        if (root != null && root.getLocation() != null) {
                            File workspaceDir = root.getLocation().toFile();
                            targetFile = new File(workspaceDir, binName);
                        }
                    } catch (Exception e) {}

                    try (InputStream in = url.openStream();
                         FileOutputStream out = new FileOutputStream(targetFile)) {
                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        while ((bytesRead = in.read(buffer)) != -1) {
                            out.write(buffer, 0, bytesRead);
                        }
                    }
                    targetFile.setExecutable(true);

                    final String downloadedPath = targetFile.getAbsolutePath();
                    Display.getDefault().asyncExec(() -> {
                        pathText.setText(downloadedPath);
                        MessageDialog.openInformation(getShell(), "Download Complete", "Ollama downloaded to: " + downloadedPath);
                    });

                } catch (Exception e) {
                    return new Status(IStatus.ERROR, "eu.kalafatic.evolution.view", "Download failed", e);
                } finally {
                    monitor.done();
                }
                return Status.OK_STATUS;
            }
        };
        job.setUser(true);
        job.schedule();
    }

    private void openUrl(String url) {
        try {
            PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL(url));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getOllamaUrl() { return urlText.getText(); }
    public String getModelName() { return modelText.getText(); }
    public String getExecutablePath() { return pathText.getText(); }
    public boolean isSkipped() { return skipCheck.getSelection(); }
}
