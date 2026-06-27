package eu.kalafatic.evolution.controller.orchestration.diagnostics;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;
import org.json.JSONObject;
import org.json.JSONArray;
import java.util.Map;
import java.util.HashMap;

/**
 * Central diagnostics layer for reconstructing reasoning chains and identifying failures.
 */
public class IntrospectionEngine {

    public CognitiveTrace reconstruct(String traceId, List<CausalNode> nodes) {
        CognitiveTrace trace = new CognitiveTrace(traceId, "reconstructed", "none");
        nodes.forEach(trace::addNode);
        return trace;
    }

    public String generateCausalSummary(CognitiveTrace trace) {
        StringBuilder sb = new StringBuilder();
        sb.append("Causal Summary for Trace: ").append(trace.getTraceId()).append("\n");
        for (CausalNode node : trace.getCausalChain()) {
            sb.append("  [")
              .append(node.getSourceComponent())
              .append("] ")
              .append(node.getNodeType())
              .append(": ")
              .append(node.getRationale())
              .append(" (Confidence: ")
              .append(String.format("%.2f", node.getConfidence()))
              .append(")\n");
        }
        return sb.toString();
    }

    public List<CausalNode> identifyNoisyComponents(CognitiveTrace trace) {
        return trace.getCausalChain().stream()
                .filter(n -> n.getConfidence() < 0.3)
                .collect(Collectors.toList());
    }

    /**
     * Implement trace compression by collapsing redundant nodes of the same type.
     */
    public List<CausalNode> compressTrace(List<CausalNode> nodes) {
        if (nodes.size() <= 10) return nodes;

        List<CausalNode> compressed = new ArrayList<>();
        CausalNode lastNode = null;
        int repeatCount = 0;

        for (CausalNode node : nodes) {
            if (lastNode != null && lastNode.getNodeType().equals(node.getNodeType()) && lastNode.getSourceComponent().equals(node.getSourceComponent())) {
                repeatCount++;
                continue;
            } else {
                if (repeatCount > 0) {
                    Map<String, Object> meta = new HashMap<>(lastNode.getMetadata());
                    meta.put("collapsedCount", repeatCount);
                    compressed.add(new CausalNode(lastNode.getNodeId() + "-collapsed", lastNode.getNodeType(), lastNode.getSourceComponent(),
                        lastNode.getInputReferences(), lastNode.getOutputReferences(), lastNode.getConfidence(),
                        lastNode.getRationale() + " (and " + repeatCount + " similar events)", meta));
                } else if (lastNode != null) {
                    compressed.add(lastNode);
                }
                lastNode = node;
                repeatCount = 0;
            }
        }
        if (lastNode != null) compressed.add(lastNode);
        return compressed;
    }
}
