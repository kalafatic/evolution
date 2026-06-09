package eu.kalafatic.evolution.controller.mediation.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Formal model of the discovered target reality.
 * Grounding point for all evolutionary trajectories.
 */
public class TargetRealityModel {
    private String domain;
    private String purpose;
    private String architectureSummary;
    private List<String> technologies = new ArrayList<>();
    private List<Hotspot> hotspots = new ArrayList<>();
    private Map<String, String> dimensions = new HashMap<>(); // Dynamic dimensions (e.g., constraints, stakeholders)
    private List<String> objectives = new ArrayList<>();
    private List<String> risks = new ArrayList<>();

    public TargetRealityModel() {}

    public String getDomain() { return domain; }
    public void setDomain(String domain) { this.domain = domain; }

    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }

    public String getArchitectureSummary() { return architectureSummary; }
    public void setArchitectureSummary(String architectureSummary) { this.architectureSummary = architectureSummary; }

    public List<String> getTechnologies() { return technologies; }
    public void setTechnologies(List<String> technologies) { this.technologies = technologies; }

    public List<Hotspot> getHotspots() { return hotspots; }
    public void setHotspots(List<Hotspot> hotspots) { this.hotspots = hotspots; }

    public Map<String, String> getDimensions() { return dimensions; }
    public void setDimensions(Map<String, String> dimensions) { this.dimensions = dimensions; }

    public List<String> getObjectives() { return objectives; }
    public void setObjectives(List<String> objectives) { this.objectives = objectives; }

    public List<String> getRisks() { return risks; }
    public void setRisks(List<String> risks) { this.risks = risks; }

    public void addHotspot(Hotspot hotspot) {
        this.hotspots.add(hotspot);
    }

    public void addObjective(String objective) {
        this.objectives.add(objective);
    }

    public void addRisk(String risk) {
        this.risks.add(risk);
    }
}
