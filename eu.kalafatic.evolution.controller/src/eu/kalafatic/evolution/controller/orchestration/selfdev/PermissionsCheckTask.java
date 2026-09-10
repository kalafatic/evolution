package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.io.File;

public class PermissionsCheckTask extends AbstractSelfDevTask {

    public PermissionsCheckTask(String id) {
        super(id, "Filesystem Permissions Check (" + id + ")");
    }

    @Override
    protected TaskResult run(SelfDevContext context) throws Exception {
        File logDir = context.getLogDirectory();
        if (logDir == null) {
            return TaskResult.failure(id, "Log directory is null", null);
        }

        if (!logDir.exists()) {
            boolean created = logDir.mkdirs();
            if (!created) {
                return TaskResult.failure(id, "Failed to create log directory: " + logDir.getAbsolutePath(), null);
            }
        }

        try {
            File testFile = new File(logDir, ".perm_test_" + System.currentTimeMillis());
            boolean created = testFile.createNewFile();
            if (!created) {
                return TaskResult.failure(id, "Permission check failed: Could not create test file in " + logDir.getAbsolutePath(), null);
            }
            boolean deleted = testFile.delete();
            if (!deleted) {
                logError("Warning: Could not delete test permission file: " + testFile.getAbsolutePath());
            }

            return new TaskResult.Builder(id)
                    .status(TaskStatus.SUCCESS)
                    .message("Filesystem write permission verified in " + logDir.getAbsolutePath())
                    .workingDirectory(logDir)
                    .build();

        } catch (Exception e) {
            return TaskResult.failure(id, "Filesystem permission check exception: " + e.getMessage(), e);
        }
    }
}
