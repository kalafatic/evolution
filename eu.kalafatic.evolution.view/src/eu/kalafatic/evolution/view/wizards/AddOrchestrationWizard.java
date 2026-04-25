package eu.kalafatic.evolution.view.wizards;

import java.util.Arrays;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

import eu.kalafatic.evolution.controller.manager.ProjectModelManager;
import eu.kalafatic.evolution.model.orchestration.EvoProject;
import eu.kalafatic.evolution.model.orchestration.NeuronType;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

public class AddOrchestrationWizard extends Wizard implements INewWizard {
    private IFile targetFile;
    private Orchestrator orchestrator;
    private OrchestrationGeneralPage generalPage;
    private GitSettingsPage gitPage;
    private OllamaSettingsPage ollamaPage;
    private LLMSettingsPage llmPage;
    private MavenSettingsPage mavenPage;
    private AiChatSettingsPage aiChatPage;
    private NeuronAISettingsPage neuronAIPage;
    private AgentSettingsPage agentPage;

    public AddOrchestrationWizard() {
        setWindowTitle("Add Orchestration");
        this.orchestrator = ProjectModelManager.getInstance().createOrchestrator(null, null);
    }

    @Override
    public void init(IWorkbench workbench, IStructuredSelection selection) {
        if (selection != null && !selection.isEmpty()) {
            Object first = selection.getFirstElement();
            if (first instanceof IFile) {
                targetFile = (IFile) first;
            } else if (first instanceof IProject) {
                targetFile = ((IProject) first).getFile("evo_config.xml");
            } else if (first instanceof Orchestrator) {
                 Resource res = ((Orchestrator)first).eResource();
                 if (res != null) {
                     URI uri = res.getURI();
                     if (uri.isPlatformResource()) {
                         String path = uri.toPlatformString(true);
                         targetFile = ResourcesPlugin.getWorkspace().getRoot().getFile(new Path(path));
                     }
                 }
            }
        }
    }

    @Override
    public void addPages() {
        generalPage = new OrchestrationGeneralPage();
        gitPage = new GitSettingsPage();
        ollamaPage = new OllamaSettingsPage();
        llmPage = new LLMSettingsPage();
        mavenPage = new MavenSettingsPage();
        aiChatPage = new AiChatSettingsPage();
        neuronAIPage = new NeuronAISettingsPage();
        agentPage = new AgentSettingsPage();

        for (AWizardPage page : new AWizardPage[] { generalPage, gitPage, ollamaPage, llmPage, mavenPage, aiChatPage, neuronAIPage, agentPage }) {
            page.setOrchestrator(orchestrator);
        }

        addPage(generalPage);
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
        if (targetFile == null || !targetFile.exists()) {
            MessageDialog.openError(getShell(), "Error", "Target Evo configuration file not found. Please select an Evo project or configuration file.");
            return false;
        }

        try {
            ProjectModelManager modelManager = ProjectModelManager.getInstance();
            EvoProject evoProject = modelManager.loadProject(targetFile);

            if (evoProject == null) {
                MessageDialog.openError(getShell(), "Error", "Invalid Evo configuration file.");
                return false;
            }

            Resource resource = evoProject.eResource();

            modelManager.updateOrchestratorGeneral(orchestrator, generalPage.getOrchestrationId(), generalPage.getOrchestrationName());

            // Git Settings
            if (!gitPage.isSkipped()) {
                modelManager.updateGitSettings(orchestrator, gitPage.getRepoUrl(), gitPage.getBranch(), gitPage.getUsername(), gitPage.getLocalPath());
            }

            // Ollama Settings
            if (!ollamaPage.isSkipped()) {
                modelManager.updateOllamaSettings(orchestrator, ollamaPage.getOllamaUrl(), ollamaPage.getModelName(), ollamaPage.getExecutablePath());
            }

            // LLM Settings
            if (!llmPage.isSkipped()) {
                float temp = 1.0f;
                try { temp = Float.parseFloat(llmPage.getTemperature()); } catch (NumberFormatException e) {}
                modelManager.updateLlmSettings(orchestrator, llmPage.getLlmModel(), temp);
            }

            // Maven Settings
            if (!mavenPage.isSkipped()) {
                String goals = mavenPage.getGoals();
                java.util.List<String> goalsList = goals != null && !goals.isEmpty() ? Arrays.asList(goals.split("[,\\s]+")) : null;
                modelManager.updateMavenSettings(orchestrator, goalsList, null);
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

            evoProject.getOrchestrations().add(orchestrator);

            modelManager.saveResource(resource);
            targetFile.getProject().refreshLocal(IProject.DEPTH_INFINITE, null);

        } catch (Exception e) {
            MessageDialog.openError(getShell(), "Error", "Could not add orchestration: " + e.getMessage());
            e.printStackTrace();
            return false;
        }

        return true;
    }
}
