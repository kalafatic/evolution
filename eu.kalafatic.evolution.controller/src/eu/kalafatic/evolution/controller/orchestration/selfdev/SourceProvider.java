package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.io.File;

public interface SourceProvider {
    TaskResult validateRepository(File repoRoot);
    String getSourceRevision(File repoRoot);
    String getBranch(File repoRoot);
    TaskResult fetchSource(File repoRoot, File targetDir);
}
