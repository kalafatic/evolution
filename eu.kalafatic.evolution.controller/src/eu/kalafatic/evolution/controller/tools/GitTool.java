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

        // Robust command handling for complex commit messages
        List<String> fullCmd = new ArrayList<>();
        fullCmd.add("git");

        // Split by space but preserve quoted segments
        if (command.contains("\"")) {
            int firstQuote = command.indexOf("\"");
            int lastQuote = command.lastIndexOf("\"");
            String before = command.substring(0, firstQuote).trim();
            String msg = command.substring(firstQuote + 1, lastQuote);
            if (!before.isEmpty()) {
                for (String p : before.split(" ")) {
                    if (!p.isEmpty()) fullCmd.add(p);
                }
            }
            fullCmd.add(msg);
        } else {
            for (String p : command.split(" ")) {
                if (!p.isEmpty()) fullCmd.add(p);
            }
        }

        if (command.toLowerCase().startsWith("status")) {
            output.append(shell.execute("git status --porcelain", gitWorkingDir, context));
        }

        if (output.length() == 0) {
            return "No git action mapped for: " + command;
        }
        return output.toString().trim();
    }

    public List<String> getBranches(File root) throws Exception {
        String output = execute("branch --format=%(refname:short)", root, null);
        List<String> branches = new ArrayList<>();
        if (output != null) {
            for (String line : output.split("\n")) {
                if (!line.trim().isEmpty()) {
                    branches.add(line.trim());
                }
            }
        }
        return branches;
    }
}
