package eu.kalafatic.evolution.selfdev.genome.core;

import java.util.Map;

public class MediatedPackageArtifact extends GenomeArtifact {

    private Map<String, String> files;

    @Override
    public ArtifactType getType() {
        return ArtifactType.MEDIATED_PACKAGE;
    }

    public Map<String, String> getFiles() {
        return files;
    }

    public void setFiles(Map<String, String> files) {
        this.files = files;
    }
}
