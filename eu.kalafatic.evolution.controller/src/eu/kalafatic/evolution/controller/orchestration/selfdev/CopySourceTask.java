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
        ResolvedSelfDevResources res = context != null ? context.getResolvedResources() : null;

        if ("COPY_SUPERVISOR".equalsIgnoreCase(id)) {
            this.sourceRoot = (res != null && res.getSupervisorDirectory() != null) ? res.getSupervisorDirectory() : (context != null ? context.getSupervisorDirectory() : null);
            File baseSourceDir = (res != null && res.getSourceDirectory() != null) ? res.getSourceDirectory() : (context != null ? context.getSourceDirectory() : null);
            this.targetDir = baseSourceDir != null ? new File(baseSourceDir, "eu.kalafatic.evolution.supervisor") : null;
        } else {
            this.sourceRoot = (res != null && res.getReactorDirectory() != null) ? res.getReactorDirectory() : ((res != null && res.getRepositoryRoot() != null) ? res.getRepositoryRoot() : (context != null ? context.getRepositoryRoot() : null));
            this.targetDir = (res != null && res.getSourceDirectory() != null) ? res.getSourceDirectory() : (context != null ? context.getSourceDirectory() : null);
        }

        logTaskStep("PATH_SOURCE", "property=sourceRoot, value=" + (sourceRoot != null ? sourceRoot.getAbsolutePath() : "null") + ", origin=" + (res != null ? "ResolvedSelfDevResources" : "SelfDevContext"));
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
        if (sourceRoot.getCanonicalFile().equals(targetDir.getCanonicalFile())) {
            return TaskResult.failure(id, "COPY FAILED: Source repository (" + sourceRoot.getAbsolutePath() + ") and build workspace target (" + targetDir.getAbsolutePath() + ") are identical. Source repository must remain untouched by builds.", null);
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
