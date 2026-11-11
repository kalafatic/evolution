package eu.kalafatic.evolution.controller.mediation.analysis;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import eu.kalafatic.evolution.controller.mediation.analysis.ManifestFileEntry.FileCategory;
import eu.kalafatic.evolution.controller.mediation.model.KnowledgeGap;
import eu.kalafatic.evolution.controller.mediation.model.SemanticEdge;
import eu.kalafatic.evolution.controller.mediation.model.SemanticNode;
import eu.kalafatic.evolution.controller.mediation.model.Subsystem;
import eu.kalafatic.evolution.controller.mediation.model.TargetDescriptor;
import eu.kalafatic.evolution.controller.mediation.model.TargetRealityModel;
import eu.kalafatic.evolution.controller.mediation.model.TargetSnapshot;

/**
 * Selects high-value context while avoiding token floods and generates
 * structured file manifests explaining why each file was included.
 */
public class ContextCurator {

    public List<String> selectContext(TargetSnapshot snapshot, String query, int maxFiles, TargetRealityModel realityModel) {
        return selectContextWithBudget(snapshot, query, DEFAULT_TOKEN_BUDGET, realityModel, maxFiles);
    }

    public List<String> curate(TargetDescriptor target) {
        // REFACTOR: Generic curation based on semantic density and abstract significance.
        // No hardcoded technology or role weighting.
        return target.getFiles().stream()
            .filter(f -> f.getTags().contains("Executory") || f.getTags().contains("Annotated"))
            .map(f -> f.getPath())
            .distinct()
            .collect(Collectors.toList());
    }

    private static final int DEFAULT_TOKEN_BUDGET = 32000; // ~128k chars

    public List<String> selectContext(TargetSnapshot snapshot, String query, int maxFiles) {
        return selectContextWithBudget(snapshot, query, DEFAULT_TOKEN_BUDGET, null, maxFiles);
    }

    public List<String> selectContextWithBudget(TargetSnapshot snapshot, String query, int tokenBudget) {
        return selectContextWithBudget(snapshot, query, tokenBudget, null, 16);
    }

    public List<String> selectContextWithBudget(TargetSnapshot snapshot, String query, int tokenBudget, TargetRealityModel realityModel) {
        return selectContextWithBudget(snapshot, query, tokenBudget, realityModel, 16);
    }

    public List<String> selectContextWithBudget(TargetSnapshot snapshot, String query, int tokenBudget, TargetRealityModel realityModel, int maxFiles) {
        if (snapshot == null || snapshot.getNodes().isEmpty()) return new ArrayList<>();

        Map<String, Double> scores = new HashMap<>();
        String lowerQuery = query.toLowerCase();
        String[] keywords = lowerQuery.split("\\s+");

        // 1. Calculate Graph Centrality (Abstract Importance)
        Map<String, Integer> inDegree = new HashMap<>();
        Map<String, Integer> outDegree = new HashMap<>();
        for (SemanticEdge edge : snapshot.getEdges()) {
            outDegree.merge(edge.getSourceId(), 1, Integer::sum);
            inDegree.merge(edge.getTargetId(), 1, Integer::sum);
        }

        for (SemanticNode node : snapshot.getNodes().values()) {
            double score = 0.0;
            String nid = node.getId();
            String path = node.getPath();

            // A. Architectural Authority & Influence (Primary Signal)
            double authority = node.getArchitecturalAuthority();
            double influence = node.getEvolutionaryInfluenceScore();

            if (realityModel != null) {
                // Subsystem & Boundary Elevation
                Optional<Subsystem> sub = realityModel.getSubsystems().stream().filter(s -> s.getCriticalFiles().contains(path)).findFirst();
                if (sub.isPresent()) {
                    authority += 30.0;
                    if (sub.get().getBoundaries().contains(path)) authority += 20.0;
                }

                // Fact Evidence Elevation
                long factCount = realityModel.getArchitecturalFacts().stream().filter(f -> f.getEvidence().contains(path)).count();
                authority += (factCount * 15.0);

                // Unknownness / Uncertainty Elevation (Knowledge Gap DOMINANCE)
                Optional<KnowledgeGap> gap = realityModel.getKnowledgeGaps().stream()
                    .filter(g -> g.getRelatedArtifacts().contains(path))
                    .findFirst();
                if (gap.isPresent()) {
                    score += 500.0; // Dominant boost for unknown regions
                    score += (gap.get().getSignificance() * 200.0);
                }
            }

            score += (authority * 2.0);
            score += (influence * 50.0); // Influence is a high-signal multiplier

            // B. Graph Centrality Signal (Hotspot Indicator)
            int totalDegree = inDegree.getOrDefault(nid, 0) + outDegree.getOrDefault(nid, 0);
            score += (totalDegree * 5.0);

            // C. Heuristic Architectural Markers
            if (node.getSummary() != null) {
                String summary = node.getSummary().toLowerCase();
                if (summary.contains("coordinate") || summary.contains("orchestrate")) score += 20.0;
                if (summary.contains("control") || summary.contains("manager")) score += 15.0;
                if (summary.contains("kernel") || summary.contains("authority")) score += 25.0;
                if (summary.contains("subsystem") || summary.contains("boundary")) score += 10.0;
            }

            // D. Semantic Density Signal
            // Higher density of structures (methods, classes) and attributes indicates higher info value.
            int density = node.getStructures().size() + node.getAttributes().size() + node.getDependencies().size();
            score += (density * 2.0);

            // E. Abstract Relevance Signal
            if (node.getSummary() != null) {
                String summary = node.getSummary().toLowerCase();
                for (String word : keywords) {
                    if (word.length() < 3) continue;
                    if (summary.contains(word)) score += 5.0;
                }
            }
            for (String struct : node.getStructures()) {
                String lowerStruct = struct.toLowerCase();
                for (String word : keywords) {
                    if (word.length() < 3) continue;
                    if (lowerStruct.contains(word)) score += 3.0;
                }
            }

            // F. Abstract Significance Evidence
            if (node.getTags().contains("Executory")) score += 15.0; // Entry points are major hotspots
            if (node.getTags().contains("Annotated")) score += 10.0; // Components with metadata are significant

            // G. Build & Project Descriptors (Strongly prioritize for grounding)
            if (path.endsWith("pom.xml") || path.endsWith("build.gradle") || path.endsWith("package.json") || path.endsWith("sloeber.ino")
                    || path.endsWith("requirements.txt") || path.endsWith("CMakeLists.txt") || path.endsWith("Makefile")) {
                score += 300.0;
            }

            // H. Existing Documentation & Architecture Guidance
            if (path.endsWith("README.md") || path.endsWith("architecture.md") || path.startsWith("docs/") || path.contains("/docs/")) {
                score += 40.0;
            }

            if (score > 0) {
                scores.put(nid, score);
            }
        }

        // 2. Coverage-Driven Selection logic
        List<String> selected = new ArrayList<>();
        List<Map.Entry<String, Double>> sortedCandidates = scores.entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .collect(Collectors.toList());

        Set<String> selectedClusters = new HashSet<>();
        long currentTokens = 0;

        // Selection Pass A: Critical Coverage (Authority + Gaps)
        for (Map.Entry<String, Double> entry : sortedCandidates) {
            String nodeId = entry.getKey();
            SemanticNode node = snapshot.getNodes().get(nodeId);

            long estimatedTokens = estimateTokens(node);
            if (currentTokens + estimatedTokens > tokenBudget) continue;

            // Enforce maxFiles
            if (selected.size() >= maxFiles) break;

            // Stop only when adequate coverage is reached or budget is exhausted.
            if (isCoverageAdequate(selected, realityModel, currentTokens, tokenBudget, maxFiles)) break;

            String cluster = deriveCluster(node);
            if (selectedClusters.contains(cluster) && entry.getValue() < 100.0) continue;

            selected.add(node.getPath());
            selectedClusters.add(cluster);
            currentTokens += estimatedTokens;
        }

        // Selection Pass B: Subsystem Boundary Enforcement
        if (realityModel != null) {
            for (Subsystem sub : realityModel.getSubsystems()) {
                boolean covered = sub.getBoundaries().stream().anyMatch(selected::contains);
                if (!covered && !sub.getBoundaries().isEmpty()) {
                    String boundaryFile = sub.getBoundaries().get(0);
                    SemanticNode n = snapshot.getNodes().get(boundaryFile);
                    if (n != null) {
                        long tokens = estimateTokens(n);
                        if (currentTokens + tokens <= tokenBudget) {
                            selected.add(boundaryFile);
                            currentTokens += tokens;
                        }
                    }
                }
            }
        }

        // Final safety fallback: ensure at least 4 files if available and budget permits
        if (selected.size() < 4 && !snapshot.getNodes().isEmpty()) {
            List<SemanticNode> fallbacks = snapshot.getNodes().values().stream()
                .filter(n -> !selected.contains(n.getPath()))
                .sorted((n1, n2) -> {
                    int d1 = inDegree.getOrDefault(n1.getId(), 0) + outDegree.getOrDefault(n1.getId(), 0);
                    int d2 = inDegree.getOrDefault(n2.getId(), 0) + outDegree.getOrDefault(n2.getId(), 0);
                    return Integer.compare(d2, d1);
                })
                .collect(Collectors.toList());

            for (SemanticNode n : fallbacks) {
                if (selected.size() >= 4) break;
                long tokens = estimateTokens(n);
                if (currentTokens + tokens <= tokenBudget) {
                    selected.add(n.getPath());
                    currentTokens += tokens;
                }
            }
        }

        return selected;
    }

    /**
     * Builds a comprehensive FileManifest documenting all selected files with
     * their categories, architectural roles, and selection rationales.
     */
    public FileManifest buildManifest(List<String> selectedPaths, TargetSnapshot snapshot,
            TargetRealityModel realityModel, File projectRoot, String query) {

        String rootPath = projectRoot != null ? projectRoot.getAbsolutePath() : (snapshot != null ? snapshot.getRootPath() : ".");
        FileManifest manifest = new FileManifest(rootPath);
        String lowerQuery = query != null ? query.toLowerCase(Locale.ROOT) : "";

        for (String path : selectedPaths) {
            String normPath = path.replace('\\', '/');
            SemanticNode node = snapshot != null ? snapshot.getNodes().get(normPath) : null;
            long size = 0;
            if (projectRoot != null) {
                File f = new File(projectRoot, normPath);
                if (f.exists()) size = f.length();
            }

            FileCategory category = classifyFileCategory(normPath, node);
            String role = (node != null && node.getAttributes().containsKey("role"))
                    ? node.getAttributes().get("role")
                    : inferArchitecturalRole(normPath, category);
            String rationale = buildSelectionRationale(normPath, category, node, realityModel, lowerQuery);

            ManifestFileEntry entry = new ManifestFileEntry(normPath, category, rationale, role, size);
            manifest.addEntry(entry);
        }

        return manifest;
    }

    private FileCategory classifyFileCategory(String path, SemanticNode node) {
        String lower = path.toLowerCase(Locale.ROOT);
        if (lower.endsWith("pom.xml") || lower.endsWith("build.gradle") || lower.endsWith("package.json")
                || lower.endsWith("cmakelists.txt") || lower.endsWith("makefile") || lower.endsWith("sloeber.ino")) {
            return FileCategory.BUILD;
        }
        if (lower.endsWith(".json") || lower.endsWith(".yaml") || lower.endsWith(".yml")
                || lower.endsWith(".xml") || lower.endsWith(".properties") || lower.endsWith(".ini")
                || lower.endsWith(".toml")) {
            return FileCategory.CONFIG;
        }
        if (lower.endsWith("readme.md") || lower.endsWith(".md") || lower.endsWith(".adoc")
                || lower.endsWith(".txt") || lower.contains("/docs/") || lower.startsWith("docs/")) {
            return FileCategory.DOCUMENTATION;
        }
        if (lower.contains("test") || lower.contains("spec") || lower.endsWith("test.java")
                || lower.endsWith("tests.java")) {
            return FileCategory.TEST;
        }
        if (lower.endsWith(".evo")) {
            return FileCategory.MODEL;
        }
        if (lower.endsWith(".evodata") || lower.endsWith(".parquet")) {
            return FileCategory.DATASET;
        }
        if (lower.endsWith(".java") || lower.endsWith(".py") || lower.endsWith(".ts")
                || lower.endsWith(".js") || lower.endsWith(".c") || lower.endsWith(".cpp")
                || lower.endsWith(".h") || lower.endsWith(".go") || lower.endsWith(".rs")) {
            if (node != null && node.getStructures().stream().anyMatch(s -> s.toLowerCase().contains("interface "))) {
                return FileCategory.INTERFACE;
            }
            return FileCategory.SOURCE;
        }
        return FileCategory.OTHER;
    }

    private String inferArchitecturalRole(String path, FileCategory category) {
        switch (category) {
            case BUILD:
                return "Project build descriptor and dependency definition";
            case CONFIG:
                return "System configuration and runtime properties";
            case DOCUMENTATION:
                return "Architectural and developer documentation";
            case TEST:
                return "Verification suite and behavioral regression testing";
            case INTERFACE:
                return "Architectural contract / interface abstraction";
            case MODEL:
                return "Native model weight artifact";
            case DATASET:
                return "Training dataset container";
            case SOURCE:
            default:
                if (path.contains("Controller") || path.contains("Manager") || path.contains("Engine") || path.contains("Service")) {
                    return "Core coordination and business logic component";
                }
                return "Primary application source implementation";
        }
    }

    private String buildSelectionRationale(String path, FileCategory category, SemanticNode node,
            TargetRealityModel realityModel, String lowerQuery) {

        StringBuilder sb = new StringBuilder();
        if (category == FileCategory.BUILD) {
            return "Provides authoritative dependency coordinates, build targets, and compilation rules.";
        }
        if (category == FileCategory.DOCUMENTATION) {
            return "Provides foundational project overview, usage guidelines, and domain conventions.";
        }
        if (category == FileCategory.CONFIG) {
            return "Defines essential runtime parameters, environment profiles, and module boundaries.";
        }
        if (category == FileCategory.TEST) {
            return "Illustrates expected component behavior and existing regression baselines.";
        }

        // Subsystem alignment
        if (realityModel != null) {
            for (Subsystem sub : realityModel.getSubsystems()) {
                if (sub.getCriticalFiles().contains(path)) {
                    sb.append("Critical file for subsystem '").append(sub.getName()).append("'. ");
                }
                if (sub.getBoundaries().contains(path)) {
                    sb.append("Represents an architectural boundary for '").append(sub.getName()).append("'. ");
                }
            }
        }

        // Relevance overlap with user request
        if (!lowerQuery.isEmpty()) {
            String[] words = lowerQuery.split("\\s+");
            for (String w : words) {
                if (w.length() >= 4 && path.toLowerCase(Locale.ROOT).contains(w)) {
                    sb.append("Directly matches task keyword '").append(w).append("'. ");
                    break;
                }
            }
        }

        if (node != null && node.getTags().contains("Executory")) {
            sb.append("Primary execution entry point. ");
        }

        if (sb.length() == 0) {
            sb.append("High architectural authority and high connectivity to core execution pathways.");
        }

        return sb.toString().trim();
    }

    private long estimateTokens(SemanticNode node) {
        String sizeStr = node.getAttributes().get("size");
        try {
            long bytes = (sizeStr != null) ? Long.parseLong(sizeStr) : 1000;
            return bytes / 4; // Rough heuristic for tokens
        } catch (NumberFormatException e) {
            return 250;
        }
    }

    private boolean isCoverageAdequate(List<String> selected, TargetRealityModel model, long currentTokens, int budget, int maxFiles) {
        if (selected.size() < 4) return false; // Minimum context
        if (currentTokens > budget * 0.95) return true; // Budget nearing absolute limit
        if (selected.size() >= maxFiles) return true;

        if (model != null) {
            long highSignificanceGapsCovered = model.getKnowledgeGaps().stream()
                .filter(g -> g.getSignificance() > 0.8)
                .filter(g -> g.getRelatedArtifacts().stream().anyMatch(selected::contains))
                .count();

            long totalHighSignificanceGaps = model.getKnowledgeGaps().stream().filter(g -> g.getSignificance() > 0.8).count();

            // Coverage is adequate ONLY if all high-sig gaps are covered OR we hit maxFiles
            boolean gapsAdequate = (totalHighSignificanceGaps == 0) || (highSignificanceGapsCovered >= totalHighSignificanceGaps);

            if (gapsAdequate && selected.size() >= Math.min(12, maxFiles)) return true;
            return false;
        }

        return selected.size() >= maxFiles;
    }

    private String deriveCluster(SemanticNode node) {
        String path = node.getPath();
        int lastSlash = path.lastIndexOf('/');
        return (lastSlash > 0) ? path.substring(0, lastSlash) : "root";
    }
}
