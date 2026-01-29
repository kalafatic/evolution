package eu.kalafatic.evolution.view.wizards;

import java.io.File;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.fieldassist.SimpleContentProposalProvider;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
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
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.wizards.newresource.BasicNewResourceWizard;
import org.json.JSONArray;
import org.json.JSONObject;

import eu.kalafatic.evolution.model.orchestration.Agent;
import eu.kalafatic.evolution.model.orchestration.AiChat;
import eu.kalafatic.evolution.model.orchestration.EvoProject;
import eu.kalafatic.evolution.model.orchestration.Git;
import eu.kalafatic.evolution.model.orchestration.LLM;
import eu.kalafatic.evolution.model.orchestration.Maven;
import eu.kalafatic.evolution.model.orchestration.NeuronAI;
import eu.kalafatic.evolution.model.orchestration.Ollama;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.EvolutionNature;

public class NewEvoProjectWizard extends Wizard implements INewWizard {
    private IWorkbench workbench;
    private WizardNewProjectCreationPage projectPage;
    private ConfigDetailsPage configPage;
    private GitSettingsPage gitPage;
    private OllamaSettingsPage ollamaPage;
    private LLMSettingsPage llmPage;
    private MavenSettingsPage mavenPage;
    private AiChatSettingsPage aiChatPage;
    private NeuronAISettingsPage neuronAIPage;
    private AgentSettingsPage agentPage;

    public NewEvoProjectWizard() {
        setWindowTitle("New Evo Project");
    }

    @Override
    public void init(IWorkbench workbench, IStructuredSelection selection) {
        this.workbench = workbench;
    }

    @Override
    public void addPages() {
        projectPage = new WizardNewProjectCreationPage("NewEvoProjectPage");
        projectPage.setTitle("Evo Project");
        projectPage.setDescription("Create a new AI Evolution Project.");

        configPage = new ConfigDetailsPage();
        gitPage = new GitSettingsPage();
        ollamaPage = new OllamaSettingsPage();
        llmPage = new LLMSettingsPage();
        mavenPage = new MavenSettingsPage();
        aiChatPage = new AiChatSettingsPage();
        neuronAIPage = new NeuronAISettingsPage();
        agentPage = new AgentSettingsPage();

        addPage(projectPage);
        addPage(configPage);
        addPage(gitPage);
        addPage(ollamaPage);
        addPage(llmPage);
        addPage(mavenPage);
        addPage(aiChatPage);
        addPage(neuronAIPage);
        addPage(agentPage);
    }

    @Override
    public boolean performFinish() {
    	IProgressMonitor monitor = new NullProgressMonitor();
        final String projectName = projectPage.getProjectName();        
        final IPath location = projectPage.getLocationPath();
        final String fileName = configPage.getFileName();

        try {
            IProject project = projectPage.getProjectHandle();
            IProjectDescription desc = ResourcesPlugin.getWorkspace().newProjectDescription(projectName);
            if (!projectPage.useDefaults()) {
                desc.setLocation(location);
            }

            // Add Evolution Nature
            String[] natures = desc.getNatureIds();
            String[] newNatures = new String[natures.length + 1];
            System.arraycopy(natures, 0, newNatures, 0, natures.length);
            newNatures[natures.length] = EvolutionNature.NATURE_ID;
            desc.setNatureIds(newNatures);

            if (!project.exists()) {
                project.create(desc, monitor);
            }
            if (!project.isOpen()) {
                project.open(monitor);
            }
            project.setDescription(desc, monitor);

            String filePath = project.getLocation().append(fileName).toOSString();
            ResourceSet resSet = new ResourceSetImpl();
            resSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("xml", new XMIResourceFactoryImpl());
            resSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("evo", new XMIResourceFactoryImpl());

            URI fileURI = URI.createFileURI(filePath);
            Resource resource = resSet.createResource(fileURI);

            OrchestrationFactory factory = OrchestrationFactory.eINSTANCE;

            EvoProject evoProject = factory.createEvoProject();
            evoProject.setName(projectName);

            Orchestrator orchestrator = factory.createOrchestrator();
            orchestrator.setName("Initial Orchestration");
            orchestrator.setId("orch1");

            // Git Settings
            if (!gitPage.isSkipped()) {
                Git git = factory.createGit();
                git.setRepositoryUrl(gitPage.getRepoUrl());
                git.setBranch(gitPage.getBranch());
                git.setUsername(gitPage.getUsername());
                git.setLocalPath(gitPage.getLocalPath());
                orchestrator.setGit(git);
            }

            // Ollama Settings
            if (!ollamaPage.isSkipped()) {
                Ollama ollama = factory.createOllama();
                ollama.setUrl(ollamaPage.getOllamaUrl());
                ollama.setModel(ollamaPage.getModelName());
                ollama.setPath(ollamaPage.getExecutablePath());
                orchestrator.setOllama(ollama);
            }

            // LLM Settings
            if (!llmPage.isSkipped()) {
                LLM llm = factory.createLLM();
                llm.setModel(llmPage.getLlmModel());
                try {
                    llm.setTemperature(Float.parseFloat(llmPage.getTemperature()));
                } catch (NumberFormatException e) {
                    llm.setTemperature(0.7f);
                }
                orchestrator.setLlm(llm);
            }

            // Maven Settings
            if (!mavenPage.isSkipped()) {
                Maven maven = factory.createMaven();
                String goals = mavenPage.getGoals();
                if (goals != null && !goals.isEmpty()) {
                    maven.getGoals().addAll(Arrays.asList(goals.split("[,\\s]+")));
                }
                orchestrator.setMaven(maven);
            }

            // AiChat Settings
            if (!aiChatPage.isSkipped()) {
                AiChat aiChat = factory.createAiChat();
                aiChat.setUrl(aiChatPage.getChatUrl());
                aiChat.setToken(aiChatPage.getToken());
                aiChat.setPrompt(aiChatPage.getPrompt());
                orchestrator.setAiChat(aiChat);
            }

            // Neuron AI Settings
            if (!neuronAIPage.isSkipped()) {
                NeuronAI neuronAI = factory.createNeuronAI();
                neuronAI.setUrl(neuronAIPage.getUrl());
                neuronAI.setModel(neuronAIPage.getModelName());
                orchestrator.setNeuronAI(neuronAI);
            }

            // Agent Settings
            if (!agentPage.isSkipped()) {
                String agentsData = agentPage.getAgentsData();
                if (agentsData != null && !agentsData.isEmpty()) {
                    String[] lines = agentsData.split("\\r?\\n");
                    for (String line : lines) {
                        String[] parts = line.split(":");
                        if (parts.length >= 2) {
                            Agent agent = factory.createAgent();
                            agent.setId(parts[0].trim());
                            agent.setType(parts[1].trim());
                            orchestrator.getAgents().add(agent);
                        }
                    }
                }
            }

            evoProject.getOrchestrations().add(orchestrator);
            resource.getContents().add(evoProject);

            resource.save(Collections.emptyMap());
            project.refreshLocal(IProject.DEPTH_INFINITE, null);

            // Automatically open project in project view and open editor with project orchestration
            IFile file = project.getFile(fileName);
            if (file.exists() && workbench != null) {
                IWorkbenchWindow dw = workbench.getActiveWorkbenchWindow();
                if (dw != null) {
                    IWorkbenchPage page = dw.getActivePage();
                    if (page != null) {
                        try {
                            // Ensure Project Explorer is visible
                            page.showView(IPageLayout.ID_PROJECT_EXPLORER);
                            BasicNewResourceWizard.selectAndReveal(file, dw);
                            // Open created file with the specific MultiPageEditor ID
                            IDE.openEditor(page, file, "eu.kalafatic.evolution.view.editors.MultiPageEditor", true);
                        } catch (PartInitException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

        } catch (Exception e) {
            MessageDialog.openError(getShell(), "Error", "Could not create project: " + e.getMessage());
            e.printStackTrace();
            return false;
        }

        return true;
    }

    private class ConfigDetailsPage extends WizardPage {
        private Text fileNameText;

        protected ConfigDetailsPage() {
            super("ConfigDetailsPage");
            setTitle("Configuration Details");
            setDescription("Enter the configuration file name.");
        }

        @Override
        public void createControl(Composite parent) {
            Composite container = new Composite(parent, SWT.NONE);
            container.setLayout(new GridLayout(2, false));

            new Label(container, SWT.NONE).setText("Config File Name (.xml):");
            fileNameText = new Text(container, SWT.BORDER);
            fileNameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            fileNameText.setText("evo_config.xml");

            setControl(container);
        }

        public String getFileName() { return fileNameText.getText(); }
    }

    private class GitSettingsPage extends WizardPage {
        private Text repoUrlText, branchText, usernameText, localPathText;
        private Button skipCheck;
        private ControlDecoration gitDecorator;
        private Job validationJob;

        protected GitSettingsPage() {
            super("GitSettingsPage");
            setTitle("Git Settings");
            setDescription("Configure Git repository settings.");
        }

        @Override
        public void createControl(Composite parent) {
            Composite container = new Composite(parent, SWT.NONE);
            container.setLayout(new GridLayout(2, false));

            new Label(container, SWT.NONE).setText("Repository URL:");
            repoUrlText = new Text(container, SWT.BORDER);
            repoUrlText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            repoUrlText.setText("https://github.com/kalafatic/evo.git");

            gitDecorator = new ControlDecoration(repoUrlText, SWT.TOP | SWT.LEFT);
            gitDecorator.setImage(FieldDecorationRegistry.getDefault()
                    .getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
            gitDecorator.hide();

            repoUrlText.addModifyListener(e -> validateGit());

            Link gitHelpLink = new Link(container, SWT.NONE);
            gitHelpLink.setText("<a>How to install Git?</a>");
            gitHelpLink.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false, 2, 1));
            gitHelpLink.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    openUrl("https://git-scm.com/downloads");
                }
            });

            new Label(container, SWT.NONE).setText("Branch:");
            branchText = new Text(container, SWT.BORDER);
            branchText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            branchText.setText("master");

            new Label(container, SWT.NONE).setText("Username:");
            usernameText = new Text(container, SWT.BORDER);
            usernameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            usernameText.setText("admin");

            new Label(container, SWT.NONE).setText("Local Path:");
            localPathText = new Text(container, SWT.BORDER);
            localPathText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            localPathText.setText("./repo");

            skipCheck = new Button(container, SWT.CHECK);
            skipCheck.setText("Skip this step and setup later");
            skipCheck.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, true, false, 2, 1));
            skipCheck.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    validateGit();
                }
            });

            setControl(container);
            validateGit();
        }

        private void validateGit() {
            if (skipCheck.getSelection()) {
                if (validationJob != null) validationJob.cancel();
                gitDecorator.hide();
                setPageComplete(true);
                setErrorMessage(null);
                return;
            }
            if (validationJob != null) validationJob.cancel();
            validationJob = new Job("Validate Git") {
                @Override
                protected IStatus run(IProgressMonitor monitor) {
                    boolean success = false;
                    try {
                        Process process = new ProcessBuilder("git", "--version").start();
                        success = (process.waitFor() == 0);
                    } catch (Exception e) {}

                    final boolean finalSuccess = success;
                    Display.getDefault().asyncExec(() -> {
                        if (finalSuccess) {
                            gitDecorator.hide();
                            setPageComplete(true);
                            setErrorMessage(null);
                        } else {
                            showGitError();
                        }
                    });
                    return Status.OK_STATUS;
                }
            };
            validationJob.setSystem(true);
            validationJob.schedule(500);
        }

        private void showGitError() {
            gitDecorator.setDescriptionText("Git is not installed or not in PATH.");
            gitDecorator.show();
            setPageComplete(false);
            setErrorMessage("Git is required to clone the repository.");
        }

        public String getRepoUrl() { return repoUrlText.getText(); }
        public String getBranch() { return branchText.getText(); }
        public String getUsername() { return usernameText.getText(); }
        public String getLocalPath() { return localPathText.getText(); }
        public boolean isSkipped() { return skipCheck.getSelection(); }
    }

    private class OllamaSettingsPage extends WizardPage {
        private Text urlText, modelText, pathText;
        private Button skipCheck;
        private ControlDecoration pathDecorator, modelDecorator;
        private SimpleContentProposalProvider proposalProvider;
        private Job validationJob;

        protected OllamaSettingsPage() {
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

        public String getOllamaUrl() { return urlText.getText(); }
        public String getModelName() { return modelText.getText(); }
        public String getExecutablePath() { return pathText.getText(); }
        public boolean isSkipped() { return skipCheck.getSelection(); }
    }

    private class LLMSettingsPage extends WizardPage {
        private Text modelText, tempText;
        private Button skipCheck;

        protected LLMSettingsPage() {
            super("LLMSettingsPage");
            setTitle("LLM Settings");
            setDescription("Configure LLM model and parameters.");
        }

        @Override
        public void createControl(Composite parent) {
            Composite container = new Composite(parent, SWT.NONE);
            container.setLayout(new GridLayout(2, false));

            new Label(container, SWT.NONE).setText("LLM Model:");
            modelText = new Text(container, SWT.BORDER);
            modelText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            modelText.setText("gpt-4o");

            new Label(container, SWT.NONE).setText("Temperature:");
            tempText = new Text(container, SWT.BORDER);
            tempText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            tempText.setText("0.7");

            Link setupLink = new Link(container, SWT.NONE);
            setupLink.setText("<a>Setup LLM...</a>");
            setupLink.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false, 2, 1));
            setupLink.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    OrchestrationFactory factory = OrchestrationFactory.eINSTANCE;
                    Orchestrator tempOrch = factory.createOrchestrator();
                    tempOrch.setLlm(factory.createLLM());
                    tempOrch.setAiChat(factory.createAiChat());

                    SetupLLMWizard wizard = new SetupLLMWizard(tempOrch);
                    WizardDialog dialog = new WizardDialog(getShell(), wizard);
                    if (dialog.open() == WizardDialog.OK) {
                        modelText.setText(tempOrch.getLlm().getModel());
                        tempText.setText(String.valueOf(tempOrch.getLlm().getTemperature()));
                    }
                }
            });

            skipCheck = new Button(container, SWT.CHECK);
            skipCheck.setText("Skip this step and setup later");
            skipCheck.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, true, false, 2, 1));

            setControl(container);
        }

        public String getLlmModel() { return modelText.getText(); }
        public String getTemperature() { return tempText.getText(); }
        public boolean isSkipped() { return skipCheck.getSelection(); }
    }

    private class MavenSettingsPage extends WizardPage {
        private Text goalsText;
        private Button skipCheck;
        private ControlDecoration mavenDecorator;
        private Job validationJob;

        protected MavenSettingsPage() {
            super("MavenSettingsPage");
            setTitle("Maven Settings");
            setDescription("Configure Maven build goals.");
        }

        @Override
        public void createControl(Composite parent) {
            Composite container = new Composite(parent, SWT.NONE);
            container.setLayout(new GridLayout(2, false));

            new Label(container, SWT.NONE).setText("Goals (comma separated):");
            goalsText = new Text(container, SWT.BORDER);
            goalsText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            goalsText.setText("clean, install");

            mavenDecorator = new ControlDecoration(goalsText, SWT.TOP | SWT.LEFT);
            mavenDecorator.setImage(FieldDecorationRegistry.getDefault()
                    .getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
            mavenDecorator.hide();

            goalsText.addModifyListener(e -> validateMaven());

            skipCheck = new Button(container, SWT.CHECK);
            skipCheck.setText("Skip this step and setup later");
            skipCheck.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, true, false, 2, 1));
            skipCheck.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    validateMaven();
                }
            });

            Link mavenHelpLink = new Link(container, SWT.NONE);
            mavenHelpLink.setText("<a>How to install Maven?</a>");
            mavenHelpLink.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false, 2, 1));
            mavenHelpLink.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    openUrl("https://maven.apache.org/download.cgi");
                }
            });

            setControl(container);
            validateMaven();
        }

        private void validateMaven() {
            if (skipCheck.getSelection()) {
                if (validationJob != null) validationJob.cancel();
                mavenDecorator.hide();
                setPageComplete(true);
                setErrorMessage(null);
                return;
            }
            if (validationJob != null) validationJob.cancel();
            validationJob = new Job("Validate Maven") {
                @Override
                protected IStatus run(IProgressMonitor monitor) {
                    boolean success = false;
                    try {
                        String cmd = System.getProperty("os.name").toLowerCase().contains("win") ? "mvn.cmd" : "mvn";
                        Process process = new ProcessBuilder(cmd, "-v").start();
                        success = (process.waitFor() == 0);
                    } catch (Exception e) {}

                    final boolean finalSuccess = success;
                    Display.getDefault().asyncExec(() -> {
                        if (finalSuccess) {
                            mavenDecorator.hide();
                            setPageComplete(true);
                            setErrorMessage(null);
                        } else {
                            showMavenError();
                        }
                    });
                    return Status.OK_STATUS;
                }
            };
            validationJob.setSystem(true);
            validationJob.schedule(500);
        }

        private void showMavenError() {
            mavenDecorator.setDescriptionText("Maven is not installed or not in PATH.");
            mavenDecorator.show();
            setPageComplete(false);
            setErrorMessage("Maven is required to build the project.");
        }

        public String getGoals() { return goalsText.getText(); }
        public boolean isSkipped() { return skipCheck.getSelection(); }
    }

    private void openUrl(String url) {
        try {
            PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL(url));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class AiChatSettingsPage extends WizardPage {
        private Text urlText, tokenText, promptText;
        private Button skipCheck;

        protected AiChatSettingsPage() {
            super("AiChatSettingsPage");
            setTitle("AI Chat Settings");
            setDescription("Configure AI Chat service settings.");
        }

        @Override
        public void createControl(Composite parent) {
            Composite container = new Composite(parent, SWT.NONE);
            container.setLayout(new GridLayout(2, false));

            new Label(container, SWT.NONE).setText("Chat URL:");
            urlText = new Text(container, SWT.BORDER);
            urlText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            urlText.setText("http://localhost:58080/ai");

            new Label(container, SWT.NONE).setText("Token:");
            tokenText = new Text(container, SWT.BORDER | SWT.PASSWORD);
            tokenText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            tokenText.setText("ENTER_TOKEN_HERE");

            new Label(container, SWT.NONE).setText("Initial Prompt:");
            promptText = new Text(container, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL);
            GridData gd = new GridData(GridData.FILL_BOTH);
            gd.heightHint = 60;
            promptText.setLayoutData(gd);
            promptText.setText("You are a helpful assistant.");

            skipCheck = new Button(container, SWT.CHECK);
            skipCheck.setText("Skip this step and setup later");
            skipCheck.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, true, false, 2, 1));

            setControl(container);
        }

        public String getChatUrl() { return urlText.getText(); }
        public String getToken() { return tokenText.getText(); }
        public String getPrompt() { return promptText.getText(); }
        public boolean isSkipped() { return skipCheck.getSelection(); }
    }
}
