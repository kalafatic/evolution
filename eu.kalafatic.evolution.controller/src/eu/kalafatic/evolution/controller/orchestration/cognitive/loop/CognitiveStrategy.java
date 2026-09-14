package eu.kalafatic.evolution.controller.orchestration.cognitive.loop;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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

    public static CognitiveStrategy acquisitionStrategy(String id, String sourceType, String repository, String split, Map<String, Object> extraParams) {
        Map<String, Object> params = new HashMap<>();
        if (extraParams != null) params.putAll(extraParams);
        if (sourceType != null) params.put("sourceType", sourceType);
        if (repository != null) params.put("repository", repository);
        if (split != null) params.put("split", split);
        return new CognitiveStrategy(id, "Acquire training data from " + sourceType + ":" + repository + ":" + split, List.of("dataset_acquisition"), params, "Strategy for dataset acquisition", 0.9);
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

    public String getSignature() {
        String capability = preferredCapabilities.isEmpty() ? "GENERAL" : preferredCapabilities.get(0);
        Object src = parameters.get("sourceType");
        Object repo = parameters.get("repository");
        if (repo == null) repo = parameters.get("domain");
        Object split = parameters.get("split");

        if (src != null || repo != null || split != null) {
            String s = src != null ? src.toString().trim().toUpperCase() : "ANY";
            String r = repo != null ? repo.toString().trim().toLowerCase() : "ANY";
            String sp = split != null ? split.toString().trim().toLowerCase() : "train";
            return (capability + ":" + s + ":" + r + ":" + sp).toLowerCase();
        }

        return (capability + ":" + identifier).toLowerCase();
    }

    public boolean isEquivalent(CognitiveStrategy other) {
        if (other == null) return false;
        return Objects.equals(this.getSignature(), other.getSignature());
    }

    @Override
    public String toString() {
        return "CognitiveStrategy{" +
                "id='" + identifier + '\'' +
                ", sig='" + getSignature() + '\'' +
                ", conf=" + String.format("%.2f", confidence) +
                '}';
    }
}
