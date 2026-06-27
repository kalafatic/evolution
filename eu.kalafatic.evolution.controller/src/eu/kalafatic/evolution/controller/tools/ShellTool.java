package eu.kalafatic.evolution.controller.tools;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import eu.kalafatic.evolution.controller.orchestration.TaskContext;

/**
 * Tool for executing shell commands with security and resource management.
 *
 * @evo:1:1 reason=merge-shelltool-improvements
 */
public class ShellTool implements ITool {

    private static final Set<String> ALLOWED_COMMANDS = Set.of(
        "mvn", "mvn.cmd", "git", "ls", "pwd", "cd", "mkdir", "echo",
        "ollama", "gcc", "g++", "make", "cmake", "java", "javac",
        "curl", "sh", "bash"
    );

    private static final long DEFAULT_TIMEOUT_SECONDS = 300; // Increased to 5 mins for long builds
    private static final long MAX_OUTPUT_SIZE = 5 * 1024 * 1024; // 5MB limit

    @Override
    public String getName() {
        return "ShellTool";
    }

    @Override
    public String execute(String command, File workingDir, TaskContext context) throws Exception {
        if (command == null || command.isBlank()) {
            throw new IllegalArgumentException("Empty command");
        }

        List<String> cmdList = parseArguments(command);
        if (cmdList.isEmpty()) {
            throw new IllegalArgumentException("Malformed command");
        }

        String baseCmd = cmdList.get(0).toLowerCase();
        validateCommand(baseCmd);

        if (context != null) {
            context.log("Tool [ShellTool]: Running " + String.join(" ", cmdList));
        }

        ProcessBuilder pb = new ProcessBuilder(cmdList);
        if (workingDir != null) {
            pb.directory(workingDir);
        }
        pb.redirectErrorStream(true);
        Process process = pb.start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append(System.lineSeparator());
                if (context != null) {
                    context.consoleLog(line);
                }

                // Safety: Cap output size
                if (output.length() > MAX_OUTPUT_SIZE) {
                    process.destroyForcibly();
                    String truncated = output.substring(0, (int) MAX_OUTPUT_SIZE) + "\n[Error: Output exceeded " + (MAX_OUTPUT_SIZE / (1024 * 1024)) + "MB limit. Process killed.]";
                    return truncated;
                }
            }

            // Timeout handling
            boolean finished = process.waitFor(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new Exception("Command timed out after " + DEFAULT_TIMEOUT_SECONDS + "s: " + command);
            }

            String finalOutput = output.toString().trim();
            if (process.exitValue() != 0) {
                throw new Exception("Shell command failed (exit code " + process.exitValue() + "):\n" + finalOutput);
            }
            return finalOutput;
        }
    }

    private void validateCommand(String baseCmd) throws SecurityException {
        // Prevent recursive shell nesting if it's considered dangerous in specific contexts,
        // but here we keep sh/bash for flexibility as long as they are whitelisted.
        if (!ALLOWED_COMMANDS.contains(baseCmd)) {
            throw new SecurityException("Security Violation: Command '" + baseCmd + "' is not in the allowed policy for ShellTool.");
        }
    }

    private List<String> parseArguments(String command) {
        List<String> list = new ArrayList<>();
        // Improved regex to handle both single and double quotes, including escaped quotes
        Matcher m = Pattern.compile("([^\"'\\s]\\S*|\"(?:\\\\\"|[^\"])*?\"|'(?:\\\\'|[^'])*?')\\s*").matcher(command);
        while (m.find()) {
            String arg = m.group(1);
            if ((arg.startsWith("\"") && arg.endsWith("\"")) || (arg.startsWith("'") && arg.endsWith("'"))) {
                arg = arg.substring(1, arg.length() - 1);
            }
            // Unescape escaped quotes
            arg = arg.replace("\\\"", "\"").replace("\\'", "'");
            list.add(arg);
        }
        return list;
    }
}
