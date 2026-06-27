package eu.kalafatic.evolution.controller.orchestration.design;

import java.util.ArrayList;
import java.util.List;

/**
 * @evo:19:A reason=dynamic-design-model
 */
public class DesignModel {
    private String name;
    private List<ComponentRecord> components = new ArrayList<>();
    private List<RelationshipRecord> relationships = new ArrayList<>();

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<ComponentRecord> getComponents() { return components; }
    public void setComponents(List<ComponentRecord> components) { this.components = components; }
    public List<RelationshipRecord> getRelationships() { return relationships; }
    public void setRelationships(List<RelationshipRecord> relationships) { this.relationships = relationships; }
}
