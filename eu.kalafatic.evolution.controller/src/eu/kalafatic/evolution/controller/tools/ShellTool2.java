package eu.kalafatic.evolution.controller.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import eu.kalafatic.evolution.controller.orchestration.TaskContext;

import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class ShellTool2 implements ITool {
    // Increase security by using a Set for O(1) lookups and immutable config
    private static final Set<String> ALLOWED_COMMANDS = Set.of(
        "mvn", "git", "ls", "pwd", "mkdir", "echo", "gcc", "g++", "make", "java", "javac"
    );

    private static final long DEFAULT_TIMEOUT_SECONDS = 60;

    @Override
    public String getName() { return "ShellTool"; }

    @Override
    public String execute(String command, File workingDir, TaskContext context) throws Exception {
        // 1. Use a more robust split (handles basic shell quoting)
        List<String> cmdList = parseArguments(command);
        if (cmdList.isEmpty()) throw new IllegalArgumentException("Empty command");

        String baseCmd = cmdList.get(0).toLowerCase();
        validateCommand(baseCmd);

        if (context != null) context.log("Executing: " + String.join(" ", cmdList));

        ProcessBuilder pb = new ProcessBuilder(cmdList);
        if (workingDir != null) pb.directory(workingDir);
        
        pb.redirectErrorStream(true);
        Process process = pb.start();

        // 2. Stream handling with size limits to prevent OOM
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append(System.lineSeparator());
                // Cap output at 1MB to protect the JVM
                if (output.length() > 1024 * 1024) {
                    process.destroyForcibly();
                    return output.append("\n[Error: Output exceeded 1MB limit]").toString();
                }
            }

            // 3. Mandatory Timeout
            boolean finished = process.waitFor(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new Exception("Command timed out after " + DEFAULT_TIMEOUT_SECONDS + "s");
            }

            if (process.exitValue() != 0) {
                throw new Exception("Exit Code " + process.exitValue() + ": " + output);
            }

            return output.toString().trim();
        }
    }

    private void validateCommand(String baseCmd) throws SecurityException {
        // Block sub-shell execution wrappers which bypass whitelists
        if (baseCmd.equals("sh") || baseCmd.equals("bash") || baseCmd.equals("cmd")) {
            throw new SecurityException("Direct shell access (sh/bash) is forbidden for agents.");
        }
        if (!ALLOWED_COMMANDS.contains(baseCmd)) {
            throw new SecurityException("Command '" + baseCmd + "' is not in the allowed policy.");
        }
    }

    private List<String> parseArguments(String command) {
        // Note: For production, consider using a library like Apache Commons Exec 
        // or a dedicated Shell utility to handle complex escaping.
        List<String> list = new ArrayList<>();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("([^\s\"']+|\"[^\"]*\"|'[^']*')").matcher(command);
        while (m.find()) {
            String s = m.group();
            if (s.startsWith("\"") || s.startsWith("'")) {
                s = s.substring(1, s.length() - 1);
            }
            list.add(s);
        }
        return list;
    }
}