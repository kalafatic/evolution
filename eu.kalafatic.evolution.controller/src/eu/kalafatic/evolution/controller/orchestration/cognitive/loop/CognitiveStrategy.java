package eu.kalafatic.evolution.controller.orchestration.cognitive.loop;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Executable strategy representation that influences action selection and planning.
 */
public class CognitiveStrategy {

    private final String identifier;
    private final String objective;
    private final List<String> preferredCapabilities;
    private final Map<String, Object> parameters;
    private final String rationale;
    private final double confidence;

    public CognitiveStrategy(String identifier, String objective, List<String> preferredCapabilities, Map<String, Object> parameters, String rationale, double confidence) {
        this.identifier = identifier != null ? identifier : "DEFAULT_STRATEGY";
        this.objective = objective != null ? objective : "";
        this.preferredCapabilities = preferredCapabilities != null ? List.copyOf(preferredCapabilities) : Collections.emptyList();
        this.parameters = parameters != null ? new HashMap<>(parameters) : new HashMap<>();
        this.rationale = rationale != null ? rationale : "";
        this.confidence = Math.max(0.0, Math.min(1.0, confidence));
    }

    public static CognitiveStrategy ofDefault(String identifier) {
        return new CognitiveStrategy(identifier, "Execute default strategy", Collections.emptyList(), Collections.emptyMap(), "Baseline default strategy", 0.8);
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getObjective() {
        return objective;
    }

    public List<String> getPreferredCapabilities() {
        return preferredCapabilities;
    }

    public Map<String, Object> getParameters() {
        return Collections.unmodifiableMap(parameters);
    }

    public String getRationale() {
        return rationale;
    }

    public double getConfidence() {
        return confidence;
    }

    @Override
    public String toString() {
        return "CognitiveStrategy{" +
                "id='" + identifier + '\'' +
                ", preferredCaps=" + preferredCapabilities +
                ", conf=" + String.format("%.2f", confidence) +
                '}';
    }
}
