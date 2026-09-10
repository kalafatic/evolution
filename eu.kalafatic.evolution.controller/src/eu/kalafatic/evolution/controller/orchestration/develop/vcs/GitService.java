package eu.kalafatic.evolution.controller.orchestration.develop.vcs;

import java.io.File;
import java.util.List;

/**
 * Dedicated Git service interface for VCS operations.
 */
public interface GitService {

    void cloneRepository(String repoUrl, String branch, File destinationDir) throws Exception;

    void createAndCheckoutBranch(File repoDir, String branchName) throws Exception;

    String getDiff(File repoDir, String revision) throws Exception;

    List<String> getChangedFiles(File repoDir, String revision) throws Exception;

    void commitChanges(File repoDir, String message) throws Exception;

    void push(File repoDir, String remote, String branch) throws Exception;

    boolean isGitRepository(File repoDir);

    void ensureInitialCommit(File repoDir) throws Exception;
}
