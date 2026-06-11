package eu.kalafatic.evolution.selfdev.genome.milestone;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;

import eu.kalafatic.utils.semantic.AIContextTool;
import eu.kalafatic.utils.semantic.EvoMetadata;

public class MilestoneGenerator {

    private final AIContextTool contextTool = new AIContextTool();

    public void generateMilestone(File root, String projectName, String version) {
        List<EvoMetadata> allMetadata = scanAllMetadata(root);
        
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("ddMMyy"));
        File milestoneDir = new File(root, "milestones/genome_" + timestamp);
        milestoneDir.mkdirs();

        generateGenomeJson(milestoneDir, projectName, version, timestamp, allMetadata);
        generateArchitectureMd(milestoneDir, allMetadata);
        generateUseCasesMd(milestoneDir, allMetadata);
        generateMilestoneV1Md(milestoneDir, allMetadata);
        
        applyRetentionPolicy(new File(root, "milestones"));
    }

    private List<EvoMetadata> scanAllMetadata(File current) {
        List<EvoMetadata> result = new ArrayList<>();
        scanRecursive(current, result);
        return result;
    }

    private void scanRecursive(File current, List<EvoMetadata> result) {
        if (current.isDirectory()) {
            if (current.getName().equals(".git") || current.getName().equals("target") || current.getName().equals("milestones")) return;
            File[] children = current.listFiles();
            if (children != null) {
                for (File child : children) scanRecursive(child, result);
            }
        } else {
            if (current.getName().endsWith(".ai.json") && !current.getName().equals("PACKAGE_CONTEXT.md.ai.json")) {
                // Try to load metadata for the corresponding source file
                String sourceName = current.getName().substring(0, current.getName().length() - ".ai.json".length());
                File sourceFile = new File(current.getParentFile(), sourceName);
                if (sourceFile.exists()) {
                    EvoMetadata meta = contextTool.loadMetadata(sourceFile);
                    if (meta != null) result.add(meta);
                }
            }
        }
    }

    private void generateGenomeJson(File dir, String name, String version, String timestamp, List<EvoMetadata> metadata) {
        JSONObject genome = new JSONObject();
        
        JSONObject identity = new JSONObject();
        identity.put("name", name);
        identity.put("version", version);
        identity.put("timestamp", timestamp);
        genome.put("identity", identity);

        JSONArray concepts = new JSONArray();
        metadata.stream()
            .flatMap(m -> m.getConcepts().stream())
            .distinct()
            .forEach(concepts::put);
        genome.put("concepts", concepts);

        JSONObject modules = new JSONObject();
        metadata.stream()
            .collect(Collectors.groupingBy(m -> m.getDomain() != null ? m.getDomain() : "unknown"))
            .forEach((domain, list) -> {
                JSONArray files = new JSONArray();
                list.forEach(m -> files.put(m.getPath()));
                modules.put(domain, files);
            });
        genome.put("moduleMap", modules);

        JSONObject flows = new JSONObject();
        // Heuristic flow detection
        flows.put("orchestration", new JSONArray(metadata.stream().filter(m -> "orchestration".equals(m.getDomain())).map(EvoMetadata::getPath).toList()));
        flows.put("data", new JSONArray(metadata.stream().filter(m -> "model".equals(m.getArchitecturalLayer())).map(EvoMetadata::getPath).toList()));
        flows.put("control", new JSONArray(metadata.stream().filter(m -> "controller".equals(m.getArchitecturalLayer())).map(EvoMetadata::getPath).toList()));
        genome.put("executionFlows", flows);

        try {
            Files.write(new File(dir, "genome.json").toPath(), genome.toString(2).getBytes());
        } catch (IOException e) {}
    }

    private void generateArchitectureMd(File dir, List<EvoMetadata> metadata) {
        StringBuilder sb = new StringBuilder("# Architecture Overview\n\n");
        sb.append("## Major Modules\n");
        metadata.stream()
            .collect(Collectors.groupingBy(m -> m.getDomain() != null ? m.getDomain() : "unknown"))
            .forEach((domain, list) -> {
                sb.append("### ").append(domain).append("\n");
                sb.append("Files: ").append(list.size()).append("\n\n");
                list.stream().limit(5).forEach(m -> sb.append("- ").append(m.getPath()).append(": ").append(m.getSummary()).append("\n"));
                if (list.size() > 5) sb.append("- ...\n");
                sb.append("\n");
            });

        sb.append("## Critical Components\n");
        metadata.stream()
            .filter(m -> "HIGH".equals(m.getSystemCriticality()))
            .forEach(m -> sb.append("- **").append(m.getPath()).append("**: ").append(m.getPurpose()).append("\n"));

        try {
            Files.write(new File(dir, "architecture.md").toPath(), sb.toString().getBytes());
        } catch (IOException e) {}
    }

    private void generateUseCasesMd(File dir, List<EvoMetadata> metadata) {
        StringBuilder sb = new StringBuilder("# Use Cases and Behaviors\n\n");
        
        metadata.stream()
            .filter(m -> m.getCapabilities() != null && !m.getCapabilities().isEmpty())
            .forEach(m -> {
                sb.append("## ").append(m.getPath()).append("\n");
                sb.append("Capabilities: ").append(String.join(", ", m.getCapabilities())).append("\n");
                sb.append("Purpose: ").append(m.getPurpose()).append("\n\n");
            });

        try {
            Files.write(new File(dir, "use_cases.md").toPath(), sb.toString().getBytes());
        } catch (IOException e) {}
    }

    private void generateMilestoneV1Md(File dir, List<EvoMetadata> metadata) {
        StringBuilder sb = new StringBuilder("# Milestone Freezepoint v1\n\n");
        
        sb.append("## Stable Core\n");
        metadata.stream()
            .filter(m -> "STABLE".equals(m.getStability()))
            .forEach(m -> sb.append("- ").append(m.getPath()).append("\n"));

        sb.append("\n## Controlled Evolution Zone\n");
        metadata.stream()
            .filter(m -> "EVOLVING".equals(m.getStability()) || "MEDIUM".equals(m.getMutationRisk()))
            .forEach(m -> sb.append("- ").append(m.getPath()).append("\n"));

        sb.append("\n## Core Invariants\n");
        sb.append("- System must maintain architectural integrity between model and controller.\n");
        sb.append("- Evolution must be grounded in metadata before code mutation.\n");

        sb.append("\n## Supervisor Contract\n");
        sb.append("- Build gates: Maven 'clean install'\n");
        sb.append("- Test gates: JUnit integration tests\n");
        sb.append("- Rollback triggers: Build failure or test regression\n");

        try {
            Files.write(new File(dir, "milestone_v1.md").toPath(), sb.toString().getBytes());
        } catch (IOException e) {}
    }

    private void applyRetentionPolicy(File milestonesDir) {
        if (!milestonesDir.exists()) return;
        File[] dirs = milestonesDir.listFiles(File::isDirectory);
        if (dirs != null && dirs.length > 8) {
            Arrays.sort(dirs, Comparator.comparingLong(File::lastModified));
            for (int i = 0; i < dirs.length - 8; i++) {
                deleteDirectory(dirs[i]);
            }
        }
    }

    private void deleteDirectory(File dir) {
        File[] children = dir.listFiles();
        if (children != null) {
            for (File child : children) deleteDirectory(child);
        }
        dir.delete();
    }
}
