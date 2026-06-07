package eu.kalafatic.evolution.controller.trajectory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Encapsulates an abstract cognitive signal used for dynamic decision making.
 * Replaces hardcoded state checks with semantic and structural metrics.
 */
public class CognitiveSignal {
    private final String id;
    private final String source;
    private final double intensity;
    private final Map<String, Object> metadata = new ConcurrentHashMap<>();

    public CognitiveSignal(String id, String source, double intensity) {
        this.id = id;
        this.source = source;
        this.intensity = intensity;
    }

    public String getId() { return id; }
    public String getSource() { return source; }
    public double getIntensity() { return intensity; }
    public Map<String, Object> getMetadata() { return metadata; }

    @Override
    public String toString() {
        return String.format("CognitiveSignal[id=%s, source=%s, intensity=%.2f]", id, source, intensity);
    }
}
