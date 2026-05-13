package eu.kalafatic.evolution.controller.orchestration.mediated.analysis;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import eu.kalafatic.evolution.controller.orchestration.mediated.model.SemanticNode;
import eu.kalafatic.evolution.controller.orchestration.mediated.model.SemanticEdge;
import eu.kalafatic.evolution.controller.orchestration.mediated.model.TargetSnapshot;

/**
 * Reasoning layer that operates ONLY on the TargetSnapshot metadata.
 */
public class MediatedDarwinEngine {

    public static class Hypothesis {
        public String description;
        public double confidence;
        public List<String> affectedNodes = new ArrayList<>();
        public String riskLevel; // LOW, MEDIUM, HIGH
    }

    public List<Hypothesis> runDarwinLoop(TargetSnapshot snapshot, String query) {
        // 1. Select relevant subgraph
        List<SemanticNode> subgraph = selectSubgraph(snapshot, query);

        // 2. Iterative Darwin Loop (metadata-only)
        List<Hypothesis> hypotheses = new ArrayList<>();

        // Initial hypothesis generation based on structural patterns
        hypotheses.addAll(generateInitialHypotheses(subgraph, snapshot));

        // Refine/Prune (Simulated loop)
        return hypotheses.stream()
            .filter(h -> h.confidence > 0.6)
            .collect(Collectors.toList());
    }

    public List<SemanticNode> selectSubgraph(TargetSnapshot snapshot, String query) {
        // Simple heuristic: nodes whose tags or path match query keywords,
        // plus their immediate neighbors in the graph.
        String lowerQuery = query.toLowerCase();
        List<SemanticNode> initialNodes = snapshot.getNodes().values().stream()
            .filter(node -> node.getPath().toLowerCase().contains(lowerQuery) ||
                            node.getTags().stream().anyMatch(t -> t.toLowerCase().contains(lowerQuery)))
            .collect(Collectors.toList());

        Map<String, SemanticNode> subgraphMap = new HashMap<>();
        for (SemanticNode node : initialNodes) {
            subgraphMap.put(node.getId(), node);
            // Add neighbors
            for (SemanticEdge edge : snapshot.getEdges()) {
                if (edge.getSourceId().equals(node.getId())) {
                    subgraphMap.put(edge.getTargetId(), snapshot.getNodes().get(edge.getTargetId()));
                } else if (edge.getTargetId().equals(node.getId())) {
                    subgraphMap.put(edge.getSourceId(), snapshot.getNodes().get(edge.getSourceId()));
                }
            }
        }

        return new ArrayList<>(subgraphMap.values());
    }

    private List<Hypothesis> generateInitialHypotheses(List<SemanticNode> subgraph, TargetSnapshot snapshot) {
        List<Hypothesis> hypotheses = new ArrayList<>();

        // Heuristic 1: Circular Dependency detection
        // (Simplified for this metadata-only example)

        // Heuristic 2: Missing Interface implementations

        // Heuristic 3: High-complexity node detection (e.g., many methods)
        for (SemanticNode node : subgraph) {
            long methodCount = node.getStructures().stream().filter(s -> s.startsWith("method:")).count();
            if (methodCount > 15) {
                Hypothesis h = new Hypothesis();
                h.description = "Refactor " + node.getPath() + " to improve modularity (Detected " + methodCount + " methods).";
                h.confidence = 0.8;
                h.affectedNodes.add(node.getId());
                h.riskLevel = "MEDIUM";
                hypotheses.add(h);
            }
        }

        return hypotheses;
    }
}
