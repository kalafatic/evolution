package eu.kalafatic.evolution.controller.orchestration;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

/**
 * Tool for executing shell commands.
 */
public class ShellTool implements ITool {
    @Override
    public String getName() {
        return "ShellTool";
    }

    @Override
    public String execute(String command, File workingDir, TaskContext context) throws Exception {
        context.log("ShellTool: Running " + command);

        // Basic whitelist for safer execution
        String firstArg = command.split("\\s+")[0].toLowerCase();
        if (!isWhitelisted(firstArg)) {
            throw new Exception("Security Violation: Command '" + firstArg + "' is not whitelisted for ShellTool.");
        }

        // Handle quoted arguments for git commit and other commands
        java.util.List<String> cmdList = new java.util.ArrayList<>();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("([^\"]\\S*|\".+?\")\\s*").matcher(command);
        while (m.find()) {
            String arg = m.group(1);
            if (arg.startsWith("\"") && arg.endsWith("\"")) {
                arg = arg.substring(1, arg.length() - 1);
            }
            cmdList.add(arg);
        }

        ProcessBuilder processBuilder = new ProcessBuilder(cmdList);
        if (workingDir != null) {
            processBuilder.directory(workingDir);
        }
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String output = reader.lines().collect(Collectors.joining(System.lineSeparator()));
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new Exception("Shell command failed (exit code " + exitCode + "):\n" + output);
            }
            return output;
        }
    }

    private boolean isWhitelisted(String command) {
        return command.equals("mvn") || command.equals("mvn.cmd") ||
               command.equals("git") || command.equals("ls") ||
               command.equals("mkdir") || command.equals("echo") ||
               command.equals("gcc") || command.equals("g++") ||
               command.equals("make") || command.equals("cmake");
    }
}
