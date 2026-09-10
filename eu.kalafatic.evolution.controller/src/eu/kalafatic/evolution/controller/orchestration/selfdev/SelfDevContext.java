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
    private final File projectRoot;
    private final File sourceDirectory;
    private final File buildDirectory;
    private final File exportDirectory;
    private final File runtimeDirectory;
    private final File logDirectory;
    private final Orchestrator orchestrator;

    private String sourceRevision;
    private boolean debugMode;

    private final Map<String, TaskResult> taskResults = new ConcurrentHashMap<>();
    private final Map<ArtifactType, BuildArtifact> artifacts = new ConcurrentHashMap<>();

    public SelfDevContext(File projectRoot, Orchestrator orchestrator) {
        this.projectRoot = projectRoot != null ? projectRoot.getAbsoluteFile() : new File(".").getAbsoluteFile();
        this.orchestrator = orchestrator;

        String timestamp = new SimpleDateFormat("ddMMyy_HHmmss").format(new Date());
        this.runId = "run_" + timestamp;

        File runDir = new File(this.projectRoot, "projects/evo/supervisor/" + new SimpleDateFormat("ddMMyy").format(new Date()));
        this.sourceDirectory = new File(runDir, "source");
        this.buildDirectory = new File(runDir, "build");
        this.exportDirectory = new File(runDir, "export");
        this.runtimeDirectory = new File(runDir, "runtime");
        this.logDirectory = new File(this.projectRoot, "self-dev-run/logs");

        ensureDirectories();
    }

    public SelfDevContext(File projectRoot, File baseRunDir, Orchestrator orchestrator) {
        this.projectRoot = projectRoot != null ? projectRoot.getAbsoluteFile() : new File(".").getAbsoluteFile();
        this.orchestrator = orchestrator;

        String timestamp = new SimpleDateFormat("ddMMyy_HHmmss").format(new Date());
        this.runId = "run_" + timestamp;

        File runDir = baseRunDir != null ? baseRunDir : new File(this.projectRoot, "self-dev-run");
        this.sourceDirectory = new File(runDir, "source");
        this.buildDirectory = new File(runDir, "build");
        this.exportDirectory = new File(runDir, "export");
        this.runtimeDirectory = new File(runDir, "runtime");
        this.logDirectory = new File(runDir, "logs");

        ensureDirectories();
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

    public File getProjectRoot() {
        return projectRoot;
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
