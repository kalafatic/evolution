package eu.kalafatic.evolution.controller.mediation.model;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a discovered architectural use case.
 */
public class ArchitecturalUseCase {
    private String id;
    private String name;
    private String description;
    private List<String> supportingComponents = new ArrayList<>();
    private List<String> supportingFiles = new ArrayList<>();
    private double confidence;
    private String rationale;

    public ArchitecturalUseCase() {}

    public ArchitecturalUseCase(String id, String name, String description, double confidence) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.confidence = confidence;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<String> getSupportingComponents() { return supportingComponents; }
    public void setSupportingComponents(List<String> supportingComponents) { this.supportingComponents = supportingComponents; }

    public List<String> getSupportingFiles() { return supportingFiles; }
    public void setSupportingFiles(List<String> supportingFiles) { this.supportingFiles = supportingFiles; }

    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }

    public String getRationale() { return rationale; }
    public void setRationale(String rationale) { this.rationale = rationale; }
}
