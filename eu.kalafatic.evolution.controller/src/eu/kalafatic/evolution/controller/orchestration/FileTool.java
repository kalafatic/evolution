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
                context.log("Tool [FileTool]: Successfully wrote " + contentPart.length() + " bytes to " + pathPart);
            } catch (IOException e) {
                throw new Exception("Failed to write file: " + pathPart + " - " + e.getMessage(), e);
            }
            context.log("Tool [FileTool]: Wrote file " + pathPart);
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
            String content = new String(bytes);
            context.log("Tool [FileTool]: Read " + content.length() + " bytes from " + pathPart);
            return content;
        } else if (command.startsWith("DELETE")) {
            String pathPart = command.substring(6).trim();
            if (pathPart.contains("..") || pathPart.startsWith("/") || pathPart.contains(":")) {
                throw new Exception("Security Violation: Path traversal attempt or absolute path detected: " + pathPart);
            }
            File file = new File(workingDir, pathPart);
            if (file.exists()) {
                if (deleteRecursively(file)) {
                    context.log("Tool [FileTool]: Deleted " + pathPart);
                    return "SUCCESS: Deleted " + pathPart;
                } else {
                    throw new Exception("Failed to delete " + pathPart);
                }
            }
            return "SUCCESS: File did not exist " + pathPart;
        } else if (command.startsWith("MKDIR")) {
            String pathPart = command.substring(5).trim();
            if (pathPart.contains("..") || pathPart.startsWith("/") || pathPart.contains(":")) {
                throw new Exception("Security Violation: Path traversal attempt or absolute path detected: " + pathPart);
            }
            File dir = new File(workingDir, pathPart);
            if (dir.exists()) {
                return "SUCCESS: Directory already exists " + pathPart;
            }
            if (dir.mkdirs()) {
                context.log("Tool [FileTool]: Created directory " + pathPart);
                return "SUCCESS: Created directory " + pathPart;
            } else {
                throw new Exception("Failed to create directory " + pathPart);
            }
        }
        throw new Exception("Unsupported command for FileTool: " + command);
    }

    private boolean deleteRecursively(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) {
                    deleteRecursively(f);
                }
            }
        }
        return file.delete();
    }
}
