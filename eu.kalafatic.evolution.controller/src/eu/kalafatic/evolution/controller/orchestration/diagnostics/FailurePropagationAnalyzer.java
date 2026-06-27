package eu.kalafatic.evolution.controller.orchestration.diagnostics;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Identifies root-cause failures and cascading instability.
 */
public class FailurePropagationAnalyzer {

    /**
     * Identifies the primary root cause of a failure chain.
     */
    public List<CausalNode> identifyRootCauses(CognitiveTrace trace) {
        List<CausalNode> failures = trace.getCausalChain().stream()
                .filter(n -> isFailureNode(n))
                .collect(Collectors.toList());

        if (failures.isEmpty()) return new ArrayList<>();

        // Root cause is generally the first failure in the causal chain that isn't a secondary effect
        CausalNode root = failures.get(0);
        return List.of(root);
    }

    private boolean isFailureNode(CausalNode node) {
        String type = node.getNodeType();
        return type.contains("FAILURE") || type.contains("CRITICAL") || type.contains("REJECTED") || node.getConfidence() < 0.2;
    }

    /**
     * Detects if failures are propagating and causing system-wide instability.
     */
    public boolean detectCascadingInstability(CognitiveTrace trace) {
        double lastConfidence = 1.0;
        int instabilityScore = 0;

        for (CausalNode node : trace.getCausalChain()) {
            if (node.getConfidence() < lastConfidence) {
                instabilityScore++;
            } else if (node.getConfidence() > lastConfidence + 0.2) {
                instabilityScore = Math.max(0, instabilityScore - 1);
            }
            lastConfidence = node.getConfidence();
        }

        return instabilityScore >= 4;
    }
}
