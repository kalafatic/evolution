package eu.kalafatic.evolution.controller.orchestration.cognitive;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

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
     * Legacy constructor for 4-argument calls.
     */
    public CapabilitySignal(CapabilityType capability, double weight, SessionIntent intent, String source) {
        this(capability, weight, 0.5, intent, new ArrayList<>(), source);
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
