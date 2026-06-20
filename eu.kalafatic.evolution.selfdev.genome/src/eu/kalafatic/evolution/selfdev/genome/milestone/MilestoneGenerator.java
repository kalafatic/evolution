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

import eu.kalafatic.evolution.selfdev.genome.model.KnowledgeMetadata;
import org.json.JSONArray;
import org.json.JSONObject;

import eu.kalafatic.utils.semantic.AIContextTool;
import eu.kalafatic.utils.semantic.EvoMetadata;

public class MilestoneGenerator {

    private final AIContextTool contextTool = new AIContextTool();

    public String generateMilestone(File root, String projectName, String version) {
        List<EvoMetadata> allMetadata = scanAllMetadata(root);
        
        LocalDateTime now = LocalDateTime.now();
        String year = String.valueOf(now.getYear());
        String date = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String timestamp = now.format(DateTimeFormatter.ofPattern("ddMMyy_HHmmss"));

        File genomeRoot = new File(root, "genome");
        File currentDir = new File(genomeRoot, "current");
        File historyDir = new File(new File(genomeRoot, "history"), year);
        File dailyDir = new File(historyDir, date);
        
        currentDir.mkdirs();
        dailyDir.mkdirs();

        // 1. Generate artifacts in 'current' directory (Latest state)
        generateArtifacts(currentDir, projectName, version, timestamp, allMetadata);

        // 2. Preserve historical snapshot (Never overwritten)
        // If dailyDir exists, we might want a subfolder with timestamp if multiple updates per day
        File snapshotDir = new File(dailyDir, timestamp);
        snapshotDir.mkdirs();
        generateArtifacts(snapshotDir, projectName, version, timestamp, allMetadata);

        // 3. Handle Milestones if requested (can be expanded later)
        // For now, use the old milestone logic to keep it compatible but within the new structure
        File milestonesDir = new File(genomeRoot, "milestones");
        milestonesDir.mkdirs();

        return timestamp;
    }

    private void generateArtifacts(File dir, String projectName, String version, String timestamp, List<EvoMetadata> allMetadata) {
        generateGenomeJson(dir, projectName, version, timestamp, allMetadata);
        generateArchitectureMd(dir, allMetadata);
        generateUseCasesMd(dir, allMetadata);
        generateMilestoneV1Md(dir, allMetadata);
        generateDashboardHtml(dir, projectName, timestamp);
    }

    private void generateDashboardHtml(File dir, String projectName, String timestamp) {
        try {
            String archMd = Files.readString(new File(dir, "architecture.md").toPath());
            String ucMd = Files.readString(new File(dir, "use_cases.md").toPath());
            String milestoneMd = Files.readString(new File(dir, "milestone_v1.md").toPath());
            String genomeJson = Files.readString(new File(dir, "genome.json").toPath());

            String archHtml = eu.kalafatic.evolution.selfdev.genome.util.SimpleMarkdownConverter.toHtml(archMd);
            String ucHtml = eu.kalafatic.evolution.selfdev.genome.util.SimpleMarkdownConverter.toHtml(ucMd);
            String milestoneHtml = eu.kalafatic.evolution.selfdev.genome.util.SimpleMarkdownConverter.toHtml(milestoneMd);

            String dashboardHtml = eu.kalafatic.evolution.selfdev.genome.util.DashboardTemplate.getHtml(
                    projectName, timestamp, archHtml, ucHtml, milestoneHtml, genomeJson);

            Files.write(new File(dir, "milestone_dashboard.html").toPath(), dashboardHtml.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
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
        KnowledgeMetadata km = new KnowledgeMetadata();
        km.setId("arch-overview");
        km.setTitle("Architecture Overview");
        km.setDocumentType("ARCHITECTURE");
        km.setSummaryLevel("HIGH");
        km.setCreated(LocalDateTime.now().toString());
        km.setStatus("PUBLISHED");

        StringBuilder sb = new StringBuilder();
        sb.append(km.toMarkdownHeader());
        sb.append("# Architecture Overview\n\n");
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
        KnowledgeMetadata km = new KnowledgeMetadata();
        km.setId("use-cases");
        km.setTitle("Use Cases and Behaviors");
        km.setDocumentType("REQUIREMENTS");
        km.setSummaryLevel("DETAILED");
        km.setCreated(LocalDateTime.now().toString());
        km.setStatus("PUBLISHED");

        StringBuilder sb = new StringBuilder();
        sb.append(km.toMarkdownHeader());
        sb.append("# Use Cases and Behaviors\n\n");
        
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
        KnowledgeMetadata km = new KnowledgeMetadata();
        km.setId("milestone-v1");
        km.setTitle("Milestone Freezepoint v1");
        km.setDocumentType("MILESTONE");
        km.setSummaryLevel("HIGH");
        km.setCreated(LocalDateTime.now().toString());
        km.setStatus("STABLE");

        StringBuilder sb = new StringBuilder();
        sb.append(km.toMarkdownHeader());
        sb.append("# Milestone Freezepoint v1\n\n");
        
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
