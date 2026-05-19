package eu.kalafatic.evolution.controller.kernel;

import eu.kalafatic.evolution.controller.orchestration.selfdev.GitManager;

public class DefaultBranchManager implements BranchManager {
    private final GitManager gitManager;

    public DefaultBranchManager(GitManager gitManager) {
        this.gitManager = gitManager;
    }

    @Override
    public void provisionBranch(String base, String newBranch) throws Exception {
        gitManager.createBranchFrom(base, newBranch);
    }

    @Override
    public void createWorktree(String branch, String path) throws Exception {
        gitManager.createWorktree(branch, path);
    }

    @Override
    public void removeWorktree(String path) throws Exception {
        gitManager.removeWorktree(path);
    }

    @Override
    public void cleanupWorktrees() throws Exception {
        gitManager.cleanupWorktrees();
    }
}
