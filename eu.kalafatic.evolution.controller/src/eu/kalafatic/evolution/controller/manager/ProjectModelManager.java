package eu.kalafatic.evolution.controller.manager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.core.resources.IFile;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;

import eu.kalafatic.evolution.controller.discovery.SourceDiscoveryRequest;
import eu.kalafatic.evolution.controller.discovery.SourceDiscoveryResult;
import eu.kalafatic.evolution.controller.discovery.WorkspaceSourceResolver;
import eu.kalafatic.evolution.controller.orchestration.llm.OllamaProvider;
import eu.kalafatic.evolution.controller.tools.EclipseGitEvoTool;
import eu.kalafatic.evolution.controller.providers.AiProviders;
import eu.kalafatic.evolution.controller.providers.ProviderConfig;
import eu.kalafatic.evolution.controller.security.TokenSecurityService;
import eu.kalafatic.evolution.controller.tools.GitTool;
import eu.kalafatic.evolution.controller.trajectory.EvolutionRegistry;
import eu.kalafatic.evolution.model.orchestration.AIProvider;
import eu.kalafatic.evolution.model.orchestration.Agent;
import eu.kalafatic.evolution.model.orchestration.AiChat;
import eu.kalafatic.evolution.model.orchestration.AiMode;
import eu.kalafatic.evolution.model.orchestration.ServerSettings;
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
import eu.kalafatic.evolution.model.orchestration.SupervisorSettings;

/**
 * Singleton manager for Evolution project models.
 * Centralizes all model-related operations: create, load, save, and update.
 *
 * @evo:1:1 reason=centralize-model-management
 */
public class ProjectModelManager {

    private static final ProjectModelManager INSTANCE = new ProjectModelManager();

    /**
     * Artifact-path resolution seam for "evo native" model filtering. The default
     * resolves against the production model directories; tests may override it
     * with a hermetic resolver and restore the default via resetArtifactPathResolver().
     */
    private static volatile java.util.function.Function<String, File> artifactPathResolver =
            OllamaProvider::resolveEvoArtifactPath;

    private final ResourceSet resourceSet;
    private final EvolutionRegistry evolutionRegistry = new EvolutionRegistry();
    private SourceDiscoveryResult cachedDiscoveryResult;
    private final List<Runnable> modelChangeListeners = new CopyOnWriteArrayList<>();

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

    public static void setArtifactPathResolver(java.util.function.Function<String, File> resolver) {
        artifactPathResolver = resolver;
    }

    public static void resetArtifactPathResolver() {
        artifactPathResolver = OllamaProvider::resolveEvoArtifactPath;
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

    /**
     * Persists an EMF resource, skipping the write entirely when the serialized
     * content is byte-for-byte identical to what is already on disk.
     *
     * <p>Writing an unchanged configuration file externally makes Eclipse's
     * {@code ResourceTextFileBuffer} re-synchronize the document of any editor that has
     * the file open (e.g. a WTP SSE structured text editor showing {@code evo_config.xml}).
     * That re-synchronization fires a document change through
     * {@code org.eclipse.text.undo.DocumentUndoManager} into the global operation history,
     * which notifies undo/redo {@code OperationHistoryActionHandler}s. A handler whose
     * {@code IUndoContext} is null then fails in
     * {@code AbstractOperation.hasContext(null)} with
     * {@code AssertionFailedException: null argument}. Avoiding the no-op write removes the
     * trigger without disabling undo/redo or suppressing the error.
     */
    public void saveResource(Resource resource) throws IOException {
        if (resource == null) {
            return;
        }

        // Serialize to memory first so we can compare against the on-disk content.
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        byte[] newContent;
        try {
            resource.save(baos, Collections.EMPTY_MAP);
            newContent = baos.toByteArray();
        } catch (java.lang.UnsupportedOperationException uoe) {
            // A generic ResourceImpl cannot serialize to an OutputStream. Fall back to the
            // plain save (which routes through the concrete resource's URI handler).
            try {
                resource.save(Collections.EMPTY_MAP);
            } catch (org.eclipse.core.runtime.AssertionFailedException afe) {
                eu.kalafatic.evolution.controller.log.Log.log("[MODEL] AssertionFailedException during save: " + afe.getMessage());
            }
            return;
        } catch (org.eclipse.core.runtime.AssertionFailedException afe) {
            // EMF's own serialization assertion; unrelated to operation-history. Log and
            // stop so we never propagate a half-serialized model.
            eu.kalafatic.evolution.controller.log.Log.log("[MODEL] AssertionFailedException during serialization: " + afe.getMessage());
            return;
        }

        URI uri = resource.getURI();
        if (uri == null) {
            // No resolvable location: fall back to the plain EMF save.
            try {
                resource.save(Collections.EMPTY_MAP);
            } catch (org.eclipse.core.runtime.AssertionFailedException afe) {
                eu.kalafatic.evolution.controller.log.Log.log("[MODEL] AssertionFailedException during save: " + afe.getMessage());
            }
            return;
        }

        if (uri.isPlatformResource()) {
            org.eclipse.core.resources.IFile file = org.eclipse.core.resources.ResourcesPlugin.getWorkspace().getRoot()
                    .getFile(new org.eclipse.core.runtime.Path(uri.toPlatformString(true)));
            if (fileContentEquals(file, newContent)) {
                return; // unchanged: no external write, open editors are left untouched
            }
            try {
                file.setContents(new java.io.ByteArrayInputStream(newContent),
                        org.eclipse.core.resources.IResource.KEEP_HISTORY | org.eclipse.core.resources.IResource.FORCE,
                        new org.eclipse.core.runtime.NullProgressMonitor());
            } catch (org.eclipse.core.runtime.CoreException e) {
                throw new IOException("Failed to persist workspace resource '" + uri + "': " + e.getMessage(), e);
            }
        } else if (uri.isFile()) {
            java.io.File file = new java.io.File(uri.toFileString());
            if (fileContentEquals(file, newContent)) {
                return; // unchanged: no external write
            }
            java.nio.file.Files.write(file.toPath(), newContent);
        } else {
            // Non-file resource (e.g. remote URI): keep the existing EMF save path.
            try {
                resource.save(Collections.EMPTY_MAP);
            } catch (org.eclipse.core.runtime.AssertionFailedException afe) {
                eu.kalafatic.evolution.controller.log.Log.log("[MODEL] AssertionFailedException during save: " + afe.getMessage());
            }
        }
    }

    /** Returns true when the workspace file already holds exactly {@code expected} bytes. */
    private static boolean fileContentEquals(org.eclipse.core.resources.IFile file, byte[] expected) {
        if (file == null || !file.exists()) {
            return false;
        }
        try (java.io.InputStream in = file.getContents()) {
            return java.util.Arrays.equals(expected, in.readAllBytes());
        } catch (Exception e) {
            return false;
        }
    }

    /** Returns true when the on-disk file already holds exactly {@code expected} bytes. */
    private static boolean fileContentEquals(java.io.File file, byte[] expected) {
        if (file == null || !file.isFile()) {
            return false;
        }
        try {
            return java.util.Arrays.equals(expected, java.nio.file.Files.readAllBytes(file.toPath()));
        } catch (Exception e) {
            return false;
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
        String userHome = System.getProperty("user.home");
        String defaultGitRepo = new File(userHome, "git/evolution").getAbsolutePath();

        if (orchestrator.getGit() == null) {
            Git git = OrchestrationFactory.eINSTANCE.createGit();
            git.setRepositoryUrl("https://github.com/kalafatic/evolution.git");
            git.setBranch("main");
            git.setLocalPath(defaultGitRepo);
            orchestrator.setGit(git);
        } else {
            if (orchestrator.getGit().getRepositoryUrl() == null || orchestrator.getGit().getRepositoryUrl().isEmpty()) {
                orchestrator.getGit().setRepositoryUrl("https://github.com/kalafatic/evolution.git");
            }
            if (orchestrator.getGit().getLocalPath() == null || orchestrator.getGit().getLocalPath().isEmpty()) {
                orchestrator.getGit().setLocalPath(defaultGitRepo);
            }
            if (orchestrator.getGit().getBranch() == null || orchestrator.getGit().getBranch().isEmpty()) {
                orchestrator.getGit().setBranch("main");
            }
        }

        if (orchestrator.getSupervisorSettings() == null) {
            SupervisorSettings supervisor = OrchestrationFactory.eINSTANCE.createSupervisorSettings();
            File runtimeInstances = new File(userHome, "workspace/runtime/instances");
            File repoSupervisorModule = new File(userHome, "git/evolution/eu.kalafatic.evolution.supervisor");
            supervisor.setExecutablePath(runtimeInstances.getAbsolutePath());
            supervisor.setSourcePath(repoSupervisorModule.getAbsolutePath());
            orchestrator.setSupervisorSettings(supervisor);
        } else {
            String oldExec = orchestrator.getSupervisorSettings().getExecutablePath();
            String oldSrc = orchestrator.getSupervisorSettings().getSourcePath();
            if (oldExec != null && oldExec.contains("workspace/runtime/builds")) {
                orchestrator.getSupervisorSettings().setExecutablePath(new File(userHome, "workspace/runtime/instances").getAbsolutePath());
            } else if (oldExec != null) {
                orchestrator.getSupervisorSettings().setExecutablePath(migratePath(oldExec));
            }
            if (oldSrc != null && oldSrc.contains("workspace/runtime/sources")) {
                orchestrator.getSupervisorSettings().setSourcePath(new File(userHome, "git/evolution/eu.kalafatic.evolution.supervisor").getAbsolutePath());
            } else if (oldSrc != null) {
                orchestrator.getSupervisorSettings().setSourcePath(migratePath(oldSrc));
            }
        }

        if (orchestrator.getSupervisorSettings().getGit() == null) {
            Git supGit = OrchestrationFactory.eINSTANCE.createGit();
            supGit.setRepositoryUrl(EclipseGitEvoTool.getRepositoryRemote(EclipseGitEvoTool.REPO_WORKSPACE));
            supGit.setBranch(EclipseGitEvoTool.getRepositoryBranch(EclipseGitEvoTool.REPO_WORKSPACE));
            supGit.setLocalPath(EclipseGitEvoTool.getRepositoryPath(EclipseGitEvoTool.REPO_WORKSPACE));
            orchestrator.getSupervisorSettings().setGit(supGit);
        } else {
            if (orchestrator.getSupervisorSettings().getGit().getRepositoryUrl() == null || orchestrator.getSupervisorSettings().getGit().getRepositoryUrl().isEmpty()) {
                orchestrator.getSupervisorSettings().getGit().setRepositoryUrl(EclipseGitEvoTool.getRepositoryRemote(EclipseGitEvoTool.REPO_WORKSPACE));
            }
            if (orchestrator.getSupervisorSettings().getGit().getLocalPath() == null || orchestrator.getSupervisorSettings().getGit().getLocalPath().isEmpty()) {
                orchestrator.getSupervisorSettings().getGit().setLocalPath(EclipseGitEvoTool.getRepositoryPath(EclipseGitEvoTool.REPO_WORKSPACE));
            }
            if (orchestrator.getSupervisorSettings().getGit().getBranch() == null || orchestrator.getSupervisorSettings().getGit().getBranch().isEmpty()) {
                orchestrator.getSupervisorSettings().getGit().setBranch(EclipseGitEvoTool.getRepositoryBranch(EclipseGitEvoTool.REPO_WORKSPACE));
            }
        }
        
        

        if (orchestrator.getDefaultTarget() == null || orchestrator.getDefaultTarget().isEmpty()) {
            // The configured source repository (EMF Git.localPath) is authoritative.
            // Workspace discovery may only report what is open in Eclipse; it must
            // never silently override the configured repository with an unrelated
            // project root (e.g. a sibling product checkout or runtime workspace).
            Git configuredGit = orchestrator.getGit();
            String configured = (configuredGit != null && configuredGit.getLocalPath() != null)
                    ? configuredGit.getLocalPath().trim() : "";
            if (configured.isEmpty()) {
                configured = defaultGitRepo;
            }
            orchestrator.setDefaultTarget(configured);
            eu.kalafatic.evolution.controller.log.Log.log(
                    "[MODEL] Default target uses the configured repository: " + configured);
        }

        if (orchestrator.getLlm() == null) {
            LLM llm = OrchestrationFactory.eINSTANCE.createLLM();
            llm.setModel("gpt-4o");
            llm.setTemperature(0.4f);
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

        if (orchestrator.getServerSettings() == null) {
            ServerSettings serverSettings = OrchestrationFactory.eINSTANCE.createServerSettings();
            orchestrator.setServerSettings(serverSettings);
        }

        eu.kalafatic.evolution.controller.resource.PortAllocationManager pam =
                eu.kalafatic.evolution.controller.resource.PortAllocationManager.getInstance();
        boolean migrated = pam.migrateFromLegacy(orchestrator);
        eu.kalafatic.evolution.controller.resource.PortPersistence.getInstance().loadFromEmf(orchestrator);
        if (migrated) {
            eu.kalafatic.evolution.controller.resource.PortPersistence.getInstance().saveToEmf(orchestrator);
        }
    }

    public void updateDefaultTarget(Orchestrator orchestrator, String path) {
        orchestrator.setDefaultTarget(path);
    }

    public void updateGitSettings(Orchestrator orchestrator, String url, String branch, String username, String password, String localPath) {
        if (orchestrator == null) return;
        Git git = orchestrator.getGit();
        if (git == null) {
            git = OrchestrationFactory.eINSTANCE.createGit();
            orchestrator.setGit(git);
        }
        updateGitSettings(git, url, branch, username, password, localPath);
    }

    public void updateGitSettings(Git git, String url, String branch, String username, String password, String localPath) {
        if (git == null) return;
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
            if (modeList.contains(AiMode.INTENT) && info.isLocal() && !isProxy(info)) {
                include = true;
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

    // ------------------------------------------------------------------
    // Model lifecycle / registration consistency helpers
    // ------------------------------------------------------------------

    public ModelStateStore getModelStateStore() {
        return ModelStateStore.getInstance();
    }

    /**
     * Registers a listener invoked whenever model registrations or discovery
     * state change. Listeners must marshal to the SWT display thread themselves.
     */
    public void addModelChangeListener(Runnable listener) {
        if (listener != null) {
            modelChangeListeners.add(listener);
        }
    }

    public void removeModelChangeListener(Runnable listener) {
        modelChangeListeners.remove(listener);
    }

    public void notifyModelsChanged() {
        for (Runnable listener : modelChangeListeners) {
            try {
                listener.run();
            } catch (Exception e) {
                eu.kalafatic.evolution.controller.log.Log.log(
                        "[MODEL] Model change listener failed: " + e.getMessage());
            }
        }
    }

    /**
     * Invalidates runtime discovery caches and notifies UI consumers.
     */
    public void refreshModelDiscovery() {
        Orchestrator current = null;
        try {
            current = eu.kalafatic.evolution.controller.orchestration.OrchestratorServiceImpl
                    .getInstance().getOrchestrator();
        } catch (Exception ignored) {
            // Headless/test context without an active orchestration
        }
        refreshModelDiscovery(current);
    }

    /**
     * Invalidates every model discovery cache, including the AI Commander
     * provider-model cache, so a removed model cannot survive in a selection
     * control.
     */
    public void refreshModelDiscovery(Orchestrator orchestrator) {
        OllamaManager.getInstance().clearAllModelCaches();
        try {
            eu.kalafatic.evolution.controller.orchestration.llm.LlmRouter.getInstance()
                    .refreshProviderModels(orchestrator);
        } catch (Exception e) {
            eu.kalafatic.evolution.controller.log.Log.log(
                    "[MODEL] Failed to refresh provider-model cache: " + e.getMessage());
        }
        notifyModelsChanged();
    }

    /**
     * Finds the EMF provider entries that belong to the resolved model identity.
     * This is the authoritative registration lookup used by removal and editing.
     */
    public List<AIProvider> findRegisteredProviders(Orchestrator orchestrator, ModelIdentity identity) {
        List<AIProvider> matches = new ArrayList<>();
        if (orchestrator == null || identity == null) {
            return matches;
        }
        for (AIProvider provider : orchestrator.getAiProviders()) {
            if (matchesIdentity(orchestrator, identity, provider)) {
                matches.add(provider);
            }
        }
        return matches;
    }

    public boolean isRegistered(Orchestrator orchestrator, ModelIdentity identity) {
        return !findRegisteredProviders(orchestrator, identity).isEmpty();
    }

    /**
     * Registration state for UI display: Registered / Unregistered / Discovered.
     */
    public String getRegistrationState(Orchestrator orchestrator, AIProvider provider) {
        if (provider == null) {
            return "Unknown";
        }
        ModelIdentity identity = ModelIdentityResolver.resolve(orchestrator, provider);
        if (!findRegisteredProviders(orchestrator, identity).isEmpty()) {
            return "Registered";
        }
        if (getModelStateStore().isSuppressed(identity.getCanonicalId())) {
            return "Unregistered";
        }
        return "Discovered";
    }

    /**
     * True when the candidate provider represents the same underlying model as
     * the resolved identity. Cross-kind matches are intentionally conservative:
     * an Ollama registration is not blindly treated as an EVO Native artifact.
     */
    public boolean matchesIdentity(Orchestrator orchestrator, ModelIdentity identity, AIProvider candidate) {
        if (identity == null || candidate == null) {
            return false;
        }
        ModelIdentity candidateIdentity = ModelIdentityResolver.resolve(orchestrator, candidate);
        if (identity.getCanonicalId().equals(candidateIdentity.getCanonicalId())) {
            return true;
        }
        if (identity.getKind() != candidateIdentity.getKind()) {
            return false;
        }
        switch (identity.getKind()) {
            case OLLAMA:
                // Names are only equivalent within the same Ollama endpoint.
                if (!identity.getEndpoint().isBlank() && !candidateIdentity.getEndpoint().isBlank()
                        && !identity.getEndpoint().equalsIgnoreCase(candidateIdentity.getEndpoint())) {
                    return false;
                }
                return ModelIdentityResolver.ollamaNamesEquivalent(
                        identity.getTechnicalId(), candidateIdentity.getTechnicalId())
                        || ModelIdentityResolver.ollamaNamesEquivalent(
                                identity.getRuntimeName(), candidateIdentity.getRuntimeName());
            case EVO_NATIVE:
            case GGUF:
            case DEMO:
                if (identity.getArtifactPath() != null && candidateIdentity.getArtifactPath() != null) {
                    return identity.getArtifactPath().toAbsolutePath().normalize()
                            .equals(candidateIdentity.getArtifactPath().toAbsolutePath().normalize());
                }
                // When only one side can resolve a path, fall back to the exact
                // technical id. Never match by a bare file name alias, which
                // could remove a same-named artifact in a different directory.
                return identity.getTechnicalId().equalsIgnoreCase(candidateIdentity.getTechnicalId());
            case REMOTE:
                // Remote registrations are matched by provider/model name only.
                // Matching by endpoint would remove unrelated models that share
                // the same provider URL.
                String name = candidateIdentity.getTechnicalId();
                return name.equalsIgnoreCase(identity.getTechnicalId())
                        || identity.getAliases().contains(name);
            default:
                return false;
        }
    }

    /**
     * Removes every EMF provider registration belonging to the identity.
     *
     * @return names of the removed provider entries.
     */
    public List<String> removeRegisteredProviders(Orchestrator orchestrator, ModelIdentity identity) {
        List<String> removed = new ArrayList<>();
        if (orchestrator == null || identity == null) {
            return removed;
        }
        java.util.Iterator<AIProvider> iterator = orchestrator.getAiProviders().iterator();
        while (iterator.hasNext()) {
            AIProvider provider = iterator.next();
            if (matchesIdentity(orchestrator, identity, provider)) {
                removed.add(provider.getName());
                iterator.remove();
            }
        }
        return removed;
    }

    /**
     * Clears default model references that point at the identity.
     *
     * @return true when at least one reference was cleared.
     */
    public boolean clearModelDefaultReferences(Orchestrator orchestrator, ModelIdentity identity) {
        if (orchestrator == null || identity == null) {
            return false;
        }
        boolean changed = false;

        if (referencesModel(identity, orchestrator.getLocalModel())) {
            updateLocalModel(orchestrator, "");
            changed = true;
        }
        if (referencesModel(identity, orchestrator.getRemoteModel())) {
            updateRemoteModel(orchestrator, "");
            changed = true;
        }
        if (referencesModel(identity, orchestrator.getHybridModel())) {
            updateHybridModel(orchestrator, "");
            changed = true;
        }
        if (referencesModel(identity, orchestrator.getOpenAiModel())) {
            updateOpenAiModel(orchestrator, "");
            changed = true;
        }
        if (orchestrator.getOllama() != null && referencesModel(identity, orchestrator.getOllama().getModel())) {
            orchestrator.getOllama().setModel("");
            changed = true;
        }
        if (orchestrator.getLlm() != null && referencesModel(identity, orchestrator.getLlm().getModel())) {
            orchestrator.getLlm().setModel("");
            changed = true;
        }
        if (orchestrator.getNeuronAI() != null && referencesModel(identity, orchestrator.getNeuronAI().getModel())) {
            orchestrator.getNeuronAI().setModel("");
            changed = true;
        }
        return changed;
    }

    /**
     * True when a configured model reference denotes the identity.
     */
    public boolean referencesModel(ModelIdentity identity, String reference) {
        if (identity == null || reference == null || reference.isBlank()) {
            return false;
        }
        String ref = reference.trim();
        if (identity.isOllama()) {
            return ModelIdentityResolver.ollamaNamesEquivalent(identity.getTechnicalId(), ref);
        }
        if (identity.isArtifactBacked()) {
            if (ref.equalsIgnoreCase(identity.getTechnicalId())) {
                return true;
            }
            if (identity.getArtifactPath() != null) {
                try {
                    java.nio.file.Path candidate = java.nio.file.Paths.get(ref)
                            .toAbsolutePath().normalize();
                    if (candidate.equals(identity.getArtifactPath().toAbsolutePath().normalize())) {
                        return true;
                    }
                } catch (Exception ignored) {
                    // Not a path
                }
            }
            String fileName = identity.getArtifactPath() != null
                    ? identity.getArtifactPath().getFileName().toString() : identity.getTechnicalId();
            return ref.equalsIgnoreCase(fileName);
        }
        return ref.equalsIgnoreCase(identity.getTechnicalId());
    }

    /**
     * Fetches all available models from all sources.
     *
     * <p>Explicitly unregistered models are excluded. The model management page
     * uses {@link #getAllModels(Orchestrator, boolean)} to optionally reveal them
     * for diagnostics; every other consumer (AI Commander, selection combos,
     * Forge) must never see a removed model.</p>
     *
     * @param orchestrator The orchestrator instance.
     * @return List of AIProvider objects.
     */
    public List<AIProvider> getAllModels(Orchestrator orchestrator) {
        return getAllModels(orchestrator, false);
    }

    /**
     * @param includeSuppressed when true, explicitly unregistered models that
     *                          still exist externally are included and marked as
     *                          unregistered (Properties → Models diagnostics).
     */
    public List<AIProvider> getAllModels(Orchestrator orchestrator, boolean includeSuppressed) {
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

        java.util.Set<String> localNames = new java.util.HashSet<>();
        boolean ollamaOnline = false;

        try {
            List<OllamaModel> localModels = ollamaService.loadModels();
            ollamaOnline = true;
            for (OllamaModel m : localModels) {
                String nameLower = m.getName().toLowerCase();
                localNames.add(nameLower);
                if (nameLower.contains(":")) {
                    localNames.add(nameLower.split(":")[0]);
                }
            }

            for (OllamaModel m : localModels) {
                // Only add if not already in models (which contains EMF providers)
                if (models.stream().noneMatch(i -> i.getName().equalsIgnoreCase(m.getName()))) {
                    AIProvider item = factory.createAIProvider();
                    item.setName(m.getName());
                    item.setLocal(true);
                    item.setUrl(ollamaUrl);
                    item.setState("OK");
                    item.setFormat("ollama");
                    item.setStateDescription("Model is registered and ready in Ollama.");
                    models.add(item);
                }
            }
        } catch (Exception e) {
            // Ollama is offline, handle below
        }

        // Helper to check if a model file exists on disk
        java.util.function.BiFunction<String, String, Boolean> checkGgufOnDisk = (modelName, targetName) -> {
            // Check internal controller models folder first as default
            File controllerModelsDir = LlamaService.resolveControllerModelsDir();
            if (controllerModelsDir != null && controllerModelsDir.exists() && controllerModelsDir.isDirectory()) {
                File f1 = new File(controllerModelsDir, modelName + ".gguf");
                File f2 = new File(controllerModelsDir, "evo.gguf");
                if (f1.exists() || (modelName.equalsIgnoreCase("evo") && f2.exists())) {
                    return true;
                }
            }
            // Check internal llama-cpp lib folder
            File llamaCppDir = LlamaService.resolveLlamaCppLibDir();
            if (llamaCppDir != null && llamaCppDir.exists() && llamaCppDir.isDirectory()) {
                File f1 = new File(llamaCppDir, modelName + ".gguf");
                File f2 = new File(llamaCppDir, "evo.gguf");
                if (f1.exists() || (modelName.equalsIgnoreCase("evo") && f2.exists())) {
                    return true;
                }
            }
            // Check default Ollama folder
            File ollamaHomeModelsDir = new File(System.getProperty("user.home"), ".ollama/models");
            if (ollamaHomeModelsDir.exists() && ollamaHomeModelsDir.isDirectory()) {
                File f1 = new File(ollamaHomeModelsDir, modelName + ".gguf");
                File f2 = new File(ollamaHomeModelsDir, "evo.gguf");
                if (f1.exists() || (modelName.equalsIgnoreCase("evo") && f2.exists())) {
                    return true;
                }
            }
            // Check workspace source/models folder
            String codebasePath = getCodebasePath();
            if (codebasePath != null) {
                File sourceModelsDir = new File(codebasePath, "source/models");
                if (sourceModelsDir.exists() && sourceModelsDir.isDirectory()) {
                    File f1 = new File(sourceModelsDir, modelName + ".gguf");
                    File f2 = new File(sourceModelsDir, "evo.gguf");
                    if (f1.exists() || (modelName.equalsIgnoreCase("evo") && f2.exists())) {
                        return true;
                    }
                }
            }
            // Check codebase dist folder
            if (codebasePath != null) {
                File distDir = new File(codebasePath, "dist");
                if (distDir.exists() && distDir.isDirectory()) {
                    File[] subdirs = distDir.listFiles(File::isDirectory);
                    if (subdirs != null) {
                        for (File subdir : subdirs) {
                            if (subdir.getName().equalsIgnoreCase(modelName) || subdir.getName().startsWith("evo-")) {
                                File f = new File(subdir, "evo.gguf");
                                if (f.exists()) {
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
            // Check forge-output folder
            String workspacePathStr = getWorkspacePath();
            if (workspacePathStr != null && !workspacePathStr.isEmpty()) {
                File forgeOutputDir = new File(workspacePathStr, "forge-output");
                if (forgeOutputDir.exists() && forgeOutputDir.isDirectory()) {
                    File[] subdirs = forgeOutputDir.listFiles(File::isDirectory);
                    if (subdirs != null) {
                        for (File subdir : subdirs) {
                            if (subdir.getName().equalsIgnoreCase(modelName) || subdir.getName().startsWith("evo-")) {
                                File f = new File(subdir, "evo.gguf");
                                if (f.exists()) {
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
            return false;
        };

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

        // 5. Scan local exported and default Ollama models to ensure forged evo models are ALWAYS listed
        try {
            // Check internal controller models folder first
            File controllerModelsDir = LlamaService.resolveControllerModelsDir();
            if (controllerModelsDir != null && controllerModelsDir.exists() && controllerModelsDir.isDirectory()) {
                File[] files = controllerModelsDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".gguf") && name.toLowerCase().contains("evo"));
                if (files != null) {
                    for (File f : files) {
                        String filename = f.getName();
                        String modelName = filename.substring(0, filename.length() - 5);
                        if (models.stream().noneMatch(i -> i.getName().equalsIgnoreCase(modelName))) {
                            AIProvider item = factory.createAIProvider();
                            item.setName(modelName);
                            item.setLocal(true);
                            item.setUrl(ollamaUrl);
                            item.setFormat("ollama");
                            if (ollamaOnline) {
                                item.setState("NA");
                                item.setStateDescription("Model in controller models folder - GGUF exists on disk but is not registered in Ollama.");
                            } else {
                                item.setState("ERR");
                                item.setStateDescription("Ollama server offline");
                            }
                            models.add(item);
                        }
                    }
                }
            }

            // Check internal llama-cpp lib folder
            File llamaCppDir = LlamaService.resolveLlamaCppLibDir();
            if (llamaCppDir != null && llamaCppDir.exists() && llamaCppDir.isDirectory()) {
                File[] files = llamaCppDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".gguf") && name.toLowerCase().contains("evo"));
                if (files != null) {
                    for (File f : files) {
                        String filename = f.getName();
                        String modelName = filename.substring(0, filename.length() - 5);
                        if (models.stream().noneMatch(i -> i.getName().equalsIgnoreCase(modelName))) {
                            AIProvider item = factory.createAIProvider();
                            item.setName(modelName);
                            item.setLocal(true);
                            item.setUrl(ollamaUrl);
                            item.setFormat("ollama");
                            if (ollamaOnline) {
                                item.setState("NA");
                                item.setStateDescription("Model in llama-cpp lib folder - GGUF exists on disk but is not registered in Ollama.");
                            } else {
                                item.setState("ERR");
                                item.setStateDescription("Ollama server offline");
                            }
                            models.add(item);
                        }
                    }
                }
            }

            // Check default Ollama folder
            File ollamaHomeModelsDir = new File(System.getProperty("user.home"), ".ollama/models");
            if (ollamaHomeModelsDir.exists() && ollamaHomeModelsDir.isDirectory()) {
                File[] files = ollamaHomeModelsDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".gguf") && name.toLowerCase().contains("evo"));
                if (files != null) {
                    for (File f : files) {
                        String filename = f.getName();
                        String modelName = filename.substring(0, filename.length() - 5); // remove .gguf
                        if (models.stream().noneMatch(i -> i.getName().equalsIgnoreCase(modelName))) {
                            AIProvider item = factory.createAIProvider();
                            item.setName(modelName);
                            item.setLocal(true);
                            item.setUrl(ollamaUrl);
                            item.setFormat("ollama");
                            // Since we didn't find it in registered localModels, but it is on disk:
                            if (ollamaOnline) {
                                item.setState("NA");
                                item.setStateDescription("Model GGUF exists on disk but is not registered in Ollama. Self-healing will register it on first use.");
                            } else {
                                item.setState("ERR");
                                item.setStateDescription("Ollama server offline");
                            }
                            models.add(item);
                        }
                    }
                }
            }

            // Check workspace source/models folder
            String codebasePath = getCodebasePath();
            if (codebasePath != null) {
                File sourceModelsDir = new File(codebasePath, "source/models");
                if (sourceModelsDir.exists() && sourceModelsDir.isDirectory()) {
                    File[] files = sourceModelsDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".gguf") && name.toLowerCase().contains("evo"));
                    if (files != null) {
                        for (File f : files) {
                            String filename = f.getName();
                            String modelName = filename.substring(0, filename.length() - 5); // remove .gguf
                            if (models.stream().noneMatch(i -> i.getName().equalsIgnoreCase(modelName))) {
                                AIProvider item = factory.createAIProvider();
                                item.setName(modelName);
                                item.setLocal(true);
                                item.setUrl(ollamaUrl);
                                item.setFormat("ollama");
                                if (ollamaOnline) {
                                    item.setState("NA");
                                    item.setStateDescription("Model in source/models folder - GGUF exists on disk but is not registered in Ollama.");
                                } else {
                                    item.setState("ERR");
                                    item.setStateDescription("Ollama server offline");
                                }
                                models.add(item);
                            }
                        }
                    }
                }
            }

            // Check codebase dist folder
            if (codebasePath != null) {
                File distDir = new File(codebasePath, "dist");
                if (distDir.exists() && distDir.isDirectory()) {
                    File[] subdirs = distDir.listFiles(File::isDirectory);
                    if (subdirs != null) {
                        for (File subdir : subdirs) {
                            if (subdir.getName().startsWith("evo-") || subdir.getName().startsWith("forging-")) {
                                File ggufFile = new File(subdir, "evo.gguf");
                                if (ggufFile.exists()) {
                                    String modelName = subdir.getName().startsWith("evo-") ? subdir.getName() : "evo-" + subdir.getName().substring(8);
                                    if (models.stream().noneMatch(i -> i.getName().equalsIgnoreCase(modelName))) {
                                        AIProvider item = factory.createAIProvider();
                                        item.setName(modelName);
                                        item.setLocal(true);
                                        item.setUrl(ollamaUrl);
                                        item.setFormat("ollama");
                                        if (ollamaOnline) {
                                            item.setState("NA");
                                            item.setStateDescription("Exported forged model in dist folder - GGUF exists on disk but is not registered in Ollama.");
                                        } else {
                                            item.setState("ERR");
                                            item.setStateDescription("Ollama server offline");
                                        }
                                        models.add(item);
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Check forge-output folder and scan for both directories and .evo files
            List<File> forgeDirsToScan = new ArrayList<>();
            String workspacePathStr = getWorkspacePath();
            if (workspacePathStr != null && !workspacePathStr.isEmpty()) {
                forgeDirsToScan.add(new File(workspacePathStr, "forge-output"));
            }
            String codebasePathStr = getCodebasePath();
            if (codebasePathStr != null && !codebasePathStr.isEmpty()) {
                forgeDirsToScan.add(new File(codebasePathStr, "forge-output"));
            }
            forgeDirsToScan.add(new File(System.getProperty("user.dir"), "forge-output"));

            for (File forgeOutputDir : forgeDirsToScan) {
                if (forgeOutputDir.exists() && forgeOutputDir.isDirectory()) {
                    File[] allFiles = forgeOutputDir.listFiles();
                    if (allFiles != null) {
                        for (File f : allFiles) {
                            if (f.isFile() && f.getName().toLowerCase().endsWith(".evo")) {
                                String modelName = f.getName();
                                if (models.stream().noneMatch(i -> i.getName().equalsIgnoreCase(modelName))) {
                                    AIProvider item = factory.createAIProvider();
                                    item.setName(modelName);
                                    item.setLocal(true);
                                    item.setUrl(f.getAbsolutePath());
                                    item.setFormat("evo_native");
                                    item.setState("OK");
                                    item.setStateDescription("Canonical EVO Native Model Artifact (.evo)");
                                    models.add(item);
                                }
                            } else if (f.isDirectory()) {
                                File[] subEvos = f.listFiles((d, n) -> n.toLowerCase().endsWith(".evo"));
                                if (subEvos != null) {
                                    for (File subEvo : subEvos) {
                                        String modelName = subEvo.getName();
                                        if (models.stream().noneMatch(i -> i.getName().equalsIgnoreCase(modelName))) {
                                            AIProvider item = factory.createAIProvider();
                                            item.setName(modelName);
                                            item.setLocal(true);
                                            item.setUrl(subEvo.getAbsolutePath());
                                            item.setFormat("evo_native");
                                            item.setState("OK");
                                            item.setStateDescription("Canonical EVO Native Model Artifact (.evo)");
                                            models.add(item);
                                        }
                                    }
                                }

                                if (f.getName().startsWith("evo-")) {
                                    File ggufFile = new File(f, "evo.gguf");
                                    File weightsFile = new File(f, "weights.bin");
                                    File configJsonFile = new File(f, "config.json");
                                    if (ggufFile.exists() || (weightsFile.exists() && configJsonFile.exists())) {
                                        String modelName = f.getName();
                                        if (models.stream().noneMatch(i -> i.getName().equalsIgnoreCase(modelName))) {
                                            AIProvider item = factory.createAIProvider();
                                            item.setName(modelName);
                                            item.setLocal(true);
                                            item.setUrl(ollamaUrl);
                                            item.setFormat("ollama");
                                            if (ollamaOnline) {
                                                if (ggufFile.exists()) {
                                                    item.setState("NA");
                                                    item.setStateDescription("Exported forged model in forge-output folder - GGUF exists on disk but is not registered in Ollama.");
                                                } else {
                                                    item.setState("NA");
                                                    item.setStateDescription("Forged model artifact in forge-output folder - weights and config exist, but GGUF is not exported yet.");
                                                }
                                            } else {
                                                item.setState("ERR");
                                                item.setStateDescription("Ollama server offline");
                                            }
                                            models.add(item);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            // Silent fallback
        }

        // 6. Final verification and status update for ALL local models (including EMF models) in the list
        for (AIProvider item : models) {
            if (item.isLocal() && !item.getName().startsWith("demo/")) {
                if (!ollamaOnline) {
                    item.setState("ERR");
                    item.setStateDescription("Ollama server offline");
                } else {
                    boolean foundInOllama = localNames.contains(item.getName().toLowerCase());
                    if (foundInOllama) {
                        item.setState("OK");
                        item.setStateDescription("Model is registered and ready in Ollama.");
                    } else {
                        boolean existsOnDisk = checkGgufOnDisk.apply(item.getName(), "evo");
                        if (existsOnDisk) {
                            item.setState("NA");
                            item.setStateDescription("Model GGUF exists on disk but is not registered in Ollama. Self-healing will register it on first use.");
                        } else {
                            // Check if it is an unexported artifact
                            String wp = getWorkspacePath();
                            boolean isArtifact = false;
                            if (wp != null && !wp.isEmpty()) {
                                File dir = new File(wp, "forge-output/" + item.getName());
                                if (dir.exists() && dir.isDirectory() && new File(dir, "weights.bin").exists() && new File(dir, "config.json").exists()) {
                                    isArtifact = true;
                                }
                            }
                            if (isArtifact) {
                                item.setState("NA");
                                item.setStateDescription("Forged model artifact - weights and config exist. Ready to be exported.");
                            } else {
                                item.setState("ERR");
                                item.setStateDescription("Model NOT found in Ollama and GGUF is missing.");
                            }
                        }
                    }
                }
            }
        }

        // Reconcile duplicate discovery/registration representations into one
        // logical model and drop identities that were explicitly unregistered.
        models = applyIdentityConsistency(models, orchestrator, ollamaOnline, localNames, includeSuppressed);

        // Sort models descending by rating, with a secondary sort alphabetically by name
        models.sort((m1, m2) -> {
            int comp = Integer.compare(m2.getRating(), m1.getRating());
            if (comp != 0) {
                return comp;
            }
            String n1 = m1.getName() != null ? m1.getName() : "";
            String n2 = m2.getName() != null ? m2.getName() : "";
            return n1.compareToIgnoreCase(n2);
        });

        return models;
    }

    /**
     * Collapses multiple representations of the same model (for example
     * {@code model}, {@code model:latest} and a discovered artifact entry) into
     * one logical row, and hides identities that were explicitly unregistered
     * unless they still exist externally.
     */
    private List<AIProvider> applyIdentityConsistency(List<AIProvider> models, Orchestrator orchestrator,
                                                      boolean ollamaOnline, Set<String> localNames,
                                                      boolean includeSuppressed) {
        Map<String, AIProvider> byIdentity = new LinkedHashMap<>();
        for (AIProvider item : models) {
            if (item == null || item.getName() == null || item.getName().isBlank()) {
                continue;
            }
            ModelIdentity identity = ModelIdentityResolver.resolve(orchestrator, item);
            AIProvider existing = byIdentity.get(identity.getCanonicalId());
            if (existing == null) {
                byIdentity.put(identity.getCanonicalId(), item);
            } else {
                mergeModelEntries(existing, item);
            }
        }

        ModelStateStore store = getModelStateStore();
        List<AIProvider> result = new ArrayList<>();
        for (AIProvider item : byIdentity.values()) {
            ModelIdentity identity = ModelIdentityResolver.resolve(orchestrator, item);
            if (store.isSuppressed(identity.getCanonicalId())) {
                if (!includeSuppressed) {
                    // Removed models must never reach AI Commander, Forge or any
                    // selection combo.
                    continue;
                }
                boolean registered = isRegistered(orchestrator, identity);
                boolean externallyPresent = isExternallyPresent(identity, item, ollamaOnline, localNames);
                if (!registered && !externallyPresent) {
                    continue;
                }
                if (!registered) {
                    String description = item.getStateDescription() != null ? item.getStateDescription() : "";
                    if (!description.startsWith("[Unregistered]")) {
                        item.setStateDescription("[Unregistered] " + description);
                    }
                }
            }
            result.add(item);
        }
        return result;
    }

    private boolean isExternallyPresent(ModelIdentity identity, AIProvider item,
                                        boolean ollamaOnline, Set<String> localNames) {
        if (identity.isOllama()) {
            if (!ollamaOnline || localNames == null) {
                return false;
            }
            String runtime = identity.getRuntimeName().toLowerCase(Locale.ROOT);
            String base = runtime.endsWith(":latest")
                    ? runtime.substring(0, runtime.length() - ":latest".length()) : runtime;
            return localNames.contains(runtime) || localNames.contains(base);
        }
        if (identity.isArtifactBacked()) {
            if (identity.getArtifactPath() != null
                    && java.nio.file.Files.exists(identity.getArtifactPath())) {
                return true;
            }
            String url = item.getUrl();
            if (url != null && !url.startsWith("http")) {
                try {
                    return java.nio.file.Files.exists(java.nio.file.Paths.get(url));
                } catch (Exception ignored) {
                    return false;
                }
            }
            return false;
        }
        return identity.getKind() == ModelIdentity.Kind.REMOTE;
    }

    private void mergeModelEntries(AIProvider target, AIProvider source) {
        if (!"OK".equals(target.getState()) && "OK".equals(source.getState())) {
            target.setState("OK");
            target.setStateDescription(source.getStateDescription());
        } else if ((target.getState() == null || target.getState().isBlank())
                && source.getState() != null) {
            target.setState(source.getState());
            target.setStateDescription(source.getStateDescription());
        }
        if (target.getRating() == 0 && source.getRating() != 0) {
            target.setRating(source.getRating());
            target.setRatingAnalyze(source.getRatingAnalyze());
            target.setRatingChat(source.getRatingChat());
            target.setRatingProgramming(source.getRatingProgramming());
        }
        boolean targetIsHttp = target.getUrl() == null || target.getUrl().startsWith("http");
        boolean sourceIsPath = source.getUrl() != null && !source.getUrl().startsWith("http");
        if (targetIsHttp && sourceIsPath) {
            target.setUrl(source.getUrl());
            if (source.getFormat() != null) {
                target.setFormat(source.getFormat());
            }
            target.setLocal(true);
        }
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
            SourceDiscoveryRequest fastReq = new SourceDiscoveryRequest();
            fastReq.setFastMode(true);
            cachedDiscoveryResult = resolver.discover(fastReq);

            // Asynchronously build full inventory in low-priority background thread
            Thread inventoryThread = new Thread(() -> {
                try {
                    SourceDiscoveryRequest fullReq = new SourceDiscoveryRequest();
                    SourceDiscoveryResult fullResult = resolver.discover(fullReq);
                    synchronized (ProjectModelManager.this) {
                        cachedDiscoveryResult = fullResult;
                    }
                } catch (Throwable t) {
                    // Ignore background inventory build errors
                }
            }, "WorkspaceInventoryDiscovery");
            inventoryThread.setDaemon(true);
            inventoryThread.setPriority(Thread.MIN_PRIORITY);
            inventoryThread.start();
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

	/**
	 * Returns the codebase folder/repository path where the real source code is.
	 * This method attempts multiple robust strategies to locate the real source code repository,
	 * including discovering workspace source roots, querying active Eclipse project locations,
	 * searching local Git repositories, checking environment variables, and traversing up
	 * from the user directory.
	 *
	 * @return the absolute path of the codebase folder/repository, or null if it cannot be determined.
	 */
	public static String getCodebasePath() {
		return eu.kalafatic.evolution.controller.resource.ResourceManager.getInstance()
				.getPath(eu.kalafatic.evolution.controller.resource.EvoPath.EVO_GIT_REPOSITORY)
				.toString();
	}

	/**
	 * Returns the codebase folder/repository path where the real source code is (instance method).
	 *
	 * @return the absolute path of the codebase folder/repository, or null if it cannot be determined.
	 */
	public String getCodebaseFolderPath() {
		return getCodebasePath();
	}

	/**
	 * Returns the workspace folder path.
	 * This method returns the canonical workspace root folder.
	 *
	 * @return the absolute path of the workspace folder.
	 */
	public static String getWorkspacePath() {
		return eu.kalafatic.evolution.controller.resource.ResourceManager.getInstance()
				.getPath(eu.kalafatic.evolution.controller.resource.EvoPath.WORKSPACE)
				.toString();
	}

	/**
	 * Returns the workspace folder path (instance method).
	 *
	 * @return the absolute path of the workspace folder, or null if it cannot be determined.
	 */
	public String getWorkspaceFolderPath() {
		return getWorkspacePath();
	}

    public static String migratePath(String path) {
        if (path == null) return null;
        String userHomeNorm = System.getProperty("user.home").replace("\\", "/");

        // Normalize backslashes/forwardslashes to simplify comparisons
        String normalized = path.replace("\\", "/");

        // Strip hardcoded user prefix if present
        if (normalized.matches("(?i)[a-z]:/users/[^/]+/.*")) {
            normalized = userHomeNorm + normalized.substring(normalized.indexOf('/', 10));
        }

        boolean isWinSlash = path.contains("\\");

        if (normalized.contains("supervisor") || normalized.contains("self-dev-run")) {
            String runtimeBase = userHomeNorm + "/workspace/runtime";
            String migrated;
            if (normalized.endsWith("/sources") || normalized.endsWith("/source") || normalized.contains("/sources/") || normalized.contains("/source/")) {
                migrated = runtimeBase + "/sources";
            } else if (normalized.endsWith("/builds") || normalized.endsWith("/build") || normalized.endsWith("/bin") || normalized.contains("/builds/") || normalized.contains("/build/") || normalized.contains("/bin/")) {
                migrated = runtimeBase + "/builds";
            } else if (normalized.endsWith("/export") || normalized.endsWith("/exports") || normalized.contains("/export/")) {
                migrated = runtimeBase + "/exports";
            } else {
                migrated = runtimeBase;
            }
            return isWinSlash ? migrated.replace("/", "\\") : migrated;
        }

        return path;
    }
}
