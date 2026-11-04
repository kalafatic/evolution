package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import eu.kalafatic.evolution.controller.log.Log;

public class RollbackTask extends AbstractSelfDevTask {

    public RollbackTask(String id) {
        super(id, "Rollback Deployment and State Task (" + id + ")");
    }

    @Override
    protected TaskResult run(SelfDevContext context) throws Exception {
        long startTime = System.currentTimeMillis();
        if (context == null) {
            return TaskResult.failure(id, "RollbackTask failed: SelfDevContext is null", null);
        }

        String runId = context.getRunId();
        String rollbackReason = resolveRollbackReason(context);
        File runtimeDir = context.getRuntimeDirectory();
        File evoRuntime = new File(runtimeDir, "evo");
        BuildArtifact candidateArtifact = context.getArtifact(ArtifactType.EVO_RCP);
        File candidateFile = candidateArtifact != null ? candidateArtifact.getPath() : null;

        Log.log("[SELF-DEV][ROLLBACK] Initiating rollback for run: " + runId);
        Log.log("[SELF-DEV][ROLLBACK] Reason: " + rollbackReason);
        System.out.println("[SELF-DEV][ROLLBACK] Initiating rollback for run: " + runId);
        System.out.println("[SELF-DEV][ROLLBACK] Reason: " + rollbackReason);

        // 1. Ensure any running processes owned by this run are stopped
        try {
            new StopEvoTask("STOP_EVO").execute(context);
        } catch (Exception ignored) {}
        try {
            new StopSupervisorTask("STOP_SUPERVISOR").execute(context);
        } catch (Exception ignored) {}

        // 2. Identify previous known-good artifact
        File previousKnownGood = findPreviousKnownGoodArtifact(context);
        String deploymentState = "ROLLED_BACK";
        boolean restored = false;

        if (previousKnownGood != null && previousKnownGood.exists()) {
            Log.log("[SELF-DEV][ROLLBACK] Restoring previous known-good artifact from: " + previousKnownGood.getAbsolutePath());
            System.out.println("[SELF-DEV][ROLLBACK] Restoring previous known-good artifact from: " + previousKnownGood.getAbsolutePath());

            try {
                if (evoRuntime.exists()) {
                    deleteRecursively(evoRuntime);
                }
                evoRuntime.mkdirs();

                if (previousKnownGood.isDirectory()) {
                    copyDirectory(previousKnownGood, evoRuntime);
                } else {
                    File dest = new File(evoRuntime, previousKnownGood.getName());
                    Files.copy(previousKnownGood.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
                restored = true;
                deploymentState = "RESTORED_PREVIOUS_KNOWN_GOOD";
            } catch (Exception e) {
                Log.log("[SELF-DEV][ROLLBACK] Warning: Failed to restore previous known-good artifact: " + e.getMessage());
                deploymentState = "CLEARED_BROKEN_DEPLOYMENT";
            }
        } else {
            // No previous known good - clear broken runtime deployment cleanly
            if (evoRuntime.exists()) {
                deleteRecursively(evoRuntime);
            }
            deploymentState = "CLEARED_CANDIDATE_DEPLOYMENT";
        }

        // 3. Rollback Git changes in prepared reactor if modified
        try {
            File preparedSource = context.getPreparedReactorDirectory();
            if (preparedSource != null && preparedSource.exists() && new GitManager(preparedSource).isGitRepository()) {
                new GitManager(preparedSource).rollback(null);
            }
        } catch (Exception ignored) {}

        long duration = System.currentTimeMillis() - startTime;
        String actualResult = restored ? "Restored previous known-good artifact" : "Candidate artifact cleaned and rolled back";

        TaskResult result = new TaskResult.Builder(id)
                .status(TaskStatus.SUCCESS)
                .message("Rollback executed successfully: " + actualResult + ". Reason: " + rollbackReason)
                .duration(duration)
                .diagnostic("currentRun", runId)
                .diagnostic("rollbackReason", rollbackReason)
                .diagnostic("deploymentState", deploymentState)
                .diagnostic("actualRollbackResult", actualResult)
                .diagnostic("previousKnownGoodArtifact", previousKnownGood != null ? previousKnownGood.getAbsolutePath() : "none")
                .diagnostic("candidateArtifact", candidateFile != null ? candidateFile.getAbsolutePath() : "none")
                .diagnostic("processOwnership", SelfDevContext.ProcessOwnership.CURRENT_RUN.name())
                .build();

        context.recordTaskResult(result);
        return result;
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

    private File findPreviousKnownGoodArtifact(SelfDevContext context) {
        File exportDir = context.getExportDirectory();
        if (exportDir != null) {
            File promotedDir = new File(exportDir.getParentFile(), "promoted");
            if (promotedDir.exists() && promotedDir.isDirectory()) {
                File[] files = promotedDir.listFiles();
                if (files != null && files.length > 0) {
                    return files[0];
                }
            }
        }
        return null;
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
