package eu.kalafatic.evolution.controller.orchestration.mediated.analysis;

import java.util.List;
import java.util.ArrayList;
import eu.kalafatic.evolution.controller.orchestration.mediated.model.TargetSnapshot;
import eu.kalafatic.evolution.controller.orchestration.mediated.model.SemanticNode;

/**
 * Validates proposed changes against safety rules, especially for SELF mode.
 */
public class StagingValidator {

    public static class ValidationResult {
        public boolean passed;
        public String riskLevel; // LOW, MEDIUM, HIGH
        public List<String> warnings = new ArrayList<>();
        public String reason;
    }

    public ValidationResult validate(TargetSnapshot snapshot, List<MediatedDarwinEngine.Hypothesis> hypotheses) {
        ValidationResult result = new ValidationResult();
        result.passed = true;
        result.riskLevel = "LOW";

        boolean isSelfMode = snapshot.getTargetType() == TargetSnapshot.TargetType.SELF;

        for (MediatedDarwinEngine.Hypothesis h : hypotheses) {
            // Check for high-risk nodes
            for (String nodeId : h.affectedNodes) {
                SemanticNode node = snapshot.getNodes().get(nodeId);
                if (node != null && isCriticalNode(node, isSelfMode)) {
                    result.riskLevel = "HIGH";
                    result.warnings.add("Proposed change affects critical node: " + node.getPath());
                }
            }

            if ("HIGH".equals(h.riskLevel)) {
                result.riskLevel = "HIGH";
            }
        }

        if (isSelfMode && "HIGH".equals(result.riskLevel)) {
            result.warnings.add("SELF-DEV MODE: High-risk changes detected. Mandatory simulation required.");
        }

        return result;
    }

    private boolean isCriticalNode(SemanticNode node, boolean isSelfMode) {
        if (isSelfMode) {
            // In self-mode, kernel/orchestration files are critical
            return node.getPath().contains("controller/orchestration") ||
                   node.getPath().contains("IterationManager");
        }
        // In general mode, entry points are critical
        return node.getTags().contains("Entry Point");
    }
}
