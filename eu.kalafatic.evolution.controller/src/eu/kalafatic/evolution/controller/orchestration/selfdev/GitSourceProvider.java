package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.io.File;
import eu.kalafatic.evolution.controller.tools.GitTool;

public class GitSourceProvider implements SourceProvider {
    private final GitTool gitTool = new GitTool();

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
            String rev = gitManager.getHeadCommit();
            return rev != null ? rev : "UNKNOWN";
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }

    @Override
    public String getBranch(File repoRoot) {
        try {
            GitManager gitManager = new GitManager(repoRoot);
            String branch = gitManager.getCurrentBranch();
            return branch != null ? branch : "main";
        } catch (Exception e) {
            return "main";
        }
    }

    @Override
    public String getRemote(File repoRoot) {
        try {
            String remote = gitTool.execute("remote get-url origin", repoRoot, null).trim();
            if (remote != null && !remote.isEmpty() && !remote.contains("fatal:")) {
                return remote;
            }
        } catch (Exception e) {
            // Ignore and try fallback
        }
        try {
            String remotes = gitTool.execute("remote", repoRoot, null).trim();
            return remotes.isEmpty() ? "none" : remotes;
        } catch (Exception e) {
            return "none";
        }
    }

    @Override
    public TaskResult updateRepository(File repoRoot) {
        try {
            String remote = getRemote(repoRoot);
            if ("none".equalsIgnoreCase(remote)) {
                return new TaskResult.Builder("GIT_UPDATE")
                        .status(TaskStatus.SUCCESS)
                        .message("No remote configured, skipping pull/fetch.")
                        .build();
            }
            try {
                gitTool.execute("pull --ff-only", repoRoot, null);
            } catch (Exception e) {
                try {
                    gitTool.execute("fetch origin", repoRoot, null);
                } catch (Exception ex) {
                    return new TaskResult.Builder("GIT_UPDATE")
                            .status(TaskStatus.FAILED)
                            .message("Failed to update repository from remote: " + ex.getMessage())
                            .build();
                }
            }
            return new TaskResult.Builder("GIT_UPDATE")
                    .status(TaskStatus.SUCCESS)
                    .message("Repository updated from remote.")
                    .build();
        } catch (Exception e) {
            return new TaskResult.Builder("GIT_UPDATE")
                    .status(TaskStatus.FAILED)
                    .message("Failed to update repository: " + e.getMessage())
                    .build();
        }
    }

    public String getStatus(File repoRoot) {
        try {
            String status = gitTool.execute("status --porcelain", repoRoot, null).trim();
            return status.isEmpty() ? "CLEAN" : "CHANGED";
        } catch (Exception e) {
            return "FAILED";
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
            String dirName = sourceLocation.getName();
            if (isExcludedDirectory(sourceLocation, dirName)) {
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

    private boolean isExcludedDirectory(File sourceLocation, String dirName) {
        if (dirName.equalsIgnoreCase(".git") ||
            dirName.equalsIgnoreCase("projects") ||
            dirName.equalsIgnoreCase("self-dev-run") ||
            dirName.equalsIgnoreCase("iterations") ||
            dirName.equalsIgnoreCase(".settings") ||
            dirName.equalsIgnoreCase("forge-output") ||
            dirName.equalsIgnoreCase("dist")) {
            return true;
        }
        if (dirName.equalsIgnoreCase("target") || dirName.equalsIgnoreCase("bin")) {
            return !isInsideSourceDirectory(sourceLocation);
        }
        return false;
    }

    private boolean isInsideSourceDirectory(File file) {
        File parent = file.getParentFile();
        while (parent != null) {
            if ("src".equalsIgnoreCase(parent.getName())) {
                return true;
            }
            parent = parent.getParentFile();
        }
        return false;
    }
}
