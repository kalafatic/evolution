package eu.kalafatic.evolution.view.wizards;

import java.io.ByteArrayInputStream;
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
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.wizards.newresource.BasicNewResourceWizard;

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
import eu.kalafatic.evolution.view.nature.EvolutionNature;

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
                String profiles = mavenPage.getProfiles();
                if (profiles != null && !profiles.isEmpty()) {
                    maven.getProfiles().addAll(Arrays.asList(profiles.split("[,\\s]+")));
                }
                orchestrator.setMaven(maven);
            }

            // AiChat Settings
            if (!aiChatPage.isSkipped()) {
                AiChat aiChat = factory.createAiChat();
                aiChat.setUrl(aiChatPage.getChatUrl());
                aiChat.setToken(aiChatPage.getToken());
                aiChat.setPrompt(aiChatPage.getPrompt());
                aiChat.setProxyUrl(aiChatPage.getProxyUrl());
                orchestrator.setAiChat(aiChat);
            }

            // Neuron AI Settings
            if (!neuronAIPage.isSkipped()) {
                NeuronAI neuronAI = factory.createNeuronAI();
                neuronAI.setUrl(neuronAIPage.getUrl());
                neuronAI.setModel(neuronAIPage.getModelName());
                neuronAI.setType(neuronAIPage.getModelType());
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
