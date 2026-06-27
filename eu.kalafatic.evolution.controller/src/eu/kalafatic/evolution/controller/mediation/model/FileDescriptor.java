package eu.kalafatic.evolution.controller.mediation.model;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

import java.util.ArrayList;
import java.util.List;

/**
 * Metadata about a single file in the mediated target.
 */
public class FileDescriptor {
    private final String path;
    private final String type;
    private final long size;
    private final List<String> tags = new ArrayList<>();
    private String semanticSummary;

    public FileDescriptor(String path, String type, long size) {
        this.path = path;
        this.type = type;
        this.size = size;
    }

    public String getPath() { return path; }
    public String getType() { return type; }
    public long getSize() { return size; }
    public List<String> getTags() { return tags; }
    public String getSemanticSummary() { return semanticSummary; }
    public void setSemanticSummary(String semanticSummary) { this.semanticSummary = semanticSummary; }
}
