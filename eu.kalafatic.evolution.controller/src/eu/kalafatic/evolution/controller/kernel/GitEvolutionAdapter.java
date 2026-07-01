package eu.kalafatic.evolution.controller.kernel;

import eu.kalafatic.evolution.controller.orchestration.TaskContext;

/**
 * Interface for evolution-aware Git operations.
 */
public interface GitEvolutionAdapter {
    boolean isGitRepository();
    void ensureInitialCommit() throws Exception;
    String getCurrentBranch() throws Exception;
    String getHeadCommit() throws Exception;
    void forceCheckout(String branchName) throws Exception;
    void merge(String branchName) throws Exception;
    void commit(String message, TaskContext context) throws Exception;
    void rollback() throws Exception;
}
