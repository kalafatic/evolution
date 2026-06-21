package eu.kalafatic.evolution.controller.orchestration.cognitive;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single cognitive signal detected from an interaction.
 * Refactored for evidence-based cognitive analysis.
 */
public class CapabilitySignal {
    private final CapabilityType capability;
    private final double score;
    private final double confidence;
    private final SessionIntent intent;
    private final List<Evidence> evidence;
    private final String explanation;

    public CapabilitySignal(CapabilityType capability, double score, double confidence, SessionIntent intent, List<Evidence> evidence, String explanation) {
        this.capability = capability;
        this.score = score;
        this.confidence = confidence;
        this.intent = intent;
        this.evidence = (evidence != null) ? evidence : new ArrayList<>();
        this.explanation = explanation;
    }

    /**
     * Compatibility constructor for legacy calls.
     */
    public CapabilitySignal(CapabilityType capability, double score, SessionIntent intent, String explanation) {
        this(capability, score, 1.0, intent, null, explanation);
    }

    public CapabilityType getCapability() { return capability; }
    public double getScore() { return score; }
    public double getConfidence() { return confidence; }
    public SessionIntent getIntent() { return intent; }
    public List<Evidence> getEvidence() { return evidence; }
    public String getExplanation() { return explanation; }

    /**
     * Legacy support for weight (maps to score).
     */
    public double getWeight() { return score; }

    /**
     * Legacy support for source (maps to explanation).
     */
    public String getSource() { return explanation; }
}
