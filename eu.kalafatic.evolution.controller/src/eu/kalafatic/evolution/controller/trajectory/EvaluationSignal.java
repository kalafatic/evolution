package eu.kalafatic.evolution.controller.trajectory;

import java.util.Collections;
import java.util.Map;
import java.util.HashMap;

/**
 * Standardized telemetry object for evaluator signals.
 * Observations only, NOT decisions.
 */
public final class EvaluationSignal {
    private final String variantId;
    private final String evaluatorId;
    private final double score;
    private final double confidence;
    private final SignalSeverity severity;
    private final String explanation;
    private final long timestamp;
    private final Map<String, Object> metadata;

    public EvaluationSignal(String variantId, String evaluatorId, double score, double confidence,
                            SignalSeverity severity, String explanation) {
        this(variantId, evaluatorId, score, confidence, severity, explanation, new HashMap<>());
    }

    public EvaluationSignal(String variantId, String evaluatorId, double score, double confidence,
                            SignalSeverity severity, String explanation, Map<String, Object> metadata) {
        this.variantId = variantId;
        this.evaluatorId = evaluatorId;
        this.score = score;
        this.confidence = confidence;
        this.severity = severity;
        this.explanation = explanation;
        this.timestamp = System.currentTimeMillis();
        this.metadata = Collections.unmodifiableMap(new HashMap<>(metadata));
    }

    public String getVariantId() { return variantId; }
    public String getEvaluatorId() { return evaluatorId; }
    public double getScore() { return score; }
    public double getConfidence() { return confidence; }
    public SignalSeverity getSeverity() { return severity; }
    public String getExplanation() { return explanation; }
    public long getTimestamp() { return timestamp; }
    public Map<String, Object> getMetadata() { return metadata; }

    @Override
    public String toString() {
        return String.format("EvaluationSignal[variant=%s, evaluator=%s, score=%.2f, confidence=%.2f, severity=%s, explanation=%s]",
                variantId, evaluatorId, score, confidence, severity, explanation);
    }
}
