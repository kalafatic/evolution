package eu.kalafatic.evolution.media.model;

import java.util.ArrayList;
import java.util.List;

public class Group {
    private String id;
    private String label;
    private List<String> nodeIds = new ArrayList<>();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public List<String> getNodeIds() { return nodeIds; }
}
