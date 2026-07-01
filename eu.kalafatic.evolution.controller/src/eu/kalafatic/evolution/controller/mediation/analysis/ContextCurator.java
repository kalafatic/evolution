package eu.kalafatic.evolution.controller.mediation.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import eu.kalafatic.evolution.controller.mediation.model.KnowledgeGap;
import eu.kalafatic.evolution.controller.mediation.model.SemanticEdge;
import eu.kalafatic.evolution.controller.mediation.model.SemanticNode;
import eu.kalafatic.evolution.controller.mediation.model.Subsystem;
import eu.kalafatic.evolution.controller.mediation.model.TargetDescriptor;
import eu.kalafatic.evolution.controller.mediation.model.TargetRealityModel;
import eu.kalafatic.evolution.controller.mediation.model.TargetSnapshot;

/**
 * Selects high-value context while avoiding token floods.
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
            if (path.endsWith("pom.xml") || path.endsWith("build.gradle") || path.endsWith("package.json") || path.endsWith("sloeber.ino")) {
                score += 300.0;
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
