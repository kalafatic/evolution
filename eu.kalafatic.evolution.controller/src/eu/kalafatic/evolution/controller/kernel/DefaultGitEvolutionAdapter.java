package eu.kalafatic.evolution.controller.kernel;

import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.selfdev.GitManager;

public class DefaultGitEvolutionAdapter implements GitEvolutionAdapter {
    private final GitManager gitManager;

    public DefaultGitEvolutionAdapter(GitManager gitManager) {
        this.gitManager = gitManager;
    }

    @Override
    public boolean isGitRepository() {
        return gitManager.isGitRepository();
    }

    @Override
    public void ensureInitialCommit() throws Exception {
        gitManager.ensureInitialCommit();
    }

    @Override
    public String getCurrentBranch() throws Exception {
        return gitManager.getCurrentBranch();
    }

    @Override
    public String getHeadCommit() throws Exception {
        return gitManager.getHeadCommit();
    }

    @Override
    public void forceCheckout(String branchName) throws Exception {
        gitManager.forceCheckout(branchName);
    }

    @Override
    public void merge(String branchName) throws Exception {
        gitManager.merge(branchName);
    }

    @Override
    public void commit(String message, TaskContext context) throws Exception {
        gitManager.commit(message, context);
    }

    @Override
    public void rollback() throws Exception {
        gitManager.rollback();
    }
}
