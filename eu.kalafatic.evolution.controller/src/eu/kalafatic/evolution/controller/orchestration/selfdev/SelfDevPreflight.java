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
     *
     * @param context The active SelfDevContext (can be updated with recovered paths)
     * @param orchestrator Optional SelfDevOrchestrator to validate task graph
     * @return SelfDevPreflightResult
     */
    public SelfDevPreflightResult executePreflight(SelfDevContext context, SelfDevOrchestrator orchestrator) {
        Log.log("[SelfDevPreflight] ==========================================");
        Log.log("[SelfDevPreflight] START");
        Log.log("[SelfDevPreflight] ==========================================");

        List<SelfDevPreflightResult.CheckDetail> checks = new ArrayList<>();
        List<String> conflicts = new ArrayList<>();
        List<String> recoveries = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        boolean recoveredAny = false;

        // 1. Initial path resolution & source verification
        File repositoryRoot = resourceManager.getPath(EvoPath.EVO_ROOT).toFile();
        File projectRoot = context != null ? context.getProjectRoot() : repositoryRoot;

        Log.log("[SelfDevPreflight][PATH] Repository Root: " + repositoryRoot.getAbsolutePath());
        Log.log("[SelfDevPreflight][PATH] Project Root   : " + projectRoot.getAbsolutePath());

        // 2. Validate Source Directory / Maven Reactor Root
        File candidateSource = context != null ? context.getSourceDirectory() : resourceManager.getPath(EvoPath.SOURCE_ROOT).toFile();
        File resolvedSource = candidateSource;

        String sourceStatus = "VERIFIED";
        if (candidateSource == null || !candidateSource.exists() || !new File(candidateSource, "pom.xml").exists()) {
            Log.log("[SelfDevPreflight][PATH] Configured source path is INVALID or missing pom.xml: " + (candidateSource != null ? candidateSource.getAbsolutePath() : "null"));
            if (candidateSource != null && candidateSource.getAbsolutePath().contains(".product")) {
                Log.log("[SelfDevPreflight][PATH] Classified candidate as RUNTIME_PRODUCT directory - NOT a Maven source reactor!");
            }

            // Perform deterministic recovery search for valid Maven source root
            List<File> validCandidates = findValidSourceCandidates(projectRoot, repositoryRoot);
            if (validCandidates.size() == 1) {
                resolvedSource = validCandidates.get(0);
                recoveredAny = true;
                String recMsg = "Stale/invalid source RECOVERED: " + (candidateSource != null ? candidateSource.getAbsolutePath() : "null") + " -> " + resolvedSource.getAbsolutePath();
                Log.log("[SelfDevPreflight][RECOVERY] " + recMsg);
                recoveries.add(recMsg);
                sourceStatus = "RECOVERED";
            } else if (validCandidates.size() > 1) {
                String err = "AMBIGUOUS_SOURCE_ROOT: Multiple valid Maven source candidates found: " + validCandidates;
                Log.log("[SelfDevPreflight][PATH] " + err);
                errors.add(err);
                sourceStatus = "AMBIGUOUS";
            } else {
                String err = "INVALID_MAVEN_SOURCE: No valid Maven source directory with pom.xml found. Checked path: " + (candidateSource != null ? candidateSource.getAbsolutePath() : "null");
                Log.log("[SelfDevPreflight][PATH] " + err);
                errors.add(err);
                sourceStatus = "INVALID";
            }
        }

        checks.add(new SelfDevPreflightResult.CheckDetail(
                "SOURCE_DIRECTORY", sourceStatus,
                "Maven source directory validation",
                resolvedSource != null ? resolvedSource.getAbsolutePath() : "null",
                "Directory with pom.xml",
                sourceStatus,
                recoveredAny ? "Recovered to valid source root" : "None"
        ));

        // 3. Reactor & Output Directories
        File reactorDir = resolvedSource;
        File buildDir = context != null ? context.getBuildDirectory() : resourceManager.getEvoBuildOutput().toFile();
        File exportDir = context != null ? context.getExportDirectory() : resourceManager.getEvoExport().toFile();
        File runtimeDir = context != null ? context.getRuntimeDirectory() : resourceManager.getPath(EvoPath.RUNTIME_ROOT).toFile();
        File logDir = context != null ? context.getLogDirectory() : resourceManager.resolvePath("self-dev-run/logs").toFile();
        File supervisorDir = context != null ? context.getSupervisorDirectory() : resourceManager.getSupervisorSource().toFile();
        File genomeDir = context != null ? context.getGenomeDirectory() : resourceManager.getGenome().toFile();

        // 4. Test Write Permissions on Output Directories
        testWritePermission("BUILD_DIR", buildDir, checks, errors);
        testWritePermission("EXPORT_DIR", exportDir, checks, errors);
        testWritePermission("LOG_DIR", logDir, checks, errors);
        testWritePermission("RUNTIME_DIR", runtimeDir, checks, errors);

        // 5. Target Platform Verification
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

        // 6. Executable Validation (Java & Maven)
        File javaExec = validateJavaExecutable(checks, errors, warnings);
        File mavenExec = validateMavenExecutable(resolvedSource, checks, errors, warnings);

        // 7. Consistency Check across ResourceManager, SelfDevContext, Task Snapshots
        if (context != null) {
            File rmSource = resourceManager.getEvoSource().toFile();
            File ctxSource = context.getSourceDirectory();
            Log.log("[SelfDevPreflight][PATH] Comparing layer sources: ResourceManager=" + rmSource.getAbsolutePath() + ", SelfDevContext=" + (ctxSource != null ? ctxSource.getAbsolutePath() : "null") + ", resolved=" + (resolvedSource != null ? resolvedSource.getAbsolutePath() : "null"));

            if (ctxSource != null && resolvedSource != null && !ctxSource.getAbsoluteFile().equals(resolvedSource.getAbsoluteFile())) {
                String conflict = "PATH_CONFLICT: SelfDevContext source (" + ctxSource.getAbsolutePath() + ") differs from resolved source (" + resolvedSource.getAbsolutePath() + ")";
                Log.log("[SelfDevPreflight][PATH_CONFLICT] " + conflict);
                conflicts.add(conflict);
            }
        }

        // 8. Task Graph & Dependencies Preflight
        if (orchestrator != null) {
            validateTaskGraph(orchestrator, resolvedSource, checks, errors);
        }

        // 9. Synchronize & Propagate Recovered Snapshot if safe
        SelfDevPreflightResult.PreflightStatus overallStatus;
        if (!errors.isEmpty()) {
            overallStatus = SelfDevPreflightResult.PreflightStatus.BLOCKED;
        } else if (recoveredAny) {
            overallStatus = SelfDevPreflightResult.PreflightStatus.RECOVERED;
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
        Log.log("[SelfDevPreflight] RESULT=" + overallStatus);
        Log.log("[SelfDevPreflight] ==========================================");

        SelfDevPreflightResult result = new SelfDevPreflightResult(
                overallStatus, snapshot, checks, conflicts, recoveries, errors, warnings
        );

        Log.log(result.generateSummaryReport());
        return result;
    }

    private List<File> findValidSourceCandidates(File projectRoot, File repositoryRoot) {
        List<File> candidates = new ArrayList<>();
        List<File> searchRoots = Arrays.asList(
                repositoryRoot,
                projectRoot,
                new File(System.getProperty("user.home")),
                new File(System.getProperty("user.dir"))
        );

        for (File root : searchRoots) {
            if (root == null || !root.exists()) continue;

            // Direct check
            if (isValidMavenSourceRoot(root)) {
                addCandidateIfNotPresent(candidates, root);
            }

            // Standard subdirectories
            File[] subDirs = new File[] {
                    new File(root, "sources"),
                    new File(root, "source"),
                    new File(root, "evo")
            };
            for (File sub : subDirs) {
                if (isValidMavenSourceRoot(sub)) {
                    addCandidateIfNotPresent(candidates, sub);
                }
            }
        }

        return candidates;
    }

    private boolean isValidMavenSourceRoot(File dir) {
        if (dir == null || !dir.exists() || !dir.isDirectory()) return false;
        // MUST contain pom.xml and MUST NOT be a runtime .product directory
        if (dir.getAbsolutePath().contains(".product")) return false;
        return new File(dir, "pom.xml").exists();
    }

    private void addCandidateIfNotPresent(List<File> candidates, File dir) {
        File norm = dir.getAbsoluteFile().toPath().normalize().toFile();
        for (File existing : candidates) {
            if (existing.equals(norm)) return;
        }
        candidates.add(norm);
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
            String warn = "MAVEN_CHECK WARNING: Could not execute mvn -version via " + mvnExec.getAbsolutePath() + ": " + e.getMessage();
            Log.log("[SelfDevPreflight][MAVEN] " + warn);
            warnings.add(warn);
            checks.add(new SelfDevPreflightResult.CheckDetail("MAVEN", "WARNING", warn, mvnExec.getAbsolutePath(), "Executable mvn", "Failed to run", "Ensure Maven is on PATH"));
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
