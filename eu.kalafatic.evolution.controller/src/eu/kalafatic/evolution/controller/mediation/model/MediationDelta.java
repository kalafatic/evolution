package eu.kalafatic.evolution.controller.mediation.model;


import java.util.ArrayList;
import java.util.List;

public class MediationDelta {
    private boolean hasChanges;
    private String summary;
    private List<String> addedFiles = new ArrayList<>();
    private List<String> modifiedFiles = new ArrayList<>();
    private List<String> removedFiles = new ArrayList<>();
    private List<String> semanticChanges = new ArrayList<>();
    
    public boolean hasChanges() { return hasChanges; }
    public void setHasChanges(boolean hasChanges) { this.hasChanges = hasChanges; }
    
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    
    public List<String> getAddedFiles() { return addedFiles; }
    public List<String> getModifiedFiles() { return modifiedFiles; }
    public List<String> getRemovedFiles() { return removedFiles; }
    public List<String> getSemanticChanges() { return semanticChanges; }
    public List<String> getChangedFiles() {
        List<String> all = new ArrayList<>();
        all.addAll(addedFiles);
        all.addAll(modifiedFiles);
        all.addAll(removedFiles);
        return all;
    }
}