package eu.kalafatic.evolution.controller.trajectory;

/**
 * Encapsulates a system-level signal (build, test, environment) for the evolution engine.
 */
public class SystemSignal extends CognitiveSignal {
    public enum SignalType {
        STABILITY,
        COMPLEXITY,
        PRESSURE,
        FEEDBACK,
        ENVIRONMENT
    }

    private final SignalType type;

    public SystemSignal(String id, String source, double intensity, SignalType type) {
        super(id, source, intensity);
        this.type = type;
    }

    public SignalType getType() { return type; }
}
