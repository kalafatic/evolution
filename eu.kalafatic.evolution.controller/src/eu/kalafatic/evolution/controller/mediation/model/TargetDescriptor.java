package eu.kalafatic.evolution.controller.mediation.model;

import java.util.ArrayList;
import java.util.List;

/**
 * High-level descriptor of a mediated target (project, folder, or file).
 */
public class TargetDescriptor {
    private final String rootPath;
    private final List<FileDescriptor> files = new ArrayList<>();
    private final List<String> detectedTechnologies = new ArrayList<>();
    private String structuralSummary;
    private String architectureInference;

    public TargetDescriptor(String rootPath) {
        this.rootPath = rootPath;
    }

    public String getRootPath() { return rootPath; }
    public List<FileDescriptor> getFiles() { return files; }
    public List<String> getDetectedTechnologies() { return detectedTechnologies; }
    public String getStructuralSummary() { return structuralSummary; }
    public void setStructuralSummary(String structuralSummary) { this.structuralSummary = structuralSummary; }
    public String getArchitectureInference() { return architectureInference; }
    public void setArchitectureInference(String architectureInference) { this.architectureInference = architectureInference; }
}
