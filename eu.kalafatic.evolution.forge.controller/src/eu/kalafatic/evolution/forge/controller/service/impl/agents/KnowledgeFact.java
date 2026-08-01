package eu.kalafatic.evolution.forge.controller.service.impl.agents;

import java.util.Map;
import java.util.HashMap;

public class KnowledgeFact {
    private final String sourcePath;
    private final String sourceHash;
    private final String sectionLocation;
    private final String concept;
    private final String definition;
    private final String evidence;
    private final Map<String, Object> metadata = new HashMap<>();

    public KnowledgeFact(String sourcePath, String sourceHash, String sectionLocation, String concept, String definition, String evidence) {
        this.sourcePath = sourcePath;
        this.sourceHash = sourceHash;
        this.sectionLocation = sectionLocation;
        this.concept = concept;
        this.definition = definition;
        this.evidence = evidence;
    }

    public String getSourcePath() {
        return sourcePath;
    }

    public String getSourceHash() {
        return sourceHash;
    }

    public String getSectionLocation() {
        return sectionLocation;
    }

    public String getConcept() {
        return concept;
    }

    public String getDefinition() {
        return definition;
    }

    public String getEvidence() {
        return evidence;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }
}
