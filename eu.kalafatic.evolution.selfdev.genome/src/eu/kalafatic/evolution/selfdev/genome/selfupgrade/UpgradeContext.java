package eu.kalafatic.evolution.selfdev.genome.selfupgrade;

import java.util.Map;

import eu.kalafatic.evolution.selfdev.genome.core.GenomeArtifact;
import eu.kalafatic.evolution.selfdev.genome.core.Mode;
import eu.kalafatic.evolution.selfdev.genome.core.ProjectSnapshot;

public class UpgradeContext {

    private ProjectSnapshot project;

    private GenomeArtifact artifact;

    private Mode mode;

    private Map<String, Object> metadata;

    public ProjectSnapshot getProject() {
        return project;
    }

    public void setProject(ProjectSnapshot project) {
        this.project = project;
    }

    public GenomeArtifact getArtifact() {
        return artifact;
    }

    public void setArtifact(GenomeArtifact artifact) {
        this.artifact = artifact;
    }

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}
