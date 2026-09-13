package eu.kalafatic.evolution.controller.resource;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
                String codebase = ProjectModelManager.getCodebasePath();
                if (codebase != null) {
                    File file = new File(codebase, "data/orchestrator.xml");
                    if (!file.exists()) {
                        file = new File(codebase, "orchestrator.xml");
                    }
                    if (file.exists()) {
                        try {
                            org.eclipse.emf.common.util.URI uri = org.eclipse.emf.common.util.URI.createFileURI(file.getAbsolutePath());
                            orchestrator = (Orchestrator) pmm.loadModel(uri);
                        } catch (Exception e) {
                            Log.log("[ResourceManager] Could not load EMF orchestrator from file: " + e.getMessage());
                        }
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
    // 1. CENTRALIZED PATH RESOLUTION
    // =========================================================================

    /**
     * Resolves the canonical root path for the specified semantic EvoPath type.
     *
     * @param pathType The semantic path category.
     * @return The resolved absolute Path on disk.
     */
    public Path getPath(EvoPath pathType) {
        if (pathType == null) {
            return getPath(EvoPath.EVO_ROOT);
        }

        String codebase = ProjectModelManager.getCodebasePath();
        Path evoRoot = codebase != null ? Paths.get(codebase).toAbsolutePath().normalize() : Paths.get(".").toAbsolutePath().normalize();

        switch (pathType) {
            case EVO_ROOT:
                return evoRoot;

            case WORKSPACE: {
                String wsStr = ProjectModelManager.getWorkspacePath();
                return wsStr != null ? Paths.get(wsStr).toAbsolutePath().normalize() : evoRoot;
            }

            case PROJECT_ROOT:
                return evoRoot;

            case SOURCE_ROOT: {
                Orchestrator orch = getOrchestrator();
                if (orch != null && orch.getSupervisorSettings() != null && orch.getSupervisorSettings().getSourcePath() != null) {
                    return resolvePath(EvoPath.EVO_ROOT, orch.getSupervisorSettings().getSourcePath());
                }
                return evoRoot.resolve("self-dev-run/source").toAbsolutePath().normalize();
            }

            case RUNTIME_ROOT:
                return evoRoot.resolve("self-dev-run/runtime").toAbsolutePath().normalize();

            case BUILD_ROOT:
                return evoRoot.resolve("self-dev-run/build").toAbsolutePath().normalize();

            case EXPORT_ROOT:
                return evoRoot.resolve("self-dev-run/export").toAbsolutePath().normalize();

            case SUPERVISOR_SOURCE: {
                Orchestrator orch = getOrchestrator();
                if (orch != null && orch.getSupervisorSettings() != null && orch.getSupervisorSettings().getSourcePath() != null) {
                    return resolvePath(EvoPath.EVO_ROOT, orch.getSupervisorSettings().getSourcePath());
                }
                Path candidate = evoRoot.resolve("eu.kalafatic.evolution.supervisor");
                return resolvePathWithRecovery(candidate, "eu.kalafatic.evolution.supervisor");
            }

            case SUPERVISOR_RUNTIME: {
                Orchestrator orch = getOrchestrator();
                if (orch != null && orch.getSupervisorSettings() != null && orch.getSupervisorSettings().getExecutablePath() != null) {
                    return resolvePath(EvoPath.EVO_ROOT, orch.getSupervisorSettings().getExecutablePath());
                }
                return evoRoot.resolve("self-dev-run/builds").toAbsolutePath().normalize();
            }

            case GENOME: {
                Path candidate = evoRoot.resolve("eu.kalafatic.evolution.selfdev.genome");
                return resolvePathWithRecovery(candidate, "eu.kalafatic.evolution.selfdev.genome");
            }

            case FORGE_INPUT:
                return evoRoot.resolve("data").toAbsolutePath().normalize();

            case FORGE_OUTPUT:
                return evoRoot.resolve("forge-output").toAbsolutePath().normalize();

            case MODELS:
                return evoRoot.resolve("source/models").toAbsolutePath().normalize();

            case DATASETS:
                return evoRoot.resolve("data").toAbsolutePath().normalize();

            default:
                return evoRoot;
        }
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
     * Resolves a configured path against a semantic base path.
     *
     * @param semanticBase The base directory.
     * @param configuredPath The path string to resolve.
     * @return The resolved, normalized absolute Path.
     */
    public Path resolvePath(Path semanticBase, String configuredPath) {
        if (configuredPath == null || configuredPath.trim().isEmpty()) {
            return semanticBase != null ? semanticBase.toAbsolutePath().normalize() : getPath(EvoPath.EVO_ROOT);
        }

        String expanded = expandVariables(configuredPath.trim());
        Path p = Paths.get(expanded);

        // HARD REQUIREMENT 7 & 8: Absolute paths MUST NOT have roots prepended to them!
        if (p.isAbsolute()) {
            Path norm = p.toAbsolutePath().normalize();
            if (!norm.toFile().exists()) {
                return recoverStalePath(norm);
            }
            return norm;
        }

        Path base = semanticBase != null ? semanticBase.toAbsolutePath().normalize() : getPath(EvoPath.EVO_ROOT);
        Path resolved = base.resolve(p).toAbsolutePath().normalize();

        if (!resolved.toFile().exists()) {
            return recoverStalePath(resolved);
        }
        return resolved;
    }

    private String expandVariables(String rawPath) {
        if (rawPath == null) return "";
        String codebase = ProjectModelManager.getCodebasePath();
        if (codebase == null) codebase = new File(".").getAbsolutePath();
        String workspace = ProjectModelManager.getWorkspacePath();
        if (workspace == null) workspace = codebase;
        String userHome = System.getProperty("user.home");

        String res = rawPath;
        res = res.replace("${EVO_ROOT}", codebase);
        res = res.replace("${WORKSPACE}", workspace);
        res = res.replace("${USER_HOME}", userHome);
        res = res.replace("${user.home}", userHome);
        return res;
    }

    private Path resolvePathWithRecovery(Path target, String moduleName) {
        Path norm = target.toAbsolutePath().normalize();
        if (norm.toFile().exists()) {
            return norm;
        }
        return recoverStalePathForModule(moduleName, norm);
    }

    private Path recoverStalePath(Path missingPath) {
        File file = missingPath.toFile();
        String name = file.getName();
        if (name == null || name.isEmpty()) return missingPath;

        Path recovered = recoverStalePathForModule(name, missingPath);
        return recovered;
    }

    private Path recoverStalePathForModule(String moduleName, Path fallback) {
        Path[] searchRoots = new Path[] {
            getPath(EvoPath.EVO_ROOT),
            getPath(EvoPath.WORKSPACE),
            Paths.get(System.getProperty("user.home"))
        };

        for (Path root : searchRoots) {
            if (root == null || !root.toFile().exists()) continue;

            Path candidate = root.resolve(moduleName);
            if (candidate.toFile().exists()) {
                Path resolved = candidate.toAbsolutePath().normalize();
                Log.log("[ResourceManager] Stale path RECOVERED for module '" + moduleName + "': " +
                        fallback + " -> " + resolved);
                return resolved;
            }

            File[] children = root.toFile().listFiles(File::isDirectory);
            if (children != null) {
                for (File child : children) {
                    File sub = new File(child, moduleName);
                    if (sub.exists()) {
                        Path resolved = sub.toPath().toAbsolutePath().normalize();
                        Log.log("[ResourceManager] Stale path RECOVERED for module '" + moduleName + "': " +
                                fallback + " -> " + resolved);
                        return resolved;
                    }
                }
            }
        }

        return fallback;
    }

    // =========================================================================
    // 2. URL / IP / PORT CENTRALIZATION & SERVICE ENUMERATION
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
    // 3. REPOSITORY ENUMERATION & ACCESS
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
    // 4. MODELS & DATASETS ENUMERATION
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
