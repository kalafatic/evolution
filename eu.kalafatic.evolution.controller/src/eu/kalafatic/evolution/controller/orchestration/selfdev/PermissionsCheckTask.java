package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.io.File;

public class PermissionsCheckTask extends AbstractSelfDevTask {

    public PermissionsCheckTask(String id) {
        super(id, "Filesystem Permissions Check (" + id + ")");
    }

    private File logDir;

    @Override
    protected void resolveResources(SelfDevContext context) throws Exception {
        ResolvedSelfDevResources res = context != null ? context.getResolvedResources() : null;
        this.logDir = (res != null && res.getLogDirectory() != null) ? res.getLogDirectory() : (context != null ? context.getLogDirectory() : null);
    }

    @Override
    protected TaskResult preValidate(SelfDevContext context) throws Exception {
        if (logDir == null) {
            return TaskResult.failure(id, "PermissionsCheckTask pre-validation failed: Log directory is null", null);
        }

        if (!logDir.exists()) {
            boolean created = logDir.mkdirs();
            if (!created) {
                return TaskResult.failure(id, "PermissionsCheckTask pre-validation failed: Could not create log directory at " + logDir.getAbsolutePath(), null);
            }
        }

        return new TaskResult.Builder(id)
                .status(TaskStatus.READY)
                .message("Log directory verified at " + logDir.getAbsolutePath())
                .workingDirectory(logDir)
                .build();
    }

    @Override
    protected TaskResult run(SelfDevContext context) throws Exception {
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
