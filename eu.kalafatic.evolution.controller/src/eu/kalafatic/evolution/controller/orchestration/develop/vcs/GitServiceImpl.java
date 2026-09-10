package eu.kalafatic.evolution.controller.orchestration.develop.vcs;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import eu.kalafatic.evolution.controller.orchestration.develop.executor.ProcessExecutor;
import eu.kalafatic.evolution.controller.orchestration.develop.executor.ProcessExecutorImpl;
import eu.kalafatic.evolution.controller.orchestration.selfdev.GitManager;
import eu.kalafatic.evolution.controller.vcs.GitVersionControlProvider;

/**
 * Concrete GitService implementation using GitVersionControlProvider and GitManager.
 */
public class GitServiceImpl implements GitService {

    private final GitVersionControlProvider gitProvider = new GitVersionControlProvider();
    private final ProcessExecutor processExecutor = new ProcessExecutorImpl();

    @Override
    public void cloneRepository(String repoUrl, String branch, File destinationDir) throws Exception {
        if (repoUrl == null || repoUrl.trim().isEmpty()) {
            ensureInitialCommit(destinationDir);
            return;
        }

        String targetBranch = (branch != null && !branch.trim().isEmpty()) ? branch.trim() : "main";
        if (!destinationDir.exists()) {
            destinationDir.mkdirs();
        }

        try {
            ProcessExecutor.ProcessResult res = processExecutor.execute("git clone --branch " + targetBranch + " " + repoUrl.trim() + " .", destinationDir, null, 120000);
            if (!res.isSuccess()) {
                processExecutor.execute("git clone " + repoUrl.trim() + " .", destinationDir, null, 120000);
            }
        } catch (Exception ex) {
            ensureInitialCommit(destinationDir);
        }
    }

    @Override
    public void createAndCheckoutBranch(File repoDir, String branchName) throws Exception {
        GitManager gitManager = new GitManager(repoDir);
        gitManager.ensureInitialCommit();
        if (branchName != null && !branchName.trim().isEmpty()) {
            try {
                gitManager.createBranch(branchName.trim());
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public String getDiff(File repoDir, String revision) throws Exception {
        if (!repoDir.exists()) return "";
        String ref = (revision != null && !revision.trim().isEmpty()) ? revision.trim() : "HEAD";
        return gitProvider.getDiff(repoDir, ref);
    }

    @Override
    public List<String> getChangedFiles(File repoDir, String revision) throws Exception {
        if (!repoDir.exists()) return new ArrayList<>();
        String ref = (revision != null && !revision.trim().isEmpty()) ? revision.trim() : "HEAD";
        return gitProvider.getChangedFiles(repoDir, ref);
    }

    @Override
    public void commitChanges(File repoDir, String message) throws Exception {
        if (!repoDir.exists()) throw new IllegalStateException("Repository directory does not exist: " + repoDir);
        String msg = (message != null && !message.trim().isEmpty()) ? message.trim() : "Develop autonomous update";
        gitProvider.commitChanges(repoDir, msg);
    }

    @Override
    public void push(File repoDir, String remote, String branch) throws Exception {
        if (!repoDir.exists()) throw new IllegalStateException("Repository directory does not exist: " + repoDir);
        gitProvider.push(repoDir);
    }

    @Override
    public boolean isGitRepository(File repoDir) {
        GitManager gitManager = new GitManager(repoDir);
        return gitManager.isGitRepository();
    }

    @Override
    public void ensureInitialCommit(File repoDir) throws Exception {
        GitManager gitManager = new GitManager(repoDir);
        gitManager.ensureInitialCommit();
    }
}
