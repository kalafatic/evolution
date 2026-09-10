package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.io.File;

public class GitSourceProvider implements SourceProvider {

    @Override
    public TaskResult validateRepository(File repoRoot) {
        if (repoRoot == null || !repoRoot.exists()) {
            return new TaskResult.Builder("GIT_VAL")
                    .status(TaskStatus.FAILED)
                    .message("Repository root directory does not exist.")
                    .build();
        }
        GitManager gitManager = new GitManager(repoRoot);
        if (gitManager.isGitRepository()) {
            return new TaskResult.Builder("GIT_VAL")
                    .status(TaskStatus.SUCCESS)
                    .message("Git repository verified.")
                    .build();
        }
        return new TaskResult.Builder("GIT_VAL")
                .status(TaskStatus.FAILED)
                .message("Directory is not a valid Git repository.")
                .build();
    }

    @Override
    public String getSourceRevision(File repoRoot) {
        try {
            GitManager gitManager = new GitManager(repoRoot);
            return gitManager.getHeadCommit();
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }

    @Override
    public String getBranch(File repoRoot) {
        try {
            GitManager gitManager = new GitManager(repoRoot);
            return gitManager.getCurrentBranch();
        } catch (Exception e) {
            return "main";
        }
    }

    @Override
    public TaskResult fetchSource(File repoRoot, File targetDir) {
        try {
            if (!targetDir.exists()) {
                targetDir.mkdirs();
            }
            // Copy repository tree
            copyDirectory(repoRoot, targetDir);
            return new TaskResult.Builder("GIT_FETCH")
                    .status(TaskStatus.SUCCESS)
                    .message("Repository source fetched/copied successfully.")
                    .build();
        } catch (Exception e) {
            return new TaskResult.Builder("GIT_FETCH")
                    .status(TaskStatus.FAILED)
                    .message("Failed to fetch source: " + e.getMessage())
                    .build();
        }
    }

    private void copyDirectory(File sourceLocation, File targetLocation) throws Exception {
        if (sourceLocation.isDirectory()) {
            if (sourceLocation.getName().equals(".git") || sourceLocation.getName().equals("target")) {
                return;
            }
            if (!targetLocation.exists()) {
                targetLocation.mkdirs();
            }
            String[] children = sourceLocation.list();
            if (children != null) {
                for (String child : children) {
                    copyDirectory(new File(sourceLocation, child), new File(targetLocation, child));
                }
            }
        } else {
            java.nio.file.Files.copy(sourceLocation.toPath(), targetLocation.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
