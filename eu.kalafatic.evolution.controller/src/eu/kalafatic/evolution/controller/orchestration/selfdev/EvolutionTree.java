package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Manages the persistent hierarchy of evolutionary nodes.
 * Provides utilities for lineage reconstruction and tree navigation.
 */
public class EvolutionTree {
    private Map<String, EvolutionNode> nodes = new HashMap<>();
    private String rootId;
    private String currentWinnerId;

    public void addNode(EvolutionNode node) {
        nodes.put(node.getId(), node);
        if (node.getParentId() != null) {
            EvolutionNode parent = nodes.get(node.getParentId());
            if (parent != null) {
                if (!parent.getChildIds().contains(node.getId())) {
                    parent.getChildIds().add(node.getId());
                }
                node.setBranchDepth(parent.getBranchDepth() + 1);
                node.getAncestorIds().addAll(parent.getAncestorIds());
                node.getAncestorIds().add(parent.getId());
            }
        } else if (rootId == null) {
            rootId = node.getId();
            node.setBranchDepth(0);
            node.setStatus("ROOT");
        }
    }

    public EvolutionNode getNode(String id) {
        return nodes.get(id);
    }

    public List<EvolutionNode> getLineage(String nodeId) {
        List<EvolutionNode> lineage = new ArrayList<>();
        EvolutionNode current = nodes.get(nodeId);
        while (current != null) {
            lineage.add(0, current);
            current = nodes.get(current.getParentId());
        }
        return lineage;
    }

    public List<EvolutionNode> getSiblings(String nodeId) {
        EvolutionNode node = nodes.get(nodeId);
        if (node == null || node.getParentId() == null) return Collections.emptyList();
        EvolutionNode parent = nodes.get(node.getParentId());
        if (parent == null) return Collections.emptyList();

        return parent.getChildIds().stream()
                .filter(id -> !id.equals(nodeId))
                .map(this::getNode)
                .collect(Collectors.toList());
    }

    public String getRootId() { return rootId; }
    public void setRootId(String rootId) { this.rootId = rootId; }

    public String getCurrentWinnerId() { return currentWinnerId; }
    public void setCurrentWinnerId(String currentWinnerId) { this.currentWinnerId = currentWinnerId; }

    public Map<String, EvolutionNode> getNodes() { return nodes; }
    public void setNodes(Map<String, EvolutionNode> nodes) { this.nodes = nodes; }

    public EvolutionNode getWinnerNode() {
        return currentWinnerId != null ? nodes.get(currentWinnerId) : null;
    }

    /**
     * Reconstructs the full evolutionary history context for prompting.
     */
    public String reconstructLineagePrompt(String nodeId) {
        List<EvolutionNode> lineage = getLineage(nodeId);
        StringBuilder sb = new StringBuilder();

        if (!lineage.isEmpty()) {
            EvolutionNode root = lineage.get(0);
            sb.append("ROOT STRATEGY: ").append(root.getStrategy()).append("\n");

            if (lineage.size() > 1) {
                EvolutionNode parent = lineage.get(lineage.size() - 1);
                sb.append("PARENT STRATEGY: ").append(parent.getStrategy()).append("\n");
                sb.append("PARENT PHILOSOPHY: ").append(parent.getSemanticPhilosophy()).append("\n");
            }

            if (lineage.size() > 2) {
                EvolutionNode grandparent = lineage.get(lineage.size() - 2);
                sb.append("GRANDPARENT STRATEGY: ").append(grandparent.getStrategy()).append("\n");
            }
        }

        // Add explored but rejected regions from the siblings of the lineage nodes
        sb.append("\nEXPLORED REGIONS (TO AVOID):\n");
        for (EvolutionNode node : lineage) {
            List<EvolutionNode> siblings = getSiblings(node.getId());
            for (EvolutionNode sibling : siblings) {
                sb.append("- REJECTED: ").append(sibling.getStrategy()).append(" (Reason: ").append(sibling.getRejectionReason()).append(")\n");
            }
        }

        return sb.toString();
    }
}
