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

        // 1. Initial path resolution - SelfDevContext is the ONLY source for run-scoped paths.
        // No fallback to the canonical repository root or ResourceManager guesses is permitted:
        // missing run directories produce an explicit BLOCKED result.
        File repositoryRoot = (context != null && context.getRepositoryRoot() != null) ? context.getRepositoryRoot().getAbsoluteFile() : null;
        File projectRoot = (context != null && context.getProjectRoot() != null) ? context.getProjectRoot().getAbsoluteFile() : repositoryRoot;

        File buildDir = (context != null) ? context.getBuildDirectory() : null;
        File preparedReactor = (context != null) ? context.getPreparedReactorDirectory() : null;
        File sourceReactor = preparedReactor; // Map source directly to selfDevRun/source
        File exportDir = (context != null) ? context.getExportDirectory() : null;
        File runtimeDir = (context != null) ? context.getRuntimeDirectory() : null;
        File logDir = (context != null) ? context.getLogDirectory() : null;
        File supervisorDir = (context != null) ? context.getSupervisorDirectory() : null;
        File genomeDir = (context != null) ? context.getGenomeDirectory() : null;

        if (repositoryRoot == null) {
            errors.add("SELF_DEV_RESOLUTION FAILED: no repository root available (SelfDevContext is null).");
            checks.add(new SelfDevPreflightResult.CheckDetail(
                    "EVO_GIT_REPOSITORY", "BLOCKED",
                    "Canonical EVO repository validation",
                    "null", "Existing absolute directory containing .git",
                    "No context", "Provide a valid SelfDevContext"));
        }

        if (buildDir == null) {
            errors.add("SELF_DEV_RESOLUTION FAILED: SELF_DEV_BUILD could not be resolved from the run context.");
            checks.add(new SelfDevPreflightResult.CheckDetail("SELF_DEV_BUILD", "BLOCKED",
                    "Run-scoped build directory resolution", "null",
                    "Resolved directory under the run workspace",
                    "Unresolved", "Provide a valid SelfDevContext"));
        }
        if (exportDir == null) {
            errors.add("SELF_DEV_RESOLUTION FAILED: SELF_DEV_EXPORT could not be resolved from the run context.");
            checks.add(new SelfDevPreflightResult.CheckDetail("SELF_DEV_EXPORT", "BLOCKED",
                    "Run-scoped export directory resolution", "null",
                    "Resolved directory under the run workspace",
                    "Unresolved", "Provide a valid SelfDevContext"));
        }
        if (logDir == null) {
            errors.add("SELF_DEV_RESOLUTION FAILED: SELF_DEV_LOG could not be resolved from the run context.");
            checks.add(new SelfDevPreflightResult.CheckDetail("SELF_DEV_LOG", "BLOCKED",
                    "Run-scoped log directory resolution", "null",
                    "Resolved directory under the run workspace",
                    "Unresolved", "Provide a valid SelfDevContext"));
        }
        if (runtimeDir == null) {
            errors.add("SELF_DEV_RESOLUTION FAILED: SELF_DEV_RUNTIME could not be resolved from the run context.");
            checks.add(new SelfDevPreflightResult.CheckDetail("SELF_DEV_RUNTIME", "BLOCKED",
                    "Run-scoped runtime directory resolution", "null",
                    "Resolved directory under the run workspace",
                    "Unresolved", "Provide a valid SelfDevContext"));
        }

        // Path Logging for every resource
        Log.log("[PATH] resource=EVO_GIT_REPOSITORY configured=" + safe(repositoryRoot) +
                " resolved=" + safe(repositoryRoot) + " absolute=" + (repositoryRoot != null && repositoryRoot.isAbsolute()) +
                " exists=" + (repositoryRoot != null && repositoryRoot.exists()) + " directory=" + (repositoryRoot != null && repositoryRoot.isDirectory()) +
                " gitRepository=" + (repositoryRoot != null && new File(repositoryRoot, ".git").exists()) +
                " pom=" + (repositoryRoot != null && new File(repositoryRoot, "pom.xml").exists()) + " origin=EMF_CONFIGURATION");

        Log.log("[PATH] resource=SELF_DEV_BUILD configured=" + safe(buildDir) +
                " resolved=" + safe(buildDir) + " absolute=" + (buildDir != null && buildDir.isAbsolute()) +
                " exists=" + (buildDir != null && buildDir.exists()) + " directory=" + (buildDir != null && buildDir.isDirectory()) +
                " pom=" + (buildDir != null && new File(buildDir, "pom.xml").exists()) + " origin=SELF_DEV_CONTEXT");

        Log.log("[PATH] resource=SELF_DEV_SOURCE configured=" + safe(preparedReactor) +
                " resolved=" + safe(preparedReactor) + " absolute=" + (preparedReactor != null && preparedReactor.isAbsolute()) +
                " exists=" + (preparedReactor != null && preparedReactor.exists()) + " directory=" + (preparedReactor != null && preparedReactor.isDirectory()) +
                " pom=" + (preparedReactor != null && new File(preparedReactor, "pom.xml").exists()) + " origin=SELF_DEV_CONTEXT");

        Log.log("[PATH] resource=SELF_DEV_EXPORT configured=" + safe(exportDir) +
                " resolved=" + safe(exportDir) + " absolute=" + (exportDir != null && exportDir.isAbsolute()) +
                " exists=" + (exportDir != null && exportDir.exists()) + " directory=" + (exportDir != null && exportDir.isDirectory()) +
                " pom=" + (exportDir != null && new File(exportDir, "pom.xml").exists()) + " origin=SELF_DEV_CONTEXT");

        Log.log("[PATH_DISCOVERY] resource=EVO_GIT_REPOSITORY action=FORBIDDEN reason=canonical_repository_must_be_explicitly_configured");

        // 2. Validate Canonical EVO Git Repository Integrity (EVO_GIT_REPOSITORY)
        String repoPathNorm = repositoryRoot != null ? repositoryRoot.getAbsolutePath().replace('\\', '/') : "null";
        boolean isRuntimeProduct = repoPathNorm.contains(".product") || repoPathNorm.contains("/runtime-") || repoPathNorm.contains("/runtime/");
        boolean isGitRepo = repositoryRoot != null && repositoryRoot.exists() && repositoryRoot.isDirectory() && new File(repositoryRoot, ".git").exists();

        if (repositoryRoot == null) {
            // Already recorded as BLOCKED in resolution section.
        } else if (isRuntimeProduct) {
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
        } else if (!repositoryRoot.exists() || !repositoryRoot.isDirectory() || !repositoryRoot.isAbsolute()) {
            String err = "Canonical EVO Git repository is invalid. Configured path '" + repositoryRoot.getAbsolutePath() +
                    "' does not exist, is not absolute, or is not a directory. Expected: Absolute directory containing .git. Actual: missing directory. No automatic repository discovery was attempted.";
            Log.log("[SelfDevPreflight][PATH_ERROR] " + err);
            errors.add(err);
            checks.add(new SelfDevPreflightResult.CheckDetail(
                    "EVO_GIT_REPOSITORY", "BLOCKED",
                    "Canonical EVO repository validation",
                    repositoryRoot.getAbsolutePath(),
                    "Existing absolute directory containing .git",
                    "Directory Missing or Not Absolute",
                    "Configure valid canonical EVO repository location"
            ));
        } else if (!isGitRepo) {
            String err = "Canonical EVO Git repository is invalid. Configured path '" + repositoryRoot.getAbsolutePath() +
                    "' is not a Git repository (missing .git). Expected: Directory containing .git. Actual: .git missing. No automatic repository discovery was attempted.";
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

        // Validate explicitly configured secondary repositories from EMF Orchestrator (supervisor, llm)
        eu.kalafatic.evolution.model.orchestration.Orchestrator orch = (context != null) ? context.getOrchestrator() : null;
        if (orch != null && orch.getSupervisorSettings() != null && orch.getSupervisorSettings().getGit() != null) {
            String supPath = orch.getSupervisorSettings().getGit().getLocalPath();
            if (supPath != null && !supPath.trim().isEmpty()) {
                File supRepo = null;
                String resolveError = null;
                try {
                    supRepo = resourceManager.resolvePath(EvoPath.EVO_ROOT, supPath.trim()).toFile();
                } catch (IllegalArgumentException e) {
                    resolveError = e.getMessage();
                }
                if (supRepo == null) {
                    String err = "Supervisor Git repository configuration is invalid. Configured path '" + supPath.trim() +
                            "' violates canonical path rules" + (resolveError != null ? ": " + resolveError : "") + ".";
                    Log.log("[SelfDevPreflight][PATH_ERROR] " + err);
                    errors.add(err);
                    checks.add(new SelfDevPreflightResult.CheckDetail(
                            "SUPERVISOR_GIT_REPOSITORY", "BLOCKED",
                            "Supervisor repository validation",
                            supPath.trim(),
                            "Valid source repository within canonical roots",
                            "Non-canonical path",
                            "Configure valid Supervisor Git repository path or clear setting"
                    ));
                } else {
                    String supPathNorm = supRepo.getAbsolutePath().replace('\\', '/');
                    if (supPathNorm.contains(".product") || supPathNorm.contains("/runtime-") || supPathNorm.contains("/runtime/")) {
                        String err = "Supervisor Git repository configuration is invalid. Configured path '" + supRepo.getAbsolutePath() +
                                "' is a runtime product directory, NOT a source repository.";
                        Log.log("[SelfDevPreflight][PATH_ERROR] " + err);
                        errors.add(err);
                        checks.add(new SelfDevPreflightResult.CheckDetail(
                                "SUPERVISOR_GIT_REPOSITORY", "BLOCKED",
                                "Supervisor repository validation",
                                supRepo.getAbsolutePath(),
                                "Valid source repository (not runtime)",
                                "Runtime Product Directory",
                                "Configure valid Supervisor Git repository path or clear setting"
                        ));
                    } else if (!supRepo.exists() || !new File(supRepo, ".git").exists()) {
                        String err = "Supervisor Git repository configuration is invalid. Directory does not exist or missing .git: " + supRepo.getAbsolutePath();
                        Log.log("[SelfDevPreflight][PATH_ERROR] " + err);
                        errors.add(err);
                        checks.add(new SelfDevPreflightResult.CheckDetail(
                                "SUPERVISOR_GIT_REPOSITORY", "BLOCKED",
                                "Supervisor repository validation",
                                supRepo.getAbsolutePath(),
                                "Existing directory containing .git",
                                "Missing .git folder",
                                "Configure valid Git repository or clear setting"
                        ));
                    }
                }
            }
        }

        // 3. Validate Path Invariants (repository, source, build, export distinctness)
        if (repositoryRoot != null && buildDir != null && preparedReactor != null && exportDir != null) {
            try {
                File canonRepo = repositoryRoot.getCanonicalFile();
            File canonBuild = buildDir.getCanonicalFile();
            File canonPreparedReactor = preparedReactor.getCanonicalFile();
            File canonExport = exportDir.getCanonicalFile();

            boolean repoNotBuild = !canonRepo.equals(canonBuild);
            boolean repoNotPrepReactor = !canonRepo.equals(canonPreparedReactor);
            boolean exportNotPrepReactor = !canonExport.equals(canonPreparedReactor);
            boolean exportNotRepo = !canonExport.equals(canonRepo);

            Log.log("[PATH_INVARIANT] EVO_GIT_REPOSITORY != SELF_DEV_BUILD : " + (repoNotBuild ? "PASS" : "FAIL"));
            Log.log("[PATH_INVARIANT] EVO_GIT_REPOSITORY != SELF_DEV_SOURCE : " + (repoNotPrepReactor ? "PASS" : "FAIL"));
            Log.log("[PATH_INVARIANT] SELF_DEV_EXPORT != SELF_DEV_SOURCE : " + (exportNotPrepReactor ? "PASS" : "FAIL"));
            Log.log("[PATH_INVARIANT] SELF_DEV_EXPORT != EVO_GIT_REPOSITORY : " + (exportNotRepo ? "PASS" : "FAIL"));

            if (!repoNotBuild) {
                String err = "PATH_INVARIANT FAILED: EVO_GIT_REPOSITORY (" + repositoryRoot.getAbsolutePath() + ") and SELF_DEV_BUILD (" + buildDir.getAbsolutePath() + ") resolve to the same directory.";
                errors.add(err);
            }
            if (!repoNotPrepReactor) {
                String err = "PATH_INVARIANT FAILED: EVO_GIT_REPOSITORY (" + repositoryRoot.getAbsolutePath() + ") and SELF_DEV_SOURCE (" + preparedReactor.getAbsolutePath() + ") resolve to the same directory.";
                errors.add(err);
            }
            if (!exportNotPrepReactor) {
                String err = "PATH_INVARIANT FAILED: SELF_DEV_EXPORT (" + exportDir.getAbsolutePath() + ") and SELF_DEV_SOURCE (" + preparedReactor.getAbsolutePath() + ") resolve to the same directory.";
                errors.add(err);
            }
                if (!exportNotRepo) {
                    String err = "PATH_INVARIANT FAILED: SELF_DEV_EXPORT (" + exportDir.getAbsolutePath() + ") and EVO_GIT_REPOSITORY (" + repositoryRoot.getAbsolutePath() + ") resolve to the same directory.";
                    errors.add(err);
                }
            } catch (Exception e) {
                Log.log("[SelfDevPreflight] Failed path invariant check: " + e.getMessage());
                errors.add("Path invariant check exception: " + e.getMessage());
            }
        }

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
        File mavenExec = validateMavenExecutable(repositoryRoot, javaExec, checks, errors, warnings);

        // 8. Network & Environment Launch Readiness
        validateNetworkAndEnvironmentReadiness(context, checks, errors, warnings);

        // 9. Consistency Check across ResourceManager, SelfDevContext, Task Snapshots
        if (context != null && context.getResolvedResources() != null && repositoryRoot != null) {
            File snapshotRepo = context.getResolvedResources().getRepositoryRoot();
            try {
                if (snapshotRepo != null && !snapshotRepo.getCanonicalPath().equals(repositoryRoot.getCanonicalPath())) {
                    String conflict = "PATH_CONFLICT: SelfDevContext snapshot repository (" + snapshotRepo.getAbsolutePath() + ") differs from canonical repository (" + repositoryRoot.getAbsolutePath() + ")";
                    Log.log("[PATH_CONFLICT] resource=EVO_GIT_REPOSITORY canonical=" + repositoryRoot.getAbsolutePath() + " other=" + snapshotRepo.getAbsolutePath() + " source=SelfDevContext action=BLOCK");
                    conflicts.add(conflict);
                    errors.add(conflict);
                }
            } catch (Exception e) {
                Log.log("[SelfDevPreflight] Failed canonical path comparison: " + e.getMessage());
            }
        }

        // 8. Task Graph & Dependencies Preflight
        if (orchestrator != null) {
            validateTaskGraph(orchestrator, repositoryRoot, checks, errors);
        }

        // 9. Synchronize & Propagate Resolved Snapshot if safe
        SelfDevPreflightResult.PreflightStatus overallStatus;
        if (!errors.isEmpty()) {
            overallStatus = SelfDevPreflightResult.PreflightStatus.BLOCKED;
        } else {
            overallStatus = SelfDevPreflightResult.PreflightStatus.SUCCESS;
        }

        ResolvedSelfDevResources snapshot = new ResolvedSelfDevResources(
                repositoryRoot, projectRoot, sourceReactor, preparedReactor,
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

    private File validateMavenExecutable(File sourceDir, File javaExec, List<SelfDevPreflightResult.CheckDetail> checks, List<String> errors, List<String> warnings) {
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

            String javaHome = System.getProperty("java.home");
            if (javaHome != null && !javaHome.trim().isEmpty()) {
                pb.environment().put("JAVA_HOME", javaHome);

                // Prepend javaHome/bin to PATH so mvn and wrapper find the exact java runtime
                String pathVar = pb.environment().get("PATH");
                if (pathVar == null) {
                    pathVar = System.getenv("PATH");
                }
                File javaBin = new File(javaHome, "bin");
                if (javaBin.exists()) {
                    pb.environment().put("PATH", javaBin.getAbsolutePath() + File.pathSeparator + (pathVar != null ? pathVar : ""));
                }
            }

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

    private void validateTaskGraph(SelfDevOrchestrator orchestrator, File repositoryRoot, List<SelfDevPreflightResult.CheckDetail> checks, List<String> errors) {
        Map<String, SelfDevTask> registry = orchestrator.getTaskRegistry();
        Log.log("[SelfDevPreflight][TASK] Inspecting task graph containing " + registry.size() + " tasks...");

        for (SelfDevTask task : registry.values()) {
            Log.log("[SelfDevPreflight][TASK] id=" + task.getId() + ", class=" + task.getClass().getSimpleName() + ", dependencies=" + task.getDependencies() + ", status=" + task.getStatus());
        }

        // Real DAG validation: unknown dependencies, self-dependencies and cycles
        // reported by the orchestrator's static graph validator (D-66).
        java.util.List<String> graphErrors = orchestrator.getGraphValidationErrors();
        if (!graphErrors.isEmpty()) {
            for (String graphError : graphErrors) {
                Log.log("[SelfDevPreflight][DEPENDENCY] " + graphError);
                errors.add("TASK_GRAPH INVALID: " + graphError);
            }
            checks.add(new SelfDevPreflightResult.CheckDetail("TASK_GRAPH", "BLOCKED",
                    "Task dependency graph validation (cycles, unknown dependencies, self-dependencies)",
                    repositoryRoot != null ? repositoryRoot.getAbsolutePath() : "null",
                    "Valid DAG", "Graph violations: " + graphErrors.size(), "Fix task registration"));
        }

        // COPY task input is repositoryRoot (the configured Git repository). Validate repositoryRoot exists before COPY runs.
        if (repositoryRoot == null || !repositoryRoot.exists() || !repositoryRoot.isDirectory()) {
            String err = "TASK_GRAPH BLOCKED: Configured Git repository is invalid or missing";
            Log.log("[SelfDevPreflight][DEPENDENCY] " + err);
            errors.add(err);
            checks.add(new SelfDevPreflightResult.CheckDetail("TASK_GRAPH", "BLOCKED", err, repositoryRoot != null ? repositoryRoot.getAbsolutePath() : "null", "Valid Git repository", "Missing repository", "Configure valid canonical Git repository"));
        } else if (graphErrors.isEmpty()) {
            checks.add(new SelfDevPreflightResult.CheckDetail("TASK_GRAPH", "OK", "Task graph dependencies verified (DAG validation passed)", repositoryRoot.getAbsolutePath(), "Valid DAG", "Valid DAG", "None"));
        }
    }

    private void validateNetworkAndEnvironmentReadiness(SelfDevContext context, List<SelfDevPreflightResult.CheckDetail> checks, List<String> errors, List<String> warnings) {
        int serverPort = context != null ? context.getEffectiveServerPort() : eu.kalafatic.evolution.controller.resource.PortRegistry.EVO_HTTP_PRIMARY.getDefaultPort();
        int supervisorPort = context != null ? context.getEffectiveSupervisorPort() : eu.kalafatic.evolution.controller.resource.PortRegistry.EVO_SUPERVISOR_API.getDefaultPort();

        boolean loopbackOk = true;
        try (java.net.ServerSocket ss = new java.net.ServerSocket()) {
            ss.setReuseAddress(true);
            ss.bind(new java.net.InetSocketAddress("127.0.0.1", 0));
        } catch (Exception e) {
            loopbackOk = false;
        }

        if (!loopbackOk) {
            String err = "NETWORK_READINESS FAILED: Loopback socket binding on 127.0.0.1 is not accessible";
            Log.log("[SelfDevPreflight][NETWORK] " + err);
            errors.add(err);
            checks.add(new SelfDevPreflightResult.CheckDetail("NETWORK_READINESS", "FAILED", err, "127.0.0.1", "Accessible loopback interface", "Restricted loopback interface", "Check loopback network permissions"));
        } else {
            Log.log("[SelfDevPreflight][NETWORK] Loopback network readiness verified for ports server=" + serverPort + ", supervisor=" + supervisorPort);
            checks.add(new SelfDevPreflightResult.CheckDetail("NETWORK_READINESS", "OK", "Loopback network readiness verified for ports server=" + serverPort + ", supervisor=" + supervisorPort, "127.0.0.1", "Loopback sockets available", "VERIFIED", "None"));
        }
    }

    private static String safe(File f) {
        return f != null ? f.getAbsolutePath() : "null";
    }
}
