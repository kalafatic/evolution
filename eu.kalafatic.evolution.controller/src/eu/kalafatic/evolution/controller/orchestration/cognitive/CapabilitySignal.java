package eu.kalafatic.evolution.controller.orchestration.cognitive;

/**
 * Represents a single cognitive signal detected from an interaction.
 */
public class CapabilitySignal {
    private CapabilityType capability;
    private double weight;
    private SessionIntent intent;
    private String source;

    public CapabilitySignal(CapabilityType capability, double weight, SessionIntent intent, String source) {
        this.capability = capability;
        this.weight = weight;
        this.intent = intent;
        this.source = source;
    }

    public CapabilityType getCapability() { return capability; }
    public double getWeight() { return weight; }
    public SessionIntent getIntent() { return intent; }
    public String getSource() { return source; }
}
