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
        // Selection strategy:
        // 1. Entry points
        // 2. Main configuration files (pom.xml, package.json)
        // 3. High-density semantic markers (Interfaces, Components)

        List<String> curatedPaths = new ArrayList<>();

        curatedPaths.addAll(target.getFiles().stream()
            .filter(f -> f.getTags().contains("Entry Point"))
            .map(f -> f.getPath())
            .collect(Collectors.toList()));

        curatedPaths.addAll(target.getFiles().stream()
            .filter(f -> f.getPath().endsWith("pom.xml") || f.getPath().endsWith("package.json") ||
                         f.getPath().endsWith(".project") || f.getPath().endsWith(".cproject") ||
                         f.getPath().endsWith(".gitignore"))
            .map(f -> f.getPath())
            .collect(Collectors.toList()));

        curatedPaths.addAll(target.getFiles().stream()
            .filter(f -> f.getTags().contains("Interface") || f.getTags().contains("Spring Component") ||
                         f.getPath().endsWith(".ino") || f.getPath().endsWith(".cpp") || f.getPath().endsWith(".h"))
            .map(f -> f.getPath())
            .collect(Collectors.toList()));

        return curatedPaths.stream().distinct().collect(Collectors.toList());
    }

    public List<String> selectContext(TargetSnapshot snapshot, String query, int maxFiles) {
        if (snapshot == null || snapshot.getNodes().isEmpty()) return new ArrayList<>();

        Map<String, Double> scores = new HashMap<>();
        String lowerQuery = query.toLowerCase();
        String[] keywords = lowerQuery.split("\\s+");

        for (SemanticNode node : snapshot.getNodes().values()) {
            double score = 0.0;
            String path = node.getPath().toLowerCase();
            String name = new java.io.File(node.getPath()).getName().toLowerCase();

            // 1. Keyword Matches (Path and Name)
            for (String word : keywords) {
                if (word.length() < 3) continue;
                if (name.contains(word)) score += 10.0;
                else if (path.contains(word)) score += 5.0;
            }

            // 2. Semantic Tags (Architectural Significance & Centrality)
            if (node.getTags().contains("Entry Point")) score += 20.0; // Higher weight for entrypoints
            if (node.getTags().contains("Interface")) score += 15.0; // Higher weight for interfaces/contracts
            if (node.getTags().contains("Evolution Component")) score += 12.0;
            if (node.getTags().contains("Spring Component")) score += 8.0;
            if (node.getTags().contains("React Component")) score += 8.0;
            if (node.getTags().contains("C++ Source")) score += 8.0;

            // 2b. Role-based Centrality (Orchestration/Control)
            String role = node.getAttributes().get("role");
            if (role != null) {
                role = role.toLowerCase();
                if (role.contains("orchestrator") || role.contains("controller") || role.contains("kernel")) score += 15.0;
                if (role.contains("service") || role.contains("manager")) score += 10.0;
                if (role.contains("provider") || role.contains("engine")) score += 10.0;
            }

            // 3. Structural Matches (Classes/Methods)
            for (String word : keywords) {
                if (word.length() < 3) continue;
                if (node.getStructures().stream().anyMatch(s -> s.toLowerCase().contains(word))) {
                    score += 8.0;
                }
            }

            // 4. Summary Content (Relevance)
            if (node.getSummary() != null) {
                String summary = node.getSummary().toLowerCase();
                for (String word : keywords) {
                    if (word.length() < 3) continue;
                    if (summary.contains(word)) score += 3.0;
                }
            }

            // 5. Config/Project File Priority (Moderate)
            if (name.equals("pom.xml") || name.equals("package.json") || name.endsWith(".ino")) score += 12.0;
            if (name.equals(".project") || name.equals(".cproject") || name.equals(".gitignore")) score += 8.0;

            // 6. Penalty for Boilerplate/Artifacts
            if (path.contains("/target/") || path.contains("/build/") || path.contains("/dist/")) score -= 20.0;
            if (path.contains("/node_modules/")) score -= 20.0;

            if (score > 0) {
                scores.put(node.getId(), score);
            }
        }

        // Boost neighbors of high-scoring nodes
        Set<String> highScorers = scores.entrySet().stream()
            .filter(e -> e.getValue() > 15.0)
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());

        for (String id : highScorers) {
            for (SemanticEdge edge : snapshot.getEdges()) {
                if (edge.getSourceId().equals(id)) scores.merge(edge.getTargetId(), 2.0, Double::sum);
                else if (edge.getTargetId().equals(id)) scores.merge(edge.getSourceId(), 2.0, Double::sum);
            }
        }

        // Final safety fallback: ensure at least some context if nothing selected
        if (scores.isEmpty()) {
            snapshot.getNodes().values().stream()
                .limit(Math.min(snapshot.getNodes().size(), 16))
                .forEach(node -> scores.put(node.getId(), 1.0));
        }

        return scores.entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .limit(maxFiles)
            .map(e -> snapshot.getNodes().get(e.getKey()).getPath())
            .collect(Collectors.toList());
    }
}
