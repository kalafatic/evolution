package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import eu.kalafatic.evolution.controller.trajectory.EvaluationSignal;
import eu.kalafatic.evolution.controller.trajectory.SignalSeverity;
import eu.kalafatic.evolution.controller.workflow.RuntimeEvent;
import eu.kalafatic.evolution.controller.workflow.RuntimeEventBus;
import eu.kalafatic.evolution.controller.workflow.RuntimeEventType;

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

            // Emit Semantic Alignment Signal
            emitSemanticSignal(v, semanticAlign);

            // Emit Complexity Signal (Placeholder)
            emitComplexitySignal(v);

            recommendations.add(new ActivationRecommendation(
                v.getId(),
                v.getStrategy(),
                confidence,
                i + 1,
                semanticAlign,
                rationale,
                DEFAULT_ACTIVATION_THRESHOLD
            ));
        }

        return recommendations;
    }

    private void emitSemanticSignal(BranchVariant v, double semanticAlign) {
        EvaluationSignal signal = new EvaluationSignal(
            v.getId(),
            "SemanticAnalyzer",
            semanticAlign,
            0.7, // confidence
            SignalSeverity.INFO,
            "Semantic alignment score for variant strategy: " + v.getStrategy()
        );

        eu.kalafatic.evolution.controller.trajectory.SignalBus.getInstance().publish(signal);

        RuntimeEventBus.getInstance().publish(new RuntimeEvent(
            RuntimeEventType.EVALUATION_SIGNAL_CREATED,
            "system",
            "SemanticAnalyzer",
            signal
        ));
    }

    private void emitComplexitySignal(BranchVariant v) {
        // Placeholder complexity calculation
        double complexityScore = 1.0 - (v.getActions().size() * 0.1);
        complexityScore = Math.max(0.1, complexityScore);

        EvaluationSignal signal = new EvaluationSignal(
            v.getId(),
            "ComplexityScorer",
            complexityScore,
            0.6, // confidence
            SignalSeverity.INFO,
            "Calculated complexity based on action count: " + v.getActions().size()
        );

        eu.kalafatic.evolution.controller.trajectory.SignalBus.getInstance().publish(signal);

        RuntimeEventBus.getInstance().publish(new RuntimeEvent(
            RuntimeEventType.EVALUATION_SIGNAL_CREATED,
            "system",
            "ComplexityScorer",
            signal
        ));
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

    /**
     * @return the default activation threshold for recommendations
     */
    public double getDefaultActivationThreshold() {
        return DEFAULT_ACTIVATION_THRESHOLD;
    }
}
