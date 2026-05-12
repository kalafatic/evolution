package eu.kalafatic.evolution.controller.orchestration.decision;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import eu.kalafatic.evolution.controller.orchestration.evolution.EvaluationSignal;
import eu.kalafatic.evolution.controller.orchestration.selfdev.ActivationRecommendation;
import eu.kalafatic.evolution.controller.orchestration.workspace.SemanticWorkspace;

/**
 * Resolver policy that penalizes variants using strategies with historical high failure rates.
 */
public class TrajectoryStabilityPolicy implements ResolverPolicy {
    private final SemanticWorkspace workspace;

    public TrajectoryStabilityPolicy(SemanticWorkspace workspace) {
        this.workspace = workspace;
    }

    @Override
    public DecisionSnapshot resolve(String iterationId, List<EvaluationSignal> signals, List<ActivationRecommendation> recommendations) {
        Map<String, Double> variantScores = new HashMap<>();

        for (ActivationRecommendation rec : recommendations) {
            String variantId = rec.getBranchId();
            String strategy = rec.getStrategy();

            double reliability = workspace.getTrajectoryMemory().getStrategyReliability(strategy);

            // Base score from recommendation weighted by historical reliability
            double adjustedScore = rec.getConfidenceScore() * (0.5 + 0.5 * reliability);
            variantScores.put(variantId, adjustedScore);
        }

        String bestVariantId = variantScores.entrySet().stream()
                .max(Comparator.comparingDouble(Map.Entry::getValue))
                .map(Map.Entry::getKey)
                .orElse(null);

        return new DecisionSnapshot(
            iterationId,
            bestVariantId,
            new ArrayList<>(variantScores.keySet()),
            variantScores,
            new ArrayList<>(),
            "TrajectoryStabilityPolicy",
            "Selected based on historical reliability of mutation strategies.",
            bestVariantId != null ? variantScores.get(bestVariantId) : 0.0,
            "Strategy reliability applied as multiplier to recommendation scores."
        );
    }

    @Override
    public String getName() {
        return "TrajectoryStabilityPolicy";
    }
}
