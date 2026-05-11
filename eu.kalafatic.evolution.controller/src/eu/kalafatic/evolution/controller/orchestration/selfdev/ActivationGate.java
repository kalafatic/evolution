package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;

/**
 * Pure recommendation layer for Darwin branch activation.
 * It analyzes, scores, and proposes candidates without mutating state or enforcing selection.
 * NO SIDE EFFECTS. NO STATE CHANGE. NO EXECUTION CONTROL.
 */
public class ActivationGate {

    private static final double DEFAULT_ACTIVATION_THRESHOLD = 0.8;

    /**
     * Analyzes branch variants and returns a list of recommendations.
     * Pure function: NO side effects, NO state change.
     *
     * @param variants the list of variants to analyze
     * @return a list of activation recommendations
     */
    public List<ActivationRecommendation> recommendActivations(List<BranchVariant> variants) {
        if (variants == null || variants.isEmpty()) {
            return new ArrayList<>();
        }

        // Sort variants by score to determine ranking
        List<BranchVariant> sorted = new ArrayList<>(variants);
        sorted.sort(Comparator.comparingDouble(BranchVariant::getScore).reversed());

        List<ActivationRecommendation> recommendations = new ArrayList<>();
        for (int i = 0; i < sorted.size(); i++) {
            BranchVariant v = sorted.get(i);

            // Heuristic scoring for recommendation
            double confidence = v.getScore();
            double semanticAlign = 0.9; // Placeholder for semantic analysis
            String rationale = determineRationale(v, i + 1);

            recommendations.add(new ActivationRecommendation(
                v.getId(),
                confidence,
                i + 1,
                semanticAlign,
                rationale,
                DEFAULT_ACTIVATION_THRESHOLD
            ));
        }

        return recommendations;
    }

    private String determineRationale(BranchVariant v, int rank) {
        if (rank == 1 && v.getScore() >= DEFAULT_ACTIVATION_THRESHOLD) {
            return "Strong architectural alignment and high predicted success rate.";
        } else if (v.getScore() > 0.5) {
            return "Viable alternative with balanced trade-offs.";
        } else {
            return "Higher risk or experimental path.";
        }
    }
}
