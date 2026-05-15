package eu.kalafatic.evolution.controller.supervision;

import java.util.*;
import java.util.stream.Collectors;
import eu.kalafatic.evolution.controller.trajectory.EvaluationSignal;
import eu.kalafatic.evolution.controller.trajectory.SignalSeverity;
import eu.kalafatic.evolution.controller.orchestration.selfdev.ActivationRecommendation;

/**
 * Policy that rejects variants with critical failures.
 */
public class CriticalFailurePolicy implements ResolverPolicy {

    @Override
    public DecisionSnapshot resolve(String iterationId, List<EvaluationSignal> signals, List<ActivationRecommendation> recommendations) {
        Set<String> criticalVariantIds = signals.stream()
                .filter(s -> s.getSeverity() == SignalSeverity.CRITICAL)
                .map(EvaluationSignal::getVariantId)
                .collect(Collectors.toSet());

        List<ActivationRecommendation> filtered = recommendations.stream()
                .filter(r -> !criticalVariantIds.contains(r.getBranchId()))
                .collect(Collectors.toList());

        ActivationRecommendation top = filtered.stream()
                .max(Comparator.comparingDouble(ActivationRecommendation::getConfidenceScore))
                .orElse(null);

        String selectedId = top != null ? top.getBranchId() : null;
        String reason = selectedId != null ? "Selected based on highest score after filtering critical failures." : "No variants remaining after filtering critical failures.";

        List<String> criticalFailures = signals.stream()
                .filter(s -> s.getSeverity() == SignalSeverity.CRITICAL)
                .map(s -> s.getVariantId() + ": " + s.getExplanation())
                .collect(Collectors.toList());

        return new DecisionSnapshot(
                iterationId,
                selectedId,
                new ArrayList<>(),
                new HashMap<>(),
                criticalFailures,
                reason,
                getName(),
                top != null ? top.getConfidenceScore() : 0.0,
                "Critical failures detected: " + criticalVariantIds.size()
        );
    }

    @Override
    public String getName() {
        return "CriticalFailurePolicy";
    }
}
