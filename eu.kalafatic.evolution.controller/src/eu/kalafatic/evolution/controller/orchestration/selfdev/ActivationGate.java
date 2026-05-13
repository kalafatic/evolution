package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import eu.kalafatic.evolution.controller.orchestration.evolution.EvaluationSignal;
import eu.kalafatic.evolution.controller.orchestration.evolution.SignalSeverity;
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

        // ActivationGate now consumes signals from the SignalBus for its recommendations.
        // It no longer relies on variant.getScore() which is deprecated.
        List<eu.kalafatic.evolution.controller.orchestration.evolution.EvaluationSignal> allSignals =
            eu.kalafatic.evolution.controller.orchestration.evolution.SignalBus.getInstance().getAllSignals();

        List<ActivationRecommendation> recommendations = new ArrayList<>();
        for (int i = 0; i < variants.size(); i++) {
            BranchVariant v = variants.get(i);

            List<eu.kalafatic.evolution.controller.orchestration.evolution.EvaluationSignal> vSignals =
                allSignals.stream().filter(s -> s.getVariantId().equals(v.getId())).collect(java.util.stream.Collectors.toList());

            // Aggregated confidence from signals
            double confidence = vSignals.stream().mapToDouble(s -> s.getScore() * s.getConfidence()).average().orElse(0.5);
            double semanticAlign = vSignals.stream()
                .filter(s -> s.getEvaluatorId().equals("SemanticAnalyzer"))
                .mapToDouble(s -> s.getScore()).findFirst().orElse(0.9);
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

        eu.kalafatic.evolution.controller.orchestration.evolution.SignalBus.getInstance().publish(signal);

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

        eu.kalafatic.evolution.controller.orchestration.evolution.SignalBus.getInstance().publish(signal);

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
