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
        if (context != null) context.log("Tool [GitTool]: Running " + command);
        File gitWorkingDir = workingDir;
        String branch = "master";

        if (context != null && context.getOrchestrator() != null) {
            Git gitSettings = context.getOrchestrator().getGit();
            if (gitSettings != null) {
                if (gitSettings.getLocalPath() != null && !gitSettings.getLocalPath().isEmpty()) {
                    File subDir = new File(workingDir, gitSettings.getLocalPath());
                    if (subDir.exists() && subDir.isDirectory()) {
                        gitWorkingDir = subDir;
                    }
                }
                if (gitSettings.getBranch() != null && !gitSettings.getBranch().isEmpty()) {
                    branch = gitSettings.getBranch();
                }
            }
        }

        ShellTool shell = new ShellTool();

        // SPECIALIZED LOGIC: Commit metadata injection
        if (command.startsWith("commit")) {
            shell.execute("git add .", gitWorkingDir, context);

            String metadata = "";
            if (context != null && context.getOrchestrationState() != null) {
                String iterationId = context.getOrchestrationState().getCurrentIterationId();
                String taskId = context.getCurrentTaskId();
                metadata = String.format(" [EVO-META] [Iteration: %s] [Task: %s]",
                    iterationId != null ? iterationId : "unknown",
                    taskId != null ? taskId : "none");
            }

            String commitMsg = command.contains("-m") ? "" : " -m \"Darwin evolution step\"";
            String fullCommand = "git " + command + commitMsg + metadata;
            return shell.execute(fullCommand, gitWorkingDir, context);
        }

        // SPECIALIZED LOGIC: Push handling
        if (command.startsWith("push")) {
            String fullCommand = "git push origin " + branch;
            return shell.execute(fullCommand, gitWorkingDir, context);
        }

        // Generic pass-through for other commands (init, checkout, branch, etc.)
        String fullCommand = command.startsWith("git ") ? command : "git " + command;
        return shell.execute(fullCommand, gitWorkingDir, context);
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
