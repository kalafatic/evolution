package eu.kalafatic.evolution.controller.tools;

import java.io.File;
import java.util.List;
import java.util.ArrayList;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.workflow.RuntimeEvent;
import eu.kalafatic.evolution.controller.workflow.RuntimeEventBus;
import eu.kalafatic.evolution.controller.workflow.RuntimeEventType;
import eu.kalafatic.utils.semantic.EvolutionComponent;
import eu.kalafatic.utils.semantic.EvolutionaryImpact;
import eu.kalafatic.utils.semantic.Stability;

/**
 * Tool for executing Git commands.
 */
@EvolutionComponent(
    domain = "tools",
    role = "physical-truth-accessor",
    purpose = "Executes Git commands and emits truth signals",
    stability = Stability.STABLE,
    evolutionaryImpact = EvolutionaryImpact.HIGH
)
public class GitTool implements ITool {

    @Override
    public String execute(String command, File projectRoot, TaskContext context) throws Exception {
        if (context != null) {
            context.log("[GIT] Executing: git " + command);
        }

        ProcessBuilder pb = new ProcessBuilder();
        pb.directory(projectRoot);

        // Robust command handling for complex commit messages
        List<String> fullCmd = new ArrayList<>();
        fullCmd.add("git");
        if (command.contains("\"")) {
            // Primitive handling for quoted strings (commit messages)
            int firstQuote = command.indexOf("\"");
            int lastQuote = command.lastIndexOf("\"");
            String before = command.substring(0, firstQuote).trim();
            String msg = command.substring(firstQuote + 1, lastQuote);
            for (String p : before.split(" ")) fullCmd.add(p);
            fullCmd.add(msg);
        } else {
            for (String p : command.split(" ")) fullCmd.add(p);
        }

        pb.command(fullCmd);
        Process process = pb.start();

        String output = new String(process.getInputStream().readAllBytes());
        String error = new String(process.getErrorStream().readAllBytes());
        int exitCode = process.waitFor();

        if (exitCode == 0) {
            RuntimeEventBus.getInstance().publish(
                new RuntimeEvent(RuntimeEventType.TOOL_EXECUTION_SUCCEEDED,
                                 context != null ? context.getSessionId() : "system",
                                 "GitTool", output));
            return output;
        } else {
             throw new Exception("Git command failed: " + error);
        }
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

    @Override
    public String getName() {
        return "Git";
    }
}
