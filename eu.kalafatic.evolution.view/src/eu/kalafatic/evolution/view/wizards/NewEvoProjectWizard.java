package eu.kalafatic.evolution.view.wizards;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.wizards.newresource.BasicNewResourceWizard;

import eu.kalafatic.evolution.model.orchestration.Agent;
import eu.kalafatic.evolution.model.orchestration.AiChat;
import eu.kalafatic.evolution.model.orchestration.FileConfig;
import eu.kalafatic.evolution.model.orchestration.EvoProject;
import eu.kalafatic.evolution.model.orchestration.Git;
import eu.kalafatic.evolution.model.orchestration.LLM;
import eu.kalafatic.evolution.model.orchestration.Maven;
import eu.kalafatic.evolution.model.orchestration.NeuronAI;
import eu.kalafatic.evolution.model.orchestration.Ollama;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.nature.EvolutionNature;

public class NewEvoProjectWizard extends Wizard implements INewWizard {
    private IWorkbench workbench;
    private Orchestrator orchestrator;
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
        this.orchestrator = OrchestrationFactory.eINSTANCE.createOrchestrator();
        this.orchestrator.setName("Initial Orchestration");
        this.orchestrator.setId("orch1");
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

        for (AWizardPage page : new AWizardPage[] { configPage, gitPage, ollamaPage, llmPage, mavenPage, aiChatPage, neuronAIPage, agentPage }) {
            if (page != null) {
                page.setOrchestrator(orchestrator);
            }
        }

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

            // Add Evolution, Java and Maven Natures
            String[] natures = desc.getNatureIds();
            String[] newNatures = new String[natures.length + 3];
            System.arraycopy(natures, 0, newNatures, 0, natures.length);
            newNatures[natures.length] = EvolutionNature.NATURE_ID;
            newNatures[natures.length + 1] = "org.eclipse.jdt.core.javanature";
            newNatures[natures.length + 2] = "org.eclipse.m2e.core.maven2Nature";
            desc.setNatureIds(newNatures);

            if (!project.exists()) {
                project.create(desc, monitor);
            }
            if (!project.isOpen()) {
                project.open(monitor);
            }
            project.setDescription(desc, monitor);

            // Create default structure
            createFolder(project, "resources/download", monitor);
            createFolder(project, "resources/lib", monitor);
            createFolder(project, "resources/models", monitor);
            createFolder(project, "git", monitor);
            createFolder(project, "mvn", monitor);

            // Initialize local Git repository if no remote is provided
            if (gitPage.isSkipped() || gitPage.getRepoUrl() == null || gitPage.getRepoUrl().isEmpty()) {
                try {
                    File projectDir = project.getLocation().toFile();
                    new ProcessBuilder("git", "init").directory(projectDir).start().waitFor();

                    File gitignore = new File(projectDir, ".gitignore");
                    if (!gitignore.exists()) {
                        java.nio.file.Files.write(gitignore.toPath(),
                            ("target/\n" +
                             ".settings/\n" +
                             ".project\n" +
                             ".classpath\n" +
                             "bin/\n" +
                             "*.class\n" +
                             "*.evo\n" +
                             "*.log\n").getBytes());
                    }
                } catch (Exception e) {
                    System.err.println("Failed to initialize local git: " + e.getMessage());
                }
            }

            // Create basic pom.xml
            IFile pomFile = project.getFile("pom.xml");
            if (!pomFile.exists()) {
                String pomContent = "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" +
                                   "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                                   "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" +
                                   "    <modelVersion>4.0.0</modelVersion>\n" +
                                   "    <groupId>" + projectName + "</groupId>\n" +
                                   "    <artifactId>" + projectName + "</artifactId>\n" +
                                   "    <version>0.0.1-SNAPSHOT</version>\n" +
                                   "</project>";
                InputStream source = new ByteArrayInputStream(pomContent.getBytes());
                pomFile.create(source, true, monitor);
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

            // Git Settings
            if (!gitPage.isSkipped()) {
                Git git = orchestrator.getGit();
                if (git == null) {
                    git = factory.createGit();
                    orchestrator.setGit(git);
                }
                git.setRepositoryUrl(gitPage.getRepoUrl());
                git.setBranch(gitPage.getBranch());
                git.setUsername(gitPage.getUsername());
                git.setLocalPath(gitPage.getLocalPath());
            }

            // Ollama Settings
            Ollama ollama = orchestrator.getOllama();
            if (ollama == null) {
                ollama = factory.createOllama();
                orchestrator.setOllama(ollama);
            }
            if (!ollamaPage.isSkipped()) {
                ollama.setUrl(ollamaPage.getOllamaUrl());
                ollama.setModel(ollamaPage.getModelName());
                ollama.setPath(ollamaPage.getExecutablePath());
            } else {
                if (ollama.getUrl() == null || ollama.getUrl().isEmpty()) ollama.setUrl("http://localhost:11434");
                if (ollama.getModel() == null || ollama.getModel().isEmpty()) ollama.setModel("llama3.2:3b");
            }

            // LLM Settings
            LLM llm = orchestrator.getLlm();
            if (llm == null) {
                llm = factory.createLLM();
                orchestrator.setLlm(llm);
            }
            if (!llmPage.isSkipped()) {
                llm.setModel(llmPage.getLlmModel());
                try {
                    llm.setTemperature(Float.parseFloat(llmPage.getTemperature()));
                } catch (NumberFormatException e) {
                    llm.setTemperature(1.0f);
                }
            } else {
                if (llm.getModel() == null || llm.getModel().isEmpty()) llm.setModel("gpt-4o");
            }

            // Maven Settings
            if (!mavenPage.isSkipped()) {
                Maven maven = orchestrator.getMaven();
                if (maven == null) {
                    maven = factory.createMaven();
                    orchestrator.setMaven(maven);
                }
                String goals = mavenPage.getGoals();
                if (goals != null && !goals.isEmpty()) {
                    maven.getGoals().clear();
                    maven.getGoals().addAll(Arrays.asList(goals.split("[,\\s]+")));
                }
                String profiles = mavenPage.getProfiles();
                if (profiles != null && !profiles.isEmpty()) {
                    maven.getProfiles().clear();
                    maven.getProfiles().addAll(Arrays.asList(profiles.split("[,\\s]+")));
                }
            }

            // AiChat Settings
            if (!aiChatPage.isSkipped()) {
                AiChat aiChat = orchestrator.getAiChat();
                if (aiChat == null) {
                    aiChat = factory.createAiChat();
                    orchestrator.setAiChat(aiChat);
                }
                aiChat.setUrl(aiChatPage.getChatUrl());
                aiChat.setToken(aiChatPage.getToken());
                aiChat.setPrompt(aiChatPage.getPrompt());
                aiChat.setProxyUrl(aiChatPage.getProxyUrl());
            }

            // Neuron AI Settings
            if (!neuronAIPage.isSkipped()) {
                NeuronAI neuronAI = orchestrator.getNeuronAI();
                if (neuronAI == null) {
                    neuronAI = factory.createNeuronAI();
                    orchestrator.setNeuronAI(neuronAI);
                }
                neuronAI.setUrl(neuronAIPage.getUrl());
                neuronAI.setModel(neuronAIPage.getModelName());
                neuronAI.setType(neuronAIPage.getModelType());
            }

            // Agent Settings
            if (!agentPage.isSkipped()) {
                for (AgentSettingsPage.AgentEntry entry : agentPage.getSelectedAgents()) {
                    Agent agent = factory.createAgent();
                    agent.setId(entry.id);
                    agent.setType(entry.type);
                    orchestrator.getAgents().add(agent);
                }
            }

            FileConfig fileConfig = factory.createFileConfig();
            fileConfig.setLocalPath(project.getLocation().append("resources").toOSString());
            orchestrator.setFileConfig(fileConfig);

            evoProject.getOrchestrations().add(orchestrator);
            resource.getContents().add(evoProject);
                       
            resource.save(Collections.emptyMap());
            project.refreshLocal(IProject.DEPTH_INFINITE, null);

            
            IFile file = project.getFile(fileName);
   		 // Automatically open project in project view and open editor with project orchestration 
            
//            new ProjectManager().openProject(workbench, project, fileName);            
            
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
            
            Job job = new Job("Background Refresh") {
                @Override
                protected IStatus run(IProgressMonitor monitor) {
                    try {
                    	project.open(new NullProgressMonitor());
                    	
                        // Refresh everything asynchronously
                        ResourcesPlugin.getWorkspace().getRoot()
                            .refreshLocal(IResource.DEPTH_INFINITE, monitor);
                        
                        // Explicitly refresh Evo Navigator
                        Display.getDefault().asyncExec(() -> {
                            IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
                            if (page != null) {
                                try {
                                    IViewPart view = page.showView("eu.kalafatic.views.EvoNavigator");
                                    if (view instanceof eu.kalafatic.evolution.view.views.EvoNavigator) {
                                        ((eu.kalafatic.evolution.view.views.EvoNavigator) view).refresh();
                                    }
                                } catch (PartInitException e) {
                                    e.printStackTrace();
                                }
                            }
                        });

                        return Status.OK_STATUS;
                    } catch (CoreException e) {
                        return e.getStatus();
                    }
                }
            };

            // Priority DECORATE tells Eclipse this is a UI-update priority job
            job.setPriority(Job.DECORATE);
            job.schedule();

        } catch (Exception e) {
            MessageDialog.openError(getShell(), "Error", "Could not create project: " + e.getMessage());
            e.printStackTrace();
            return false;
        }

        return true;
    }

    private void createFolder(IContainer container, String path, IProgressMonitor monitor) throws CoreException {
        IPath folderPath = new Path(path);
        for (int i = 1; i <= folderPath.segmentCount(); i++) {
            IFolder folder = container.getFolder(folderPath.uptoSegment(i));
            if (!folder.exists()) {
                folder.create(true, true, monitor);
            }
        }
    }
}
