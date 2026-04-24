package eu.kalafatic.evolution.supervisor;

import java.util.List;

public class EvoPlan {
    private int iteration;
    private String variant;
    private List<String> files;

    public EvoPlan() {}

    public int getIteration() { return iteration; }
    public void setIteration(int iteration) { this.iteration = iteration; }

    public String getVariant() { return variant; }
    public void setVariant(String variant) { this.variant = variant; }

    public List<String> getFiles() { return files; }
    public void setFiles(List<String> files) { this.files = files; }
}
