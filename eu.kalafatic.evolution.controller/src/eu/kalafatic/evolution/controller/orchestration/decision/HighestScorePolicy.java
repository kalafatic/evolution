package eu.kalafatic.evolution.controller.orchestration.decision;

import java.util.*;
import java.util.stream.Collectors;
import eu.kalafatic.evolution.controller.orchestration.evolution.EvaluationSignal;
import eu.kalafatic.evolution.controller.orchestration.selfdev.ActivationRecommendation;

/**
 * Policy that selects the variant with the highest aggregated score.
 */
public class HighestScorePolicy implements ResolverPolicy {

    @Override
    public DecisionSnapshot resolve(String iterationId, List<EvaluationSignal> signals, List<ActivationRecommendation> recommendations) {
        Set<String> validIds = recommendations.stream().map(ActivationRecommendation::getBranchId).collect(Collectors.toSet());
        Map<String, Double> scores = new HashMap<>();
        Map<String, Integer> counts = new HashMap<>();

        for (EvaluationSignal signal : signals) {
            if (validIds.contains(signal.getVariantId())) {
                scores.merge(signal.getVariantId(), signal.getScore(), Double::sum);
                counts.merge(signal.getVariantId(), 1, Integer::sum);
            }
        }

        Map<String, Double> avgScores = new HashMap<>();
        scores.forEach((id, total) -> avgScores.put(id, total / counts.get(id)));

        String bestVariantId = avgScores.entrySet().stream()
                .filter(e -> e.getValue() >= 0.5)
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        if (bestVariantId == null && !avgScores.isEmpty()) {
            bestVariantId = avgScores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
        }

        List<String> ranked = avgScores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        double confidence = bestVariantId != null ? avgScores.get(bestVariantId) : 0.0;
        String reason = bestVariantId != null ? "Selected based on highest aggregated signal score." : "No variants available for scoring.";

        return new DecisionSnapshot(
                iterationId,
                bestVariantId,
                ranked,
                avgScores,
                new ArrayList<>(),
                reason,
                getName(),
                confidence,
                "Highest Score: " + confidence
        );
    }

    @Override
    public String getName() {
        return "HighestScorePolicy";
    }
}
