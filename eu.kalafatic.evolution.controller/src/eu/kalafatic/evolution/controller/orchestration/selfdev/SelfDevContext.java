package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.io.File;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import eu.kalafatic.evolution.controller.resource.EvoPath;
import eu.kalafatic.evolution.controller.resource.ProductDefinition;
import eu.kalafatic.evolution.controller.resource.ResourceManager;
import eu.kalafatic.evolution.controller.resource.TargetPlatform;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

public class SelfDevContext {
    private final String runId;
    private final ResourceManager resourceManager;
    private final File repositoryRoot;
    private final File sourceReactorDirectory;
    private final File projectRoot;
    private final File buildDirectory;
    private final File preparedReactorDirectory;
    private final File exportDirectory;
    private final File runtimeDirectory;
    private final File logDirectory;
    private final Orchestrator orchestrator;

    private File supervisorDirectory;
    private File genomeDirectory;

    private String os;
    private String ws;
    private String arch;
    private String productId = "evolution";
    private String launcher = "evo";

    private String sourceRevision;
    private boolean debugMode;

    private long supervisorPid = -1;
    private String supervisorExecutable;
    private File supervisorWorkingDirectory;

    private ResolvedSelfDevResources resolvedResources;

    public enum ProcessOwnership {
        CURRENT_RUN,
        OTHER_RUN,
        PARENT_RCP,
        UNKNOWN
    }

    private final Map<String, TaskResult> taskResults = new ConcurrentHashMap<>();
    private final Map<ArtifactType, BuildArtifact> artifacts = new ConcurrentHashMap<>();

    public SelfDevContext(File projectRoot, Orchestrator orchestrator) {
        this(projectRoot, null, orchestrator);
    }

    public SelfDevContext(File projectRoot, File baseRunDir, Orchestrator orchestrator) {
        this.resourceManager = ResourceManager.getInstance();
        if (orchestrator != null) {
            this.resourceManager.setOrchestrator(orchestrator);
        }
        this.orchestrator = this.resourceManager.getOrchestrator();

        File pRoot = projectRoot != null ? projectRoot.getAbsoluteFile().toPath().normalize().toFile() : null;
        File defaultRepo = this.resourceManager.getPath(EvoPath.EVO_GIT_REPOSITORY).toFile().getAbsoluteFile().toPath().normalize().toFile();

        boolean defaultRepoValid = defaultRepo.exists() && new File(defaultRepo, ".git").exists();
        if (!defaultRepoValid) {
            File appRepo = new File("/app");
            if (appRepo.exists() && new File(appRepo, ".git").exists()) {
                this.repositoryRoot = appRepo;
            } else if (pRoot != null && pRoot.exists() && new File(pRoot, ".git").exists()) {
                this.repositoryRoot = pRoot;
            } else {
                this.repositoryRoot = defaultRepo;
            }
        } else {
            this.repositoryRoot = defaultRepo;
        }

        this.projectRoot = pRoot != null ? pRoot : this.repositoryRoot;

        String timestamp = new SimpleDateFormat("ddMMyy_HHmmss").format(new Date());
        this.runId = "run_" + timestamp;

        File wsRoot = this.resourceManager.getPath(EvoPath.WORKSPACE).toFile().getAbsoluteFile();

        File runDir;
        if (baseRunDir != null) {
            runDir = resolvePath(wsRoot, baseRunDir);
        } else {
            runDir = wsRoot.toPath().resolve("self-dev/" + this.runId).toAbsolutePath().normalize().toFile();
        }

        this.preparedReactorDirectory = resolvePath(runDir, "source");
        this.sourceReactorDirectory = this.preparedReactorDirectory; // Map source reactor directly to selfDevRun/source
        this.buildDirectory = resolvePath(runDir, "build");
        this.exportDirectory = resolvePath(runDir, "export");
        this.runtimeDirectory = resolvePath(runDir, "runtime");
        this.logDirectory = resolvePath(runDir, "logs");
        File runModuleSup = new File(this.preparedReactorDirectory, "eu.kalafatic.evolution.supervisor");
        File repoModuleSup = new File(this.repositoryRoot, "eu.kalafatic.evolution.supervisor");
        if (runModuleSup.exists()) {
            this.supervisorDirectory = runModuleSup.getAbsoluteFile().toPath().normalize().toFile();
        } else if (repoModuleSup.exists()) {
            this.supervisorDirectory = repoModuleSup.getAbsoluteFile().toPath().normalize().toFile();
        } else {
            this.supervisorDirectory = this.resourceManager.getPath(EvoPath.SUPERVISOR_SOURCE).toFile().getAbsoluteFile().toPath().normalize().toFile();
        }

        File runModuleGenome = new File(this.preparedReactorDirectory, "eu.kalafatic.evolution.selfdev.genome");
        File repoModuleGenome = new File(this.repositoryRoot, "eu.kalafatic.evolution.selfdev.genome");
        if (runModuleGenome.exists()) {
            this.genomeDirectory = runModuleGenome.getAbsoluteFile().toPath().normalize().toFile();
        } else if (repoModuleGenome.exists()) {
            this.genomeDirectory = repoModuleGenome.getAbsoluteFile().toPath().normalize().toFile();
        } else {
            this.genomeDirectory = this.resourceManager.getPath(EvoPath.GENOME).toFile().getAbsoluteFile().toPath().normalize().toFile();
        }

        initTargetPlatform();
        ensureDirectories();
        printPreflightReport();
    }

    public ResourceManager getResourceManager() {
        return resourceManager;
    }

    private void initTargetPlatform() {
        TargetPlatform platform = resourceManager.getTargetPlatform();
        this.os = platform.getOs();
        this.ws = platform.getWs();
        this.arch = platform.getArch();

        ProductDefinition prodDef = resourceManager.getProductDefinition();
        this.productId = prodDef.getProductId();
        this.launcher = prodDef.getLauncherName();
    }

    public static File resolvePath(File semanticBase, File configured) {
        if (configured == null) {
            return semanticBase != null ? semanticBase.getAbsoluteFile().toPath().normalize().toFile() : null;
        }
        if (configured.isAbsolute()) {
            return configured.getAbsoluteFile().toPath().normalize().toFile();
        }
        Path base = semanticBase != null ? semanticBase.toPath().toAbsolutePath().normalize() : ResourceManager.getInstance().getPath(EvoPath.EVO_ROOT);
        return base.resolve(configured.toPath()).toAbsolutePath().normalize().toFile();
    }

    public static File resolvePath(File semanticBase, String configuredPath) {
        if (configuredPath == null || configuredPath.trim().isEmpty()) {
            return semanticBase != null ? semanticBase.getAbsoluteFile().toPath().normalize().toFile() : null;
        }
        Path p = java.nio.file.Paths.get(configuredPath.trim());
        if (p.isAbsolute()) {
            return p.toAbsolutePath().normalize().toFile();
        }
        Path base = semanticBase != null ? semanticBase.toPath().toAbsolutePath().normalize() : ResourceManager.getInstance().getPath(EvoPath.EVO_ROOT);
        return base.resolve(p).toAbsolutePath().normalize().toFile();
    }


    public ResolvedSelfDevResources getResolvedResources() {
        return resolvedResources;
    }

    public void setResolvedResources(ResolvedSelfDevResources resolvedResources) {
        this.resolvedResources = resolvedResources;
        if (resolvedResources != null) {
            if (resolvedResources.getSupervisorDirectory() != null) {
                this.supervisorDirectory = resolvedResources.getSupervisorDirectory();
            }
            if (resolvedResources.getGenomeDirectory() != null) {
                this.genomeDirectory = resolvedResources.getGenomeDirectory();
            }
            if (resolvedResources.getOs() != null) {
                this.os = resolvedResources.getOs();
            }
            if (resolvedResources.getWs() != null) {
                this.ws = resolvedResources.getWs();
            }
            if (resolvedResources.getArch() != null) {
                this.arch = resolvedResources.getArch();
            }
            if (resolvedResources.getProductId() != null) {
                this.productId = resolvedResources.getProductId();
            }
            if (resolvedResources.getLauncher() != null) {
                this.launcher = resolvedResources.getLauncher();
            }
        }
    }

    public void printPreflightReport() {
        System.out.println("================================================================================");
        System.out.println("SELF-DEV PREFLIGHT CONTEXT REPORT");
        System.out.println("--------------------------------------------------------------------------------");
        System.out.println("Run ID          : " + runId);
        System.out.println("Repository Root : " + repositoryRoot.getAbsolutePath() + " (exists: " + repositoryRoot.exists() + ")");
        System.out.println("Source Reactor  : " + sourceReactorDirectory.getAbsolutePath() + " (exists: " + sourceReactorDirectory.exists() + ")");
        System.out.println("Project Root    : " + projectRoot.getAbsolutePath() + " (exists: " + projectRoot.exists() + ")");
        System.out.println("Build Directory : " + buildDirectory.getAbsolutePath() + " (exists: " + buildDirectory.exists() + ")");
        System.out.println("Prepared Reactor: " + preparedReactorDirectory.getAbsolutePath() + " (exists: " + preparedReactorDirectory.exists() + ")");
        System.out.println("Export Directory: " + exportDirectory.getAbsolutePath() + " (exists: " + exportDirectory.exists() + ")");
        System.out.println("Runtime Dir     : " + runtimeDirectory.getAbsolutePath() + " (exists: " + runtimeDirectory.exists() + ")");
        System.out.println("Log Directory   : " + logDirectory.getAbsolutePath() + " (exists: " + logDirectory.exists() + ")");
        System.out.println("Supervisor Dir  : " + (supervisorDirectory != null ? supervisorDirectory.getAbsolutePath() : "null") + " (exists: " + (supervisorDirectory != null && supervisorDirectory.exists()) + ")");
        System.out.println("Genome Dir      : " + (genomeDirectory != null ? genomeDirectory.getAbsolutePath() : "null") + " (exists: " + (genomeDirectory != null && genomeDirectory.exists()) + ")");
        System.out.println("Platform        : " + os + "." + ws + "." + arch);
        System.out.println("Product ID      : " + productId);
        System.out.println("Launcher Name   : " + launcher);
        System.out.println("Debug Mode      : " + debugMode + " (portOffset: +" + getPortOffset() + ")");
        System.out.println("Effective Ports : Supervisor=" + getEffectiveSupervisorPort() + ", Control=" + getEffectiveSupervisorControlPort() + ", Server=" + getEffectiveServerPort());
        System.out.println("================================================================================");
    }

    private void ensureDirectories() {
        createDirIfNeeded(buildDirectory);
        createDirIfNeeded(preparedReactorDirectory);
        createDirIfNeeded(exportDirectory);
        createDirIfNeeded(runtimeDirectory);
        createDirIfNeeded(logDirectory);
    }

    private void createDirIfNeeded(File dir) {
        if (dir != null && !dir.exists()) {
            dir.mkdirs();
        }
    }

    public String getRunId() {
        return runId;
    }

    public File getRepositoryRoot() {
        return resolvedResources != null && resolvedResources.getRepositoryRoot() != null ? resolvedResources.getRepositoryRoot() : repositoryRoot;
    }

    public File getProjectRoot() {
        return resolvedResources != null && resolvedResources.getProjectRoot() != null ? resolvedResources.getProjectRoot() : projectRoot;
    }

    public File getSupervisorDirectory() {
        return resolvedResources != null && resolvedResources.getSupervisorDirectory() != null ? resolvedResources.getSupervisorDirectory() : supervisorDirectory;
    }

    public void setSupervisorDirectory(File supervisorDirectory) {
        this.supervisorDirectory = supervisorDirectory;
    }

    public File getGenomeDirectory() {
        return resolvedResources != null && resolvedResources.getGenomeDirectory() != null ? resolvedResources.getGenomeDirectory() : genomeDirectory;
    }

    public void setGenomeDirectory(File genomeDirectory) {
        this.genomeDirectory = genomeDirectory;
    }

    public String getOs() {
        return resolvedResources != null && resolvedResources.getOs() != null ? resolvedResources.getOs() : os;
    }

    public String getWs() {
        return resolvedResources != null && resolvedResources.getWs() != null ? resolvedResources.getWs() : ws;
    }

    public String getArch() {
        return resolvedResources != null && resolvedResources.getArch() != null ? resolvedResources.getArch() : arch;
    }

    public String getProductId() {
        return resolvedResources != null && resolvedResources.getProductId() != null ? resolvedResources.getProductId() : productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getLauncher() {
        return resolvedResources != null && resolvedResources.getLauncher() != null ? resolvedResources.getLauncher() : launcher;
    }

    public void setLauncher(String launcher) {
        this.launcher = launcher;
    }

    public File getSourceReactorDirectory() {
        return resolvedResources != null && resolvedResources.getSourceReactorDirectory() != null ? resolvedResources.getSourceReactorDirectory() : sourceReactorDirectory;
    }

    public File getSourceDirectory() {
        return getSourceReactorDirectory();
    }

    public File getPreparedReactorDirectory() {
        return resolvedResources != null && resolvedResources.getPreparedReactorDirectory() != null ? resolvedResources.getPreparedReactorDirectory() : preparedReactorDirectory;
    }

    public File getReactorDirectory() {
        return getPreparedReactorDirectory();
    }

    public File getBuildDirectory() {
        return resolvedResources != null && resolvedResources.getBuildDirectory() != null ? resolvedResources.getBuildDirectory() : buildDirectory;
    }

    public File getExportDirectory() {
        return resolvedResources != null && resolvedResources.getExportDirectory() != null ? resolvedResources.getExportDirectory() : exportDirectory;
    }

    public File getRuntimeDirectory() {
        return resolvedResources != null && resolvedResources.getRuntimeDirectory() != null ? resolvedResources.getRuntimeDirectory() : runtimeDirectory;
    }

    public File getLogDirectory() {
        return resolvedResources != null && resolvedResources.getLogDirectory() != null ? resolvedResources.getLogDirectory() : logDirectory;
    }

    public Orchestrator getOrchestrator() {
        return orchestrator;
    }

    public String getSourceRevision() {
        return sourceRevision;
    }

    public void setSourceRevision(String sourceRevision) {
        this.sourceRevision = sourceRevision;
    }

    public boolean isDebugMode() {
        return debugMode;
    }

    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
    }

    public int getPortOffset() {
        return isDebugMode() ? 10 : 0;
    }

    public int getEffectiveSupervisorPort() {
        eu.kalafatic.evolution.controller.resource.EvoService svc = resourceManager.getService("SUPERVISOR");
        int basePort = svc != null && svc.getPort() > 0 ? svc.getPort() : 8089;
        return basePort + getPortOffset();
    }

    public int getEffectiveSupervisorControlPort() {
        int basePort = 28080;
        return basePort + getPortOffset();
    }

    public int getEffectiveServerPort() {
        eu.kalafatic.evolution.controller.resource.EvoService svc = resourceManager.getService("SERVER");
        int basePort = svc != null && svc.getPort() > 0 ? svc.getPort() : 48081;
        return basePort + getPortOffset();
    }

    public long getSupervisorPid() {
        return supervisorPid;
    }

    public void setSupervisorPid(long supervisorPid) {
        this.supervisorPid = supervisorPid;
    }

    public String getSupervisorExecutable() {
        return supervisorExecutable;
    }

    public void setSupervisorExecutable(String supervisorExecutable) {
        this.supervisorExecutable = supervisorExecutable;
    }

    public File getSupervisorWorkingDirectory() {
        return supervisorWorkingDirectory;
    }

    public void setSupervisorWorkingDirectory(File supervisorWorkingDirectory) {
        this.supervisorWorkingDirectory = supervisorWorkingDirectory;
    }

    public void recordTaskResult(TaskResult result) {
        if (result != null && result.getTaskId() != null) {
            taskResults.put(result.getTaskId(), result);
            if (result.getArtifact() != null) {
                artifacts.put(result.getArtifact().getType(), result.getArtifact());
            }
        }
    }

    public TaskResult getTaskResult(String taskId) {
        return taskResults.get(taskId);
    }

    public Map<String, TaskResult> getTaskResults() {
        return Collections.unmodifiableMap(taskResults);
    }

    public void recordArtifact(BuildArtifact artifact) {
        if (artifact != null && artifact.getType() != null) {
            artifacts.put(artifact.getType(), artifact);
        }
    }

    public BuildArtifact getArtifact(ArtifactType type) {
        return artifacts.get(type);
    }

    public Map<ArtifactType, BuildArtifact> getArtifacts() {
        return Collections.unmodifiableMap(artifacts);
    }
}
