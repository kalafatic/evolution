package eu.kalafatic.evolution.controller.orchestration.cognitive.loop;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Encapsulates the domain-independent goal for a Cognitive Loop execution.
 */
public class CognitiveGoal {
    private final String description;
    private final String targetDomain;
    private final Map<String, Object> parameters;

    public CognitiveGoal(String description) {
        this(description, "GENERAL", Collections.emptyMap());
    }

    public CognitiveGoal(String description, String targetDomain, Map<String, Object> parameters) {
        this.description = description;
        this.targetDomain = targetDomain != null ? targetDomain : "GENERAL";
        this.parameters = parameters != null ? new HashMap<>(parameters) : new HashMap<>();
    }

    public String getDescription() {
        return description;
    }

    public String getTargetDomain() {
        return targetDomain;
    }

    public Map<String, Object> getParameters() {
        return Collections.unmodifiableMap(parameters);
    }

    public Object getParameter(String key) {
        return parameters.get(key);
    }

    @Override
    public String toString() {
        return "CognitiveGoal{" +
                "description='" + description + '\'' +
                ", targetDomain='" + targetDomain + '\'' +
                ", parameters=" + parameters +
                '}';
    }
}
