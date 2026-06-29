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
import org.eclipse.emf.ecore.resource.Resource;
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

import eu.kalafatic.evolution.controller.manager.ProjectModelManager;
import eu.kalafatic.evolution.model.orchestration.EvoProject;
import eu.kalafatic.evolution.model.orchestration.NeuronType;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.nature.EvolutionNature;

public class NewEvoProjectWizard extends Wizard implements INewWizard {
    private static final String GITIGNORE_TEMPLATE = "target/\n" +
            ".settings/\n" +
            ".project\n" +
            ".classpath\n" +
            "bin/\n" +
            "*.class\n" +
            "*.evo\n" +
            "*.log\n";

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
    private SupervisorSettingsPage supervisorPage;

    public NewEvoProjectWizard() {
        setWindowTitle("New Evo Project");
        this.orchestrator = ProjectModelManager.getInstance().createOrchestrator("orch1", "Initial Orchestration");

        // Use discovery to set initial project name if possible
        eu.kalafatic.evolution.controller.discovery.SourceDiscoveryResult discovery =
            ProjectModelManager.getInstance().getOrDiscoverWorkspace();
        if (discovery.getPrimaryRepository() != null) {
            String name = discovery.getPrimaryRepository().getName();
            if (name != null && !name.isEmpty()) {
                orchestrator.setName(name + " Orchestration");
            }
        }
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
        supervisorPage = new SupervisorSettingsPage();

        for (AWizardPage page : new AWizardPage[] { configPage, gitPage, ollamaPage, llmPage, mavenPage, aiChatPage, neuronAIPage, agentPage, supervisorPage }) {
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
        addPage(supervisorPage);
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
                        java.nio.file.Files.write(gitignore.toPath(), GITIGNORE_TEMPLATE.getBytes());
                    }

                    registerGitRepository(projectDir);
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
            ProjectModelManager modelManager = ProjectModelManager.getInstance();
            Resource resource = modelManager.createResource(filePath);
            
            EvoProject evoProject = modelManager.createProject(projectName);

            // General Settings
            modelManager.updateDefaultTarget(orchestrator, configPage.getDefaultTargetPath());

            // Git Settings
            if (!gitPage.isSkipped()) {
                modelManager.updateGitSettings(orchestrator, gitPage.getRepoUrl(), gitPage.getBranch(), gitPage.getUsername(), gitPage.getPassword(), gitPage.getLocalPath());

                String localPathStr = gitPage.getLocalPath();
                if (localPathStr != null && !localPathStr.isEmpty()) {
                    registerGitRepository(new File(localPathStr));
                }
            }

            // Ollama Settings
            if (!ollamaPage.isSkipped()) {
                String modelName = ollamaPage.getModelName();
                modelManager.updateOllamaSettings(orchestrator, ollamaPage.getOllamaUrl(), modelName, ollamaPage.getExecutablePath());
                modelManager.updateLocalModel(orchestrator, modelName);
                modelManager.updateHybridModel(orchestrator, modelName);
            } else {
                modelManager.updateOllamaSettings(orchestrator, "http://localhost:11434", "llama3.2:3b", null);
                modelManager.updateLocalModel(orchestrator, "llama3.2:3b");
                modelManager.updateHybridModel(orchestrator, "llama3.2:3b");
            }

            // LLM Settings
            if (!llmPage.isSkipped()) {
                float temp = 0.4f;
                try { temp = Float.parseFloat(llmPage.getTemperature()); } catch (NumberFormatException e) {}
                modelManager.updateLlmSettings(orchestrator, llmPage.getLlmModel(), temp);
                modelManager.updateRemoteModel(orchestrator, llmPage.getLlmModel());
            } else {
                modelManager.updateLlmSettings(orchestrator, "gpt-4o", 0.4f);
                modelManager.updateRemoteModel(orchestrator, "gpt-4o");
            }

            // Maven Settings
            if (!mavenPage.isSkipped()) {
                String goals = mavenPage.getGoals();
                java.util.List<String> goalsList = goals != null && !goals.isEmpty() ? Arrays.asList(goals.split("[,\\s]+")) : null;
                String profiles = mavenPage.getProfiles();
                java.util.List<String> profilesList = profiles != null && !profiles.isEmpty() ? Arrays.asList(profiles.split("[,\\s]+")) : null;
                modelManager.updateMavenSettings(orchestrator, goalsList, profilesList);
            }

            // AiChat Settings
            if (!aiChatPage.isSkipped()) {
                modelManager.updateAiChatSettings(orchestrator, aiChatPage.getChatUrl(), aiChatPage.getToken(), aiChatPage.getPrompt(), aiChatPage.getProxyUrl());
            }

            // Neuron AI Settings
            if (!neuronAIPage.isSkipped()) {
                modelManager.updateNeuronAISettings(orchestrator, neuronAIPage.getUrl(), neuronAIPage.getModelName(), neuronAIPage.getModelType());
            }

            // Agent Settings
            if (!agentPage.isSkipped()) {
                for (AgentSettingsPage.AgentEntry entry : agentPage.getSelectedAgents()) {
                    modelManager.addAgent(orchestrator, entry.id, entry.type);
                }
            }

            // Supervisor Settings
            if (!supervisorPage.isSkipped()) {
                modelManager.updateSupervisorSettings(orchestrator, supervisorPage.getExecutablePath(), supervisorPage.getSourcePath(), supervisorPage.getCommands(), supervisorPage.getSettings(), supervisorPage.isDeployed());
            }

            modelManager.setFileConfig(orchestrator, project.getLocation().append("resources").toOSString());

            evoProject.getOrchestrations().add(orchestrator);
            resource.getContents().add(evoProject);
                       
            modelManager.saveResource(resource);
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
                                        ((eu.kalafatic.evolution.view.views.EvoNavigator) view).refreshAndExpand(project);
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

    private void registerGitRepository(File repoDir) {
        if (repoDir == null || !repoDir.exists()) return;

        File gitDir = new File(repoDir, ".git");
        if (gitDir.exists() && gitDir.isDirectory()) {
            try {
                // Try to register via EGit RepositoryUtil using reflection to avoid direct dependency issues
                Class<?> utilClass = null;
                try {
                    utilClass = Class.forName("org.eclipse.egit.core.RepositoryUtil");
                } catch (ClassNotFoundException e) {
                    try {
                        utilClass = Class.forName("org.eclipse.egit.core.internal.util.RepositoryUtil");
                    } catch (ClassNotFoundException e2) {
                        // Ignore
                    }
                }

                if (utilClass != null) {
                    Object repoUtil = null;
                    try {
                        // Try static getInstance()
                        repoUtil = utilClass.getMethod("getInstance").invoke(null);
                    } catch (Exception e) {
                        // Try Activator.getDefault().getRepositoryUtil()
                        try {
                            Class<?> activatorClass = Class.forName("org.eclipse.egit.core.Activator");
                            Object activator = activatorClass.getMethod("getDefault").invoke(null);
                            repoUtil = activator.getClass().getMethod("getRepositoryUtil").invoke(activator);
                        } catch (Exception e2) {
                             // Try internal Activator
                            try {
                                Class<?> activatorClass = Class.forName("org.eclipse.egit.core.internal.Activator");
                                Object activator = activatorClass.getMethod("getDefault").invoke(null);
                                repoUtil = activator.getClass().getMethod("getRepositoryUtil").invoke(activator);
                            } catch (Exception e3) {
                                // Try UI Activator
                                try {
                                    Class<?> activatorClass = Class.forName("org.eclipse.egit.ui.Activator");
                                    Object activator = activatorClass.getMethod("getDefault").invoke(null);
                                    repoUtil = activator.getClass().getMethod("getRepositoryUtil").invoke(activator);
                                } catch (Exception e4) {
                                    // Ignore
                                }
                            }
                        }
                    }

                    if (repoUtil != null) {
                        repoUtil.getClass().getMethod("addConfiguredRepository", File.class).invoke(repoUtil, gitDir);
                        System.out.println("Successfully registered Git repository with EGit: " + gitDir.getAbsolutePath());
                    }
                }
            } catch (Exception e) {
                System.err.println("Failed to register Git repository with EGit: " + e.getMessage());
            }
        }
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
