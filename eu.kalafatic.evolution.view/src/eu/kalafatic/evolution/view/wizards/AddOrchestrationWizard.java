package eu.kalafatic.evolution.view.wizards;

import java.util.Arrays;
import java.util.Collections;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

import eu.kalafatic.evolution.controller.parsers.RuleParser;
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
        this.orchestrator = OrchestrationFactory.eINSTANCE.createOrchestrator();
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
            ResourceSet resSet = new ResourceSetImpl();
            resSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("xml", new XMIResourceFactoryImpl());
            resSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("evo", new XMIResourceFactoryImpl());

            URI fileURI = URI.createPlatformResourceURI(targetFile.getFullPath().toString(), true);
            Resource resource = resSet.getResource(fileURI, true);

            if (resource.getContents().isEmpty() || !(resource.getContents().get(0) instanceof EvoProject)) {
                MessageDialog.openError(getShell(), "Error", "Invalid Evo configuration file.");
                return false;
            }

            EvoProject evoProject = (EvoProject) resource.getContents().get(0);
            OrchestrationFactory factory = OrchestrationFactory.eINSTANCE;

            orchestrator.setName(generalPage.getOrchestrationName());
            orchestrator.setId(generalPage.getOrchestrationId());

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
            if (!ollamaPage.isSkipped()) {
                Ollama ollama = orchestrator.getOllama();
                if (ollama == null) {
                    ollama = factory.createOllama();
                    orchestrator.setOllama(ollama);
                }
                ollama.setUrl(ollamaPage.getOllamaUrl());
                ollama.setModel(ollamaPage.getModelName());
                ollama.setPath(ollamaPage.getExecutablePath());
            }

            // LLM Settings
            if (!llmPage.isSkipped()) {
                LLM llm = orchestrator.getLlm();
                if (llm == null) {
                    llm = factory.createLLM();
                    orchestrator.setLlm(llm);
                }
                llm.setModel(llmPage.getLlmModel());
                try {
                    llm.setTemperature(Float.parseFloat(llmPage.getTemperature()));
                } catch (NumberFormatException e) {
                    llm.setTemperature(1.0f);
                }
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
            String agentsData = agentPage.getAgentsData();
            if (agentsData != null && !agentsData.isEmpty()) {
                String[] lines = agentsData.split("\\r?\\n");
                for (String line : lines) {
                    String[] parts = line.split(":", 3);
                    if (parts.length >= 2) {
                        Agent agent = factory.createAgent();
                        agent.setId(parts[0].trim());
                        agent.setType(parts[1].trim());
                        if (parts.length >= 3) {
                            RuleParser.parseAndAddRules(agent, parts[2].trim());
                        }
                        orchestrator.getAgents().add(agent);
                    }
                }
            }

            evoProject.getOrchestrations().add(orchestrator);

            resource.save(Collections.emptyMap());
            targetFile.getProject().refreshLocal(IProject.DEPTH_INFINITE, null);

        } catch (Exception e) {
            MessageDialog.openError(getShell(), "Error", "Could not add orchestration: " + e.getMessage());
            e.printStackTrace();
            return false;
        }

        return true;
    }
}
