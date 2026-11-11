package eu.kalafatic.evolution.controller.workflow;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import eu.kalafatic.evolution.controller.mediation.analysis.ContextCurator;
import eu.kalafatic.evolution.controller.mediation.analysis.FileManifest;
import eu.kalafatic.evolution.controller.mediation.analysis.MediatedSpecification;
import eu.kalafatic.evolution.controller.mediation.analysis.MediatedSpecificationEngine;
import eu.kalafatic.evolution.controller.mediation.model.ArchitecturalFact;
import eu.kalafatic.evolution.controller.mediation.model.ArchitecturalGene;
import eu.kalafatic.evolution.controller.mediation.model.Hotspot;
import eu.kalafatic.evolution.controller.mediation.model.KnowledgeGap;
import eu.kalafatic.evolution.controller.mediation.model.Subsystem;
import eu.kalafatic.evolution.controller.mediation.model.TargetRealityModel;
import eu.kalafatic.evolution.controller.mediation.model.TargetSnapshot;
import eu.kalafatic.evolution.controller.orchestration.design.ComponentRecord;
import eu.kalafatic.evolution.controller.orchestration.design.DesignModel;
import eu.kalafatic.evolution.controller.orchestration.design.DesignRenderer;
import eu.kalafatic.evolution.controller.orchestration.design.RelationshipRecord;

/**
 * Handles packaging of mediated context, improved engineering specifications,
 * and instructions into a standalone ZIP archive for external AI coding platforms.
 *
 * Strict Invariant: Never alters or creates temporary files inside the target project.
 */
public class MediatedExportManager {

    public enum ExportProfile {
        FULL,
        ARCHITECTURE,
        IMPLEMENTATION,
        GENOME
    }

    /**
     * Creates a complete, self-explanatory Mediated Package archive (ZIP).
     */
    public File createStructuredMediatedPackage(TargetRealityModel model, TargetSnapshot snapshot,
            MediatedSpecification specification, List<String> selectedPaths, File projectRoot,
            String outputPath, String sessionId) throws IOException {

        File targetExportDir = resolveExternalExportDirectory(projectRoot, outputPath);
        String fileName = "mediated_package_" + (sessionId != null ? sessionId : "session") + "_" + System.currentTimeMillis() + ".zip";
        File zipFile = new File(targetExportDir, fileName);

        ContextCurator curator = new ContextCurator();
        String query = specification != null ? specification.getOriginalRequest() : (model != null ? model.getPurpose() : "");
        FileManifest manifest = curator.buildManifest(selectedPaths, snapshot, model, projectRoot, query);

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {

            // 1. Root README.md
            String rootReadme = buildRootReadme(specification, model, manifest);
            addStringToZip(zos, "README.md", rootReadme);

            // 2. Root PROMPT.md (Master prompt instructing external AI)
            String masterPrompt = buildExternalAiPrompt(specification, model, manifest);
            addStringToZip(zos, "PROMPT.md", masterPrompt);
            // Backward-compatible alias
            addStringToZip(zos, "prompt.md", masterPrompt);

            // 3. PROJECT_CONTEXT/
            addProjectContextSection(zos, model, manifest, projectRoot);

            // 4. REQUIREMENTS/
            addRequirementsSection(zos, specification);

            // 5. DOCUMENTATION/
            addDocumentationSection(zos, selectedPaths, projectRoot);

            // 6. Observability & Lineage
            if (model != null) {
                addLogSection(zos, model);
            }

            // 7. Backward Compatibility: Architecture & Implementation root aliases
            addBackwardCompatibilityAliases(zos, model, selectedPaths, projectRoot, specification);
        }

        return zipFile;
    }

    /**
     * Resolves export directory outside of target directory to preserve read-only target guarantee.
     */
    public File resolveExternalExportDirectory(File projectRoot, String outputPath) {
        if (outputPath != null && !outputPath.trim().isEmpty()) {
            File outDir = new File(outputPath.trim());
            if (!outDir.exists()) {
                outDir.mkdirs();
            }
            return outDir;
        }

        // Fallback to default external directory outside the target project
        File userHome = new File(System.getProperty("user.home", "."));
        File evoExportDir = new File(userHome, ".evo/exports/mediated");
        if (!evoExportDir.exists()) {
            evoExportDir.mkdirs();
        }
        return evoExportDir;
    }

    private String buildRootReadme(MediatedSpecification spec, TargetRealityModel model, FileManifest manifest) {
        StringBuilder sb = new StringBuilder();
        sb.append("# EVO Mediated Development Package\n\n");
        sb.append("This package was prepared by the local EVO Darwin Mediated Engine for execution by an external AI coding assistant (ChatGPT, Claude, Gemini, etc.).\n\n");

        sb.append("## How to Use This Package\n");
        sb.append("1. **Do not send your entire repository to cloud AI.** This package already contains the high-signal context and technical reasoning needed.\n");
        sb.append("2. Open `PROMPT.md` and copy its contents, or attach this ZIP directly to your external AI assistant.\n");
        sb.append("3. The external AI will follow the structured instructions in `PROMPT.md`, reference the exact files in `PROJECT_CONTEXT/RELEVANT_FILES/`, and satisfy the requirements in `REQUIREMENTS/`.\n");
        sb.append("4. Review the AI's proposed code modifications and manually apply them to your repository.\n\n");

        sb.append("## Package Directory Layout\n");
        sb.append("```\n");
        sb.append("MEDIATED_PACKAGE/\n");
        sb.append("|-- README.md                     # Package documentation and workflow instructions\n");
        sb.append("|-- PROMPT.md                     # Comprehensive external AI master prompt\n");
        sb.append("|-- PROJECT_CONTEXT/\n");
        sb.append("|   |-- ARCHITECTURE.md           # Architecture overview and discovered patterns\n");
        sb.append("|   |-- PROJECT_OVERVIEW.md       # Target domain, technologies, and statistics\n");
        sb.append("|   |-- DEPENDENCIES.md           # Dependency interactions and coordinates\n");
        sb.append("|   |-- FILE_MANIFEST.json        # Manifest detailing each included file and rationale\n");
        sb.append("|   +-- RELEVANT_FILES/           # Selected files preserving original relative paths\n");
        sb.append("|-- REQUIREMENTS/\n");
        sb.append("|   |-- ORIGINAL_REQUEST.md       # Raw user request as provided\n");
        sb.append("|   |-- IMPROVED_SPECIFICATION.md # Darwin-evolved engineering specification\n");
        sb.append("|   +-- ACCEPTANCE_CRITERIA.md    # Concrete verification criteria\n");
        sb.append("+-- DOCUMENTATION/                # Selected project documentation and guides\n");
        sb.append("```\n\n");

        sb.append("## Summary of Request\n");
        sb.append("- **Original Goal:** ").append(spec != null ? spec.getOriginalRequest() : "N/A").append("\n");
        sb.append("- **Primary Objective:** ").append(spec != null ? spec.getPrimaryObjective() : "N/A").append("\n");
        sb.append("- **Target Domain:** ").append(model != null && model.getDomain() != null ? model.getDomain() : "General Software").append("\n");
        sb.append("- **Included Relevant Files:** ").append(manifest.getTotalFiles()).append("\n\n");

        return sb.toString();
    }

    private String buildExternalAiPrompt(MediatedSpecification spec, TargetRealityModel model, FileManifest manifest) {
        StringBuilder sb = new StringBuilder();
        sb.append("# AI CODING ASSISTANT TASK INSTRUCTION\n\n");

        sb.append("## ROLE & OBJECTIVE\n");
        sb.append("You are acting as an expert senior software engineer. Your task is to implement the changes specified below on the target project.\n");
        sb.append("You have been supplied with an architecturally grounded context package prepared by local repository analysis.\n\n");

        sb.append("## INSTRUCTIONS FOR EXTERNAL AI\n");
        sb.append("1. **Understand Project Context**: Read `PROJECT_CONTEXT/ARCHITECTURE.md`, `PROJECT_OVERVIEW.md`, and the files in `PROJECT_CONTEXT/RELEVANT_FILES/`.\n");
        sb.append("2. **Follow Existing Architecture**: Adhere strictly to existing coding standards, design patterns, and package conventions. Avoid unnecessary rewrites or gratuitous refactorings.\n");
        sb.append("3. **Satisfy Requirements**: Implement the technical specification detailed in `REQUIREMENTS/IMPROVED_SPECIFICATION.md`.\n");
        sb.append("4. **Respect Constraints**: Adhere to the technical constraints and preserve backward compatibility.\n");
        sb.append("5. **Verify Acceptance Criteria**: Ensure all acceptance criteria in `REQUIREMENTS/ACCEPTANCE_CRITERIA.md` are satisfied.\n");
        sb.append("6. **Report Modifications**: Clearly state which files were modified, created, or deleted, providing complete code snippets with clear instructions for application.\n\n");

        sb.append("## ENGINEERING SPECIFICATION\n");
        if (spec != null) {
            sb.append(spec.toMarkdown()).append("\n\n");
        } else {
            sb.append("> Implement requested changes based on provided project context.\n\n");
        }

        sb.append("## INCLUDED CONTEXT FILES (FROM PROJECT_CONTEXT/RELEVANT_FILES/)\n");
        sb.append(manifest.toMarkdown()).append("\n\n");

        if (model != null && model.getArchitectureSummary() != null && !model.getArchitectureSummary().isEmpty()) {
            sb.append("## TARGET ARCHITECTURE OVERVIEW\n");
            sb.append(model.getArchitectureSummary()).append("\n\n");
        }

        return sb.toString();
    }

    private void addProjectContextSection(ZipOutputStream zos, TargetRealityModel model, FileManifest manifest, File projectRoot) throws IOException {
        // 1. ARCHITECTURE.md
        StringBuilder archSb = new StringBuilder();
        archSb.append("# Project Architecture\n\n");
        if (model != null) {
            archSb.append("Domain: ").append(model.getDomain() != null ? model.getDomain() : "General").append("\n");
            archSb.append("Purpose: ").append(model.getPurpose() != null ? model.getPurpose() : "N/A").append("\n\n");
            archSb.append("## Architecture Summary\n").append(model.getArchitectureSummary() != null ? model.getArchitectureSummary() : "N/A").append("\n\n");

            if (!model.getSubsystems().isEmpty()) {
                archSb.append("## Discovered Subsystems\n");
                for (Subsystem sub : model.getSubsystems()) {
                    archSb.append("### Subsystem: ").append(sub.getName()).append("\n");
                    archSb.append("- Purpose: ").append(sub.getPurpose()).append("\n");
                    if (sub.getDescription() != null) archSb.append("- Description: ").append(sub.getDescription()).append("\n");
                    if (!sub.getBoundaries().isEmpty()) archSb.append("- Boundaries: ").append(String.join(", ", sub.getBoundaries())).append("\n");
                    if (!sub.getCriticalFiles().isEmpty()) archSb.append("- Critical Files: ").append(String.join(", ", sub.getCriticalFiles())).append("\n");
                    archSb.append("\n");
                }
            }

            if (!model.getHotspots().isEmpty()) {
                archSb.append("## Identified Hotspots\n");
                for (Hotspot h : model.getHotspots()) {
                    archSb.append("- **").append(h.getName()).append("** (").append(h.getFile()).append("): ").append(h.getDescription()).append("\n");
                }
                archSb.append("\n");
            }

            if (!model.getArchitecturalFacts().isEmpty()) {
                archSb.append("## Architectural Facts\n");
                for (ArchitecturalFact f : model.getArchitecturalFacts()) {
                    archSb.append("- ").append(f.toString()).append("\n");
                }
                archSb.append("\n");
            }
        }
        addStringToZip(zos, "PROJECT_CONTEXT/ARCHITECTURE.md", archSb.toString());

        // 2. PROJECT_OVERVIEW.md
        StringBuilder ovSb = new StringBuilder();
        ovSb.append("# Project Overview\n\n");
        if (model != null) {
            ovSb.append("- **Domain:** ").append(model.getDomain() != null ? model.getDomain() : "N/A").append("\n");
            ovSb.append("- **Files Scanned:** ").append(model.getFilesScanned()).append("\n");
            ovSb.append("- **Completeness Index:** ").append(String.format("%.2f", model.getRealityCompleteness())).append("\n\n");
        }
        ovSb.append(manifest.toMarkdown());
        addStringToZip(zos, "PROJECT_CONTEXT/PROJECT_OVERVIEW.md", ovSb.toString());

        // 3. DEPENDENCIES.md
        StringBuilder depSb = new StringBuilder();
        depSb.append("# Dependencies & Integrations\n\n");
        if (model != null && model.getMetadata().containsKey("dependencies")) {
            depSb.append(model.getMetadata().get("dependencies")).append("\n\n");
        } else {
            depSb.append("Refer to project build descriptors (pom.xml, build.gradle, package.json, requirements.txt) in RELEVANT_FILES/ for explicit dependencies.\n");
        }
        addStringToZip(zos, "PROJECT_CONTEXT/DEPENDENCIES.md", depSb.toString());

        // 4. FILE_MANIFEST.json
        addStringToZip(zos, "PROJECT_CONTEXT/FILE_MANIFEST.json", manifest.toJson());

        // 5. RELEVANT_FILES/
        for (var entry : manifest.getFiles()) {
            String relPath = entry.getRelativePath();
            File srcFile = new File(projectRoot, relPath);
            if (!srcFile.exists() || !srcFile.isFile()) {
                File absFile = new File(relPath);
                if (absFile.exists() && absFile.isFile()) {
                    srcFile = absFile;
                }
            }

            if (srcFile.exists() && srcFile.isFile()) {
                // Exclude huge binaries > 5MB
                if (srcFile.length() <= 5000000) {
                    addFileToZip(zos, "PROJECT_CONTEXT/RELEVANT_FILES/" + relPath, srcFile);
                }
            }
        }
    }

    private void addRequirementsSection(ZipOutputStream zos, MediatedSpecification spec) throws IOException {
        String originalReq = spec != null ? spec.getOriginalRequest() : "No raw request recorded.";
        addStringToZip(zos, "REQUIREMENTS/ORIGINAL_REQUEST.md", "# Original User Request\n\n> " + originalReq + "\n");

        String improvedSpec = spec != null ? spec.toMarkdown() : "# Improved Specification\n\nN/A\n";
        addStringToZip(zos, "REQUIREMENTS/IMPROVED_SPECIFICATION.md", improvedSpec);

        StringBuilder acSb = new StringBuilder();
        acSb.append("# Acceptance & Verification Criteria\n\n");
        if (spec != null && !spec.getAcceptanceCriteria().isEmpty()) {
            for (String ac : spec.getAcceptanceCriteria()) {
                acSb.append("- [ ] ").append(ac).append("\n");
            }
        } else {
            acSb.append("- [ ] Verify build and tests pass without regression.\n");
        }
        addStringToZip(zos, "REQUIREMENTS/ACCEPTANCE_CRITERIA.md", acSb.toString());
    }

    private void addDocumentationSection(ZipOutputStream zos, List<String> selectedPaths, File projectRoot) throws IOException {
        boolean docFound = false;
        for (String path : selectedPaths) {
            String lower = path.toLowerCase();
            if (lower.endsWith("readme.md") || lower.contains("/docs/") || lower.startsWith("docs/") || lower.endsWith(".adoc")) {
                File docFile = new File(projectRoot, path);
                if (docFile.exists() && docFile.isFile() && docFile.length() <= 1000000) {
                    addFileToZip(zos, "DOCUMENTATION/" + path.replace('\\', '/'), docFile);
                    docFound = true;
                }
            }
        }

        if (!docFound) {
            File rootReadme = new File(projectRoot, "README.md");
            if (rootReadme.exists() && rootReadme.isFile()) {
                addFileToZip(zos, "DOCUMENTATION/README.md", rootReadme);
            } else {
                addStringToZip(zos, "DOCUMENTATION/PROJECT_DOCS.md", "# Project Documentation\n\nNo dedicated project documentation was selected.\n");
            }
        }
    }

    private void addBackwardCompatibilityAliases(ZipOutputStream zos, TargetRealityModel model,
            List<String> selectedPaths, File projectRoot, MediatedSpecification spec) throws IOException {

        // Legacy affected-files/
        for (String path : selectedPaths) {
            String norm = path.replace('\\', '/');
            File f = new File(projectRoot, norm);
            if (!f.exists()) f = new File(norm);
            if (f.exists() && f.isFile() && f.length() <= 5000000) {
                addFileToZip(zos, "affected-files/" + norm, f);
            }
        }

        if (model != null) {
            addStringToZip(zos, "reality-model.json", serializeToJson(model));
            if (!model.getSubsystems().isEmpty()) {
                addStringToZip(zos, "subsystems.json", serializeListToJson(model.getSubsystems()));
            }
            if (!model.getArchitecturalFacts().isEmpty()) {
                addStringToZip(zos, "architectural-facts.json", serializeListToJson(model.getArchitecturalFacts()));
            }
            if (model.getArchitectureSummary() != null) {
                addStringToZip(zos, "architecture.md", model.getArchitectureSummary());
            }
        }
    }

    // =========================================================================
    // LEGACY & UNIFIED EXPORT ADAPTER METHODS (PRESERVED FOR BACKWARD COMPAT)
    // =========================================================================

    public File createUnifiedExport(TargetRealityModel model, ExportProfile profile, String goal, File projectRoot, String outputPath, String sessionId) throws IOException {
        MediatedSpecificationEngine specEngine = new MediatedSpecificationEngine();
        MediatedSpecification spec = specEngine.buildDeterministicSpecification(goal, null, model);
        List<String> selected = model != null ? model.getSelectedFiles() : new ArrayList<>();
        return createStructuredMediatedPackage(model, null, spec, selected, projectRoot, outputPath, sessionId);
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
        TargetRealityModel model = new TargetRealityModel();
        if (architectureSummary != null) model.setArchitectureSummary(architectureSummary);
        if (prompt != null) model.setPurpose(prompt);
        if (selectedPaths != null) model.getSelectedFiles().addAll(selectedPaths);

        MediatedSpecificationEngine specEngine = new MediatedSpecificationEngine();
        MediatedSpecification spec = specEngine.buildDeterministicSpecification(prompt, null, model);
        return createStructuredMediatedPackage(model, null, spec, selectedPaths != null ? selectedPaths : new ArrayList<>(), projectRoot, outputPath, sessionId);
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

        // Lineage Preservation
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
}
