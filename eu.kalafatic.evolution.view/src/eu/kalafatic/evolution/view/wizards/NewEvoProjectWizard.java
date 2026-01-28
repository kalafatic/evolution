package eu.kalafatic.evolution.view.wizards;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.wizards.newresource.BasicNewResourceWizard;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import eu.kalafatic.evolution.model.orchestration.*;
import java.util.Collections;
import java.util.Arrays;
import org.eclipse.swt.layout.GridData;

public class NewEvoProjectWizard extends Wizard implements INewWizard {
    private IWorkbench workbench;
    private NewEvoProjectPage projectPage;
    private GitSettingsPage gitPage;
    private OllamaSettingsPage ollamaPage;
    private LLMSettingsPage llmPage;
    private MavenSettingsPage mavenPage;
    private AiChatSettingsPage aiChatPage;

    public NewEvoProjectWizard() {
        setWindowTitle("New Evo Project");
    }

    @Override
    public void init(IWorkbench workbench, IStructuredSelection selection) {
        this.workbench = workbench;
    }

    @Override
    public void addPages() {
        projectPage = new NewEvoProjectPage();
        gitPage = new GitSettingsPage();
        ollamaPage = new OllamaSettingsPage();
        llmPage = new LLMSettingsPage();
        mavenPage = new MavenSettingsPage();
        aiChatPage = new AiChatSettingsPage();

        addPage(projectPage);
        addPage(gitPage);
        addPage(ollamaPage);
        addPage(llmPage);
        addPage(mavenPage);
        addPage(aiChatPage);
    }

    @Override
    public boolean performFinish() {
        final String projectName = projectPage.getProjectName();
        final String fileName = projectPage.getFileName();

        try {
            IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
            if (!project.exists()) {
                project.create(null);
            }
            if (!project.isOpen()) {
                project.open(null);
            }

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
            Git git = factory.createGit();
            git.setRepositoryUrl(gitPage.getRepoUrl());
            git.setBranch(gitPage.getBranch());
            git.setUsername(gitPage.getUsername());
            git.setLocalPath(gitPage.getLocalPath());
            orchestrator.setGit(git);

            // Ollama Settings
            Ollama ollama = factory.createOllama();
            ollama.setUrl(ollamaPage.getOllamaUrl());
            ollama.setModel(ollamaPage.getModelName());
            ollama.setPath(ollamaPage.getExecutablePath());
            orchestrator.setOllama(ollama);

            // LLM Settings
            LLM llm = factory.createLLM();
            llm.setModel(llmPage.getLlmModel());
            try {
                llm.setTemperature(Float.parseFloat(llmPage.getTemperature()));
            } catch (NumberFormatException e) {
                llm.setTemperature(0.7f);
            }
            orchestrator.setLlm(llm);

            // Maven Settings
            Maven maven = factory.createMaven();
            String goals = mavenPage.getGoals();
            if (goals != null && !goals.isEmpty()) {
                maven.getGoals().addAll(Arrays.asList(goals.split("[,\\s]+")));
            }
            orchestrator.setMaven(maven);

            // AiChat Settings
            AiChat aiChat = factory.createAiChat();
            aiChat.setUrl(aiChatPage.getChatUrl());
            aiChat.setToken(aiChatPage.getToken());
            aiChat.setPrompt(aiChatPage.getPrompt());
            orchestrator.setAiChat(aiChat);

            evoProject.getOrchestrations().add(orchestrator);
            resource.getContents().add(evoProject);

            resource.save(Collections.emptyMap());
            project.refreshLocal(IProject.DEPTH_INFINITE, null);

            // Automatically open project in project view and open editor with project orchestration
            IFile file = project.getFile(fileName);
            if (file.exists() && workbench != null) {
                IWorkbenchWindow dw = workbench.getActiveWorkbenchWindow();
                if (dw != null) {
                    BasicNewResourceWizard.selectAndReveal(file, dw);
                    IWorkbenchPage page = dw.getActivePage();
                    if (page != null) {
                        try {
                            IDE.openEditor(page, file, true);
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

    private class NewEvoProjectPage extends WizardPage {
        private Text projectNameText;
        private Text fileNameText;

        protected NewEvoProjectPage() {
            super("NewEvoProjectPage");
            setTitle("Evo Project Details");
            setDescription("Enter the name for your new Evo project and the configuration file name.");
        }

        @Override
        public void createControl(Composite parent) {
            Composite container = new Composite(parent, SWT.NONE);
            container.setLayout(new GridLayout(2, false));

            new Label(container, SWT.NONE).setText("Project Name:");
            projectNameText = new Text(container, SWT.BORDER);
            projectNameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            projectNameText.setText("MyEvoProject");

            new Label(container, SWT.NONE).setText("Config File Name (.xml):");
            fileNameText = new Text(container, SWT.BORDER);
            fileNameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            fileNameText.setText("evo_config.xml");

            setControl(container);
        }

        public String getProjectName() { return projectNameText.getText(); }
        public String getFileName() { return fileNameText.getText(); }
    }

    private class GitSettingsPage extends WizardPage {
        private Text repoUrlText, branchText, usernameText, localPathText;

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

            setControl(container);
        }

        public String getRepoUrl() { return repoUrlText.getText(); }
        public String getBranch() { return branchText.getText(); }
        public String getUsername() { return usernameText.getText(); }
        public String getLocalPath() { return localPathText.getText(); }
    }

    private class OllamaSettingsPage extends WizardPage {
        private Text urlText, modelText, pathText;

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
            pathText.setText("/usr/bin/ollama");

            setControl(container);
        }

        public String getOllamaUrl() { return urlText.getText(); }
        public String getModelName() { return modelText.getText(); }
        public String getExecutablePath() { return pathText.getText(); }
    }

    private class LLMSettingsPage extends WizardPage {
        private Text modelText, tempText;

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

            setControl(container);
        }

        public String getLlmModel() { return modelText.getText(); }
        public String getTemperature() { return tempText.getText(); }
    }

    private class MavenSettingsPage extends WizardPage {
        private Text goalsText;

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

            setControl(container);
        }

        public String getGoals() { return goalsText.getText(); }
    }

    private class AiChatSettingsPage extends WizardPage {
        private Text urlText, tokenText, promptText;

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

            setControl(container);
        }

        public String getChatUrl() { return urlText.getText(); }
        public String getToken() { return tokenText.getText(); }
        public String getPrompt() { return promptText.getText(); }
    }
}
