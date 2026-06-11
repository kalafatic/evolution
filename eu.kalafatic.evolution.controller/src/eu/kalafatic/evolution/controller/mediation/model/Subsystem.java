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

    public Subsystem() {}

    public Subsystem(String id, String name) {
        this.id = id;
        this.name = name;
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
}
