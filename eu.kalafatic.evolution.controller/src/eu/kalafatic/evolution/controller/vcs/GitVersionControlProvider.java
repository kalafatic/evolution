package eu.kalafatic.evolution.controller.vcs;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import eu.kalafatic.evolution.controller.orchestration.ShellTool;

public class GitVersionControlProvider implements VersionControlProvider {
    private ShellTool shell = new ShellTool();

    @Override
    public List<String> fetchCommits(File workingDir) throws Exception {
        String output = shell.execute("git log --oneline -n 20", workingDir, null);
        if (output == null || output.isEmpty()) return new ArrayList<>();
        return Arrays.asList(output.split("\n"));
    }

    @Override
    public String getDiff(File workingDir, String commitId) throws Exception {
        if (commitId == null || commitId.isEmpty() || "HEAD".equals(commitId)) {
            return shell.execute("git diff", workingDir, null);
        }
        return shell.execute("git show " + quote(commitId), workingDir, null);
    }

    @Override
    public String getFileDiff(File workingDir, String commitId, String filePath) throws Exception {
        if (commitId == null || commitId.isEmpty() || "HEAD".equals(commitId)) {
            return shell.execute("git diff " + quote(filePath), workingDir, null);
        }
        return shell.execute("git show " + quote(commitId) + " -- " + quote(filePath), workingDir, null);
    }

    @Override
    public List<String> getChangedFiles(File workingDir, String commitId) throws Exception {
        String command = (commitId == null || commitId.isEmpty() || "HEAD".equals(commitId)) ?
                "git diff --name-only HEAD" : "git show --name-only --format= " + quote(commitId);
        String output = shell.execute(command, workingDir, null);
        if (output == null || output.isEmpty()) return new ArrayList<>();
        return Arrays.asList(output.trim().split("\n"));
    }

    @Override
    public void checkoutBranch(File workingDir, String branchName) throws Exception {
        shell.execute("git checkout " + quote(branchName), workingDir, null);
    }

    @Override
    public void commitChanges(File workingDir, String message) throws Exception {
        shell.execute("git add .", workingDir, null);
        shell.execute("git commit -m " + quote(message), workingDir, null);
    }

    @Override
    public void push(File workingDir) throws Exception {
        shell.execute("git push", workingDir, null);
    }

    private String quote(String input) {
        if (input == null) return "''";
        // Surround with single quotes and escape existing single quotes
        return "'" + input.replace("'", "'\"'\"'") + "'";
    }
}
