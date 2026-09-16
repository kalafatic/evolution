package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.io.File;

public interface SourceProvider {
    TaskResult validateRepository(File repoRoot);
    String getSourceRevision(File repoRoot);
    String getBranch(File repoRoot);

    default String getRemote(File repoRoot) {
        return "none";
    }

    default TaskResult updateRepository(File repoRoot) {
        return new TaskResult.Builder("GIT_UPDATE")
                .status(TaskStatus.SUCCESS)
                .message("No update performed.")
                .build();
    }

    TaskResult fetchSource(File repoRoot, File targetDir);
}
