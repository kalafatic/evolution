package eu.kalafatic.evolution.controller.vcs;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import eu.kalafatic.evolution.controller.tools.ShellTool;

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
            // Check if file is untracked
            String status = shell.execute("git status --porcelain -- " + quote(filePath), workingDir, null);
            if (status != null && status.contains("??")) {
                // Untracked file: return the whole content as addition
                File file = new File(workingDir, filePath);
                if (file.exists()) {
                    String content = new String(java.nio.file.Files.readAllBytes(file.toPath()));
                    StringBuilder sb = new StringBuilder();
                    sb.append("diff --git a/").append(filePath).append(" b/").append(filePath).append("\n");
                    sb.append("new file mode 100644\n");
                    sb.append("--- /dev/null\n");
                    sb.append("+++ b/").append(filePath).append("\n");
                    String[] lines = content.split("\n");
                    sb.append("@@ -0,0 +1,").append(lines.length).append(" @@\n");
                    for (String line : lines) {
                        sb.append("+").append(line).append("\n");
                    }
                    return sb.toString();
                }
            }

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
    public String getFileContent(File workingDir, String commitId, String filePath) throws Exception {
        if (commitId == null || commitId.isEmpty() || "HEAD".equals(commitId)) {
            return shell.execute("git show HEAD:" + quote(filePath), workingDir, null);
        }
        return shell.execute("git show " + quote(commitId) + ":" + quote(filePath), workingDir, null);
    }

    @Override
    public List<String> getChangedFiles(File workingDir, String commitId) throws Exception {
        if (commitId == null || commitId.isEmpty() || "HEAD".equals(commitId)) {
            String output = shell.execute("git status --porcelain", workingDir, null);
            if (output == null || output.isEmpty()) return new ArrayList<>();
            List<String> files = new ArrayList<>();
            for (String line : output.split("\n")) {
                if (line.length() > 3) {
                    char s1 = line.charAt(0);
                    char s2 = line.charAt(1);
                    String file = line.substring(3).trim();
                    if (file.contains(" -> ")) {
                        file = file.split(" -> ")[1];
                    }

                    if (s1 == 'M' || s2 == 'M') {
                        files.add("M " + file);
                    } else if (s1 == 'A' || s2 == 'A' || (s1 == '?' && s2 == '?')) {
                        files.add("A " + file);
                    } else if (s1 == 'D' || s2 == 'D') {
                        files.add("D " + file);
                    } else if (s1 == 'R' || s2 == 'R') {
                        files.add("M " + file);
                    }
                }
            }
            return files;
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

    @Override
    public void revertFile(File workingDir, String filePath) throws Exception {
        shell.execute("git checkout HEAD -- " + quote(filePath), workingDir, null);
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
