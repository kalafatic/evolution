package eu.kalafatic.evolution.controller.orchestration;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Tool for file operations within the orchestration system.
 */
public class FileTool implements ITool {
    @Override
    public String getName() {
        return "FileTool";
    }

    @Override
    public String execute(String command, File workingDir, TaskContext context) throws Exception {
        // command format expected: "WRITE path/to/file\n[CONTENT]"
        if (command.startsWith("WRITE")) {
            int newlineIndex = command.indexOf("\n");
            if (newlineIndex == -1) {
                throw new Exception("Malformed WRITE command for FileTool: No content separator.");
            }
            String pathPart = command.substring(5, newlineIndex).trim();
            String contentPart = command.substring(newlineIndex + 1);

            if (pathPart.contains("..") || pathPart.startsWith("/") || pathPart.contains(":")) {
                throw new Exception("Security Violation: Path traversal attempt or absolute path detected: " + pathPart);
            }

            File file = new File(workingDir, pathPart);
            file.getParentFile().mkdirs();

            try (FileWriter writer = new FileWriter(file)) {
                writer.write(contentPart);
            } catch (IOException e) {
                throw new Exception("Failed to write file: " + pathPart + " - " + e.getMessage(), e);
            }
            context.log("FileTool: Wrote file " + pathPart);
            return "SUCCESS: Wrote file " + pathPart;
        } else if (command.startsWith("READ")) {
            String pathPart = command.substring(4).trim();
            if (pathPart.contains("..") || pathPart.startsWith("/") || pathPart.contains(":")) {
                throw new Exception("Security Violation: Path traversal attempt or absolute path detected: " + pathPart);
            }
            File file = new File(workingDir, pathPart);
            if (!file.exists()) {
                throw new Exception("File not found: " + pathPart);
            }
            // Simple read logic
            byte[] bytes = java.nio.file.Files.readAllBytes(file.toPath());
            return new String(bytes);
        }
        throw new Exception("Unsupported command for FileTool: " + command);
    }
}
