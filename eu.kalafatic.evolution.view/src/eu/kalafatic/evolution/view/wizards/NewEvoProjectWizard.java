package eu.kalafatic.evolution.view.wizards;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.ui.IPageLayout;
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
        agentPage = new AgentSettingsPage();

        addPage(projectPage);
        addPage(configPage);
        addPage(gitPage);
        addPage(ollamaPage);
        addPage(llmPage);
        addPage(mavenPage);
        addPage(aiChatPage);
        addPage(agentPage);
    }

    @Override
    public boolean performFinish() {
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
                project.create(desc, null);
            }
            if (!project.isOpen()) {
                project.open(null);
            }
            project.setDescription(desc, null);

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

            // Agent Settings
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

}
