package eu.kalafatic.evolution.controller.agents;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import eu.kalafatic.utils.semantic.AIContextTool;
import eu.kalafatic.utils.semantic.EvoMetadata;
import eu.kalafatic.utils.semantic.Stability;

/**
 * Universal AI Metadata Generation Agent.
 * Automates creation of .ai.json sidecars and project navigation maps.
 * Optimized for MEDIATED MODE and Semantic Nervous System architecture.
 */
public class MetadataAgent {

    private static final long MAX_METADATA_FILE_SIZE = 512 * 1024;
    private static final int MAX_SUMMARY_LENGTH = 2000;
    private static final int MAX_SUMMARY_LINES = 20;
    private static final int MAX_KB_FOR_SUMMARY = 8 * 1024;
    private static final int MAX_LINES_TO_SCAN = 50;

    public static final String ARCHITECTURE_CONTEXT = "ARCHITECTURE_CONTEXT.md";
    public static final String SEMANTIC_OVERVIEW = "SEMANTIC_OVERVIEW.md";
    public static final String TRAJECTORY_MAP = "TRAJECTORY_MAP.json";
    public static final String PACKAGE_CONTEXT = "PACKAGE_CONTEXT.md";

    private final AIContextTool contextTool = new AIContextTool();
    private final Map<File, MetadataProjection> processedMetadata = new HashMap<>();
    private int filesProcessedCount = 0;

    /**
     * Options for metadata generation.
     */
    public static class Options {
        public boolean skipExisting = false;
        public boolean cleanExisting = false;
        public boolean useTimestamp = false;
    }

    /**
     * Lightweight projection of EvoMetadata to reduce memory footprint during repository-wide scans.
     */
    private static class MetadataProjection {
        private final String path;
        private final String role;
        private final double importanceScore;
        private final String summary;

        MetadataProjection(EvoMetadata meta) {
            this.path = meta.getPath();
            this.role = meta.getRole();
            this.importanceScore = meta.getImportanceScore();
            this.summary = meta.getSummary();
        }

        String getPath() { return path; }
        String getRole() { return role; }
        double getImportanceScore() { return importanceScore; }
        String getSummary() { return summary; }
    }

    public MetadataResult generate(File root) {
        return generate(root, new Options(), new NullProgressMonitor());
    }

    public MetadataResult generate(File root, IProgressMonitor monitor) {
        return generate(root, new Options(), monitor);
    }

    public MetadataResult generate(File root, Options options, IProgressMonitor monitor) {
        if (monitor == null) monitor = new NullProgressMonitor();
        if (!root.exists() || !root.isDirectory()) return null;

        if (options.cleanExisting) {
            monitor.subTask("Cleaning existing metadata...");
            cleanExistingMetadata(root);
        }

        MetadataResult result = new MetadataResult(root);
        processedMetadata.clear();
        filesProcessedCount = 0;

        int totalFiles = countFiles(root);
        monitor.beginTask("Generating AI Metadata", totalFiles + 10);

        scanAndEnrich(root, root, result, options, monitor);

        if (monitor.isCanceled()) {
            monitor.done();
            return result;
        }

        monitor.subTask("Generating Navigation Maps");
        generateNavigationMaps(root, result);
        monitor.worked(10);

        StringBuilder summary = new StringBuilder();
        summary.append("Metadata Generation Summary:\n");
        summary.append("Total sidecars: ").append(processedMetadata.size()).append("\n");
        summary.append("Breakdown by Role:\n");
        result.getRoleStats().forEach((role, count) -> {
            summary.append("- ").append(role).append(": ").append(count).append("\n");
        });
        result.setSummary(summary.toString());

        monitor.done();
        processedMetadata.clear(); // Free memory
        return result;
    }

    private void cleanExistingMetadata(File current) {
        File[] files = current.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                if (!isExcludedDirectory(file)) {
                    cleanExistingMetadata(file);
                }
            } else {
                String name = file.getName();
                if (name.endsWith(".ai.json") || ARCHITECTURE_CONTEXT.equals(name) ||
                    SEMANTIC_OVERVIEW.equals(name) || PACKAGE_CONTEXT.equals(name) ||
                    TRAJECTORY_MAP.equals(name)) {
                    file.delete();
                }
            }
        }
    }

    private int countFiles(File current) {
        int count = 0;
        File[] files = current.listFiles();
        if (files == null) return 0;

        for (File file : files) {
            if (file.isDirectory()) {
                if (!isExcludedDirectory(file)) {
                    count += countFiles(file);
                }
            } else {
                if (shouldProcess(file)) {
                    count++;
                }
            }
        }
        return count;
    }

    private void scanAndEnrich(File current, File root, MetadataResult result, Options options, IProgressMonitor monitor) {
        if (monitor.isCanceled()) return;

        File[] files = current.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (monitor.isCanceled()) return;
            if (file.isDirectory()) {
                if (!isExcludedDirectory(file)) {
                    scanAndEnrich(file, root, result, options, monitor);
                }
            } else {
                if (shouldProcess(file)) {
                    filesProcessedCount++;
                    monitor.subTask("Enriching [" + filesProcessedCount + "] " + file.getName());
                    processFile(file, root, result, options);
                    monitor.worked(1);

                    if (filesProcessedCount % 100 == 0) {
                        logProgress();
                    }
                }
            }
        }
    }

    private void logProgress() {
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
        long maxMemory = runtime.maxMemory() / (1024 * 1024);

        System.out.println(String.format("[MetadataAgent] Processed %d files. Generated %d metadata entries. Heap: %dMB / %dMB",
            filesProcessedCount, processedMetadata.size(), usedMemory, maxMemory));
    }

    private boolean isExcludedDirectory(File dir) {
        String name = dir.getName();
        return name.startsWith(".") ||
               name.equals("target") ||
               name.equals("bin") ||
               name.equals("milestones") ||
               name.equals(".git") ||
               name.equals(".settings") ||
               name.equals(".metadata");
    }

    private boolean shouldProcess(File file) {
        String name = file.getName();
        if (name.endsWith(".ai.json") || name.endsWith(".evo.json")) return false;
        if (name.startsWith(".")) return false;

        // Skip generated artifacts
        if (ARCHITECTURE_CONTEXT.equals(name) || SEMANTIC_OVERVIEW.equals(name) ||
            PACKAGE_CONTEXT.equals(name) || TRAJECTORY_MAP.equals(name)) {
            return false;
        }

        // Process most source and documentation files
        return name.endsWith(".java") || name.endsWith(".c") || name.endsWith(".cpp") ||
               name.endsWith(".py") || name.endsWith(".js") || name.endsWith(".ts") ||
               name.endsWith(".json") || name.endsWith(".xml") || name.endsWith(".yaml") || name.endsWith(".yml") ||
               name.endsWith(".md") || name.endsWith(".txt");
    }

    private void processFile(File file, File root, MetadataResult result, Options options) {
        File sidecar = new File(file.getParentFile(), file.getName() + AIContextTool.METADATA_SUFFIX);
        if (sidecar.exists()) {
            if (options.skipExisting) {
                return;
            }
            if (options.useTimestamp) {
                String suffix = new SimpleDateFormat("_ddMMyy").format(new Date());
                File renamed = new File(file.getParentFile(), file.getName() + suffix + AIContextTool.METADATA_SUFFIX);
                sidecar.renameTo(renamed);
            }
        }

        EvoMetadata meta = contextTool.loadMetadata(file);
        if (meta == null) {
            meta = new EvoMetadata();
            meta.setPath(root.toURI().relativize(file.toURI()).getPath());
        }

        // 2b: Decision Engine (Semantic Coverage Policy)
        assignSemanticRole(file, meta);
        assignScores(meta);

        // 2c: Compressed Summary
        generateSummary(file, meta);

        // 2d: Java Special Case (Inline Annotation logic)
        if (file.getName().endsWith(".java")) {
            checkAndSuggestAnnotations(file, meta);
        }

        // 2f: Save Sidecar
        contextTool.saveMetadata(file, meta);
        processedMetadata.put(file, new MetadataProjection(meta));

        result.addGeneratedFile(sidecar);
        result.incrementRoleStat(meta.getRole());
    }

    private void assignSemanticRole(File file, EvoMetadata meta) {
        String path = file.getAbsolutePath().replace(File.separatorChar, '/');
        if (path.contains("/orchestration/")) meta.setRole("orchestration");
        else if (path.contains("/mediation/")) meta.setRole("mediation");
        else if (path.contains("/supervision/")) meta.setRole("supervision");
        else if (path.contains("/trajectory/")) meta.setRole("trajectory");
        else if (path.contains("/execution/")) meta.setRole("execution");
        else if (path.contains("/view/") || path.contains("/ui/")) meta.setRole("ui");
        else if (path.contains("/model/")) meta.setRole("domain");
        else if (path.contains("/utils/")) meta.setRole("utility");
        else if (path.endsWith(".md")) meta.setRole("documentation");
        else meta.setRole("unknown");

        meta.setDomain("evolution"); // Default domain for this system
    }

    private void assignScores(EvoMetadata meta) {
        String role = meta.getRole();
        double relevance = 0.5;
        double importance = 0.5;

        // SEMANTIC COVERAGE POLICY
        if ("orchestration".equals(role) || "mediation".equals(role) || "supervision".equals(role)) {
            relevance = 1.0;
            importance = 1.0;
            meta.setStability(Stability.STABLE.name());
        } else if ("trajectory".equals(role) || "execution".equals(role)) {
            relevance = 0.9;
            importance = 0.9;
        } else if ("domain".equals(role)) {
            relevance = 0.7;
            importance = 0.7;
        } else if ("ui".equals(role)) {
            relevance = 0.4;
            importance = 0.4;
        } else if ("utility".equals(role)) {
            relevance = 0.2;
            importance = 0.2;
        }

        meta.setMediatedRelevanceScore(relevance);
        meta.setImportanceScore(importance);

        if (relevance > 0.8) {
            meta.getContextSelectionHints().add("mandatory_context");
        }
    }

    private void generateSummary(File file, EvoMetadata meta) {
        if (file.length() > MAX_METADATA_FILE_SIZE) {
            meta.setSummary("Large file - summary skipped");
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            StringBuilder sb = new StringBuilder();
            String line;
            int linesRead = 0;
            int nonEmptyLines = 0;
            int totalChars = 0;

            while ((line = reader.readLine()) != null) {
                linesRead++;
                String trimmed = line.trim();
                if (!trimmed.isEmpty() && !trimmed.startsWith("/") && !trimmed.startsWith("*") && !trimmed.startsWith("#")) {
                    sb.append(trimmed).append(" ");
                    nonEmptyLines++;
                    totalChars += trimmed.length() + 1;
                }

                if (linesRead >= MAX_LINES_TO_SCAN || nonEmptyLines >= MAX_SUMMARY_LINES || totalChars >= MAX_KB_FOR_SUMMARY) {
                    break;
                }
            }

            String summary = sb.toString().trim();
            if (summary.length() > MAX_SUMMARY_LENGTH) {
                summary = summary.substring(0, MAX_SUMMARY_LENGTH);
            }
            meta.setSummary(summary);
        } catch (IOException e) {
            meta.setSummary("Error reading file content.");
        }
    }

    private void checkAndSuggestAnnotations(File file, EvoMetadata meta) {
        if (meta.getImportanceScore() > 0.8) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                boolean found = false;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("@EvolutionComponent")) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    meta.setEvolutionaryNotes("Suggestion: Add @EvolutionComponent annotation to this high-importance class.");
                }
            } catch (IOException e) {}
        }
    }

    private void generateNavigationMaps(File root, MetadataResult result) {
        // 2e: PACKAGE_CONTEXT.md, ARCHITECTURE_CONTEXT.md, TRAJECTORY_MAP.json, SEMANTIC_OVERVIEW.md
        result.addGeneratedFile(generateArchitectureContext(root));
        result.addGeneratedFile(generateSemanticOverview(root));
        result.addGeneratedFile(generateTrajectoryMap(root));

        // Per directory package context
        generatePackageContexts(root, result);
    }

    private File generateArchitectureContext(File root) {
        File archFile = new File(root, ARCHITECTURE_CONTEXT);
        StringBuilder sb = new StringBuilder("# ARCHITECTURE CONTEXT\n\n");
        sb.append("This file provides an LLM navigation map for mediated reasoning.\n\n");
        sb.append("## Core Domains\n");
        sb.append("* **Orchestration**: Lifecycle coordination and flow management.\n");
        sb.append("* **Mediation**: Context curation and prompt synthesis.\n");
        sb.append("* **Supervision**: Decision authority and resolver policies.\n");
        sb.append("* **Trajectory**: Lineage, signals, and historical memory.\n");
        sb.append("* **Execution**: Deterministic scheduling and budget control.\n\n");

        sb.append("## High-Importance Components\n");
        for (MetadataProjection m : processedMetadata.values()) {
            if (m.getImportanceScore() > 0.9) {
                sb.append("* `").append(m.getPath()).append("`: ").append(m.getSummary()).append("\n");
            }
        }

        try {
            Files.write(archFile.toPath(), sb.toString().getBytes());
        } catch (IOException e) {}
        return archFile;
    }

    private File generateSemanticOverview(File root) {
        File overviewFile = new File(root, SEMANTIC_OVERVIEW);
        StringBuilder sb = new StringBuilder("# SEMANTIC OVERVIEW\n\n");
        sb.append("Summary of the system's semantic nervous system.\n\n");

        Map<String, List<String>> byRole = new HashMap<>();
        for (MetadataProjection m : processedMetadata.values()) {
            byRole.computeIfAbsent(m.getRole(), k -> new ArrayList<>()).add(m.getPath());
        }

        for (Map.Entry<String, List<String>> entry : byRole.entrySet()) {
            sb.append("## Role: ").append(entry.getKey()).append("\n");
            sb.append("Count: ").append(entry.getValue().size()).append("\n\n");
        }

        try {
            Files.write(overviewFile.toPath(), sb.toString().getBytes());
        } catch (IOException e) {}
        return overviewFile;
    }

    private File generateTrajectoryMap(File root) {
        File mapFile = new File(root, TRAJECTORY_MAP);
        StringBuilder sb = new StringBuilder("{\n  \"version\": \"1.0\",\n  \"components\": [\n");

        List<String> items = new ArrayList<>();
        for (MetadataProjection m : processedMetadata.values()) {
            if (m.getImportanceScore() > 0.7) {
                items.add("    { \"path\": \"" + m.getPath() + "\", \"role\": \"" + m.getRole() + "\", \"importance\": " + m.getImportanceScore() + " }");
            }
        }
        sb.append(String.join(",\n", items));
        sb.append("\n  ]\n}");

        try {
            Files.write(mapFile.toPath(), sb.toString().getBytes());
        } catch (IOException e) {}
        return mapFile;
    }

    private void generatePackageContexts(File root, MetadataResult result) {
        Map<File, List<File>> byDir = processedMetadata.keySet().stream().collect(Collectors.groupingBy(File::getParentFile));
        for (Map.Entry<File, List<File>> entry : byDir.entrySet()) {
            File dir = entry.getKey();
            File pkgFile = new File(dir, PACKAGE_CONTEXT);

            StringBuilder sb = new StringBuilder("# PACKAGE CONTEXT\n\n");
            sb.append("## Directory: ").append(root.toURI().relativize(dir.toURI()).getPath()).append("\n\n");

            String domain = inferDomain(dir);
            sb.append("## Domain: ").append(domain).append("\n\n");

            sb.append("## Components\n");
            for (File f : entry.getValue()) {
                MetadataProjection m = processedMetadata.get(f);
                if (m != null) {
                    sb.append("* `").append(f.getName()).append("`: ").append(m.getSummary()).append("\n");
                }
            }

            try {
                Files.write(pkgFile.toPath(), sb.toString().getBytes());
                result.addGeneratedFile(pkgFile);
            } catch (IOException e) {}
        }
    }

    private String inferDomain(File dir) {
        String path = dir.getAbsolutePath();
        if (path.contains("/orchestration")) return "orchestration";
        if (path.contains("/mediation")) return "mediation";
        if (path.contains("/supervision")) return "supervision";
        if (path.contains("/trajectory")) return "trajectory";
        if (path.contains("/execution")) return "execution";
        return "general";
    }
}
