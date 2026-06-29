package eu.kalafatic.evolution.supervisor.bootstrap;

import java.util.HashMap;
import java.util.Map;

public class ImprovementCandidate {
    private final String id;
    private final String category;
    private final String description;
    private double priority; // 0.0 to 1.0
    private final Map<String, Object> metadata = new HashMap<>();

    public ImprovementCandidate(String id, String category, String description) {
        this.id = id;
        this.category = category;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public String getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    public double getPriority() {
        return priority;
    }

    public void setPriority(double priority) {
        this.priority = priority;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void addMetadata(String key, Object value) {
        this.metadata.put(key, value);
    }
}
