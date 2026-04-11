package eu.kalafatic.evolution.controller.vcs;

import java.io.File;
import java.util.List;

public interface VersionControlProvider {
    List<String> fetchCommits(File workingDir) throws Exception;
    String getDiff(File workingDir, String commitId) throws Exception;
    String getFileDiff(File workingDir, String commitId, String filePath) throws Exception;
    List<String> getChangedFiles(File workingDir, String commitId) throws Exception;
    void checkoutBranch(File workingDir, String branchName) throws Exception;
    void commitChanges(File workingDir, String message) throws Exception;
    void push(File workingDir) throws Exception;
}
