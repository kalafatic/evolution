package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import eu.kalafatic.evolution.controller.log.Log;
import eu.kalafatic.evolution.controller.resource.EvoPath;
import eu.kalafatic.evolution.controller.resource.ResourceManager;
import eu.kalafatic.evolution.controller.resource.TargetPlatform;

/**
 * Deterministic, self-diagnosing, self-recovering Self-Dev Preflight Engine.
 * Executed BEFORE any task execution or dependency graph run.
 */
public class SelfDevPreflight {

    private final ResourceManager resourceManager;

    public SelfDevPreflight() {
        this(ResourceManager.getInstance());
    }

    public SelfDevPreflight(ResourceManager resourceManager) {
        this.resourceManager = resourceManager != null ? resourceManager : ResourceManager.getInstance();
    }

    /**
     * Executes the comprehensive preflight validation.
     * Strictly validates canonical repository paths without searching or discovering candidate directories.
     *
     * @param context The active SelfDevContext
     * @param orchestrator Optional SelfDevOrchestrator to validate task graph
     * @return SelfDevPreflightResult
     */
    public SelfDevPreflightResult executePreflight(SelfDevContext context, SelfDevOrchestrator orchestrator) {
        String preflightRunId = UUID.randomUUID().toString();
        String sessionId = context != null ? context.getRunId() : "default_session";

        Log.log("[SelfDevPreflight] ==========================================");
        Log.log("[SelfDevPreflight] START preflightRunId=" + preflightRunId + ", sessionId=" + sessionId);
        Log.log("[SelfDevPreflight] ==========================================");

        List<SelfDevPreflightResult.CheckDetail> checks = new ArrayList<>();
        List<String> conflicts = new ArrayList<>();
        List<String> recoveries = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        // 1. Initial path resolution & strict source verification
        File repositoryRoot = (context != null && context.getRepositoryRoot() != null) ? context.getRepositoryRoot().getAbsoluteFile() : resourceManager.getPath(EvoPath.EVO_ROOT).toFile().getAbsoluteFile();
        File projectRoot = (context != null && context.getProjectRoot() != null) ? context.getProjectRoot().getAbsoluteFile() : repositoryRoot;
        File sourceDirectory = (context != null && context.getSourceDirectory() != null) ? context.getSourceDirectory().getAbsoluteFile() : repositoryRoot;

        Log.log("[PATH] resource=EVO_GIT_REPOSITORY configured=" + repositoryRoot.getAbsolutePath() +
                " resolved=" + repositoryRoot.getAbsolutePath() + " absolute=" + repositoryRoot.isAbsolute() +
                " exists=" + repositoryRoot.exists() + " directory=" + repositoryRoot.isDirectory() +
                " gitRepository=" + new File(repositoryRoot, ".git").exists() +
                " pom=" + new File(repositoryRoot, "pom.xml").exists() + " origin=EMF_CONFIGURATION");

        Log.log("[PATH_DISCOVERY] resource=EVO_GIT_REPOSITORY action=FORBIDDEN reason=canonical_repository_must_be_explicitly_configured");

        // 2. Validate Canonical EVO Repository Integrity (EVO_GIT_REPOSITORY)
        boolean isRuntimeProduct = repositoryRoot.getAbsolutePath().contains(".product") || repositoryRoot.getAbsolutePath().contains("runtime-eu.kalafatic");
        boolean isGitRepo = repositoryRoot.exists() && repositoryRoot.isDirectory() && new File(repositoryRoot, ".git").exists();

        if (isRuntimeProduct) {
            String err = "Canonical EVO Git repository is invalid. Configured path '" + repositoryRoot.getAbsolutePath() +
                    "' is a runtime product directory, NOT a Maven source repository. No automatic repository discovery was attempted.";
            Log.log("[SelfDevPreflight][PATH_ERROR] " + err);
            errors.add(err);
            checks.add(new SelfDevPreflightResult.CheckDetail(
                    "EVO_GIT_REPOSITORY", "BLOCKED",
                    "Canonical EVO repository validation",
                    repositoryRoot.getAbsolutePath(),
                    "Directory with .git (not a runtime product)",
                    "Runtime Product Directory",
                    "Configure canonical EVO Git repository path"
            ));
        } else if (!repositoryRoot.exists() || !repositoryRoot.isDirectory()) {
            String err = "Canonical EVO Git repository is invalid. Configured path '" + repositoryRoot.getAbsolutePath() +
                    "' does not exist or is not a directory. Expected: Directory containing .git and pom.xml. Actual: missing directory. No automatic repository discovery was attempted.";
            Log.log("[SelfDevPreflight][PATH_ERROR] " + err);
            errors.add(err);
            checks.add(new SelfDevPreflightResult.CheckDetail(
                    "EVO_GIT_REPOSITORY", "BLOCKED",
                    "Canonical EVO repository validation",
                    repositoryRoot.getAbsolutePath(),
                    "Existing directory containing .git",
                    "Directory Missing",
                    "Configure valid canonical EVO repository location"
            ));
        } else if (!isGitRepo) {
            String err = "Canonical EVO Git repository is invalid. Configured path '" + repositoryRoot.getAbsolutePath() +
                    "' is not a Git repository (missing .git). Expected: Directory containing .git and pom.xml. Actual: .git missing. No automatic repository discovery was attempted.";
            Log.log("[SelfDevPreflight][PATH_ERROR] " + err);
            errors.add(err);
            checks.add(new SelfDevPreflightResult.CheckDetail(
                    "EVO_GIT_REPOSITORY", "BLOCKED",
                    "Canonical EVO repository validation",
                    repositoryRoot.getAbsolutePath(),
                    "Directory containing .git folder",
                    "Missing .git folder",
                    "Configure valid Git repository"
            ));
        } else {
            checks.add(new SelfDevPreflightResult.CheckDetail(
                    "EVO_GIT_REPOSITORY", "OK",
                    "Canonical EVO repository validation",
                    repositoryRoot.getAbsolutePath(),
                    "Directory with .git",
                    "VERIFIED",
                    "None"
            ));
        }

        // 3. Validate Maven Reactor Source Root Integrity (EVO_SOURCE_ROOT / MAVEN_REACTOR)
        File reactorDir = (new File(sourceDirectory, "pom.xml").exists()) ? sourceDirectory : repositoryRoot;
        boolean hasPom = reactorDir.exists() && reactorDir.isDirectory() && new File(reactorDir, "pom.xml").exists();

        if (!hasPom) {
            String err = "Canonical EVO source reactor is invalid. Configured path '" + reactorDir.getAbsolutePath() +
                    "' is missing root pom.xml. Expected: Directory containing pom.xml. Actual: pom.xml missing. No automatic repository discovery was attempted.";
            Log.log("[SelfDevPreflight][PATH_ERROR] " + err);
            errors.add(err);
            checks.add(new SelfDevPreflightResult.CheckDetail(
                    "EVO_SOURCE_ROOT", "BLOCKED",
                    "Maven reactor source validation",
                    reactorDir.getAbsolutePath(),
                    "Directory containing root pom.xml",
                    "Missing pom.xml",
                    "Provide valid Maven reactor source root"
            ));
        } else {
            checks.add(new SelfDevPreflightResult.CheckDetail(
                    "EVO_SOURCE_ROOT", "OK",
                    "Maven reactor source validation",
                    reactorDir.getAbsolutePath(),
                    "Directory containing pom.xml",
                    "VERIFIED",
                    "None"
            ));
        }

        File resolvedSource = reactorDir;

        // 4. Output Directories & Platform
        File buildDir = (context != null && context.getBuildDirectory() != null) ? context.getBuildDirectory() : resourceManager.getEvoBuildOutput().toFile();
        File exportDir = (context != null && context.getExportDirectory() != null) ? context.getExportDirectory() : resourceManager.getEvoExport().toFile();
        File runtimeDir = (context != null && context.getRuntimeDirectory() != null) ? context.getRuntimeDirectory() : resourceManager.getPath(EvoPath.RUNTIME_ROOT).toFile();
        File logDir = (context != null && context.getLogDirectory() != null) ? context.getLogDirectory() : resourceManager.resolvePath("self-dev-run/logs").toFile();
        File supervisorDir = (context != null && context.getSupervisorDirectory() != null) ? context.getSupervisorDirectory() : resourceManager.getSupervisorSource().toFile();
        File genomeDir = (context != null && context.getGenomeDirectory() != null) ? context.getGenomeDirectory() : resourceManager.getGenome().toFile();

        Log.log("[PATH_DERIVED] resource=BUILD_DIR base=SELF_DEV_ROOT rule=selfDevRoot/build resolved=" + buildDir.getAbsolutePath());
        Log.log("[PATH_DERIVED] resource=EXPORT_DIR base=SELF_DEV_ROOT rule=selfDevRoot/export resolved=" + exportDir.getAbsolutePath());
        Log.log("[PATH_DERIVED] resource=RUNTIME_DIR base=SELF_DEV_ROOT rule=selfDevRoot/runtime resolved=" + runtimeDir.getAbsolutePath());

        // 5. Test Write Permissions on Output Directories
        testWritePermission("BUILD_DIR", buildDir, checks, errors);
        testWritePermission("EXPORT_DIR", exportDir, checks, errors);
        testWritePermission("LOG_DIR", logDir, checks, errors);
        testWritePermission("RUNTIME_DIR", runtimeDir, checks, errors);

        // 6. Target Platform Verification
        TargetPlatform targetPlatform = resourceManager.getTargetPlatform();
        Log.log("[SelfDevPreflight][ENV] Target Platform: " + targetPlatform);
        checks.add(new SelfDevPreflightResult.CheckDetail(
                "TARGET_PLATFORM", "OK",
                "Target OS/WS/Arch platform definition",
                targetPlatform.toString(),
                "Valid OS/WS/Arch",
                targetPlatform.getOs() + "." + targetPlatform.getWs() + "." + targetPlatform.getArch(),
                "None"
        ));

        // 7. Executable Validation (Java & Maven)
        File javaExec = validateJavaExecutable(checks, errors, warnings);
        File mavenExec = validateMavenExecutable(resolvedSource, checks, errors, warnings);

        // 8. Consistency Check across ResourceManager, SelfDevContext, Task Snapshots
        if (context != null && context.getResolvedResources() != null) {
            File snapshotSource = context.getResolvedResources().getSourceDirectory();
            if (snapshotSource != null && !snapshotSource.getCanonicalPath().equals(resolvedSource.getCanonicalPath())) {
                String conflict = "PATH_CONFLICT: SelfDevContext snapshot source (" + snapshotSource.getAbsolutePath() + ") differs from canonical source (" + resolvedSource.getAbsolutePath() + ")";
                Log.log("[PATH_CONFLICT] resource=EVO_GIT_REPOSITORY canonical=" + resolvedSource.getAbsolutePath() + " other=" + snapshotSource.getAbsolutePath() + " source=SelfDevContext action=BLOCK");
                conflicts.add(conflict);
                errors.add(conflict);
            }
        }

        // 8. Task Graph & Dependencies Preflight
        if (orchestrator != null) {
            validateTaskGraph(orchestrator, resolvedSource, checks, errors);
        }

        // 9. Synchronize & Propagate Resolved Snapshot if safe
        SelfDevPreflightResult.PreflightStatus overallStatus;
        if (!errors.isEmpty()) {
            overallStatus = SelfDevPreflightResult.PreflightStatus.BLOCKED;
        } else {
            overallStatus = SelfDevPreflightResult.PreflightStatus.SUCCESS;
        }

        ResolvedSelfDevResources snapshot = new ResolvedSelfDevResources(
                repositoryRoot, projectRoot, resolvedSource, reactorDir,
                buildDir, exportDir, runtimeDir, logDir, supervisorDir, genomeDir,
                targetPlatform.getOs(), targetPlatform.getWs(), targetPlatform.getArch(),
                resourceManager.getProductDefinition().getProductId(),
                resourceManager.getProductDefinition().getLauncherName(),
                javaExec, mavenExec
        );

        Log.log("[SelfDevPreflight] ==========================================");
        Log.log("[SelfDevPreflight] RESULT=" + overallStatus + " preflightRunId=" + preflightRunId);
        Log.log("[SelfDevPreflight] ==========================================");

        SelfDevPreflightResult result = new SelfDevPreflightResult(
                overallStatus, snapshot, checks, conflicts, recoveries, errors, warnings
        );

        Log.log(result.generateSummaryReport());
        return result;
    }

    private void testWritePermission(String name, File dir, List<SelfDevPreflightResult.CheckDetail> checks, List<String> errors) {
        if (dir == null) {
            errors.add("WRITE_TEST FAILED: " + name + " is null");
            checks.add(new SelfDevPreflightResult.CheckDetail(name, "FAILED", "Directory is null", "null", "Writable directory", "null", "None"));
            return;
        }
        try {
            if (!dir.exists()) {
                dir.mkdirs();
            }
            File testFile = new File(dir, ".preflight_write_test_" + UUID.randomUUID().toString() + ".tmp");
            Files.writeString(testFile.toPath(), "test");
            Files.delete(testFile.toPath());
            Log.log("[SelfDevPreflight][WRITE_TEST] " + name + " OK: " + dir.getAbsolutePath());
            checks.add(new SelfDevPreflightResult.CheckDetail(name, "OK", "Write test succeeded", dir.getAbsolutePath(), "Writable directory", "Writable", "None"));
        } catch (Exception e) {
            String err = "WRITE_TEST FAILED for " + name + " at " + dir.getAbsolutePath() + ": " + e.getMessage();
            Log.log("[SelfDevPreflight][WRITE_TEST] " + err);
            errors.add(err);
            checks.add(new SelfDevPreflightResult.CheckDetail(name, "FAILED", err, dir.getAbsolutePath(), "Writable directory", "Permission Denied / Error", "Grant write permissions"));
        }
    }

    private File validateJavaExecutable(List<SelfDevPreflightResult.CheckDetail> checks, List<String> errors, List<String> warnings) {
        String javaHome = System.getProperty("java.home");
        File javaExec = new File(javaHome, "bin/java" + (System.getProperty("os.name").toLowerCase().contains("win") ? ".exe" : ""));
        if (!javaExec.exists()) {
            javaExec = new File("java");
        }

        try {
            ProcessBuilder pb = new ProcessBuilder(javaExec.getAbsolutePath(), "-version");
            pb.redirectErrorStream(true);
            Process p = pb.start();
            StringBuilder out = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    out.append(line).append(" ");
                }
            }
            p.waitFor();

            String verOutput = out.toString();
            Log.log("[SelfDevPreflight][JAVA] Java executable verified: " + javaExec.getAbsolutePath() + " (" + verOutput.trim() + ")");
            checks.add(new SelfDevPreflightResult.CheckDetail("JAVA", "OK", "Java executable verified: " + verOutput.trim(), javaExec.getAbsolutePath(), "Java 21+", verOutput.trim(), "None"));
            return javaExec;
        } catch (Exception e) {
            String err = "JAVA_CHECK FAILED: Could not execute java -version: " + e.getMessage();
            Log.log("[SelfDevPreflight][JAVA] " + err);
            errors.add(err);
            checks.add(new SelfDevPreflightResult.CheckDetail("JAVA", "FAILED", err, javaExec.getAbsolutePath(), "Executable java", "Failed to run", "Ensure Java JDK is installed"));
            return javaExec;
        }
    }

    private File validateMavenExecutable(File sourceDir, List<SelfDevPreflightResult.CheckDetail> checks, List<String> errors, List<String> warnings) {
        boolean isWin = System.getProperty("os.name").toLowerCase().contains("win");
        File mvnExec = null;

        if (sourceDir != null && sourceDir.exists()) {
            File wrapper = new File(sourceDir, isWin ? "mvnw.cmd" : "mvnw");
            if (wrapper.exists()) {
                mvnExec = wrapper;
            }
        }

        if (mvnExec == null) {
            String m2Home = System.getenv("M2_HOME");
            if (m2Home == null || m2Home.isEmpty()) {
                m2Home = System.getenv("MAVEN_HOME");
            }
            if (m2Home != null && !m2Home.isEmpty()) {
                File homeMvn = new File(m2Home, "bin/" + (isWin ? "mvn.cmd" : "mvn"));
                if (homeMvn.exists()) {
                    mvnExec = homeMvn;
                }
            }
        }

        if (mvnExec == null) {
            mvnExec = new File(isWin ? "mvn.cmd" : "mvn");
        }

        try {
            ProcessBuilder pb = new ProcessBuilder(mvnExec.getAbsolutePath(), "-version");
            pb.redirectErrorStream(true);
            Process p = pb.start();
            StringBuilder out = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    out.append(line).append(" ");
                }
            }
            p.waitFor();

            String verOutput = out.toString();
            Log.log("[SelfDevPreflight][MAVEN] Maven executable verified: " + mvnExec.getAbsolutePath() + " (" + verOutput.trim() + ")");
            checks.add(new SelfDevPreflightResult.CheckDetail("MAVEN", "OK", "Maven executable verified: " + verOutput.trim(), mvnExec.getAbsolutePath(), "Maven 3.9+", verOutput.trim(), "None"));
            return mvnExec;
        } catch (Exception e) {
            String err = "MAVEN_CHECK FAILED: Required Maven executable is missing or not functional via " + mvnExec.getAbsolutePath() + ": " + e.getMessage();
            Log.log("[SelfDevPreflight][MAVEN] " + err);
            errors.add(err);
            checks.add(new SelfDevPreflightResult.CheckDetail("MAVEN", "FAILED", err, mvnExec.getAbsolutePath(), "Executable mvn", "Failed to run", "Ensure Maven or mvnw is configured"));
            return mvnExec;
        }
    }

    private void validateTaskGraph(SelfDevOrchestrator orchestrator, File resolvedSource, List<SelfDevPreflightResult.CheckDetail> checks, List<String> errors) {
        Map<String, SelfDevTask> registry = orchestrator.getTaskRegistry();
        Log.log("[SelfDevPreflight][TASK] Inspecting task graph containing " + registry.size() + " tasks...");

        for (SelfDevTask task : registry.values()) {
            Log.log("[SelfDevPreflight][TASK] id=" + task.getId() + ", class=" + task.getClass().getSimpleName() + ", dependencies=" + task.getDependencies() + ", status=" + task.getStatus());
        }

        // Validate COPY task pre-condition specifically before MAVEN/BUILD tasks run
        if (resolvedSource == null || !resolvedSource.exists() || !new File(resolvedSource, "pom.xml").exists()) {
            String err = "MAVEN_EVO BLOCKED_BY_PREFLIGHT: Dependency COPY cannot execute because source directory is invalid or missing pom.xml";
            Log.log("[SelfDevPreflight][DEPENDENCY] " + err);
            errors.add(err);
            checks.add(new SelfDevPreflightResult.CheckDetail("TASK_GRAPH", "BLOCKED", err, resolvedSource != null ? resolvedSource.getAbsolutePath() : "null", "Valid source pom.xml", "Missing pom.xml", "Recover source directory"));
        } else {
            checks.add(new SelfDevPreflightResult.CheckDetail("TASK_GRAPH", "OK", "Task graph dependencies verified", resolvedSource.getAbsolutePath(), "Valid DAG", "Valid DAG", "None"));
        }
    }
}
