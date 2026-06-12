package eu.kalafatic.evolution.controller.mediation.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an automatically discovered subsystem.
 */
public class Subsystem {
    private String id;
    private String name;
    private String purpose;
    private String description;
    private List<String> boundaries = new ArrayList<>(); // major interfaces/boundaries
    private List<String> criticalFiles = new ArrayList<>(); // paths to critical files
    private List<String> responsibilities = new ArrayList<>();
    private List<String> uncertainty = new ArrayList<>();
    private String rationale;
    private double confidence;
    private int discoveryIteration;

    public Subsystem() {}

    public Subsystem(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public Subsystem(String id, String name, double confidence) {
        this.id = id;
        this.name = name;
        this.confidence = confidence;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<String> getBoundaries() { return boundaries; }
    public void setBoundaries(List<String> boundaries) { this.boundaries = boundaries; }

    public List<String> getCriticalFiles() { return criticalFiles; }
    public void setCriticalFiles(List<String> criticalFiles) { this.criticalFiles = criticalFiles; }

    public List<String> getResponsibilities() { return responsibilities; }
    public void setResponsibilities(List<String> responsibilities) { this.responsibilities = responsibilities; }

    public List<String> getUncertainty() { return uncertainty; }
    public void setUncertainty(List<String> uncertainty) { this.uncertainty = uncertainty; }

    public String getRationale() { return rationale; }
    public void setRationale(String rationale) { this.rationale = rationale; }

    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }

    public int getDiscoveryIteration() { return discoveryIteration; }
    public void setDiscoveryIteration(int discoveryIteration) { this.discoveryIteration = discoveryIteration; }
}
