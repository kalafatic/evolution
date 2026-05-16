package eu.kalafatic.evolution.controller.tools;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.model.orchestration.Git;

/**
 * Tool for executing Git commands.
 */
public class GitTool implements ITool {
    @Override
    public String getName() {
        return "GitTool";
    }

    @Override
    public String execute(String command, File workingDir, TaskContext context) throws Exception {
        context.log("Tool [GitTool]: Running " + command);
        File gitWorkingDir = workingDir;
        Git gitSettings = context.getOrchestrator().getGit();
        if (gitSettings != null && gitSettings.getLocalPath() != null && !gitSettings.getLocalPath().isEmpty()) {
            File subDir = new File(workingDir, gitSettings.getLocalPath());
            if (subDir.exists() && subDir.isDirectory()) {
                gitWorkingDir = subDir;
            }
        }
        String branch = (gitSettings != null && gitSettings.getBranch() != null && !gitSettings.getBranch().isEmpty()) ? gitSettings.getBranch() : "master";

        ShellTool shell = new ShellTool();
        StringBuilder output = new StringBuilder();

        if (command.toLowerCase().contains("add") || command.toLowerCase().contains("commit")) {
            context.log("Tool [GitTool]: Staging all changes and committing.");
            output.append(shell.execute("git add .", gitWorkingDir, context)).append("\n");
            output.append(shell.execute("git commit -m \"AI Evolution step: " + command + "\"", gitWorkingDir, context)).append("\n");
        }

        if (command.toLowerCase().contains("push")) {
            context.log("Tool [GitTool]: Pushing to branch " + branch);
            output.append(shell.execute("git push origin " + branch, gitWorkingDir, context));
        }

        if (command.toLowerCase().startsWith("diff")) {
            output.append(shell.execute("git diff " + command.substring(4).trim(), gitWorkingDir, context));
        }

        if (command.toLowerCase().startsWith("status")) {
            output.append(shell.execute("git status --porcelain", gitWorkingDir, context));
        }

        if (output.length() == 0) {
            return "No git action mapped for: " + command;
        }
        return output.toString().trim();
    }

    /**
     * Retrieves the list of local git branches.
     * @param workingDir The git repository directory.
     * @return List of branch names.
     */
    public List<String> getBranches(File workingDir) {
        List<String> branches = new ArrayList<>();
        try {
            ShellTool shell = new ShellTool();
            String output = shell.execute("git branch", workingDir, null);
            if (output != null && !output.isEmpty()) {
                String[] lines = output.split("\\R");
                for (String line : lines) {
                    // Remove '*' prefix for current branch and trim
                    String branch = line.replace("*", "").trim();
                    if (!branch.isEmpty()) {
                        branches.add(branch);
                    }
                }
            }
        } catch (Exception e) {
            // Silently fail or log
        }
        return branches;
    }

    /**
     * Lists all local repositories under the given search root.
     * Scans for directories containing a .git folder.
     */
    public List<File> listLocalRepositories(File searchRoot) {
        List<File> repos = new ArrayList<>();
        if (searchRoot == null || !searchRoot.exists() || !searchRoot.isDirectory()) {
            return repos;
        }

        File[] files = searchRoot.listFiles();
        if (files == null) return repos;

        for (File f : files) {
            if (f.isDirectory()) {
                if (new File(f, ".git").exists()) {
                    repos.add(f);
                } else {
                    // One level deeper search
                    File[] subFiles = f.listFiles();
                    if (subFiles != null) {
                        for (File subF : subFiles) {
                            if (subF.isDirectory() && new File(subF, ".git").exists()) {
                                repos.add(subF);
                            }
                        }
                    }
                }
            }
        }
        return repos;
    }
}
