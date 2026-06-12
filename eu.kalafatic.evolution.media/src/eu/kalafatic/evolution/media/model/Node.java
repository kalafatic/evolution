package eu.kalafatic.evolution.media.model;

import java.util.Map;
import java.util.HashMap;

public class Node {
    private String id;
    private String label;
    private String type;
    private Map<String, String> properties = new HashMap<>();

    public Node() {}
    public Node(String id, String label, String type) {
        this.id = id;
        this.label = label;
        this.type = type;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Map<String, String> getProperties() { return properties; }
}
