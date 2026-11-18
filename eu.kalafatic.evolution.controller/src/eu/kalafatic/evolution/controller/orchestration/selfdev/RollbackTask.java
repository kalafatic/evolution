package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import eu.kalafatic.evolution.controller.log.Log;

/**
 * WP10 - honest, scoped rollback.
 *
 * Explicit scopes: PROCESS_ONLY, DEPLOYMENT, SOURCE_COPY, GIT_LOCAL.
 * Every step returns an explicit result; exceptions are never swallowed; the
 * final result is SUCCESS only when all requested critical steps succeeded.
 * Git-local recovery is simulation-only by default (WP9 boundary).
 */
public class RollbackTask extends AbstractSelfDevTask {

    public enum RollbackScope {
        PROCESS_ONLY,
        DEPLOYMENT,
        SOURCE_COPY,
        GIT_LOCAL
    }

    private final List<RollbackScope> scopes;

    public RollbackTask(String id) {
        this(id, List.of(RollbackScope.PROCESS_ONLY, RollbackScope.DEPLOYMENT, RollbackScope.SOURCE_COPY, RollbackScope.GIT_LOCAL));
    }

    public RollbackTask(String id, List<RollbackScope> scopes) {
        super(id, "Rollback Deployment and State Task (" + id + ")");
        this.scopes = scopes != null ? scopes : List.of();
    }

    @Override
    protected TaskResult run(SelfDevContext context) throws Exception {
        long startTime = System.currentTimeMillis();
        if (context == null) {
            return TaskResult.failure(id, "RollbackTask failed: SelfDevContext is null", null);
        }

        String runId = context.getRunId();
        String rollbackReason = resolveRollbackReason(context);

        Log.log("[SELF-DEV][ROLLBACK] Initiating rollback for run: " + runId + " scopes=" + scopes);
        Log.log("[SELF-DEV][ROLLBACK] Reason: " + rollbackReason);

        Map<String, String> stepResults = new LinkedHashMap<>();
        List<String> failures = new ArrayList<>();
        boolean processRestoreRequired = false;

        // Determine whether a child was owned/running before rollback (for the
        // failed-startup recovery sequence).
        ProcessOwnershipRecord evoOwnershipBefore =
                ProcessOwnershipRecord.read(context, ProcessOwnershipRecord.ProcessKind.EVO_RCP);

        // 1. PROCESS_ONLY: stop owned children with explicit results.
        if (scopes.contains(RollbackScope.PROCESS_ONLY)) {
            TaskResult stopEvo = executeStop(new StopEvoTask("STOP_EVO"), context);
            stepResults.put("PROCESS_ONLY.STOP_EVO", stopEvo.getStatus() + ": " + stopEvo.getMessage());
            if (!stopEvo.isSuccess()) {
                failures.add("STOP_EVO failed: " + stopEvo.getMessage());
            }
            TaskResult stopSupervisor = executeStop(new StopSupervisorTask("STOP_SUPERVISOR"), context);
            stepResults.put("PROCESS_ONLY.STOP_SUPERVISOR", stopSupervisor.getStatus() + ": " + stopSupervisor.getMessage());
            if (!stopSupervisor.isSuccess()) {
                failures.add("STOP_SUPERVISOR failed: " + stopSupervisor.getMessage());
            }
            processRestoreRequired = evoOwnershipBefore != null;
        }

        // 2. DEPLOYMENT: deterministic known-good restore with checksum verification.
        String deploymentState = "UNCHANGED";
        if (scopes.contains(RollbackScope.DEPLOYMENT)) {
            TaskResult restoreRes = restoreDeployment(context);
            stepResults.put("DEPLOYMENT.RESTORE", restoreRes.getStatus() + ": " + restoreRes.getMessage());
            deploymentState = restoreRes.getDiagnostic("deploymentState", restoreRes.getStatus().name());
            if (!restoreRes.isSuccess()) {
                failures.add("DEPLOYMENT restore failed: " + restoreRes.getMessage());
            }
        }

        // 3. SOURCE_COPY / GIT_LOCAL: iteration-scoped restoration (simulation by default).
        if (scopes.contains(RollbackScope.SOURCE_COPY) || scopes.contains(RollbackScope.GIT_LOCAL)) {
            TaskResult gitRes = restoreSourceCopy(context);
            stepResults.put("GIT_LOCAL.RESTORE", gitRes.getStatus() + ": " + gitRes.getMessage());
            if (!gitRes.isSuccess()) {
                failures.add("GIT_LOCAL restore failed: " + gitRes.getMessage());
            }
        }

        // 4. Restart + verify the restored runtime when a child was owned before
        // (failed-startup recovery sequence).
        if (processRestoreRequired && scopes.contains(RollbackScope.DEPLOYMENT) && failures.isEmpty()) {
            TaskResult restartRes = restartRestoredRuntime(context);
            stepResults.put("RECOVERY.RESTART", restartRes.getStatus() + ": " + restartRes.getMessage());
            if (!restartRes.isSuccess()) {
                failures.add("Restored runtime verification failed: " + restartRes.getMessage());
            }
        }

        long duration = System.currentTimeMillis() - startTime;

        TaskResult.Builder builder;
        if (failures.isEmpty()) {
            builder = new TaskResult.Builder(id).status(TaskStatus.SUCCESS)
                    .message("Rollback completed successfully (scopes: " + scopes + "). Reason: " + rollbackReason);
        } else {
            builder = new TaskResult.Builder(id).status(TaskStatus.FAILED)
                    .message("Rollback FAILED: " + String.join("; ", failures));
        }
        builder.duration(duration)
                .diagnostic("currentRun", runId)
                .diagnostic("rollbackReason", rollbackReason)
                .diagnostic("deploymentState", deploymentState)
                .diagnostic("scopes", scopes.toString());
        for (Map.Entry<String, String> e : stepResults.entrySet()) {
            builder.diagnostic(e.getKey(), e.getValue());
        }

        TaskResult result = builder.build();
        context.recordTaskResult(result);
        return result;
    }

    private TaskResult executeStop(SelfDevTask stopTask, SelfDevContext context) {
        try {
            return stopTask.execute(context);
        } catch (Exception e) {
            return TaskResult.failure(stopTask.getId(), "Stop exception: " + e.getMessage(), e);
        }
    }

    /**
     * Restores the deterministic newest verified known-good deployment.
     * The previous known-good remains protected until the restore completes.
     */
    private TaskResult restoreDeployment(SelfDevContext context) {
        File runtimeDir = context.getRuntimeDirectory();
        File evoRuntime = new File(runtimeDir, "evo");

        File promotedDir = new File(context.getExportDirectory(), "promoted");
        BuildArtifact knownGood = PromoteTask.findNewestVerifiedPromoted(promotedDir);

        if (knownGood == null || knownGood.getPath() == null || !knownGood.getPath().exists()) {
            return new TaskResult.Builder("rollback_deploy")
                    .status(TaskStatus.FAILED)
                    .message("Rollback deployment restore failed: no verified known-good artifact available under " + promotedDir.getAbsolutePath())
                    .diagnostic("deploymentState", "NO_KNOWN_GOOD")
                    .build();
        }

        // Checksum verification of the known-good before touching anything.
        if (knownGood.getSha256() != null && !knownGood.getSha256().isEmpty()) {
            String actual = ArtifactManifestStore.computeSha256(knownGood.getPath());
            if (actual == null || !actual.equalsIgnoreCase(knownGood.getSha256())) {
                return new TaskResult.Builder("rollback_deploy")
                        .status(TaskStatus.FAILED)
                        .message("Rollback deployment restore failed: known-good checksum mismatch at " + knownGood.getPath().getAbsolutePath()
                                + " (index=" + knownGood.getSha256() + ", actual=" + actual + ")")
                        .diagnostic("deploymentState", "CHECKSUM_MISMATCH")
                        .build();
            }
        }

        try {
            if (evoRuntime.exists()) {
                deleteRecursively(evoRuntime);
            }
            evoRuntime.mkdirs();

            if (knownGood.getPath().isDirectory()) {
                copyDirectory(knownGood.getPath(), evoRuntime);
            } else if (knownGood.getPath().getName().endsWith(".zip")) {
                new EvoRcpDeployer().deploy(context, knownGood);
            } else {
                File dest = new File(evoRuntime, knownGood.getPath().getName());
                Files.copy(knownGood.getPath().toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }

            // Restore verification: layout validation of the restored deployment.
            EvoRcpDeployer deployer = new EvoRcpDeployer();
            File restoredRoot = knownGood.getPath().isDirectory() ? evoRuntime : evoRuntime;
            TaskResult layout = deployer.validateDeployment(context, restoredRoot);
            if (!layout.isSuccess()) {
                return new TaskResult.Builder("rollback_deploy")
                        .status(TaskStatus.FAILED)
                        .message("Restored deployment failed layout validation: " + layout.getMessage())
                        .diagnostic("deploymentState", "RESTORED_INVALID_LAYOUT")
                        .build();
            }

            return new TaskResult.Builder("rollback_deploy")
                    .status(TaskStatus.SUCCESS)
                    .message("Previous known-good deployment restored and validated: " + knownGood.getPath().getAbsolutePath())
                    .artifact(knownGood)
                    .diagnostic("deploymentState", "RESTORED_PREVIOUS_KNOWN_GOOD")
                    .build();
        } catch (Exception e) {
            return new TaskResult.Builder("rollback_deploy")
                    .status(TaskStatus.FAILED)
                    .message("Deployment restore exception: " + e.getMessage())
                    .error(e)
                    .diagnostic("deploymentState", "RESTORE_EXCEPTION")
                    .build();
        }
    }

    /**
     * Iteration-scoped source restoration (simulation by default via GitManager).
     * Failures propagate; exceptions are never swallowed (D-04).
     */
    private TaskResult restoreSourceCopy(SelfDevContext context) {
        File preparedSource = context.getPreparedReactorDirectory();
        if (preparedSource == null || !preparedSource.exists()) {
            return TaskResult.success("rollback_git", "No prepared source copy to restore.");
        }
        GitManager manager = new GitManager(preparedSource);
        if (!manager.isGitRepository()) {
            return TaskResult.success("rollback_git", "Prepared source copy is not a Git repository; nothing to restore.");
        }
        GitBaseline baseline = GitBaseline.load(context);
        if (baseline == null) {
            return TaskResult.failure("rollback_git", "Git-local rollback refused: no iteration baseline captured (destructive rollback is forbidden).", null);
        }
        try {
            List<String> currentLines = new ArrayList<>();
            String status = manager.getStatusPorcelain();
            if (status != null && !status.trim().isEmpty()) {
                for (String line : status.split("\n")) {
                    if (!line.trim().isEmpty()) {
                        currentLines.add(line);
                    }
                }
            }
            java.util.Set<String> changed = baseline.iterationChangedPaths(currentLines);
            GitManager.GitOperationResult res = manager.restoreIterationBaseline(baseline, new ArrayList<>(changed), null);
            if (!res.isSuccess()) {
                return TaskResult.failure("rollback_git", "Git-local rollback failed: " + res.getMessage(), null);
            }
            return TaskResult.success("rollback_git", "Iteration-scoped source restoration completed (simulated: " + res.isSimulated() + ").");
        } catch (Exception e) {
            return TaskResult.failure("rollback_git", "Git-local rollback exception: " + e.getMessage(), e);
        }
    }

    /**
     * Restarts and verifies the restored runtime (failed-startup recovery).
     */
    private TaskResult restartRestoredRuntime(SelfDevContext context) {
        EvoRcpRuntime runtime = new EvoRcpRuntime();
        try {
            TaskResult startRes = runtime.start(context);
            if (!startRes.isSuccess()) {
                return TaskResult.failure("rollback_restart", "Restored runtime failed to start: " + startRes.getMessage(), null);
            }
            TaskResult readyRes = runtime.waitUntilReady(context, 30);
            if (!readyRes.isSuccess()) {
                return TaskResult.failure("rollback_restart", "Restored runtime did not reach readiness: " + readyRes.getMessage(), null);
            }
            return TaskResult.success("rollback_restart", "Restored runtime started and reached readiness (" + readyRes.getDiagnostic("readinessLevel", "UNKNOWN") + ").");
        } catch (Exception e) {
            return TaskResult.failure("rollback_restart", "Restored runtime verification exception: " + e.getMessage(), e);
        }
    }

    private String resolveRollbackReason(SelfDevContext context) {
        String[] taskIdsToCheck = new String[] {
            "CREATE_INFERENCE_TASK",
            "CREATE_EVO_PROJECT",
            "START_EVO",
            "DEPLOY_EVO",
            "EXPORT_EVO",
            "BUILD_EVO"
        };
        for (String tid : taskIdsToCheck) {
            TaskResult r = context.getTaskResult(tid);
            if (r != null && !r.isSuccess()) {
                return tid + " failed: " + r.getMessage();
            }
        }
        return "Manual or defensive rollback requested";
    }

    private void copyDirectory(File src, File dest) throws IOException {
        Path srcPath = src.toPath();
        Path destPath = dest.toPath();
        Files.walk(srcPath).forEach(source -> {
            try {
                Path destination = destPath.resolve(srcPath.relativize(source));
                if (Files.isDirectory(source)) {
                    if (!Files.exists(destination)) Files.createDirectories(destination);
                } else {
                    Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void deleteRecursively(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        file.delete();
    }
}
