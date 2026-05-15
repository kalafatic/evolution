package eu.kalafatic.evolution.controller.mediation.analysis;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import eu.kalafatic.utils.semantic.AIContextTool;
import eu.kalafatic.utils.semantic.EvoMetadata;
import eu.kalafatic.utils.semantic.Stability;

/**
 * Universal AI Metadata Generation Agent.
 * Automates creation of .ai.json sidecars and project navigation maps.
 * Optimized for MEDIATED MODE and Semantic Nervous System architecture.
 */
public class MetadataGenerator {

    private final AIContextTool contextTool = new AIContextTool();
    private final Map<File, EvoMetadata> processedMetadata = new HashMap<>();

    public void generate(File root) {
        if (!root.exists() || !root.isDirectory()) return;

        processedMetadata.clear();
        scanAndEnrich(root, root);

        generateNavigationMaps(root);
    }

    private void scanAndEnrich(File current, File root) {
        File[] files = current.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                if (!file.getName().startsWith(".") && !file.getName().equals("target") && !file.getName().equals("bin")) {
                    scanAndEnrich(file, root);
                }
            } else {
                if (shouldProcess(file)) {
                    processFile(file, root);
                    processedFiles.add(file);
                }
            }
        }
    }

    private boolean shouldProcess(File file) {
        String name = file.getName();
        if (name.endsWith(".ai.json") || name.endsWith(".evo.json")) return false;
        if (name.startsWith(".")) return false;

        // Process most source and documentation files
        return name.endsWith(".java") || name.endsWith(".c") || name.endsWith(".cpp") ||
               name.endsWith(".py") || name.endsWith(".js") || name.endsWith(".ts") ||
               name.endsWith(".json") || name.endsWith(".xml") || name.endsWith(".yaml") || name.endsWith(".yml") ||
               name.endsWith(".md") || name.endsWith(".txt");
    }

    private void processFile(File file, File root) {
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
        processedMetadata.put(file, meta);
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
        try {
            List<String> lines = Files.readAllLines(file.toPath());
            StringBuilder summary = new StringBuilder();
            int count = 0;
            for (String line : lines) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty() && !trimmed.startsWith("/") && !trimmed.startsWith("*") && !trimmed.startsWith("#")) {
                    summary.append(trimmed).append(" ");
                    if (++count >= 3) break;
                }
            }
            meta.setSummary(summary.toString().trim());
        } catch (IOException e) {
            meta.setSummary("Error reading file content.");
        }
    }

    private void checkAndSuggestAnnotations(File file, EvoMetadata meta) {
        if (meta.getImportanceScore() > 0.8) {
            try {
                String content = new String(Files.readAllBytes(file.toPath()));
                if (!content.contains("@EvolutionComponent")) {
                    meta.setEvolutionaryNotes("Suggestion: Add @EvolutionComponent annotation to this high-importance class.");
                }
            } catch (IOException e) {}
        }
    }

    private void generateNavigationMaps(File root) {
        // 2e: PACKAGE_CONTEXT.md, ARCHITECTURE_CONTEXT.md, TRAJECTORY_MAP.json, SEMANTIC_OVERVIEW.md
        generateArchitectureContext(root);
        generateSemanticOverview(root);
        generateTrajectoryMap(root);

        // Per directory package context
        generatePackageContexts(root);
    }

    private void generateArchitectureContext(File root) {
        File archFile = new File(root, "ARCHITECTURE_CONTEXT.md");
        StringBuilder sb = new StringBuilder("# ARCHITECTURE CONTEXT\n\n");
        sb.append("This file provides an LLM navigation map for mediated reasoning.\n\n");
        sb.append("## Core Domains\n");
        sb.append("* **Orchestration**: Lifecycle coordination and flow management.\n");
        sb.append("* **Mediation**: Context curation and prompt synthesis.\n");
        sb.append("* **Supervision**: Decision authority and resolver policies.\n");
        sb.append("* **Trajectory**: Lineage, signals, and historical memory.\n");
        sb.append("* **Execution**: Deterministic scheduling and budget control.\n\n");

        sb.append("## High-Importance Components\n");
        for (EvoMetadata m : processedMetadata.values()) {
            if (m.getImportanceScore() > 0.9) {
                sb.append("* `").append(m.getPath()).append("`: ").append(m.getSummary()).append("\n");
            }
        }

        try {
            Files.write(archFile.toPath(), sb.toString().getBytes());
        } catch (IOException e) {}
    }

    private void generateSemanticOverview(File root) {
        File overviewFile = new File(root, "SEMANTIC_OVERVIEW.md");
        StringBuilder sb = new StringBuilder("# SEMANTIC OVERVIEW\n\n");
        sb.append("Summary of the system's semantic nervous system.\n\n");

        Map<String, List<String>> byRole = new HashMap<>();
        for (EvoMetadata m : processedMetadata.values()) {
            byRole.computeIfAbsent(m.getRole(), k -> new ArrayList<>()).add(m.getPath());
        }

        for (Map.Entry<String, List<String>> entry : byRole.entrySet()) {
            sb.append("## Role: ").append(entry.getKey()).append("\n");
            sb.append("Count: ").append(entry.getValue().size()).append("\n\n");
        }

        try {
            Files.write(overviewFile.toPath(), sb.toString().getBytes());
        } catch (IOException e) {}
    }

    private void generateTrajectoryMap(File root) {
        File mapFile = new File(root, "TRAJECTORY_MAP.json");
        StringBuilder sb = new StringBuilder("{\n  \"version\": \"1.0\",\n  \"components\": [\n");

        List<String> items = new ArrayList<>();
        for (EvoMetadata m : processedMetadata.values()) {
            if (m.getImportanceScore() > 0.7) {
                items.add("    { \"path\": \"" + m.getPath() + "\", \"role\": \"" + m.getRole() + "\", \"importance\": " + m.getImportanceScore() + " }");
            }
        }
        sb.append(String.join(",\n", items));
        sb.append("\n  ]\n}");

        try {
            Files.write(mapFile.toPath(), sb.toString().getBytes());
        } catch (IOException e) {}
    }

    private void generatePackageContexts(File root) {
        Map<File, List<File>> byDir = processedMetadata.keySet().stream().collect(Collectors.groupingBy(File::getParentFile));
        for (Map.Entry<File, List<File>> entry : byDir.entrySet()) {
            File dir = entry.getKey();
            File pkgFile = new File(dir, "PACKAGE_CONTEXT.md");

            StringBuilder sb = new StringBuilder("# PACKAGE CONTEXT\n\n");
            sb.append("## Directory: ").append(root.toURI().relativize(dir.toURI()).getPath()).append("\n\n");

            String domain = inferDomain(dir);
            sb.append("## Domain: ").append(domain).append("\n\n");

            sb.append("## Components\n");
            for (File f : entry.getValue()) {
                EvoMetadata m = processedMetadata.get(f);
                if (m != null) {
                    sb.append("* `").append(f.getName()).append("`: ").append(m.getSummary()).append("\n");
                }
            }

            try {
                Files.write(pkgFile.toPath(), sb.toString().getBytes());
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
