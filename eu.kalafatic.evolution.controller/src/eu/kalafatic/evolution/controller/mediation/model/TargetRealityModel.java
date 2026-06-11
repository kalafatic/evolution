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
    private List<ArchitecturalFact> architecturalFacts = new ArrayList<>();
    private List<Subsystem> subsystems = new ArrayList<>();
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

    public List<ArchitecturalFact> getArchitecturalFacts() { return architecturalFacts; }
    public void setArchitecturalFacts(List<ArchitecturalFact> architecturalFacts) { this.architecturalFacts = architecturalFacts; }

    public List<Subsystem> getSubsystems() { return subsystems; }
    public void setSubsystems(List<Subsystem> subsystems) { this.subsystems = subsystems; }

    public Map<String, String> getDimensions() { return dimensions; }
    public void setDimensions(Map<String, String> dimensions) { this.dimensions = dimensions; }

    public List<String> getObjectives() { return objectives; }
    public void setObjectives(List<String> objectives) { this.objectives = objectives; }

    public List<String> getRisks() { return risks; }
    public void setRisks(List<String> risks) { this.risks = risks; }

    public void addHotspot(Hotspot hotspot) {
        this.hotspots.add(hotspot);
    }

    public void addArchitecturalFact(ArchitecturalFact fact) {
        this.architecturalFacts.add(fact);
    }

    public void addSubsystem(Subsystem subsystem) {
        this.subsystems.add(subsystem);
    }

    public void addObjective(String objective) {
        this.objectives.add(objective);
    }

    public void addRisk(String risk) {
        this.risks.add(risk);
    }
}
