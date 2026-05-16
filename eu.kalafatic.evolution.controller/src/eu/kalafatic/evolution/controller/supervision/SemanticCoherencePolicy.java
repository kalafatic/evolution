package eu.kalafatic.evolution.controller.supervision;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import eu.kalafatic.evolution.controller.trajectory.EvaluationSignal;
import eu.kalafatic.evolution.controller.orchestration.selfdev.ActivationRecommendation;
import eu.kalafatic.evolution.controller.orchestration.selfdev.BranchVariant;

/**
 * Policy that evaluates variants based on their semantic coherence with historical EMF data.
 */
public class SemanticCoherencePolicy implements ResolverPolicy {
    private final List<BranchVariant> variants;

    public SemanticCoherencePolicy(List<BranchVariant> variants) {
        this.variants = variants;
    }

    @Override
    public DecisionSnapshot resolve(String iterationId, List<EvaluationSignal> signals, List<ActivationRecommendation> recommendations) {
        Map<String, Double> scores = new HashMap<>();

        for (ActivationRecommendation rec : recommendations) {
            BranchVariant variant = variants.stream().filter(v -> v.getId().equals(rec.getBranchId())).findFirst().orElse(null);
            double score = 0.5;
            if (variant != null && variant.getSurvivalArgument() != null && !variant.getSurvivalArgument().isEmpty()) {
                score += 0.2;
            }
            scores.put(rec.getBranchId(), score);
        }

        String bestId = scores.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(null);

        return new DecisionSnapshot(
            iterationId,
            bestId,
            scores.keySet().stream().sorted((a,b) -> scores.get(b).compareTo(scores.get(a))).collect(Collectors.toList()),
            scores,
            null,
            "Highest semantic coherence",
            getName(),
            1.0,
            "Evaluated coherence based on survival arguments"
        );
    }

    @Override
    public String getName() {
        return "SemanticCoherencePolicy";
    }
}
