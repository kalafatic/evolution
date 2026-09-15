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
            File prepared = (res != null && res.getPreparedReactorDirectory() != null) ? res.getPreparedReactorDirectory() : (context != null ? context.getPreparedReactorDirectory() : null);
            this.targetDir = prepared != null ? new File(prepared, "eu.kalafatic.evolution.supervisor") : null;
        } else {
            this.sourceRoot = (res != null && res.getSourceReactorDirectory() != null) ? res.getSourceReactorDirectory() : (context != null ? context.getSourceReactorDirectory() : null);
            this.targetDir = (res != null && res.getPreparedReactorDirectory() != null) ? res.getPreparedReactorDirectory() : (context != null ? context.getPreparedReactorDirectory() : null);
        }

        logTaskStep("PATH_SOURCE", "property=sourceRoot, value=" + (sourceRoot != null ? sourceRoot.getAbsolutePath() : "null") + ", origin=" + (res != null ? "ResolvedSelfDevResources" : "SelfDevContext"));
        logTaskStep("PATH_TARGET", "property=targetDir, value=" + (targetDir != null ? targetDir.getAbsolutePath() : "null") + ", origin=" + (res != null ? "ResolvedSelfDevResources" : "SelfDevContext"));
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

        boolean samePath = sourceRoot.getCanonicalFile().equals(targetDir.getCanonicalFile());
        if (samePath) {
            eu.kalafatic.evolution.controller.log.Log.log("[COPY]\nsourceReactor=" + sourceRoot.getAbsolutePath() + "\npreparedReactor=" + targetDir.getAbsolutePath() + "\nsamePath=true\ndestinationPom=false\nresult=FAILED");
            return TaskResult.failure(id, "COPY FAILED: Source reactor (" + sourceRoot.getAbsolutePath() + ") and prepared reactor (" + targetDir.getAbsolutePath() + ") resolve to the same directory.", null);
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
        boolean destinationPom = targetDir.exists() && new File(targetDir, "pom.xml").exists();
        boolean success = runResult.isSuccess() && destinationPom;

        eu.kalafatic.evolution.controller.log.Log.log("[COPY]\nsourceReactor=" + sourceRoot.getAbsolutePath() +
                "\npreparedReactor=" + targetDir.getAbsolutePath() +
                "\nsamePath=false\ndestinationPom=" + destinationPom +
                "\nresult=" + (success ? "SUCCESS" : "FAILED"));

        if (!runResult.isSuccess()) {
            return runResult;
        }

        if (!destinationPom) {
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
