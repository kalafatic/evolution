package eu.kalafatic.evolution.controller.orchestration.cognitive;

/**
 * Represents a single piece of evidence for a capability.
 */
public class Evidence {
    private final String concept;
    private final double weight;
    private final String detector;

    public Evidence(String concept, double weight, String detector) {
        this.concept = concept;
        this.weight = weight;
        this.detector = detector;
    }

    public String getConcept() { return concept; }
    public double getWeight() { return weight; }
    public String getDetector() { return detector; }

    @Override
    public String toString() {
        return String.format("+ %s (+%.1f) [%s]", concept, weight, detector);
    }
}
