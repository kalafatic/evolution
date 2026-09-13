package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.io.File;
import eu.kalafatic.evolution.controller.resource.ResourceManager;

public class CopySourceTask extends AbstractSelfDevTask {
    private final SourceProvider sourceProvider;

    public CopySourceTask(String id) {
        super(id, "Copy Codebase Task (" + id + ")");
        this.sourceProvider = new GitSourceProvider();
    }

    private File sourceRoot;
    private File targetDir;

    @Override
    protected void resolveResources(SelfDevContext context) throws Exception {
        ResourceManager rm = context.getResourceManager();
        if ("COPY_SUPERVISOR".equalsIgnoreCase(id)) {
            this.sourceRoot = rm.getSupervisorSource().toFile();
            this.targetDir = new File(context.getSourceDirectory(), "eu.kalafatic.evolution.supervisor");
        } else {
            this.sourceRoot = rm.getEvoSource().toFile();
            this.targetDir = context.getSourceDirectory();
        }
    }

    @Override
    protected TaskResult preValidate(SelfDevContext context) throws Exception {
        if (sourceRoot == null || !sourceRoot.exists()) {
            return TaskResult.failure(id, "CopySourceTask pre-validation failed: source root directory does not exist at " + (sourceRoot != null ? sourceRoot.getAbsolutePath() : "null"), null);
        }
        if (!new File(sourceRoot, "pom.xml").exists()) {
            return TaskResult.failure(id, "CopySourceTask pre-validation failed: source directory missing pom.xml at " + sourceRoot.getAbsolutePath(), null);
        }
        if (targetDir == null) {
            return TaskResult.failure(id, "CopySourceTask pre-validation failed: target directory is null", null);
        }
        return new TaskResult.Builder(id)
                .status(TaskStatus.READY)
                .message("Source directory verified: " + sourceRoot.getAbsolutePath())
                .workingDirectory(sourceRoot)
                .build();
    }

    @Override
    protected TaskResult run(SelfDevContext context) throws Exception {
        return sourceProvider.fetchSource(sourceRoot, targetDir);
    }

    @Override
    protected TaskResult postValidate(SelfDevContext context, TaskResult runResult) throws Exception {
        if (!runResult.isSuccess()) {
            return runResult;
        }

        if (!targetDir.exists() || !new File(targetDir, "pom.xml").exists()) {
            return TaskResult.failure(id, "Copy post-validation failed: destination directory " + targetDir.getAbsolutePath() + " is missing required pom.xml", null);
        }

        if (context != null) {
            context.discoverAndRepairModulePaths();
        }

        return new TaskResult.Builder(id)
                .status(TaskStatus.SUCCESS)
                .message("Source codebase copied and verified successfully at " + targetDir.getAbsolutePath())
                .workingDirectory(targetDir)
                .build();
    }
}
