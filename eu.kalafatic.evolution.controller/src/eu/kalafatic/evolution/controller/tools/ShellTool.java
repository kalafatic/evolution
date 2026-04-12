package eu.kalafatic.evolution.controller.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import eu.kalafatic.evolution.controller.orchestration.TaskContext;

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
        if (context != null) {
            context.log("Tool [ShellTool]: Running " + command);
        }

        // Basic whitelist for safer execution
        String firstArg = command.split("\\s+")[0].toLowerCase();
        if (!isWhitelisted(firstArg)) {
            throw new Exception("Security Violation: Command '" + firstArg + "' is not whitelisted for ShellTool.");
        }

        // Handle quoted arguments for git commit and other commands
        java.util.List<String> cmdList = new java.util.ArrayList<>();
        // Updated regex to handle both single and double quotes
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("([^\"'\\s]\\S*|\".+?\"|'.+?')\\s*").matcher(command);
        while (m.find()) {
            String arg = m.group(1);
            if ((arg.startsWith("\"") && arg.endsWith("\"")) || (arg.startsWith("'") && arg.endsWith("'"))) {
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
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append(System.lineSeparator());
                if (context != null) {
                    context.consoleLog(line);
                }
            }
            int exitCode = process.waitFor();
            String finalOutput = output.toString().trim();
            if (exitCode != 0) {
                throw new Exception("Shell command failed (exit code " + exitCode + "):\n" + finalOutput);
            }
            return finalOutput;
        }
    }

    private boolean isWhitelisted(String command) {
        return command.equals("mvn") || command.equals("mvn.cmd") ||
               command.equals("git") || command.equals("ls") ||
               command.equals("pwd") || command.equals("cd") ||
               command.equals("mkdir") || command.equals("echo") ||
               command.equals("ollama") ||
               command.equals("gcc") || command.equals("g++") ||
               command.equals("make") || command.equals("cmake") ||
               command.equals("java") || command.equals("javac") ||
               command.equals("curl") || command.equals("sh") ||
               command.equals("bash");
    }
}
