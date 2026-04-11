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
        return shell.execute("git show " + commitId, workingDir, null);
    }

    @Override
    public void checkoutBranch(File workingDir, String branchName) throws Exception {
        shell.execute("git checkout " + branchName, workingDir, null);
    }

    @Override
    public void commitChanges(File workingDir, String message) throws Exception {
        shell.execute("git add .", workingDir, null);
        // Safely escape single quotes for shell execution
        String escapedMessage = message.replace("'", "'\"'\"'");
        shell.execute("git commit -m '" + escapedMessage + "'", workingDir, null);
    }

    @Override
    public void push(File workingDir) throws Exception {
        shell.execute("git push", workingDir, null);
    }
}
