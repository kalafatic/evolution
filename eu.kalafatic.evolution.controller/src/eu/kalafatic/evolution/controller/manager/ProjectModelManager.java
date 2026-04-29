package eu.kalafatic.evolution.controller.manager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IFile;
import eu.kalafatic.evolution.model.orchestration.AiMode;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;

import eu.kalafatic.evolution.model.orchestration.Agent;
import eu.kalafatic.evolution.model.orchestration.AiChat;
import eu.kalafatic.evolution.model.orchestration.Compiler;
import eu.kalafatic.evolution.model.orchestration.EvoProject;
import eu.kalafatic.evolution.model.orchestration.FileConfig;
import eu.kalafatic.evolution.model.orchestration.Git;
import eu.kalafatic.evolution.model.orchestration.LLM;
import eu.kalafatic.evolution.model.orchestration.Maven;
import eu.kalafatic.evolution.model.orchestration.NeuronAI;
import eu.kalafatic.evolution.model.orchestration.NeuronType;
import eu.kalafatic.evolution.model.orchestration.Ollama;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.OrchestrationPackage;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

/**
 * Singleton manager for Evolution project models.
 * Centralizes all model-related operations: create, load, save, and update.
 *
 * @evo:1:1 reason=centralize-model-management
 */
public class ProjectModelManager {

    private static final ProjectModelManager INSTANCE = new ProjectModelManager();

    private final ResourceSet resourceSet;

    private ProjectModelManager() {
        resourceSet = new ResourceSetImpl();
        resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("xml", new XMIResourceFactoryImpl());
        resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("evo", new XMIResourceFactoryImpl());
        // Ensure package is registered
        OrchestrationPackage.eINSTANCE.eClass();
    }

    public static ProjectModelManager getInstance() {
        return INSTANCE;
    }

    public ResourceSet getResourceSet() {
        return resourceSet;
    }

    /**
     * Loads a model from the specified URI.
     * Supports both local and remote URIs.
     *
     * @param uri The URI of the model to load.
     * @return The root EObject of the loaded model.
     * @throws IOException If the model could not be loaded.
     */
    public org.eclipse.emf.ecore.EObject loadModel(URI uri) throws IOException {
        Resource resource = resourceSet.getResource(uri, true);
        if (resource != null && !resource.getContents().isEmpty()) {
            return resource.getContents().get(0);
        }
        return null;
    }

    /**
     * Loads a model from the specified workspace file.
     *
     * @param file The workspace file.
     * @return The root EObject of the loaded model.
     * @throws IOException If the model could not be loaded.
     */
    public org.eclipse.emf.ecore.EObject loadModel(IFile file) throws IOException {
        URI uri = URI.createPlatformResourceURI(file.getFullPath().toString(), true);
        return loadModel(uri);
    }

    /**
     * Loads a model from the specified URI string.
     *
     * @param uriString The URI string (local or remote).
     * @return The root EObject of the loaded model.
     * @throws IOException If the model could not be loaded.
     */
    public org.eclipse.emf.ecore.EObject loadModel(String uriString) throws IOException {
        URI uri = URI.createURI(uriString);
        return loadModel(uri);
    }

    public EvoProject loadProject(IFile file) throws IOException {
        org.eclipse.emf.ecore.EObject root = loadModel(file);
        if (root instanceof EvoProject) {
            return (EvoProject) root;
        }
        return null;
    }

    public Orchestrator loadOrchestrator(IFile file) throws IOException {
        org.eclipse.emf.ecore.EObject root = loadModel(file);
        if (root instanceof Orchestrator) {
            return (Orchestrator) root;
        } else if (root instanceof EvoProject) {
            EvoProject project = (EvoProject) root;
            if (!project.getOrchestrations().isEmpty()) {
                return project.getOrchestrations().get(0);
            }
        }
        return null;
    }

    public void saveResource(Resource resource) throws IOException {
        if (resource != null) {
            resource.save(Collections.EMPTY_MAP);
        }
    }

    public Resource createResource(String filePath) {
        URI fileURI = URI.createFileURI(filePath);
        return resourceSet.createResource(fileURI);
    }

    public EvoProject createProject(String name) {
        EvoProject project = OrchestrationFactory.eINSTANCE.createEvoProject();
        project.setName(name);
        return project;
    }

    public Orchestrator createOrchestrator(String id, String name) {
        Orchestrator orchestrator = OrchestrationFactory.eINSTANCE.createOrchestrator();
        orchestrator.setId(id);
        orchestrator.setName(name);
        initializeDefaults(orchestrator);
        return orchestrator;
    }

    public void initializeDefaults(Orchestrator orchestrator) {
        if (orchestrator.getLlm() == null) {
            LLM llm = OrchestrationFactory.eINSTANCE.createLLM();
            llm.setModel("gpt-4o");
            llm.setTemperature(1.0f);
            orchestrator.setLlm(llm);
        }
        if (orchestrator.getOllama() == null) {
            Ollama ollama = OrchestrationFactory.eINSTANCE.createOllama();
            ollama.setUrl("http://127.0.0.1:11434");
            ollama.setModel("llama3.2:3b");
            orchestrator.setOllama(ollama);
        }
        if (orchestrator.getAiChat() == null) {
            orchestrator.setAiChat(OrchestrationFactory.eINSTANCE.createAiChat());
        }
    }

    public void updateGitSettings(Orchestrator orchestrator, String url, String branch, String username, String localPath) {
        Git git = orchestrator.getGit();
        if (git == null) {
            git = OrchestrationFactory.eINSTANCE.createGit();
            orchestrator.setGit(git);
        }
        git.setRepositoryUrl(url);
        git.setBranch(branch);
        git.setUsername(username);
        git.setLocalPath(localPath);
    }

    public void updateOllamaSettings(Orchestrator orchestrator, String url, String model, String path) {
        Ollama ollama = orchestrator.getOllama();
        if (ollama == null) {
            ollama = OrchestrationFactory.eINSTANCE.createOllama();
            orchestrator.setOllama(ollama);
        }
        ollama.setUrl(url);
        ollama.setModel(model);
        ollama.setPath(path);
    }

    public void updateLlmSettings(Orchestrator orchestrator, String model, float temperature) {
        LLM llm = orchestrator.getLlm();
        if (llm == null) {
            llm = OrchestrationFactory.eINSTANCE.createLLM();
            orchestrator.setLlm(llm);
        }
        llm.setModel(model);
        llm.setTemperature(temperature);
    }

    public void updateMavenSettings(Orchestrator orchestrator, List<String> goals, List<String> profiles) {
        Maven maven = orchestrator.getMaven();
        if (maven == null) {
            maven = OrchestrationFactory.eINSTANCE.createMaven();
            orchestrator.setMaven(maven);
        }
        if (goals != null) {
            maven.getGoals().clear();
            maven.getGoals().addAll(goals);
        }
        if (profiles != null) {
            maven.getProfiles().clear();
            maven.getProfiles().addAll(profiles);
        }
    }

    public void updateAiChatSettings(Orchestrator orchestrator, String url, String token, String prompt, String proxyUrl) {
        AiChat aiChat = orchestrator.getAiChat();
        if (aiChat == null) {
            aiChat = OrchestrationFactory.eINSTANCE.createAiChat();
            orchestrator.setAiChat(aiChat);
        }
        aiChat.setUrl(url);
        aiChat.setToken(token);
        aiChat.setPrompt(prompt);
        aiChat.setProxyUrl(proxyUrl);
    }

    public void updateNeuronAISettings(Orchestrator orchestrator, String url, String model, NeuronType type) {
        NeuronAI neuronAI = orchestrator.getNeuronAI();
        if (neuronAI == null) {
            neuronAI = OrchestrationFactory.eINSTANCE.createNeuronAI();
            orchestrator.setNeuronAI(neuronAI);
        }
        neuronAI.setUrl(url);
        neuronAI.setModel(model);
        if (type != null) {
            neuronAI.setType(type);
        }
    }

    public void addAgent(Orchestrator orchestrator, String id, String type) {
        Agent agent = OrchestrationFactory.eINSTANCE.createAgent();
        agent.setId(id);
        agent.setType(type);
        orchestrator.getAgents().add(agent);
    }

    public void setFileConfig(Orchestrator orchestrator, String localPath) {
        FileConfig fileConfig = OrchestrationFactory.eINSTANCE.createFileConfig();
        fileConfig.setLocalPath(localPath);
        orchestrator.setFileConfig(fileConfig);
    }

    public void updateOrchestratorGeneral(Orchestrator orchestrator, String id, String name) {
        orchestrator.setId(id);
        orchestrator.setName(name);
    }

    public void updateCompilerSettings(Orchestrator orchestrator, String sourceVersion) {
        Compiler compiler = orchestrator.getCompiler();
        if (compiler == null) {
            compiler = OrchestrationFactory.eINSTANCE.createCompiler();
            orchestrator.setCompiler(compiler);
        }
        compiler.setSourceVersion(sourceVersion);
    }

    public void updateAiMode(Orchestrator orchestrator, AiMode mode) {
	orchestrator.setAiMode(mode);
    }

    public void updateRemoteModel(Orchestrator orchestrator, String remoteModel) {
	orchestrator.setRemoteModel(remoteModel);
    }

    public void updateMcpServerUrl(Orchestrator orchestrator, String url) {
	orchestrator.setMcpServerUrl(url);
    }

    public void updateOpenAiToken(Orchestrator orchestrator, String token) {
	orchestrator.setOpenAiToken(token);
    }

    public void updateOpenAiModel(Orchestrator orchestrator, String model) {
	orchestrator.setOpenAiModel(model);
    }

    public void updateLocalModel(Orchestrator orchestrator, String model) {
	orchestrator.setLocalModel(model);
    }

    public void updateHybridModel(Orchestrator orchestrator, String model) {
	orchestrator.setHybridModel(model);
    }

    public void updateOfflineMode(Orchestrator orchestrator, boolean offline) {
	orchestrator.setOfflineMode(offline);
    }

    public void updateDarwinMode(Orchestrator orchestrator, boolean darwin) {
	orchestrator.setDarwinMode(darwin);
    }

    /**
     * Fetches available LLM models based on the selected mode.
     *
     * @param orchestrator The orchestrator containing configuration (URLs, etc). Can be null.
     * @param mode The AI mode (LOCAL, REMOTE, HYBRID).
     * @return A list of model names.
     */
    public List<String> getLlmModels(Orchestrator orchestrator, AiMode mode) {
        List<String> models = new ArrayList<>();
        if (mode == AiMode.LOCAL || mode == AiMode.HYBRID) {
            String ollamaUrl = (orchestrator != null && orchestrator.getOllama() != null) ? orchestrator.getOllama().getUrl() : "http://localhost:11434";
            OllamaService ollama = new OllamaService(ollamaUrl, null);
            try {
                for (OllamaModel m : ollama.loadModels()) {
                    models.add(m.getName());
                }
            } catch (Exception e) {
                // log or ignore
            }
        }

        if (mode == AiMode.REMOTE || mode == AiMode.HYBRID) {
            // Placeholder for remote models (e.g. OpenAI, Gemini)
            // In a real implementation, we would fetch these from the respective providers
            models.add("gpt-4o");
            models.add("gpt-4o-mini");
            models.add("gpt-3.5-turbo");
            models.add("claude-3-5-sonnet-20240620");
            models.add("gemini-1.5-pro");
        }

        return models;
    }
}
