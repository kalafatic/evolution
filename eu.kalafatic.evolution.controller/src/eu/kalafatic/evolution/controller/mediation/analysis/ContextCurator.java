package eu.kalafatic.evolution.controller.mediation.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import eu.kalafatic.evolution.controller.mediation.model.FileDescriptor;
import eu.kalafatic.evolution.controller.mediation.model.SemanticEdge;
import eu.kalafatic.evolution.controller.mediation.model.SemanticNode;
import eu.kalafatic.evolution.controller.mediation.model.TargetDescriptor;
import eu.kalafatic.evolution.controller.mediation.model.TargetSnapshot;

/**
 * Selects high-value context while avoiding token floods.
 */
public class ContextCurator {

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
        return selectContextWithBudget(snapshot, query, DEFAULT_TOKEN_BUDGET);
    }

    public List<String> selectContextWithBudget(TargetSnapshot snapshot, String query, int tokenBudget) {
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

            // A. Graph Centrality Signal (Primary Hotspot Indicator)
            int totalDegree = inDegree.getOrDefault(nid, 0) + outDegree.getOrDefault(nid, 0);
            score += (totalDegree * 8.0); // Increased weight for centrality

            // B. Semantic Density Signal
            // Higher density of structures (methods, classes) and attributes indicates higher info value.
            int density = node.getStructures().size() + node.getAttributes().size() + node.getDependencies().size();
            score += (density * 2.0);

            // C. Abstract Relevance Signal
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

            // D. Abstract Significance Evidence
            if (node.getTags().contains("Executory")) score += 15.0; // Entry points are major hotspots
            if (node.getTags().contains("Annotated")) score += 10.0; // Components with metadata are significant

            if (score > 0) {
                scores.put(nid, score);
            }
        }

        // 2. Token-Budget Driven Selection with Diversity Preservation
        List<String> selected = new ArrayList<>();
        List<Map.Entry<String, Double>> sortedCandidates = scores.entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .collect(Collectors.toList());

        Set<String> selectedClusters = new HashSet<>();
        long currentTokens = 0;

        for (Map.Entry<String, Double> entry : sortedCandidates) {
            String nodeId = entry.getKey();
            SemanticNode node = snapshot.getNodes().get(nodeId);

            long estimatedTokens = estimateTokens(node);
            if (currentTokens + estimatedTokens > tokenBudget) continue;
            if (selected.size() >= 16) break; // Hard ceiling for mediated mode efficiency

            // Derive a generic 'cluster' based on path depth
            String cluster = deriveCluster(node);

            // Penalize context saturation (Diversity Preservation)
            if (selectedClusters.contains(cluster)) {
                if (entry.getValue() < 50.0) continue; // Skip weak duplicates unless extreme significance
            }

            selected.add(node.getPath());
            selectedClusters.add(cluster);
            currentTokens += estimatedTokens;
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

    private String deriveCluster(SemanticNode node) {
        String path = node.getPath();
        int lastSlash = path.lastIndexOf('/');
        return (lastSlash > 0) ? path.substring(0, lastSlash) : "root";
    }
}
