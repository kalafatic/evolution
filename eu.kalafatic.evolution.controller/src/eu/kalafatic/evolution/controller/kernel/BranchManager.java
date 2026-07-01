package eu.kalafatic.evolution.controller.kernel;

/**
 * Interface for managing branch provisioning and worktrees.
 */
public interface BranchManager {
    void provisionBranch(String base, String newBranch) throws Exception;
    void createWorktree(String branch, String path) throws Exception;
    void removeWorktree(String path) throws Exception;
    void cleanupWorktrees() throws Exception;
}
