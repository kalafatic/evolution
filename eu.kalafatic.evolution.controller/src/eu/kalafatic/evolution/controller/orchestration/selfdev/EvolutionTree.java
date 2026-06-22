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

    public synchronized void addNode(EvolutionNode node) {
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

    public synchronized EvolutionNode getNode(String id) {
        return nodes.get(id);
    }

    public synchronized List<EvolutionNode> getLineage(String nodeId) {
        List<EvolutionNode> lineage = new ArrayList<>();
        EvolutionNode current = nodes.get(nodeId);
        while (current != null) {
            lineage.add(0, current);
            current = nodes.get(current.getParentId());
        }
        return lineage;
    }

    public synchronized List<EvolutionNode> getSiblings(String nodeId) {
        EvolutionNode node = nodes.get(nodeId);
        if (node == null || node.getParentId() == null) return Collections.emptyList();
        EvolutionNode parent = nodes.get(node.getParentId());
        if (parent == null) return Collections.emptyList();

        return parent.getChildIds().stream()
                .filter(id -> !id.equals(nodeId))
                .map(this::getNode)
                .collect(Collectors.toList());
    }

    public synchronized String getRootId() { return rootId; }
    public void setRootId(String rootId) { this.rootId = rootId; }

    public synchronized String getCurrentWinnerId() { return currentWinnerId; }
    public void setCurrentWinnerId(String currentWinnerId) { this.currentWinnerId = currentWinnerId; }

    public synchronized Map<String, EvolutionNode> getNodes() { return new HashMap<>(nodes); }
    public void setNodes(Map<String, EvolutionNode> nodes) { this.nodes = nodes; }

    public synchronized EvolutionNode getWinnerNode() {
        return currentWinnerId != null ? nodes.get(currentWinnerId) : null;
    }

    /**
     * Reconstructs the full evolutionary history context for prompting.
     */
    public synchronized String reconstructLineagePrompt(String nodeId) {
        List<EvolutionNode> lineage = getLineage(nodeId);
        StringBuilder sb = new StringBuilder();

        if (!lineage.isEmpty()) {
            EvolutionNode root = lineage.get(0);
            sb.append("ROOT STRATEGY: ").append(root.getStrategy()).append("\n");

            if (lineage.size() > 1) {
                EvolutionNode parent = lineage.get(lineage.size() - 1);
                sb.append("PARENT STRATEGY: ").append(parent.getStrategy()).append("\n");
                sb.append("PARENT PHILOSOPHY: ").append(parent.getSemanticPhilosophy()).append("\n");
                if (parent.getMutationRecord() != null) {
                    sb.append("PARENT EXECUTION MODEL: ").append(parent.getMutationRecord().getExecutionModel()).append("\n");
                    sb.append("PARENT REASONING: ").append(parent.getMutationRecord().getReasoningFocus()).append("\n");
                    sb.append("PARENT FITNESS: ").append(parent.getFitnessScore()).append("\n");
                }
                if (parent.getGenomeSnapshot() != null) {
                    SemanticGenome genome = parent.getGenomeSnapshot();
                    sb.append("LOCKED DIMENSIONS: ").append(genome.getLockedDimensions()).append("\n");
                    sb.append("EXPLORED MUTATIONS: ").append(genome.getDiscoveredMutations().stream().map(m -> m.getStrategy()).collect(Collectors.toList())).append("\n");
                }
                if (!parent.getCodeSnapshots().isEmpty()) {
                    sb.append("### PARENT SOURCE CODE (CURRENT SPECIES ANCESTOR) ###\n");
                    sb.append("MANDATE: You MUST mutate this code to achieve the next evolutionary step. DO NOT start from scratch.\n");
                    parent.getCodeSnapshots().forEach((path, code) -> {
                        sb.append("FILE: ").append(path).append("\n")
                          .append("```java\n").append(code).append("\n```\n");
                    });
                }
            }

            if (lineage.size() > 2) {
                EvolutionNode grandparent = lineage.get(lineage.size() - 2);
                sb.append("GRANDPARENT STRATEGY: ").append(grandparent.getStrategy()).append("\n");
                if (grandparent.getMutationRecord() != null) {
                    sb.append("GRANDPARENT PHILOSOPHY: ").append(grandparent.getMutationRecord().getPhilosophy()).append("\n");
                }
                if (!grandparent.getCodeSnapshots().isEmpty()) {
                    sb.append("GRANDPARENT IMPLEMENTATION CODE (FOR HISTORICAL CONTEXT):\n");
                    grandparent.getCodeSnapshots().forEach((path, code) -> {
                        sb.append("FILE: ").append(path).append("\n")
                          .append("```java\n").append(code).append("\n```\n");
                    });
                }
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
