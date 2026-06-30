package eu.kalafatic.evolution.controller.manager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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
import eu.kalafatic.evolution.controller.discovery.SourceDiscoveryRequest;
import eu.kalafatic.evolution.controller.discovery.SourceDiscoveryResult;
import eu.kalafatic.evolution.controller.discovery.WorkspaceSourceResolver;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.OrchestrationPackage;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.SupervisorSettings;
import eu.kalafatic.evolution.controller.tools.GitTool;
import eu.kalafatic.evolution.controller.trajectory.EvolutionRegistry;
import eu.kalafatic.evolution.controller.providers.AiProviders;
import eu.kalafatic.evolution.controller.providers.ProviderConfig;
import eu.kalafatic.evolution.controller.security.TokenSecurityService;
import eu.kalafatic.evolution.model.orchestration.AIProvider;

/**
 * Singleton manager for Evolution project models.
 * Centralizes all model-related operations: create, load, save, and update.
 *
 * @evo:1:1 reason=centralize-model-management
 */
public class ProjectModelManager {

    private static final ProjectModelManager INSTANCE = new ProjectModelManager();

    private final ResourceSet resourceSet;
    private final EvolutionRegistry evolutionRegistry = new EvolutionRegistry();
    private SourceDiscoveryResult cachedDiscoveryResult;

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

    public EvolutionRegistry getEvolutionRegistry() {
        return evolutionRegistry;
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
        Orchestrator orch = null;
        if (root instanceof Orchestrator) {
            orch = (Orchestrator) root;
        } else if (root instanceof EvoProject) {
            EvoProject project = (EvoProject) root;
            if (!project.getOrchestrations().isEmpty()) {
                orch = project.getOrchestrations().get(0);
            }
        }
        if (orch != null) {
            initializeDefaults(orch);
        }
        return orch;
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
        if (orchestrator.getGit() == null) {
            Git git = OrchestrationFactory.eINSTANCE.createGit();
            git.setRepositoryUrl("https://github.com/kalafatic/evolution/");
            git.setBranch("master");
            orchestrator.setGit(git);
        } else if (orchestrator.getGit().getRepositoryUrl() == null || orchestrator.getGit().getRepositoryUrl().isEmpty()) {
            orchestrator.getGit().setRepositoryUrl("https://github.com/kalafatic/evolution/");
        }

        if (orchestrator.getSupervisorSettings() != null) {
            if (orchestrator.getSupervisorSettings().getGit() == null) {
                Git supGit = OrchestrationFactory.eINSTANCE.createGit();
                supGit.setRepositoryUrl("https://github.com/kalafatic/evo/");
                supGit.setBranch("master");
                orchestrator.getSupervisorSettings().setGit(supGit);
            }
        }

        if (orchestrator.getDefaultTarget() == null || orchestrator.getDefaultTarget().isEmpty()) {
            SourceDiscoveryResult result = getOrDiscoverWorkspace();
            if (result != null && result.getPrimaryRepository() != null) {
                orchestrator.setDefaultTarget(result.getPrimaryRepository().getAbsolutePath());
                eu.kalafatic.evolution.controller.log.Log.log("[MODEL] Default target discovered from workspace: " + orchestrator.getDefaultTarget());
            } else {
                // Fallback to legacy scan if workspace discovery failed
                orchestrator.setDefaultTarget(findEvolutionRepository());
            }
        }

        if (orchestrator.getLlm() == null) {
            LLM llm = OrchestrationFactory.eINSTANCE.createLLM();
            llm.setModel("gpt-4o");
            llm.setTemperature(0.4f);
            orchestrator.setLlm(llm);
        }
        if (orchestrator.getSupervisorSettings() == null) {
            SupervisorSettings supervisor = OrchestrationFactory.eINSTANCE.createSupervisorSettings();
            String userHome = System.getProperty("user.home");
            // OS-independent paths using user.home
            supervisor.setExecutablePath(new java.io.File(userHome, "supervisor/bin/").getPath());
            supervisor.setSourcePath(new java.io.File(userHome, "supervisor/sources/").getPath());
            orchestrator.setSupervisorSettings(supervisor);
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

    public void updateDefaultTarget(Orchestrator orchestrator, String path) {
        orchestrator.setDefaultTarget(path);
    }

    public void updateGitSettings(Orchestrator orchestrator, String url, String branch, String username, String password, String localPath) {
        Git git = orchestrator.getGit();
        if (git == null) {
            git = OrchestrationFactory.eINSTANCE.createGit();
            orchestrator.setGit(git);
        }
        git.setRepositoryUrl(url);
        git.setBranch(branch);
        git.setUsername(username);
        git.setPassword(password);
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

    public void updateSupervisorSettings(Orchestrator orchestrator, String executablePath, String sourcePath, String commands, String settings, boolean deployed) {
        SupervisorSettings supervisor = orchestrator.getSupervisorSettings();
        if (supervisor == null) {
            supervisor = OrchestrationFactory.eINSTANCE.createSupervisorSettings();
            orchestrator.setSupervisorSettings(supervisor);
        }
        supervisor.setExecutablePath(executablePath);
        supervisor.setSourcePath(sourcePath);
        supervisor.setCommands(commands);
        supervisor.setSettings(settings);
        supervisor.setDeployed(deployed);
        if (supervisor.getGit() == null) {
            supervisor.setGit(orchestrator.getGit());
        }
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
     * Fetches available LLM models based on the selected mode(s).
     *
     * @param orchestrator The orchestrator containing configuration (URLs, etc). Can be null.
     * @param modes The AI modes to filter by (LOCAL, REMOTE, HYBRID, PROXY, MEDIATED).
     * @return A list of model names.
     */
    public List<String> getLlmModels(Orchestrator orchestrator, AiMode... modes) {
        List<String> modelNames = new ArrayList<>();
        if (modes == null || modes.length == 0) return modelNames;

        List<AiMode> modeList = Arrays.asList(modes);
        List<AIProvider> allModels = getAllModels(orchestrator);

        for (AIProvider info : allModels) {
            boolean include = false;
            if (modeList.contains(AiMode.LOCAL) && info.isLocal() && !isProxy(info)) {
                include = true;
            }
            if (modeList.contains(AiMode.PROXY) && isProxy(info)) {
                include = true;
            }
            if (modeList.contains(AiMode.REMOTE) && !info.isLocal()) {
                include = true;
            }
            if (modeList.contains(AiMode.HYBRID) && info.isLocal() && !isProxy(info)) {
                include = true;
            }
            if (modeList.contains(AiMode.MEDIATED)) {
                include = true; // Include everything for mediated for now
            }

            if (include) {
                modelNames.add(info.getName());
            }
        }

        return modelNames;
    }

    /**
     * Returns names of local models from available sources.
     *
     * @param orchestrator The orchestrator instance.
     * @return List of local model names.
     */
    public List<String> getLocalModelNames(Orchestrator orchestrator) {
        return getLlmModels(orchestrator, AiMode.LOCAL);
    }

    /**
     * Returns names of remote, cloud, and hybrid models from available sources.
     *
     * @param orchestrator The orchestrator instance.
     * @return List of remote/hybrid model names.
     */
    public List<String> getRemoteModelNames(Orchestrator orchestrator) {
        return getLlmModels(orchestrator, AiMode.REMOTE);
    }

    /**
     * Fetches all available models from all sources.
     *
     * @param orchestrator The orchestrator instance.
     * @return List of AIProvider objects.
     */
    public List<AIProvider> getAllModels(Orchestrator orchestrator) {
        List<AIProvider> models = new ArrayList<>();
        TokenSecurityService security = TokenSecurityService.getInstance();
        OrchestrationFactory factory = OrchestrationFactory.eINSTANCE;

        // 1. Load Custom Providers from Model
        if (orchestrator != null) {
            for (AIProvider p : orchestrator.getAiProviders()) {
                String token = security.getToken(p);
                String state = (token != null && !token.isEmpty() && !token.equals("YOUR_API_KEY")) ? "OK" : "NA";
                if (p.getState() != null && !p.getState().isEmpty()) {
                    state = p.getState();
                }

                // We clone or wrap it? Better to create a non-contained instance for UI if needed,
                // but let's just use the instances from the model if they exist.
                // To avoid concurrent issues with the real model, we could create "transient" AIProviders.
                AIProvider item = factory.createAIProvider();
                item.setName(p.getName());
                item.setUrl(p.getUrl());
                item.setApiKey(token);
                item.setFormat(p.getFormat());
                item.setLocal(p.isLocal());
                item.setDefaultModel(p.getDefaultModel());
                item.setState(state);
                item.setStateDescription(p.getStateDescription());
                item.setRating(p.getRating());
                item.setRatingAnalyze(p.getRatingAnalyze());
                item.setRatingChat(p.getRatingChat());
                item.setRatingProgramming(p.getRatingProgramming());
                // Store reference to original for "Use" or "Edit" actions if needed,
                // but for now let's keep it simple.
                models.add(item);
            }
        }

        // 2. Load Remote Models from static map (if not explicitly in model as custom)
        for (String providerName : AiProviders.PROVIDERS.keySet()) {
            if (models.stream().anyMatch(i -> i.getName().equalsIgnoreCase(providerName))) continue;

            TokenSecurityService.ResolvedProvider resolved = (orchestrator != null)
                    ? security.resolve(orchestrator, providerName)
                    : null;

            String state = (resolved != null && resolved.token != null && !resolved.token.isEmpty() && !"YOUR_API_KEY".equals(resolved.token))
                    ? "OK" : "NA";

            AIProvider item = factory.createAIProvider();
            item.setName(providerName);
            item.setLocal(false);
            item.setUrl((resolved != null) ? resolved.url : "");
            item.setApiKey((resolved != null) ? resolved.token : "");
            item.setState(state);

            ProviderConfig config = AiProviders.PROVIDERS.get(providerName);
            if (config != null) {
                item.setStateDescription("Static provider: " + config.getFormat());
                if (resolved == null) {
                    item.setUrl(config.getEndpointUrl());
                    item.setApiKey(config.getApiKey());
                    item.setDefaultModel(config.getDefaultModel());
                    item.setFormat(config.getFormat());
                }

                // Also add the default model from the static config as an available model
                String defaultModel = config.getDefaultModel();
                if (defaultModel != null && !defaultModel.isEmpty() && !models.stream().anyMatch(m -> defaultModel.equalsIgnoreCase(m.getName()))) {
                    AIProvider modelItem = factory.createAIProvider();
                    modelItem.setName(defaultModel);
                    modelItem.setLocal(false);
                    modelItem.setUrl(item.getUrl());
                    modelItem.setApiKey(item.getApiKey());
                    modelItem.setState(item.getState());
                    modelItem.setFormat(item.getFormat());
                    modelItem.setStateDescription("Model from static provider: " + providerName);
                    models.add(modelItem);
                }
            }

            models.add(item);
        }

        // 3. Load Local Models from Ollama
        String ollamaUrl = (orchestrator != null && orchestrator.getOllama() != null) ? orchestrator.getOllama().getUrl() : "http://localhost:11434";
        OllamaService ollamaService = OllamaManager.getInstance().getService(ollamaUrl);

        try {
            List<OllamaModel> localModels = ollamaService.loadModels();
            for (OllamaModel m : localModels) {
                // Only add if not already in models (which contains EMF providers)
                if (models.stream().noneMatch(i -> i.getName().equalsIgnoreCase(m.getName()))) {
                    AIProvider item = factory.createAIProvider();
                    item.setName(m.getName());
                    item.setLocal(true);
                    item.setUrl(ollamaUrl);
                    item.setState("OK");
                    item.setFormat("ollama");
                    models.add(item);
                } else {
                    // Update state of existing EMF provider if it matches local model
                    models.stream()
                        .filter(i -> i.isLocal() && i.getName().equalsIgnoreCase(m.getName()))
                        .forEach(i -> {
                            i.setState("OK");
                            i.setStateDescription("Model found in Ollama.");
                        });
                }
            }

            // Mark EMF local providers NOT found in Ollama
            models.stream()
                .filter(i -> i.isLocal() && i.getState() != null && !i.getState().equals("OK"))
                .forEach(i -> {
                    i.setState("ERR");
                    i.setStateDescription("Model NOT found in Ollama. Please download it.");
                });

            // 4. Scan local demo models
            File demoDir = new File("./forge-lab/forge-model/src/main/resources/model/demo/");
            if (demoDir.exists() && demoDir.isDirectory()) {
                File[] files = demoDir.listFiles((dir, name) -> name.endsWith(".gguf"));
                if (files != null) {
                    for (File f : files) {
                        String modelName = "demo/" + f.getName().replace(".gguf", "");
                        if (models.stream().noneMatch(i -> i.getName().equalsIgnoreCase(modelName))) {
                            AIProvider item = factory.createAIProvider();
                            item.setName(modelName);
                            item.setLocal(true);
                            item.setUrl(f.getAbsolutePath());
                            item.setState("OK");
                            item.setFormat("ollama");
                            item.setStateDescription("Locally forged demo model");
                            models.add(item);
                        }
                    }
                }
            }

        } catch (Exception e) {
            // If it fails, maybe Ollama is offline
            AIProvider item = factory.createAIProvider();
            item.setName("Ollama");
            item.setLocal(true);
            item.setUrl(ollamaUrl);
            item.setState("ERR");
            item.setStateDescription("Ollama server offline");
            models.add(item);
        }

        return models;
    }

    /**
     * Helper to check if a provider is hybrid.
     */
    public boolean isHybrid(AIProvider provider) {
        if (provider.getName() != null && provider.getName().toLowerCase().endsWith(":cloud")) return true;
        if (!provider.isLocal()) {
            // Remote model without token is considered hybrid (ollama based big models - not entirely local)
            String token = provider.getApiKey();
            if (token == null || token.isEmpty() || "YOUR_API_KEY".equals(token)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Helper to check if a provider is a proxy model.
     */
    public boolean isProxy(AIProvider provider) {
        return provider.getName() != null && provider.getName().toLowerCase().endsWith(":cloud");
    }

    /**
     * Returns a list of available local Git repositories on the system.
     * This uses a cached list to avoid UI blocking.
     *
     * @return List of absolute paths to discovered repositories.
     */
    public List<String> getAvailableLocalRepositories() {
        List<java.io.File> repos = GitTool.getCachedLocalRepositories();
        List<String> paths = new ArrayList<>();
        for (java.io.File repo : repos) {
            paths.add(repo.getAbsolutePath());
        }
        Collections.sort(paths);
        return paths;
    }

    public synchronized SourceDiscoveryResult getOrDiscoverWorkspace() {
        if (cachedDiscoveryResult == null) {
            WorkspaceSourceResolver resolver = new WorkspaceSourceResolver();
            cachedDiscoveryResult = resolver.discover(new SourceDiscoveryRequest());
        }
        return cachedDiscoveryResult;
    }

    /**
     * Finds the evolution repository on the local system.
     *
     * @return The absolute path to the evolution repository, or null if not found.
     */
    public String findEvolutionRepository() {
        List<java.io.File> repos = GitTool.getCachedLocalRepositories();
        for (java.io.File repo : repos) {
            String name = repo.getName().toLowerCase();
            if (name.equals("evolution") || name.equals("evo")) {
                return repo.getAbsolutePath();
            }
        }
        return null;
    }
}
