package eu.kalafatic.evolution.controller.workflow;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Handles packaging of mediated context and prompts into a ZIP archive.
 */
public class MediatedExportManager {

    public File createExportPackage(String sessionId, String prompt, List<String> selectedPaths, File projectRoot, String outputPath, String metadataJson, String historyAnalysis, String architectureSummary, String dependencies, String executionInstructions) throws IOException {
        String fileName = "mediated_export_" + sessionId + "_" + System.currentTimeMillis() + ".zip";
        File targetDir = projectRoot;
        if (outputPath != null && !outputPath.isEmpty()) {
            targetDir = new File(outputPath);
            if (!targetDir.exists()) {
                targetDir.mkdirs();
            }
        }
        File zipFile = new File(targetDir, fileName);

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
            // 1. Add Prompt
            addStringToZip(zos, "prompt.md", prompt);

            // 2. Add Metadata and Analysis
            if (metadataJson != null) {
                addStringToZip(zos, "metadata.json", metadataJson);
            }
            if (historyAnalysis != null) {
                addStringToZip(zos, "evolution-analysis.md", historyAnalysis);
            }

            // 3. Add Architectural Artifacts
            if (architectureSummary != null) {
                addStringToZip(zos, "architecture.md", architectureSummary);
            }
            if (dependencies != null) {
                addStringToZip(zos, "dependencies.md", dependencies);
            }
            if (executionInstructions != null) {
                addStringToZip(zos, "execution-instructions.md", executionInstructions);
            }

            // 4. Add Selected File Contents
            for (String relativePath : selectedPaths) {
                File file = new File(projectRoot, relativePath);
                if (file.exists() && file.isFile()) {
                    addFileToZip(zos, "affected-files/" + relativePath, file);
                }
            }
        }

        return zipFile;
    }

    private void addStringToZip(ZipOutputStream zos, String entryName, String content) throws IOException {
        ZipEntry entry = new ZipEntry(entryName);
        zos.putNextEntry(entry);
        zos.write(content.getBytes(StandardCharsets.UTF_8));
        zos.closeEntry();
    }

    private void addFileToZip(ZipOutputStream zos, String entryName, File file) throws IOException {
        ZipEntry entry = new ZipEntry(entryName);
        zos.putNextEntry(entry);
        byte[] bytes = Files.readAllBytes(file.toPath());
        zos.write(bytes);
        zos.closeEntry();
    }
}
