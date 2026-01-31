package eu.kalafatic.evolution.view.wizards;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Button;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IFile;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.AiChat;
import eu.kalafatic.evolution.model.orchestration.LLM;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.eclipse.swt.layout.GridData;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;

public class SetupLLMWizard extends Wizard implements INewWizard {
    private Orchestrator orchestrator;
    private SetupLLMPage page;

    public SetupLLMWizard() {
        setWindowTitle("Setup LLM and AI Chat");
        setNeedsProgressMonitor(true);
    }

    public SetupLLMWizard(Orchestrator orchestrator) {
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
        page = new SetupLLMPage();
        addPage(page);
    }

    @Override
    public boolean performFinish() {
        AiChat chat = orchestrator.getAiChat();
        if (chat != null) {
            chat.setUrl(page.getChatUrl());
            chat.setToken(page.getChatToken());
            chat.setPrompt(page.getChatPrompt());
            chat.setProxyUrl(page.getProxyUrl());
        }

        LLM llm = orchestrator.getLlm();
        if (llm != null) {
            llm.setModel(page.getLlmModel());
            try {
                llm.setTemperature(Float.parseFloat(page.getLlmTemperature()));
            } catch (NumberFormatException e) {
                // ignore
            }
        }

        if (page.isDownloadRequested() || page.isTestRequested()) {
            final boolean test = page.isTestRequested();
            final String url = page.getChatUrl();
            final String token = page.getChatToken();

            Job job = new Job("LLM Setup Task") {
                @Override
                protected IStatus run(IProgressMonitor monitor) {
                    try {
                        if (page.isDownloadRequested()) {
                            IProject project = null;
                            if (orchestrator != null && orchestrator.eResource() != null) {
                                org.eclipse.emf.common.util.URI uri = orchestrator.eResource().getURI();
                                if (uri.isPlatformResource()) {
                                    org.eclipse.core.runtime.IPath path = new org.eclipse.core.runtime.Path(uri.toPlatformString(true));
                                    project = ResourcesPlugin.getWorkspace().getRoot().getFile(path).getProject();
                                }
                            }
                            if (project == null) {
                                IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
                                if (projects.length > 0) {
                                    project = projects[0];
                                }
                            }
                            if (project == null) return Status.OK_STATUS;

                            IFile configFile = project.getFile("ai_config.json");
                            String content = "{\n" +
                                             "  \"chat_url\": \"" + page.getChatUrl() + "\",\n" +
                                             "  \"llm_model\": \"" + page.getLlmModel() + "\",\n" +
                                             "  \"proxy_url\": \"" + page.getProxyUrl() + "\"\n" +
                                             "}";
                            InputStream source = new ByteArrayInputStream(content.getBytes());
                            if (configFile.exists()) {
                                configFile.setContents(source, true, true, null);
                            } else {
                                configFile.create(source, true, null);
                            }
                        }

                        if (test) {
                            monitor.beginTask("Testing Connection", IProgressMonitor.UNKNOWN);
                            HttpClient client = HttpClient.newHttpClient();
                            HttpRequest request = HttpRequest.newBuilder()
                                    .uri(URI.create(url))
                                    .header("Authorization", "Bearer " + token)
                                    .GET()
                                    .build();
                            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                            final String result = "Response Code: " + response.statusCode();
                            Display.getDefault().asyncExec(() -> {
                                MessageDialog.openInformation(getShell(), "Connection Test", result);
                            });
                        }
                    } catch (Exception e) {
                        return new Status(IStatus.ERROR, "eu.kalafatic.evolution.view", "Task failed", e);
                    }
                    return Status.OK_STATUS;
                }
            };
            job.schedule();
        }
        return true;
    }

    private class SetupLLMPage extends WizardPage {
        private Text chatUrlText, chatTokenText, chatPromptText, proxyUrlText;
        private Text llmModelText, llmTempText;
        private Button downloadBtn;
        private Button testBtn;

        protected SetupLLMPage() {
            super("SetupLLMPage");
            setTitle("Setup LLM and AI Chat");
            setDescription("Configure AI Chat and LLM settings.");
        }

        @Override
        public void createControl(Composite parent) {
            Composite container = new Composite(parent, SWT.NONE);
            container.setLayout(new GridLayout(2, false));

            new Label(container, SWT.NONE).setText("AI Chat URL:");
            chatUrlText = new Text(container, SWT.BORDER);
            chatUrlText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            new Label(container, SWT.NONE).setText("AI Chat Token:");
            chatTokenText = new Text(container, SWT.BORDER | SWT.PASSWORD);
            chatTokenText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            new Label(container, SWT.NONE).setText("AI Chat Prompt:");
            chatPromptText = new Text(container, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL);
            GridData gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.heightHint = 60;
            chatPromptText.setLayoutData(gd);

            new Label(container, SWT.NONE).setText("Proxy URL:");
            proxyUrlText = new Text(container, SWT.BORDER);
            proxyUrlText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            proxyUrlText.setMessage("e.g. http://proxy.example.com:8080");

            new Label(container, SWT.SEPARATOR | SWT.HORIZONTAL).setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

            new Label(container, SWT.NONE).setText("LLM Model:");
            llmModelText = new Text(container, SWT.BORDER);
            llmModelText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            Link pullModelLink = new Link(container, SWT.NONE);
            pullModelLink.setText("<a>Setup/Pull Ollama Model...</a>");
            pullModelLink.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false, 2, 1));
            pullModelLink.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
                @Override
                public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
                    SetupOllamaModelWizard wizard = new SetupOllamaModelWizard(orchestrator);
                    org.eclipse.jface.wizard.WizardDialog dialog = new org.eclipse.jface.wizard.WizardDialog(getShell(), wizard);
                    if (dialog.open() == org.eclipse.jface.wizard.WizardDialog.OK) {
                        if (orchestrator.getOllama() != null) {
                            llmModelText.setText(orchestrator.getOllama().getModel());
                        }
                    }
                }
            });

            new Label(container, SWT.NONE).setText("Temperature:");
            llmTempText = new Text(container, SWT.BORDER);
            llmTempText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            downloadBtn = new Button(container, SWT.CHECK);
            downloadBtn.setText("Save Config Template to Workspace");
            downloadBtn.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, true, false, 2, 1));

            testBtn = new Button(container, SWT.CHECK);
            testBtn.setText("Test Connection on finish");
            testBtn.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, true, false, 2, 1));

            // Load values
            if (orchestrator != null) {
                AiChat chat = orchestrator.getAiChat();
                if (chat != null) {
                    chatUrlText.setText(chat.getUrl() != null ? chat.getUrl() : "");
                    chatTokenText.setText(chat.getToken() != null ? chat.getToken() : "");
                    chatPromptText.setText(chat.getPrompt() != null ? chat.getPrompt() : "");
                    proxyUrlText.setText(chat.getProxyUrl() != null ? chat.getProxyUrl() : "");
                }
                LLM llm = orchestrator.getLlm();
                if (llm != null) {
                    llmModelText.setText(llm.getModel() != null ? llm.getModel() : "");
                    llmTempText.setText(String.valueOf(llm.getTemperature()));
                }
            }

            setControl(container);
        }

        public String getChatUrl() { return chatUrlText.getText(); }
        public String getChatToken() { return chatTokenText.getText(); }
        public String getChatPrompt() { return chatPromptText.getText(); }
        public String getProxyUrl() { return proxyUrlText.getText(); }
        public String getLlmModel() { return llmModelText.getText(); }
        public String getLlmTemperature() { return llmTempText.getText(); }
        public boolean isDownloadRequested() { return downloadBtn.getSelection(); }
        public boolean isTestRequested() { return testBtn.getSelection(); }
    }
}
