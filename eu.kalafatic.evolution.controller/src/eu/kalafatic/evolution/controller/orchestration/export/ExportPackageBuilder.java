package eu.kalafatic.evolution.controller.orchestration.export;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.json.JSONObject;

import eu.kalafatic.evolution.controller.orchestration.TaskContext;

/**
 * Builds the folder structure and creates the ZIP archive for manual use.
 */
public class ExportPackageBuilder {

    public File build(
            String originalRequest,
            JSONObject analysis,
            String optimizedPrompt,
            String architectureSummary,
            Map<String, String> contextFiles,
            TaskContext context) throws IOException {

        File tempDir = Files.createTempDirectory("selfdev_export_").toFile();
        try {
            writeFile(new File(tempDir, "optimized_prompt.md"), optimizedPrompt);
            writeFile(new File(tempDir, "analysis.md"), analysis.toString(2));
            writeFile(new File(tempDir, "architecture_summary.md"), architectureSummary);

            File contextDir = new File(tempDir, "context");
            contextDir.mkdirs();
            for (Map.Entry<String, String> entry : contextFiles.entrySet()) {
                String fileName = entry.getKey().replace("/", "_").replace("\\", "_");
                writeFile(new File(contextDir, fileName), entry.getKey() + "\n\n" + entry.getValue());
            }

            JSONObject state = new JSONObject();
            state.put("originalRequest", originalRequest);
            state.put("timestamp", System.currentTimeMillis());
            writeFile(new File(tempDir, "project_state.json"), state.toString(2));

            String readme = "# Self-Development Export Package\n\n" +
                            "This package was generated to assist in manual self-development using ChatGPT.\n\n" +
                            "## Contents\n" +
                            "- `optimized_prompt.md`: Use this as your first message to ChatGPT.\n" +
                            "- `analysis.md`: Detailed reasoning about your request.\n" +
                            "- `architecture_summary.md`: High-level overview of the system.\n" +
                            "- `context/`: Relevant source files selected for this task.\n" +
                            "- `project_state.json`: Metadata about this export.\n\n" +
                            "## How to use\n" +
                            "1. Upload the files in `context/` to ChatGPT.\n" +
                            "2. Copy the content of `optimized_prompt.md` and paste it as your instruction.\n" +
                            "3. Follow the implementation guidance provided by the AI.";
            writeFile(new File(tempDir, "README.md"), readme);

            File zipFile = new File(context.getProjectRoot(), "selfdev_export_" + System.currentTimeMillis() + ".zip");
            zipDirectory(tempDir, zipFile);

            return zipFile;

        } finally {
            deleteDirectory(tempDir);
        }
    }

    private void writeFile(File file, String content) throws IOException {
        Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
    }

    private void zipDirectory(File directory, File zipFile) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(zipFile);
             ZipOutputStream zos = new ZipOutputStream(fos)) {
            zipFolder(directory, directory, zos);
        }
    }

    private void zipFolder(File root, File folder, ZipOutputStream zos) throws IOException {
        for (File file : folder.listFiles()) {
            if (file.isDirectory()) {
                zipFolder(root, file, zos);
            } else {
                String name = root.toPath().relativize(file.toPath()).toString();
                ZipEntry zipEntry = new ZipEntry(name);
                zos.putNextEntry(zipEntry);
                Files.copy(file.toPath(), zos);
                zos.closeEntry();
            }
        }
    }

    private void deleteDirectory(File directory) {
        File[] contents = directory.listFiles();
        if (contents != null) {
            for (File file : contents) {
                if (file.isDirectory()) deleteDirectory(file);
                else file.delete();
            }
        }
        directory.delete();
    }
}
