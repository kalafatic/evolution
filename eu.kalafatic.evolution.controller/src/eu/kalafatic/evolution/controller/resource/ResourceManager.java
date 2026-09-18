package eu.kalafatic.evolution.controller.resource;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import eu.kalafatic.evolution.controller.manager.ProjectModelManager;
import eu.kalafatic.evolution.controller.log.Log;
import eu.kalafatic.evolution.model.orchestration.AIProvider;
import eu.kalafatic.evolution.model.orchestration.Git;
import eu.kalafatic.evolution.model.orchestration.Ollama;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.ServerSettings;
import eu.kalafatic.evolution.model.orchestration.SupervisorSettings;

/**
 * Single centralized access, resolution and discovery layer above the EMF model for the entire EVO platform.
 * <p>
 * **EMF is the single source of truth and persistent storage for EVO configuration.**
 * ResourceManager wraps and interprets EMF configuration, centralizing all path resolutions, URL/port
 * calculations, service definitions, repository management, and model/dataset discovery.
 */
public class ResourceManager {

    private static final ResourceManager INSTANCE = new ResourceManager();

    private Orchestrator orchestrator;

    private ResourceManager() {
    }

    public static ResourceManager getInstance() {
        return INSTANCE;
    }

    /**
     * Binds or updates the active EMF Orchestrator model instance.
     *
     * @param orchestrator The authoritative EMF Orchestrator model.
     */
    public synchronized void setOrchestrator(Orchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    /**
     * Gets the active EMF Orchestrator model instance.
     * If not explicitly set, attempts to load/initialize via ProjectModelManager.
     *
     * @return The active Orchestrator EMF model instance.
     */
    public synchronized Orchestrator getOrchestrator() {
        if (orchestrator == null) {
            ProjectModelManager pmm = ProjectModelManager.getInstance();
            if (pmm != null) {
                Path userHome = Paths.get(System.getProperty("user.home")).toAbsolutePath().normalize();
                Path defaultEvoGitRepo = userHome.resolve("git").resolve("evolution");
                File file = defaultEvoGitRepo.resolve("data/orchestrator.xml").toFile();
                if (!file.exists()) {
                    file = defaultEvoGitRepo.resolve("orchestrator.xml").toFile();
                }
                if (file.exists()) {
                    try {
                        org.eclipse.emf.common.util.URI uri = org.eclipse.emf.common.util.URI.createFileURI(file.getAbsolutePath());
                        orchestrator = (Orchestrator) pmm.loadModel(uri);
                    } catch (Exception e) {
                        Log.log("[ResourceManager] Could not load EMF orchestrator from file: " + e.getMessage());
                    }
                }
                if (orchestrator == null) {
                    orchestrator = pmm.createOrchestrator("default", "EVO Default");
                }
            }
        }
        return orchestrator;
    }

    /**
     * Re-initializes cached state and synchronizes with EMF model.
     */
    public synchronized void refresh() {
        Log.log("[ResourceManager] Refreshing resource states against authoritative EMF model.");
    }

    // =========================================================================
    // 1. CENTRALIZED PATH RESOLUTION & ACCESSORS
    // =========================================================================

    /**
     * Validates that the given path resides strictly under allowed canonical roots:
     * 1) Git repositories: ${user.home}/git/
     * 2) Persistent workspace or runtime: ${user.home}/workspace/
     *
     * Fails fast if the path is outside these roots.
     */
    public Path validateCanonicalPath(Path path) {
        if (path == null) {
            throw new IllegalArgumentException("Path cannot be null");
        }
        Path normalized = path.toAbsolutePath().normalize();
        Path userHome = Paths.get(System.getProperty("user.home")).toAbsolutePath().normalize();
        Path gitRoot = userHome.resolve("git");
        Path workspaceRoot = userHome.resolve("workspace");

        if (!normalized.startsWith(gitRoot) && !normalized.startsWith(workspaceRoot)) {
            throw new IllegalArgumentException("Path [" + normalized + "] violates canonical filesystem rules. Allowed roots: " + gitRoot + " and " + workspaceRoot);
        }
        return normalized;
    }

    public Path getEvoGitRepository() {
        return getPath(EvoPath.EVO_GIT_REPOSITORY);
    }

    public Path getEvoSourceReactor() {
        return getEvoGitRepository();
    }

    public Path getEvoSource() {
        return getEvoGitRepository();
    }

    public Path getEvoReactor() {
        return getEvoGitRepository();
    }

    public Path getEvoBuildOutput() {
        return getEvoGitRepository();
    }

    public Path getEvoExport() {
        return getEvoGitRepository();
    }

    public Path getSupervisorSource() {
        return getPath(EvoPath.SUPERVISOR_SOURCE);
    }

    public Path getSupervisorRuntime() {
        return getPath(EvoPath.SUPERVISOR_RUNTIME);
    }

    public Path getGenome() {
        return getPath(EvoPath.GENOME);
    }

    /**
     * Resolves the canonical root path for the specified semantic EvoPath type.
     * Guarantees deterministic path resolution strictly within canonical roots.
     *
     * @param pathType The semantic path category.
     * @return The resolved absolute Path on disk.
     */
    public Path getPath(EvoPath pathType) {
        if (pathType == null) {
            return getPath(EvoPath.EVO_ROOT);
        }

        Path userHome = Paths.get(System.getProperty("user.home")).toAbsolutePath().normalize();
        Path gitRoot = userHome.resolve("git");
        Path defaultEvoGitRepo = gitRoot.resolve("evolution");
        Path workspaceRoot = userHome.resolve("workspace");
        Path runtimeRoot = workspaceRoot.resolve("runtime");

        Path resolvedPath;
        switch (pathType) {
            case EVO_GIT_REPOSITORY:
            case EVO_ROOT:
            case PROJECT_ROOT: {
                Orchestrator orch = getOrchestrator();
                String source = "DEFAULT";
                if (orch != null && orch.getGit() != null && orch.getGit().getLocalPath() != null && !orch.getGit().getLocalPath().trim().isEmpty()) {
                    Path configured = Paths.get(expandVariables(orch.getGit().getLocalPath().trim())).toAbsolutePath().normalize();
                    resolvedPath = configured;
                    source = "EMF";
                } else {
                    resolvedPath = defaultEvoGitRepo;
                }
                Log.log("[RESOURCE][EVO_REPOSITORY] source=" + source + " path=" + resolvedPath);
                break;
            }

            case EVO_SOURCE_REACTOR:
            case SOURCE_ROOT:
            case BUILD_ROOT:
            case EXPORT_ROOT: {
                resolvedPath = getPath(EvoPath.EVO_GIT_REPOSITORY);
                break;
            }

            case WORKSPACE:
                resolvedPath = workspaceRoot;
                break;

            case RUNTIME_ROOT:
                resolvedPath = runtimeRoot;
                break;

            case SUPERVISOR_SOURCE: {
                Orchestrator orch = getOrchestrator();
                if (orch != null && orch.getSupervisorSettings() != null && orch.getSupervisorSettings().getSourcePath() != null) {
                    Path configured = resolvePath(getPath(EvoPath.EVO_ROOT), orch.getSupervisorSettings().getSourcePath());
                    if (configured.toFile().exists() && new File(configured.toFile(), "pom.xml").exists()) {
                        resolvedPath = configured;
                        break;
                    }
                }
                resolvedPath = getPath(EvoPath.EVO_GIT_REPOSITORY).resolve("eu.kalafatic.evolution.supervisor");
                break;
            }

            case SUPERVISOR_RUNTIME: {
                Orchestrator orch = getOrchestrator();
                if (orch != null && orch.getSupervisorSettings() != null && orch.getSupervisorSettings().getExecutablePath() != null) {
                    resolvedPath = resolvePath(getPath(EvoPath.EVO_ROOT), orch.getSupervisorSettings().getExecutablePath());
                } else {
                    resolvedPath = runtimeRoot.resolve("instances");
                }
                break;
            }

            case GENOME:
                resolvedPath = defaultEvoGitRepo.resolve("eu.kalafatic.evolution.selfdev.genome");
                break;

            case FORGE_INPUT:
                resolvedPath = workspaceRoot.resolve("forged").resolve("input");
                break;

            case FORGE_OUTPUT:
                resolvedPath = workspaceRoot.resolve("forged").resolve("output");
                break;

            case MODELS:
                resolvedPath = workspaceRoot.resolve("models");
                break;

            case DATASETS:
                resolvedPath = workspaceRoot.resolve("datasets");
                break;

            default:
                resolvedPath = defaultEvoGitRepo;
                break;
        }

        return validateCanonicalPath(resolvedPath);
    }

    /**
     * Resolves a configured path string against a semantic base EvoPath type.
     *
     * @param baseType The semantic base path type.
     * @param configuredPath The path string (absolute or relative).
     * @return The resolved absolute Path.
     */
    public Path resolvePath(EvoPath baseType, String configuredPath) {
        Path base = getPath(baseType);
        return resolvePath(base, configuredPath);
    }

    /**
     * Resolves a configured path string against EVO_ROOT.
     *
     * @param configuredPath The path string.
     * @return The resolved absolute Path.
     */
    public Path resolvePath(String configuredPath) {
        return resolvePath(EvoPath.EVO_ROOT, configuredPath);
    }

    /**
     * Resolves a configured Path against a semantic base path.
     * Absolute paths MUST NOT have roots prepended to them.
     *
     * @param semanticBase The base directory.
     * @param configuredPath The Path object to resolve.
     * @return The resolved, normalized absolute Path.
     */
    public Path resolvePath(Path semanticBase, Path configuredPath) {
        if (configuredPath == null) {
            Path base = semanticBase != null ? semanticBase.toAbsolutePath().normalize() : getPath(EvoPath.EVO_ROOT);
            return validateCanonicalPath(base);
        }
        if (configuredPath.isAbsolute()) {
            return validateCanonicalPath(configuredPath);
        }
        return resolvePath(semanticBase, configuredPath.toString());
    }

    /**
     * Resolves a configured path against a semantic base path.
     * Absolute paths MUST NOT have roots prepended to them.
     *
     * @param semanticBase The base directory.
     * @param configuredPath The path string to resolve.
     * @return The resolved, normalized absolute Path.
     */
    public Path resolvePath(Path semanticBase, String configuredPath) {
        if (configuredPath == null || configuredPath.trim().isEmpty()) {
            Path base = semanticBase != null ? semanticBase.toAbsolutePath().normalize() : getPath(EvoPath.EVO_ROOT);
            return validateCanonicalPath(base);
        }

        String expanded = expandVariables(configuredPath.trim());
        Path p = Paths.get(expanded);

        Path resolved;
        if (p.isAbsolute()) {
            resolved = p.toAbsolutePath().normalize();
        } else {
            Path base = semanticBase != null ? semanticBase.toAbsolutePath().normalize() : getPath(EvoPath.EVO_ROOT);
            resolved = base.resolve(p).toAbsolutePath().normalize();
        }

        Log.log("[RESOURCE][RESOLVE_PATH] base=" + semanticBase + " rule=" + configuredPath + " resolved=" + resolved);
        return validateCanonicalPath(resolved);
    }

    private String expandVariables(String rawPath) {
        if (rawPath == null) return "";
        String userHome = System.getProperty("user.home");
        String gitEvolution = Paths.get(userHome, "git", "evolution").toString();
        String workspace = Paths.get(userHome, "workspace").toString();

        String res = rawPath;
        res = res.replace("${EVO_ROOT}", gitEvolution);
        res = res.replace("${WORKSPACE}", workspace);
        res = res.replace("${USER_HOME}", userHome);
        res = res.replace("${user.home}", userHome);
        return res;
    }

    // =========================================================================
    // 2. TARGET PLATFORM & PRODUCT DEFINITION
    // =========================================================================

    public TargetPlatform getTargetPlatform() {
        Orchestrator orch = getOrchestrator();
        String sysOs = System.getProperty("evo.target.os");
        if (sysOs == null || sysOs.trim().isEmpty()) {
            if (orch != null && orch.getSupervisorSettings() != null && orch.getSupervisorSettings().getSettings() != null) {
                String settings = orch.getSupervisorSettings().getSettings();
                if (settings.contains("os=")) {
                    try {
                        String osStr = settings.substring(settings.indexOf("os=") + 3).trim();
                        if (osStr.contains(" ")) osStr = osStr.substring(0, osStr.indexOf(" "));
                        sysOs = osStr;
                    } catch (Exception e) {}
                }
            }
            if (sysOs == null || sysOs.trim().isEmpty()) {
                sysOs = System.getProperty("os.name");
            }
        }
        sysOs = sysOs.toLowerCase();
        if (sysOs.contains("win")) {
            return new TargetPlatform("win32", "win32", "x86_64", "zip", "-Pwindows");
        } else {
            return new TargetPlatform("linux", "gtk", "x86_64", "tar.gz", "-Plinux");
        }
    }

    public ProductDefinition getProductDefinition() {
        Path repoRoot = getEvoGitRepository();
        Path repoModuleDir = repoRoot.resolve("eu.kalafatic.evolution.repository");
        Path productFile = repoModuleDir.resolve("evolution.product");
        File pFile = productFile.toFile().exists() ? productFile.toFile() : null;

        String productId = "evolution";
        TargetPlatform tp = getTargetPlatform();
        String launcherName = tp.isWindows() ? "evo.exe" : "evo";
        String rootFolder = "evolution";
        String repoModuleName = "eu.kalafatic.evolution.repository";

        if (pFile != null) {
            try {
                String content = java.nio.file.Files.readString(productFile);
                int uidIdx = content.indexOf("uid=\"");
                if (uidIdx != -1) {
                    int start = uidIdx + 5;
                    int end = content.indexOf("\"", start);
                    if (end != -1) productId = content.substring(start, end);
                }
                int launcherIdx = content.indexOf("<launcher");
                if (launcherIdx != -1) {
                    int nameIdx = content.indexOf("name=\"", launcherIdx);
                    if (nameIdx != -1) {
                        int start = nameIdx + 6;
                        int end = content.indexOf("\"", start);
                        if (end != -1) launcherName = content.substring(start, end);
                    }
                }
            } catch (Exception e) {
            }
        }

        return new ProductDefinition(productId, launcherName, rootFolder, repoModuleName, pFile);
    }

    // =========================================================================
    // 3. URL / IP / PORT CENTRALIZATION & SERVICE ENUMERATION
    // =========================================================================

    /**
     * Enumerates all configured EVO platform services directly from authoritative EMF state.
     *
     * @return List of configured EvoService descriptors for UI and system components.
     */
    public List<EvoService> getServices() {
        List<EvoService> services = new ArrayList<>();
        Orchestrator orch = getOrchestrator();

        // 1. SUPERVISOR Service
        int supervisorPort = 48080;
        String supervisorHost = "127.0.0.1";
        if (orch != null && orch.getSupervisorSettings() != null) {
            SupervisorSettings sup = orch.getSupervisorSettings();
            if (sup.getSettings() != null && sup.getSettings().contains("port=")) {
                try {
                    String pStr = sup.getSettings().substring(sup.getSettings().indexOf("port=") + 5).trim();
                    if (pStr.contains(" ")) pStr = pStr.substring(0, pStr.indexOf(" "));
                    supervisorPort = Integer.parseInt(pStr);
                } catch (Exception e) {
                }
            }
        }
        services.add(new EvoService("SUPERVISOR", "Supervisor Service", supervisorHost, supervisorPort, true, "EMF.supervisorSettings"));

        // 2. SERVER / ORCHESTRATOR Service
        int serverPort = 48081;
        String serverHost = "127.0.0.1";
        boolean serverEnabled = true;
        if (orch != null && orch.getServerSettings() != null) {
            ServerSettings srv = orch.getServerSettings();
            if (srv.getPort() > 0) serverPort = srv.getPort();
            serverEnabled = srv.isAutoStart();
        }
        services.add(new EvoService("SERVER", "Orchestrator Control Server", serverHost, serverPort, serverEnabled, "EMF.serverSettings"));

        // 3. INFERENCE / OLLAMA Service
        int ollamaPort = 11434;
        String ollamaHost = "127.0.0.1";
        if (orch != null && orch.getOllama() != null) {
            Ollama ollama = orch.getOllama();
            if (ollama.getUrl() != null && !ollama.getUrl().isEmpty()) {
                try {
                    URL url = new URI(ollama.getUrl()).toURL();
                    ollamaHost = url.getHost();
                    if (url.getPort() > 0) ollamaPort = url.getPort();
                } catch (Exception e) {
                }
            }
        }
        services.add(new EvoService("INFERENCE", "LLM Inference Engine (Ollama/llama.cpp)", ollamaHost, ollamaPort, true, "EMF.ollama"));

        // 4. MCP Server
        int mcpPort = 48082;
        String mcpHost = "127.0.0.1";
        if (orch != null && orch.getMcpServerUrl() != null && !orch.getMcpServerUrl().isEmpty()) {
            try {
                URL url = new URI(orch.getMcpServerUrl()).toURL();
                mcpHost = url.getHost();
                if (url.getPort() > 0) mcpPort = url.getPort();
            } catch (Exception e) {
            }
        }
        services.add(new EvoService("MCP", "Model Context Protocol Server", mcpHost, mcpPort, true, "EMF.mcpServerUrl"));

        // 5. DEVELOP Autonomous Coding Agent Server
        int developPort = 48083;
        services.add(new EvoService("DEVELOP", "EVO Autonomous Coding Agent", "127.0.0.1", developPort, true, "EMF.develop"));

        return services;
    }

    /**
     * Retrieves a service descriptor by ID.
     *
     * @param serviceId The service ID (e.g., "SUPERVISOR", "SERVER", "INFERENCE", "MCP", "DEVELOP").
     * @return The EvoService descriptor, or null if not found.
     */
    public EvoService getService(String serviceId) {
        if (serviceId == null) return null;
        for (EvoService s : getServices()) {
            if (s.getId().equalsIgnoreCase(serviceId)) {
                return s;
            }
        }
        return null;
    }

    public Optional<EvoService> findService(String id) {
        return Optional.ofNullable(getService(id));
    }

    public Optional<String> getIp(String serviceId) {
        EvoService svc = getService(serviceId);
        return svc != null ? Optional.ofNullable(svc.getHost()) : Optional.empty();
    }

    public OptionalInt getPort(String serviceId) {
        EvoService svc = getService(serviceId);
        return svc != null && svc.getPort() > 0 ? OptionalInt.of(svc.getPort()) : OptionalInt.empty();
    }

    public Optional<URI> getUrl(String serviceId) {
        EvoService svc = getService(serviceId);
        if (svc != null && svc.getUrl() != null) {
            try {
                return Optional.of(new URI(svc.getUrl()));
            } catch (Exception e) {
            }
        }
        return Optional.empty();
    }

    /**
     * Validates configured service ports for collisions, invalid ranges, or malformed URLs.
     *
     * @return List of error/warning messages. Empty if all ports are valid.
     */
    public List<String> validatePorts() {
        List<String> issues = new ArrayList<>();
        Map<Integer, String> portMap = new HashMap<>();

        for (EvoService service : getServices()) {
            int port = service.getPort();
            if (port <= 0 || port > 65535) {
                issues.add("Service " + service.getName() + " has invalid port number: " + port);
                continue;
            }
            if (portMap.containsKey(port)) {
                issues.add("Port conflict detected: Port " + port + " is assigned to both " +
                        portMap.get(port) + " and " + service.getName());
            } else {
                portMap.put(port, service.getName());
            }
        }
        return issues;
    }

    // =========================================================================
    // 4. REPOSITORY ENUMERATION & ACCESS
    // =========================================================================

    /**
     * Enumerates configured and discovered Git repositories.
     *
     * @return List of EvoRepository descriptors.
     */
    public List<EvoRepository> getRepositories() {
        List<EvoRepository> repos = new ArrayList<>();
        Orchestrator orch = getOrchestrator();

        // 1. Primary Evolution Repository
        if (orch != null && orch.getGit() != null) {
            Git git = orch.getGit();
            Path path = resolvePath(EvoPath.EVO_ROOT, git.getLocalPath());
            repos.add(new EvoRepository("EVOLUTION", "EVO Core Repository", path, git.getRepositoryUrl(), git.getBranch()));
        } else {
            Path path = getPath(EvoPath.EVO_ROOT);
            repos.add(new EvoRepository("EVOLUTION", "EVO Core Repository", path, "", "main"));
        }

        // 2. Supervisor Repository
        if (orch != null && orch.getSupervisorSettings() != null && orch.getSupervisorSettings().getGit() != null) {
            Git supGit = orch.getSupervisorSettings().getGit();
            Path supPath = resolvePath(EvoPath.SUPERVISOR_SOURCE, supGit.getLocalPath());
            repos.add(new EvoRepository("SUPERVISOR", "EVO Supervisor Repository", supPath, supGit.getRepositoryUrl(), supGit.getBranch()));
        }

        // 3. Discovered local Git repositories
        List<String> localPaths = ProjectModelManager.getInstance().getAvailableLocalRepositories();
        for (String p : localPaths) {
            Path path = Paths.get(p);
            String name = path.getFileName() != null ? path.getFileName().toString() : p;
            if (repos.stream().noneMatch(r -> r.getLocalPath().equals(path))) {
                repos.add(new EvoRepository(name.toUpperCase(), name, path, "", "main"));
            }
        }

        return repos;
    }

    /**
     * Retrieves a repository descriptor by ID or name.
     *
     * @param idOrName Repository ID or name.
     * @return EvoRepository descriptor or null if not found.
     */
    public EvoRepository getRepository(String idOrName) {
        if (idOrName == null) return null;
        for (EvoRepository r : getRepositories()) {
            if (r.getId().equalsIgnoreCase(idOrName) || r.getName().equalsIgnoreCase(idOrName)) {
                return r;
            }
        }
        return null;
    }

    // =========================================================================
    // 5. MODELS & DATASETS ENUMERATION
    // =========================================================================

    /**
     * Enumerates available LLM models from authoritative EMF state and local model providers.
     *
     * @return List of AIProvider model descriptors.
     */
    public List<AIProvider> getModels() {
        return ProjectModelManager.getInstance().getAllModels(getOrchestrator());
    }

    /**
     * Enumerates configured and discovered dataset locations.
     *
     * @return List of Path instances pointing to valid datasets.
     */
    public List<Path> getDatasets() {
        List<Path> datasets = new ArrayList<>();
        Path dataDir = getPath(EvoPath.DATASETS);
        if (dataDir.toFile().exists() && dataDir.toFile().isDirectory()) {
            File[] files = dataDir.toFile().listFiles((d, name) -> name.endsWith(".evodata") || name.endsWith(".jsonl") || name.endsWith(".csv") || name.endsWith(".txt"));
            if (files != null) {
                for (File f : files) {
                    datasets.add(f.toPath().toAbsolutePath().normalize());
                }
            }
        }
        return datasets;
    }
}
