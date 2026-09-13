package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.io.File;
import eu.kalafatic.evolution.controller.resource.EvoPath;
import eu.kalafatic.evolution.controller.resource.ResourceManager;

public class CopySourceTask extends AbstractSelfDevTask {
    private final SourceProvider sourceProvider;

    public CopySourceTask(String id) {
        super(id, "Copy Codebase Task (" + id + ")");
        this.sourceProvider = new GitSourceProvider();
    }

    @Override
    protected TaskResult run(SelfDevContext context) throws Exception {
        File sourceRoot = ResourceManager.getInstance().getPath(EvoPath.EVO_ROOT).toFile();
        File targetDir = context.getSourceDirectory();

        if (sourceRoot == null || !sourceRoot.exists() || !new File(sourceRoot, "pom.xml").exists()) {
            return TaskResult.failure(id, "Source root directory invalid or missing pom.xml: " + (sourceRoot != null ? sourceRoot.getAbsolutePath() : "null"), null);
        }

        TaskResult res = sourceProvider.fetchSource(sourceRoot, targetDir);
        if (!res.isSuccess()) {
            return res;
        }

        // Post-copy verification
        if (!targetDir.exists() || !new File(targetDir, "pom.xml").exists()) {
            return TaskResult.failure(id, "Copy verification failed: destination directory " + targetDir.getAbsolutePath() + " is missing required pom.xml", null);
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
