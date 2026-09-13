package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import eu.kalafatic.evolution.model.orchestration.Orchestrator;

public class SelfDevContext {
    private final String runId;
    private final File repositoryRoot;
    private final File projectRoot;
    private final File sourceDirectory;
    private final File buildDirectory;
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

    private final Map<String, TaskResult> taskResults = new ConcurrentHashMap<>();
    private final Map<ArtifactType, BuildArtifact> artifacts = new ConcurrentHashMap<>();

    public SelfDevContext(File projectRoot, Orchestrator orchestrator) {
        this(projectRoot, null, orchestrator);
    }

    public SelfDevContext(File projectRoot, File baseRunDir, Orchestrator orchestrator) {
        this.projectRoot = projectRoot != null ? projectRoot.getAbsoluteFile().toPath().normalize().toFile() : new File(".").getAbsoluteFile().toPath().normalize().toFile();
        this.orchestrator = orchestrator;

        this.repositoryRoot = discoverRepositoryRoot(this.projectRoot);

        String timestamp = new SimpleDateFormat("ddMMyy_HHmmss").format(new Date());
        this.runId = "run_" + timestamp;

        File runDir;
        if (baseRunDir != null) {
            runDir = resolvePath(this.projectRoot, baseRunDir);
        } else {
            runDir = new File(this.projectRoot, "projects/evo/supervisor/" + new SimpleDateFormat("ddMMyy").format(new Date())).getAbsoluteFile();
        }

        this.sourceDirectory = resolvePath(runDir, "source");
        this.buildDirectory = resolvePath(runDir, "build");
        this.exportDirectory = resolvePath(runDir, "export");
        this.runtimeDirectory = resolvePath(runDir, "runtime");
        this.logDirectory = resolvePath(this.projectRoot, "self-dev-run/logs");

        initTargetPlatform();
        discoverAndRepairModulePaths();
        ensureDirectories();
        printPreflightReport();
    }

    private File discoverRepositoryRoot(File root) {
        if (root == null) return new File(".").getAbsoluteFile().toPath().normalize().toFile();
        File current = root;
        while (current != null) {
            if (new File(current, ".git").exists() || new File(current, "pom.xml").exists()) {
                return current.getAbsoluteFile().toPath().normalize().toFile();
            }
            current = current.getParentFile();
        }
        return root;
    }

    private void initTargetPlatform() {
        String sysOs = System.getProperty("evo.target.os", System.getProperty("os.name")).toLowerCase();
        if (sysOs.contains("win")) {
            this.os = "win32";
            this.ws = "win32";
            this.arch = "x86_64";
            this.launcher = "evo.exe";
        } else {
            this.os = "linux";
            this.ws = "gtk";
            this.arch = "x86_64";
            this.launcher = "evo";
        }
    }

    public static File resolvePath(File semanticBase, File configured) {
        if (configured == null) {
            return semanticBase != null ? semanticBase.getAbsoluteFile().toPath().normalize().toFile() : null;
        }
        if (configured.isAbsolute()) {
            return configured.getAbsoluteFile().toPath().normalize().toFile();
        }
        if (semanticBase == null) {
            return configured.getAbsoluteFile().toPath().normalize().toFile();
        }
        return new File(semanticBase, configured.getPath()).getAbsoluteFile().toPath().normalize().toFile();
    }

    public static File resolvePath(File semanticBase, String configuredPath) {
        if (configuredPath == null || configuredPath.trim().isEmpty()) {
            return semanticBase != null ? semanticBase.getAbsoluteFile().toPath().normalize().toFile() : null;
        }
        File configured = new File(configuredPath);
        return resolvePath(semanticBase, configured);
    }

    public void discoverAndRepairModulePaths() {
        this.supervisorDirectory = discoverModuleDirectory("eu.kalafatic.evolution.supervisor",
                new File(this.sourceDirectory, "eu.kalafatic.evolution.supervisor"),
                this.sourceDirectory, this.projectRoot, this.repositoryRoot);

        this.genomeDirectory = discoverModuleDirectory("eu.kalafatic.evolution.selfdev.genome",
                new File(this.sourceDirectory, "eu.kalafatic.evolution.selfdev.genome"),
                this.sourceDirectory, this.projectRoot, this.repositoryRoot);
    }

    public File discoverModuleDirectory(String moduleName, File primaryLocation, File... searchRoots) {
        if (primaryLocation != null && primaryLocation.exists() && isModuleDirectory(primaryLocation)) {
            return primaryLocation.getAbsoluteFile().toPath().normalize().toFile();
        }

        System.out.println("[SelfDevContext] Primary location for module '" + moduleName + "' not found at: " +
                (primaryLocation != null ? primaryLocation.getAbsolutePath() : "null") + ". Investigating search roots...");

        for (File root : searchRoots) {
            if (root == null || !root.exists()) continue;

            File directCandidate = new File(root, moduleName);
            if (directCandidate.exists() && isModuleDirectory(directCandidate)) {
                File resolved = directCandidate.getAbsoluteFile().toPath().normalize().toFile();
                System.out.println("[SelfDevContext] DISCOVERY REPAIR: Discovered valid module '" + moduleName + "' at: " + resolved.getAbsolutePath());
                return resolved;
            }

            // Search one level deep under search root
            File[] children = root.listFiles(File::isDirectory);
            if (children != null) {
                for (File child : children) {
                    File candidate = new File(child, moduleName);
                    if (candidate.exists() && isModuleDirectory(candidate)) {
                        File resolved = candidate.getAbsoluteFile().toPath().normalize().toFile();
                        System.out.println("[SelfDevContext] DISCOVERY REPAIR: Discovered valid module '" + moduleName + "' at: " + resolved.getAbsolutePath());
                        return resolved;
                    }
                }
            }
        }

        File fallback = primaryLocation != null ? primaryLocation.getAbsoluteFile().toPath().normalize().toFile() : new File(projectRoot, moduleName).getAbsoluteFile().toPath().normalize().toFile();
        System.out.println("[SelfDevContext] WARNING: Module '" + moduleName + "' could not be discovered under search roots. Defaulting to: " + fallback.getAbsolutePath());
        return fallback;
    }

    private boolean isModuleDirectory(File dir) {
        if (dir == null || !dir.exists() || !dir.isDirectory()) return false;
        return new File(dir, "pom.xml").exists() || new File(dir, "META-INF/MANIFEST.MF").exists();
    }

    public void printPreflightReport() {
        System.out.println("================================================================================");
        System.out.println("SELF-DEV PREFLIGHT CONTEXT REPORT");
        System.out.println("--------------------------------------------------------------------------------");
        System.out.println("Run ID          : " + runId);
        System.out.println("Repository Root : " + repositoryRoot.getAbsolutePath() + " (exists: " + repositoryRoot.exists() + ")");
        System.out.println("Project Root    : " + projectRoot.getAbsolutePath() + " (exists: " + projectRoot.exists() + ")");
        System.out.println("Source Directory: " + sourceDirectory.getAbsolutePath() + " (exists: " + sourceDirectory.exists() + ")");
        System.out.println("Build Directory : " + buildDirectory.getAbsolutePath() + " (exists: " + buildDirectory.exists() + ")");
        System.out.println("Export Directory: " + exportDirectory.getAbsolutePath() + " (exists: " + exportDirectory.exists() + ")");
        System.out.println("Runtime Dir     : " + runtimeDirectory.getAbsolutePath() + " (exists: " + runtimeDirectory.exists() + ")");
        System.out.println("Log Directory   : " + logDirectory.getAbsolutePath() + " (exists: " + logDirectory.exists() + ")");
        System.out.println("Supervisor Dir  : " + (supervisorDirectory != null ? supervisorDirectory.getAbsolutePath() : "null") + " (exists: " + (supervisorDirectory != null && supervisorDirectory.exists()) + ")");
        System.out.println("Genome Dir      : " + (genomeDirectory != null ? genomeDirectory.getAbsolutePath() : "null") + " (exists: " + (genomeDirectory != null && genomeDirectory.exists()) + ")");
        System.out.println("Platform        : " + os + "." + ws + "." + arch);
        System.out.println("Product ID      : " + productId);
        System.out.println("Launcher Name   : " + launcher);
        System.out.println("================================================================================");
    }

    private void ensureDirectories() {
        createDirIfNeeded(sourceDirectory);
        createDirIfNeeded(buildDirectory);
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
        return repositoryRoot;
    }

    public File getProjectRoot() {
        return projectRoot;
    }

    public File getSupervisorDirectory() {
        return supervisorDirectory;
    }

    public void setSupervisorDirectory(File supervisorDirectory) {
        this.supervisorDirectory = supervisorDirectory;
    }

    public File getGenomeDirectory() {
        return genomeDirectory;
    }

    public void setGenomeDirectory(File genomeDirectory) {
        this.genomeDirectory = genomeDirectory;
    }

    public String getOs() {
        return os;
    }

    public String getWs() {
        return ws;
    }

    public String getArch() {
        return arch;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getLauncher() {
        return launcher;
    }

    public void setLauncher(String launcher) {
        this.launcher = launcher;
    }

    public File getSourceDirectory() {
        return sourceDirectory;
    }

    public File getBuildDirectory() {
        return buildDirectory;
    }

    public File getExportDirectory() {
        return exportDirectory;
    }

    public File getRuntimeDirectory() {
        return runtimeDirectory;
    }

    public File getLogDirectory() {
        return logDirectory;
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
