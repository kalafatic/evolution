package eu.kalafatic.evolution.controller.mediation.analysis;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
            .filter(f -> f.getPath().endsWith("pom.xml") || f.getPath().endsWith("package.json"))
            .map(f -> f.getPath())
            .collect(Collectors.toList()));

        curatedPaths.addAll(target.getFiles().stream()
            .filter(f -> f.getTags().contains("Interface") || f.getTags().contains("Spring Component"))
            .limit(10)
            .map(f -> f.getPath())
            .collect(Collectors.toList()));

        return curatedPaths.stream().distinct().collect(Collectors.toList());
    }

    public List<String> selectContext(TargetSnapshot snapshot, String query, int maxFiles) {
        Set<String> selectedIds = new HashSet<>();
        String lowerQuery = query.toLowerCase();

        // 1. Direct relevance (Query keywords in path or tags)
        List<SemanticNode> directNodes = snapshot.getNodes().values().stream()
            .filter(node -> node.getPath().toLowerCase().contains(lowerQuery) ||
                            node.getTags().stream().anyMatch(t -> t.toLowerCase().contains(lowerQuery)))
            .collect(Collectors.toList());

        for (SemanticNode node : directNodes) {
            if (selectedIds.size() >= maxFiles) break;
            selectedIds.add(node.getId());
        }

        // 2. Entry points and main configs
        List<SemanticNode> criticalNodes = snapshot.getNodes().values().stream()
            .filter(node -> node.getTags().contains("Entry Point") ||
                            node.getPath().endsWith("pom.xml") ||
                            node.getPath().endsWith("package.json"))
            .collect(Collectors.toList());

        for (SemanticNode node : criticalNodes) {
            if (selectedIds.size() >= maxFiles) break;
            selectedIds.add(node.getId());
        }

        // 3. Graph proximity (Neighbors of direct nodes)
        if (selectedIds.size() < maxFiles) {
            Set<String> neighbors = new HashSet<>();
            for (String id : selectedIds) {
                for (SemanticEdge edge : snapshot.getEdges()) {
                    if (edge.getSourceId().equals(id)) neighbors.add(edge.getTargetId());
                    else if (edge.getTargetId().equals(id)) neighbors.add(edge.getSourceId());
                }
            }
            for (String neighborId : neighbors) {
                if (selectedIds.size() >= maxFiles) break;
                selectedIds.add(neighborId);
            }
        }

        return selectedIds.stream()
            .map(id -> snapshot.getNodes().get(id).getPath())
            .collect(Collectors.toList());
    }
}
