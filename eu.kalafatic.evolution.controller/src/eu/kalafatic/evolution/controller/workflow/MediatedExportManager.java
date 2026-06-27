package eu.kalafatic.evolution.controller.workflow;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import eu.kalafatic.evolution.controller.mediation.model.ArchitecturalFact;
import eu.kalafatic.evolution.controller.mediation.model.ArchitecturalGene;
import eu.kalafatic.evolution.controller.mediation.model.Hotspot;
import eu.kalafatic.evolution.controller.mediation.model.KnowledgeGap;
import eu.kalafatic.evolution.controller.mediation.model.Subsystem;
import eu.kalafatic.evolution.controller.mediation.model.TargetRealityModel;
import eu.kalafatic.evolution.controller.orchestration.design.ComponentRecord;
import eu.kalafatic.evolution.controller.orchestration.design.DesignModel;
import eu.kalafatic.evolution.controller.orchestration.design.DesignRenderer;
import eu.kalafatic.evolution.controller.orchestration.design.RelationshipRecord;

/**
 * Handles packaging of mediated context and prompts into a ZIP archive.
 */
public class MediatedExportManager {

    public enum ExportProfile {
        FULL,
        ARCHITECTURE,
        IMPLEMENTATION,
        GENOME
    }

    public File createUnifiedExport(TargetRealityModel model, ExportProfile profile, String goal, File projectRoot, String outputPath, String sessionId) throws IOException {
        String fileName = "mediated_export_" + (profile != null ? profile.name().toLowerCase() : "full") + "_" + sessionId + "_" + System.currentTimeMillis() + ".zip";
        File targetDir = projectRoot;
        if (outputPath != null && !outputPath.isEmpty()) {
            targetDir = new File(outputPath);
            if (!targetDir.exists()) targetDir.mkdirs();
        }
        File zipFile = new File(targetDir, fileName);

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
            // Genome A: The Optimized Prompt
            String prompt = (String) model.getMetadata().get("optimizedPrompt");
            if (prompt == null) prompt = "Analyze the provided project context.";
            addStringToZip(zos, "prompt.md", prompt);

            addRootReadme(zos, model, profile);

            if (profile == ExportProfile.FULL || profile == ExportProfile.ARCHITECTURE) {
                addArchitectureSection(zos, model);
            }
            if (profile == ExportProfile.FULL || profile == ExportProfile.IMPLEMENTATION) {
                addImplementationSection(zos, model, goal, projectRoot);
            }
            if (profile == ExportProfile.FULL || profile == ExportProfile.GENOME) {
                addGenomeSection(zos, model);
            }
        }
        return zipFile;
    }

    private void addRootReadme(ZipOutputStream zos, TargetRealityModel model, ExportProfile profile) throws IOException {
        StringBuilder sb = new StringBuilder("# Mediated Export Package\n\n");
        sb.append("Profile: ").append(profile != null ? profile.name() : "FULL").append("\n");
        sb.append("Domain: ").append(model.getDomain() != null ? model.getDomain() : "Unknown").append("\n\n");
        sb.append("## Overview\n").append(model.getArchitectureSummary() != null ? model.getArchitectureSummary() : "N/A").append("\n\n");
        sb.append("## Structure\n");
        if (profile == ExportProfile.FULL || profile == ExportProfile.ARCHITECTURE) {
            sb.append("- `architecture/`: Complete architectural model and visualization.\n");
        }
        sb.append("- `observability/`: Evolutionary Event Lineage and causal chains.\n");
        if (profile == ExportProfile.FULL || profile == ExportProfile.IMPLEMENTATION) {
            sb.append("- `implementation/`: implementation-ready context and source files.\n");
        }
        if (profile == ExportProfile.FULL || profile == ExportProfile.GENOME) {
            sb.append("- `genome/`: Portable architectural genes and patterns.\n");
        }

        addStringToZip(zos, "README.md", sb.toString());
    }

    public File createExportPackage(String sessionId, String prompt, List<String> selectedPaths, File projectRoot, String outputPath, String metadataJson, String historyAnalysis, String architectureSummary, String dependencies, String executionInstructions) throws IOException {
        return createExportPackage(sessionId, prompt, selectedPaths, projectRoot, outputPath, metadataJson, historyAnalysis, architectureSummary, dependencies, executionInstructions, null);
    }

    public File createExportPackage(String sessionId, String prompt, List<String> selectedPaths, File projectRoot, String outputPath, String metadataJson, String historyAnalysis, String architectureSummary, String dependencies, String executionInstructions, String realityModelJson) throws IOException {
        return createExportPackage(sessionId, prompt, selectedPaths, projectRoot, outputPath, metadataJson, historyAnalysis, architectureSummary, dependencies, executionInstructions, realityModelJson, null, null);
    }

    public File createExportPackage(String sessionId, String prompt, List<String> selectedPaths, File projectRoot, String outputPath, String metadataJson, String historyAnalysis, String architectureSummary, String dependencies, String executionInstructions, String realityModelJson, String architecturalFactsJson, String subsystemsJson) throws IOException {
        return createExportPackage(sessionId, prompt, selectedPaths, projectRoot, outputPath, metadataJson, historyAnalysis, architectureSummary, dependencies, executionInstructions, realityModelJson, architecturalFactsJson, subsystemsJson, null);
    }

    public File createExportPackage(String sessionId, String prompt, List<String> selectedPaths, File projectRoot, String outputPath, String metadataJson, String historyAnalysis, String architectureSummary, String dependencies, String executionInstructions, String realityModelJson, String architecturalFactsJson, String subsystemsJson, String knowledgeGapsJson) throws IOException {
        return createExportPackage(sessionId, prompt, selectedPaths, projectRoot, outputPath, metadataJson, historyAnalysis, architectureSummary, dependencies, executionInstructions, realityModelJson, architecturalFactsJson, subsystemsJson, knowledgeGapsJson, null);
    }

    public File createExportPackage(String sessionId, String prompt, List<String> selectedPaths, File projectRoot, String outputPath, String metadataJson, String historyAnalysis, String architectureSummary, String dependencies, String executionInstructions, String realityModelJson, String architecturalFactsJson, String subsystemsJson, String knowledgeGapsJson, String genesJson) throws IOException {
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
            if (knowledgeGapsJson != null) {
                addStringToZip(zos, "knowledge-gaps.json", knowledgeGapsJson);
            }
            if (genesJson != null) {
                addStringToZip(zos, "genome.json", genesJson);
            }

            // 4. Add Selected File Contents
            for (String path : selectedPaths) {
                String normalizedPath = path.replace('\\', '/');
                File file = new File(projectRoot, normalizedPath);
                if (file.exists() && file.isFile()) {
                    addFileToZip(zos, "implementation/files/" + normalizedPath, file);
                } else {
                    // Try absolute path if relative fails
                    File absFile = new File(normalizedPath);
                    if (absFile.exists() && absFile.isFile()) {
                        String zipEntryName = normalizedPath;
                        if (normalizedPath.startsWith(projectRoot.getAbsolutePath())) {
                            zipEntryName = projectRoot.toPath().relativize(absFile.toPath()).toString().replace('\\', '/');
                        }
                        addFileToZip(zos, "implementation/files/" + zipEntryName, absFile);
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
        return createExportFolder(sessionId, prompt, selectedPaths, projectRoot, outputPath, metadataJson, historyAnalysis, architectureSummary, dependencies, executionInstructions, realityModelJson, architecturalFactsJson, subsystemsJson, null);
    }

    public File createExportFolder(String sessionId, String prompt, List<String> selectedPaths, File projectRoot, String outputPath, String metadataJson, String historyAnalysis, String architectureSummary, String dependencies, String executionInstructions, String realityModelJson, String architecturalFactsJson, String subsystemsJson, String knowledgeGapsJson) throws IOException {
        return createExportFolder(sessionId, prompt, selectedPaths, projectRoot, outputPath, metadataJson, historyAnalysis, architectureSummary, dependencies, executionInstructions, realityModelJson, architecturalFactsJson, subsystemsJson, knowledgeGapsJson, null);
    }

    public File createExportFolder(String sessionId, String prompt, List<String> selectedPaths, File projectRoot, String outputPath, String metadataJson, String historyAnalysis, String architectureSummary, String dependencies, String executionInstructions, String realityModelJson, String architecturalFactsJson, String subsystemsJson, String knowledgeGapsJson, String genesJson) throws IOException {
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
        if (knowledgeGapsJson != null) writeStringToFile(new File(exportDir, "knowledge-gaps.json"), knowledgeGapsJson);
        if (genesJson != null) writeStringToFile(new File(exportDir, "genome.json"), genesJson);

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
        Files.copy(source.toPath(), targetFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }

    private void addArchitectureSection(ZipOutputStream zos, TargetRealityModel model) throws IOException {
        addLogSection(zos, model);

        DesignModel designModel = convertToDesignModel(model);
        DesignRenderer renderer = new DesignRenderer();

        addStringToZip(zos, "architecture/architecture.html", renderer.render(designModel));
        addStringToZip(zos, "architecture/architecture-graph.json", renderer.serializeModel(designModel));
        addStringToZip(zos, "architecture/reality-model.json", serializeToJson(model));
        addStringToZip(zos, "architecture/subsystems.json", serializeListToJson(model.getSubsystems()));
        addStringToZip(zos, "architecture/architectural-facts.json", serializeListToJson(model.getArchitecturalFacts()));
        addStringToZip(zos, "architecture/hotspots.json", serializeListToJson(model.getHotspots()));

        addStringToZip(zos, "architecture/execution-flow.json", serializeListToJson(model.getExecutionFlows()));
        addStringToZip(zos, "architecture/decision-flow.json", serializeListToJson(model.getDecisionFlows()));
        addStringToZip(zos, "architecture/influence-graph.json", serializeToJson(model.getInfluenceGraph()));
        addStringToZip(zos, "architecture/subsystem-graph.json", renderer.serializeModel(designModel));
    }

    private DesignModel convertToDesignModel(TargetRealityModel reality) {
        DesignModel model = new DesignModel();
        model.setName(reality.getDomain() != null ? reality.getDomain() : "Discovered Architecture");

        if (reality.getDomain() != null) {
            ComponentRecord d = new ComponentRecord();
            d.setId("reality:domain");
            d.setName(reality.getDomain());
            d.setType("DOMAIN");
            d.setDescription(reality.getPurpose());
            model.getComponents().add(d);
        }

        for (Subsystem sub : reality.getSubsystems()) {
            ComponentRecord sr = new ComponentRecord();
            sr.setId("subsystem:" + sub.getName());
            sr.setName(sub.getName());
            sr.setType("SUBSYSTEM");
            sr.setDescription(sub.getPurpose());
            model.getComponents().add(sr);
        }

        for (Hotspot h : reality.getHotspots()) {
            ComponentRecord hr = new ComponentRecord();
            hr.setId("reality:hotspot:" + h.getId());
            hr.setName(h.getName());
            hr.setType("HOTSPOT");
            hr.setDescription(h.getDescription());
            hr.setImportanceScore(h.getSignificance());
            model.getComponents().add(hr);

            for (String art : h.getRelatedArtifacts()) {
                RelationshipRecord rel = new RelationshipRecord();
                rel.setFrom(hr.getId());
                rel.setTo(art);
                rel.setType("HIGHLIGHTS");
                model.getRelationships().add(rel);
            }
        }

        return model;
    }

    private String serializeToJson(Object obj) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }

    private String serializeListToJson(List<?> list) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(list);
        } catch (Exception e) {
            return "[]";
        }
    }

    private void addGenomeSection(ZipOutputStream zos, TargetRealityModel model) throws IOException {
        addStringToZip(zos, "genome/genes.json", serializeListToJson(model.getGenes()));
        addStringToZip(zos, "genome/patterns.json", serializeListToJson(model.getPatterns()));
        addStringToZip(zos, "genome/lessons.json", serializeListToJson(model.getLessons()));
        addStringToZip(zos, "genome/reference-implementations.json", serializeListToJson(model.getReferenceImplementations()));

        StringBuilder sb = new StringBuilder("# Portable Architectural Genome\n\n");
        for (ArchitecturalGene gene : model.getGenes()) {
            sb.append("## Gene: ").append(gene.getName() != null ? gene.getName() : gene.getId()).append("\n");
            sb.append("- **Purpose**: ").append(gene.getPurpose()).append("\n");
            sb.append("- **Rationale**: ").append(gene.getRationale()).append("\n");
            sb.append("- **Activation**: ").append(gene.getActivationConditions()).append("\n");
            sb.append("- **Dependencies**: ").append(String.join(", ", gene.getDependencies())).append("\n");
            sb.append("- **Required Artifacts**:\n");
            for (String art : gene.getRequiredArtifacts()) sb.append("  - ").append(art).append("\n");
            sb.append("- **Example Files**:\n");
            for (String ex : gene.getExampleFiles()) sb.append("  - ").append(ex).append("\n");
            sb.append("\n");
        }
        addStringToZip(zos, "genome/genome-atlas.md", sb.toString());
    }

    private void addImplementationSection(ZipOutputStream zos, TargetRealityModel model, String goal, File projectRoot) throws IOException {
        StringBuilder sb = new StringBuilder("# Implementation Package\n\n");
        sb.append("## Goal\n").append(goal != null ? goal : "N/A").append("\n\n");
        sb.append("## Implementation Frontier\n");
        for (String f : model.getImplementationFrontierFiles()) sb.append("- ").append(f).append("\n");

        addStringToZip(zos, "implementation/implementation-package.md", sb.toString());
        addStringToZip(zos, "implementation/implementation-frontier.txt", String.join("\n", model.getImplementationFrontierFiles()));
        addStringToZip(zos, "implementation/architectural-authority.txt", String.join("\n", model.getArchitecturalAuthorityFiles()));

        StringBuilder flowSb = new StringBuilder("# Execution Flows\n\n");
        for (String flow : model.getExecutionFlows()) flowSb.append("- ").append(flow).append("\n");
        addStringToZip(zos, "implementation/execution-flow.md", flowSb.toString());

        StringBuilder impactSb = new StringBuilder("# Impact Paths\n\n");
        for (String path : model.getImpactPaths()) impactSb.append("- ").append(path).append("\n");
        addStringToZip(zos, "implementation/impact-paths.md", impactSb.toString());

        StringBuilder gapsSb = new StringBuilder("# Knowledge Gaps\n\n");
        for (KnowledgeGap gap : model.getKnowledgeGaps()) {
            gapsSb.append("### ").append(gap.getDescription()).append("\n");
            gapsSb.append("- **Significance**: ").append(gap.getSignificance()).append("\n");
            gapsSb.append("- **Type**: ").append(gap.getType()).append("\n\n");
        }
        addStringToZip(zos, "implementation/knowledge-gaps.md", gapsSb.toString());

        for (String path : model.getImplementationFrontierFiles()) {
            File src = new File(projectRoot, path);
            if (src.exists() && src.isFile()) {
                addFileToZip(zos, "implementation/files/" + path, src);
            }
        }
    }

    // PROJECTION ENGINE METHODS

    public File generateArchitectureProjection(eu.kalafatic.evolution.controller.mediation.model.TargetRealityModel model, File projectRoot) throws IOException {
        String folderName = "architecture_projection_" + System.currentTimeMillis();
        File exportDir = new File(projectRoot, folderName);
        exportDir.mkdirs();

        StringBuilder sb = new StringBuilder("# Architectural View\n\n");
        sb.append("## Summary\n").append(model.getArchitectureSummary()).append("\n\n");
        sb.append("## Subsystems\n");
        for (var sub : model.getSubsystems()) {
            sb.append("- ").append(sub.getName()).append(": ").append(sub.getPurpose()).append("\n");
        }
        sb.append("\n## Hotspots\n");
        for (var h : model.getHotspots()) {
            sb.append("- ").append(h.getName()).append(" (Significance: ").append(h.getSignificance()).append(")\n");
        }

        writeStringToFile(new File(exportDir, "architecture.md"), sb.toString());
        return exportDir;
    }

    public File generateImplementationProjection(eu.kalafatic.evolution.controller.mediation.model.TargetRealityModel model, String goal, File projectRoot) throws IOException {
        String folderName = "implementation_package_" + System.currentTimeMillis();
        File exportDir = new File(projectRoot, folderName);
        exportDir.mkdirs();

        StringBuilder sb = new StringBuilder("# Implementation Context\n\n");
        sb.append("## Goal\n").append(goal).append("\n\n");
        sb.append("## Reality Model Summary\n").append(model.getArchitectureSummary()).append("\n\n");

        sb.append("## Execution Flows\n");
        for (String flow : model.getExecutionFlows()) sb.append("- ").append(flow).append("\n");

        sb.append("\n## Knowledge Gaps\n");
        for (var gap : model.getKnowledgeGaps()) sb.append("- ").append(gap.toString()).append("\n");

        writeStringToFile(new File(exportDir, "context.md"), sb.toString());

        File filesDir = new File(exportDir, "files");
        filesDir.mkdirs();
        for (String path : model.getSelectedFiles()) {
            File src = new File(projectRoot, path);
            if (src.exists()) copyFileToExport(src, filesDir, path);
        }

        return exportDir;
    }

    private void addLogSection(ZipOutputStream zos, TargetRealityModel model) throws IOException {
        String sessionId = model.getMetadata().get("sessionId") != null ? model.getMetadata().get("sessionId").toString() : null;
        if (sessionId == null) return;

        eu.kalafatic.evolution.controller.orchestration.SessionContainer session = eu.kalafatic.evolution.controller.orchestration.SessionManager.getInstance().getSession(sessionId);
        if (session == null || session.getObservabilityManager() == null) return;

        EvolutionaryObservabilityManager obs = session.getObservabilityManager();

        addStringToZip(zos, "observability/event_projection.json", serializeToJson(obs.getTimeline()));
        addStringToZip(zos, "observability/dependency_graph.json", serializeToJson(obs.getCausalChains()));
        addStringToZip(zos, "observability/failure_space.json", serializeToJson(obs.getEventLog().stream()
            .filter(e -> "CRITICAL".equals(e.getSeverity()))
            .collect(java.util.stream.Collectors.toList())));

        addStringToZip(zos, "observability/summaries/overview.json", serializeToJson(obs.getSummaries()));

        // LINEAGE PRESERVATION: Capture rejected philosophies and failure memory (Milestone Requirement)
        if (obs.getMemoryService() != null) {
            var memory = obs.getMemoryService();
            StringBuilder lineageLossSb = new StringBuilder("# Evolutionary Lineage Preservation\n\n");
            lineageLossSb.append("## Rejected Engineering Philosophies (Avoid Rediscovery)\n");
            memory.getRecords().stream()
                .filter(r -> !"ACTIVE".equals(r.getActivationState()) && !"KEPT".equals(r.getActivationState()))
                .map(r -> "- **" + r.getStrategy() + "**: " + r.getSemanticAnchor())
                .distinct()
                .forEach(line -> lineageLossSb.append(line).append("\n"));

            lineageLossSb.append("\n## Engineering Dead-ends (Failure Memory)\n");
            memory.getFailureMemory().getFingerprints().forEach((fp, count) -> {
                lineageLossSb.append("- **Failure Pattern**: `").append(fp).append("` (Occurrences: ").append(count).append(")\n");
            });

            addStringToZip(zos, "observability/lineage_preservation.md", lineageLossSb.toString());
        }
    }

    public File generateGenomeProjection(eu.kalafatic.evolution.controller.mediation.model.TargetRealityModel model, File projectRoot) throws IOException {
        String folderName = "genome_package_" + System.currentTimeMillis();
        File exportDir = new File(projectRoot, folderName);
        exportDir.mkdirs();

        StringBuilder sb = new StringBuilder("# Portable Genome Patterns\n\n");
        for (var gene : model.getGenes()) {
            sb.append("### ").append(gene.getPattern()).append("\n");
            sb.append("- **Purpose**: ").append(gene.getPurpose()).append("\n");
            sb.append("- **Rationale**: ").append(gene.getRationale()).append("\n");
            sb.append("- **Required Artifacts**:\n");
            for (String art : gene.getRequiredArtifacts()) sb.append("  - ").append(art).append("\n");
            sb.append("\n");
        }

        writeStringToFile(new File(exportDir, "genome.md"), sb.toString());
        return exportDir;
    }
}
