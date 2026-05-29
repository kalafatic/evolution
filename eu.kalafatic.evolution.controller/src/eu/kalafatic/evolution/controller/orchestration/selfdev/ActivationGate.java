package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import eu.kalafatic.evolution.controller.trajectory.EvaluationSignal;
import eu.kalafatic.evolution.controller.trajectory.SignalSeverity;
import eu.kalafatic.evolution.controller.workflow.RuntimeEvent;
import eu.kalafatic.evolution.controller.workflow.RuntimeEventBus;
import eu.kalafatic.evolution.controller.workflow.RuntimeEventType;
import eu.kalafatic.evolution.controller.orchestration.SessionContainer;
import eu.kalafatic.evolution.controller.orchestration.SessionManager;

/**
 * Pure recommendation layer for Darwin branch activation.
 */
public class ActivationGate {

    private static final double DEFAULT_ACTIVATION_THRESHOLD = 0.8;
    private final String sessionId;

    public ActivationGate() {
        this("GLOBAL");
    }

    public ActivationGate(String sessionId) {
        this.sessionId = sessionId;
    }

    public List<ActivationRecommendation> recommendActivations(List<BranchVariant> variants) {
        if (variants == null || variants.isEmpty()) {
            return new ArrayList<>();
        }

        List<BranchVariant> sorted = new ArrayList<>(variants);
        sorted.sort(Comparator.comparingDouble(BranchVariant::getScore).reversed());

        List<ActivationRecommendation> recommendations = new ArrayList<>();
        for (int i = 0; i < sorted.size(); i++) {
            BranchVariant v = sorted.get(i);

            double confidence = v.getScore();
            double semanticAlign = 0.9;
            String rationale = determineRationale(v, i + 1);

            emitSemanticSignal(v, semanticAlign);
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
            0.7,
            SignalSeverity.INFO,
            "Semantic alignment score for variant strategy: " + v.getStrategy()
        );

        getEventBus().publish(new RuntimeEvent(
            RuntimeEventType.EVALUATION_SIGNAL_CREATED,
            sessionId,
            "SemanticAnalyzer",
            signal
        ));
    }

    private void emitComplexitySignal(BranchVariant v) {
        double complexityScore = 1.0 - (v.getActions().size() * 0.1);
        complexityScore = Math.max(0.1, complexityScore);

        EvaluationSignal signal = new EvaluationSignal(
            v.getId(),
            "ComplexityScorer",
            complexityScore,
            0.6,
            SignalSeverity.INFO,
            "Calculated complexity based on action count: " + v.getActions().size()
        );

        getEventBus().publish(new RuntimeEvent(
            RuntimeEventType.EVALUATION_SIGNAL_CREATED,
            sessionId,
            "ComplexityScorer",
            signal
        ));
    }

    private RuntimeEventBus getEventBus() {
        SessionContainer session = SessionManager.getInstance().getSession(sessionId);
        return (session != null) ? session.getEventBus() : RuntimeEventBus.getInstance();
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

    public double getDefaultActivationThreshold() {
        return DEFAULT_ACTIVATION_THRESHOLD;
    }
}
