package eu.kalafatic.evolution.media.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class Diagram {
    private String title;
    private List<Node> nodes = new ArrayList<>();
    private List<Edge> edges = new ArrayList<>();
    private List<Group> groups = new ArrayList<>();
    private List<Section> sections = new ArrayList<>();
    private Map<String, Object> metadata = new HashMap<>();

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public List<Node> getNodes() { return nodes; }
    public List<Edge> getEdges() { return edges; }
    public List<Group> getGroups() { return groups; }
    public List<Section> getSections() { return sections; }
    public Map<String, Object> getMetadata() { return metadata; }

    public void addNode(Node node) { nodes.add(node); }
    public void addEdge(Edge edge) { edges.add(edge); }
    public void addGroup(Group group) { groups.add(group); }
    public void addSection(Section section) { sections.add(section); }
}
