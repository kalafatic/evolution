package eu.kalafatic.evolution.controller.supervision;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

/**
 * Encapsulates the result of a single policy evaluation.
 */
public final class PolicyResult {
    private final double score;
    private final double confidence;
    private final String reasonTrace;

    public PolicyResult(double score, double confidence, String reasonTrace) {
        this.score = Math.max(0.0, Math.min(1.0, score));
        this.confidence = Math.max(0.0, Math.min(1.0, confidence));
        this.reasonTrace = reasonTrace;
    }

    public double getScore() { return score; }
    public double getConfidence() { return confidence; }
    public String getReasonTrace() { return reasonTrace; }

    @Override
    public String toString() {
        return String.format("PolicyResult[score=%.2f, confidence=%.2f, trace=%s]", score, confidence, reasonTrace);
    }
}
