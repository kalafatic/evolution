package eu.kalafatic.evolution.controller.supervision;

import java.util.*;
import java.util.stream.Collectors;
import eu.kalafatic.evolution.controller.trajectory.EvaluationSignal;
import eu.kalafatic.evolution.controller.orchestration.selfdev.ActivationRecommendation;

/**
 * Policy that selects based on confidence thresholds from recommendations.
 */
public class ConfidenceThresholdPolicy implements ResolverPolicy {
    private final double threshold;

    public ConfidenceThresholdPolicy(double threshold) {
        this.threshold = threshold;
    }

    @Override
    public DecisionSnapshot resolve(String iterationId, List<EvaluationSignal> signals, List<ActivationRecommendation> recommendations) {
        ActivationRecommendation top = recommendations.stream()
                .filter(r -> r.getConfidenceScore() >= threshold)
                .max(Comparator.comparingDouble(ActivationRecommendation::getConfidenceScore))
                .orElse(null);

        String selectedId = top != null ? top.getBranchId() : null;
        String reason = top != null ? "Selected based on confidence threshold: " + threshold : "No variant reached confidence threshold.";

        List<String> ranked = recommendations.stream()
                .sorted(Comparator.comparingDouble(ActivationRecommendation::getConfidenceScore).reversed())
                .map(ActivationRecommendation::getBranchId)
                .collect(Collectors.toList());

        return new DecisionSnapshot(
                iterationId,
                selectedId,
                ranked,
                new HashMap<>(),
                new ArrayList<>(),
                reason,
                getName(),
                top != null ? top.getConfidenceScore() : 0.0,
                top != null ? top.getRationale() : "Threshold check failed."
        );
    }

    @Override
    public String getName() {
        return "ConfidenceThresholdPolicy";
    }
}
