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
        return createExportPackage(sessionId, prompt, selectedPaths, projectRoot, outputPath, metadataJson, historyAnalysis, architectureSummary, dependencies, executionInstructions, null);
    }

    public File createExportPackage(String sessionId, String prompt, List<String> selectedPaths, File projectRoot, String outputPath, String metadataJson, String historyAnalysis, String architectureSummary, String dependencies, String executionInstructions, String realityModelJson) throws IOException {
        return createExportPackage(sessionId, prompt, selectedPaths, projectRoot, outputPath, metadataJson, historyAnalysis, architectureSummary, dependencies, executionInstructions, realityModelJson, null, null);
    }

    public File createExportPackage(String sessionId, String prompt, List<String> selectedPaths, File projectRoot, String outputPath, String metadataJson, String historyAnalysis, String architectureSummary, String dependencies, String executionInstructions, String realityModelJson, String architecturalFactsJson, String subsystemsJson) throws IOException {
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
            if (realityModelJson != null) {
                addStringToZip(zos, "reality-model.json", realityModelJson);
            }
            if (architecturalFactsJson != null) {
                addStringToZip(zos, "architectural-facts.json", architecturalFactsJson);
            }
            if (subsystemsJson != null) {
                addStringToZip(zos, "subsystems.json", subsystemsJson);
            }

            // 4. Add Selected File Contents
            for (String path : selectedPaths) {
                String normalizedPath = path.replace('\\', '/');
                File file = new File(projectRoot, normalizedPath);
                if (file.exists() && file.isFile()) {
                    addFileToZip(zos, "affected-files/" + normalizedPath, file);
                } else {
                    // Try absolute path if relative fails
                    File absFile = new File(normalizedPath);
                    if (absFile.exists() && absFile.isFile()) {
                        String zipEntryName = normalizedPath;
                        if (normalizedPath.startsWith(projectRoot.getAbsolutePath())) {
                            zipEntryName = projectRoot.toPath().relativize(absFile.toPath()).toString().replace('\\', '/');
                        }
                        addFileToZip(zos, "affected-files/" + zipEntryName, absFile);
                    }
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

    public File createExportFolder(String sessionId, String prompt, List<String> selectedPaths, File projectRoot, String outputPath, String metadataJson, String historyAnalysis, String architectureSummary, String dependencies, String executionInstructions, String realityModelJson, String architecturalFactsJson, String subsystemsJson) throws IOException {
        String dateStamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String folderName = "mediated_export_" + dateStamp;
        File targetDir = projectRoot;
        if (outputPath != null && !outputPath.isEmpty()) {
            targetDir = new File(outputPath);
            if (!targetDir.exists()) {
                targetDir.mkdirs();
            }
        }
        File exportDir = new File(targetDir, folderName);
        if (!exportDir.exists()) {
            exportDir.mkdirs();
        }

        // 1. Add Prompt
        writeStringToFile(new File(exportDir, "prompt.md"), prompt);

        // 2. Add Metadata and Analysis
        if (metadataJson != null) writeStringToFile(new File(exportDir, "metadata.json"), metadataJson);
        if (historyAnalysis != null) writeStringToFile(new File(exportDir, "evolution-analysis.md"), historyAnalysis);

        // 3. Add Architectural Artifacts
        if (architectureSummary != null) writeStringToFile(new File(exportDir, "architecture.md"), architectureSummary);
        if (dependencies != null) writeStringToFile(new File(exportDir, "dependencies.md"), dependencies);
        if (executionInstructions != null) writeStringToFile(new File(exportDir, "execution-instructions.md"), executionInstructions);
        if (realityModelJson != null) writeStringToFile(new File(exportDir, "reality-model.json"), realityModelJson);
        if (architecturalFactsJson != null) writeStringToFile(new File(exportDir, "architectural-facts.json"), architecturalFactsJson);
        if (subsystemsJson != null) writeStringToFile(new File(exportDir, "subsystems.json"), subsystemsJson);

        // 4. Add Selected File Contents
        File affectedFilesDir = new File(exportDir, "affected-files");
        affectedFilesDir.mkdirs();

        for (String path : selectedPaths) {
            String normalizedPath = path.replace('\\', '/');
            File file = new File(projectRoot, normalizedPath);
            if (file.exists() && file.isFile()) {
                copyFileToExport(file, affectedFilesDir, normalizedPath);
            } else {
                File absFile = new File(normalizedPath);
                if (absFile.exists() && absFile.isFile()) {
                    String relativePath = normalizedPath;
                    if (normalizedPath.startsWith(projectRoot.getAbsolutePath())) {
                        relativePath = projectRoot.toPath().relativize(absFile.toPath()).toString().replace('\\', '/');
                    }
                    copyFileToExport(absFile, affectedFilesDir, relativePath);
                }
            }
        }

        return exportDir;
    }

    private void writeStringToFile(File file, String content) throws IOException {
        Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
    }

    private void copyFileToExport(File source, File targetBase, String relativePath) throws IOException {
        File targetFile = new File(targetBase, relativePath);
        targetFile.getParentFile().mkdirs();
        Files.copy(source.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }
}
