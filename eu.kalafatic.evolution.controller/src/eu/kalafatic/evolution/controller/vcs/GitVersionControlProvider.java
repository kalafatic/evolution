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
        String output = null;
        try {
            output = shell.execute("git log --oneline -n 20", workingDir, null);
        } catch (Exception e) {
            // Log likely failed because of empty repository
            return new ArrayList<>();
        }
        if (output == null || output.isEmpty()) return new ArrayList<>();
        return Arrays.asList(output.split("\n"));
    }

    @Override
    public String getDiff(File workingDir, String commitId) throws Exception {
        if (commitId == null || commitId.isEmpty() || "HEAD".equals(commitId)) {
            if (isHeadValid(workingDir)) {
                return shell.execute("git diff HEAD", workingDir, null);
            } else {
                String unstaged = shell.execute("git diff", workingDir, null);
                String staged = shell.execute("git diff --cached", workingDir, null);
                return (staged != null ? staged : "") + (unstaged != null ? unstaged : "");
            }
        }
        return shell.execute("git show " + quote(commitId), workingDir, null);
    }

    @Override
    public String getFileDiff(File workingDir, String commitId, String filePath) throws Exception {
        if (commitId == null || commitId.isEmpty() || "HEAD".equals(commitId)) {
            if (isHeadValid(workingDir)) {
                return shell.execute("git diff HEAD -- " + quote(filePath), workingDir, null);
            } else {
                String unstaged = shell.execute("git diff -- " + quote(filePath), workingDir, null);
                String staged = shell.execute("git diff --cached -- " + quote(filePath), workingDir, null);
                return (staged != null ? staged : "") + (unstaged != null ? unstaged : "");
            }
        }
        return shell.execute("git show " + quote(commitId) + " -- " + quote(filePath), workingDir, null);
    }

    @Override
    public List<String> getChangedFiles(File workingDir, String commitId) throws Exception {
        if (commitId == null || commitId.isEmpty() || "HEAD".equals(commitId)) {
            if (isHeadValid(workingDir)) {
                String output = shell.execute("git diff --name-only HEAD", workingDir, null);
                if (output == null || output.isEmpty()) return new ArrayList<>();
                return Arrays.asList(output.trim().split("\n"));
            } else {
                String output = shell.execute("git status --porcelain", workingDir, null);
                if (output == null || output.isEmpty()) return new ArrayList<>();
                List<String> files = new ArrayList<>();
                for (String line : output.split("\n")) {
                    if (line.length() > 3) {
                        char status1 = line.charAt(0);
                        char status2 = line.charAt(1);
                        // Filter out untracked files (status is '??')
                        if (status1 != '?' || status2 != '?') {
                            String file = line.substring(3).trim();
                            // Handle staged renames: "old -> new"
                            if (file.contains(" -> ")) {
                                file = file.split(" -> ")[1];
                            }
                            files.add(file);
                        }
                    }
                }
                return files;
            }
        } else {
            String output = shell.execute("git show --name-only --format= " + quote(commitId), workingDir, null);
            if (output == null || output.isEmpty()) return new ArrayList<>();
            return Arrays.asList(output.trim().split("\n"));
        }
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

    private boolean isHeadValid(File workingDir) {
        try {
            shell.execute("git rev-parse HEAD", workingDir, null);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String quote(String input) {
        if (input == null) return "''";
        // Surround with single quotes and escape existing single quotes
        return "'" + input.replace("'", "'\"'\"'") + "'";
    }
}
