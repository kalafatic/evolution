package eu.kalafatic.evolution.controller.orchestration.adapters;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

import eu.kalafatic.evolution.controller.orchestration.selfdev.GitManager;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;

public class GitAdapter {
    private final GitManager gitManager;

    public GitAdapter(GitManager gitManager) {
        this.gitManager = gitManager;
    }

    public void commit(String message, TaskContext context) throws Exception {
        gitManager.commit(message, context);
    }

    public void checkout(String branch) throws Exception {
        gitManager.forceCheckout(branch);
    }

    public void createBranchFrom(String base, String branch) throws Exception {
        gitManager.createBranchFrom(base, branch);
    }

    public void createBranch(String branchName) throws Exception {
        gitManager.createBranch(branchName);
    }

    public String getCurrentBranch() throws Exception {
        return gitManager.getCurrentBranch();
    }

    public String getHeadCommit() throws Exception {
        return gitManager.getHeadCommit();
    }

    public boolean isGitRepository() {
        return gitManager.isGitRepository();
    }

    public void ensureInitialCommit() throws Exception {
        gitManager.ensureInitialCommit();
    }

    public void cleanupLocks() {
        gitManager.cleanupLocks();
    }

    public void rollback() throws Exception {
        gitManager.rollback();
    }

    public void rollback(TaskContext context) throws Exception {
        gitManager.rollback(context);
    }

    public void merge(String branchName) throws Exception {
        gitManager.merge(branchName);
    }
}
