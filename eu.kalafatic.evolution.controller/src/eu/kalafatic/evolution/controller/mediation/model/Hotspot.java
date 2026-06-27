package eu.kalafatic.evolution.controller.mediation.model;

import java.util.ArrayList;
import java.util.List;

public class Hotspot {
	  private String id;  // Unique identifier for the hotspot
    private String name;
    private String type;
    private String description;
    private double significance;
    private String file;  // Added for file reference
    private double complexity;
    private double changeFrequency;
    private int linesOfCode;
    private List<String> dependencies = new ArrayList<>();
    private List<String> relatedHotspots = new ArrayList<>();
    
    public Hotspot() {}
    
    public Hotspot(String name, String type, String description, double significance) {
        this.name = name;
        this.type = type;
        this.description = description;
        this.significance = significance;
    }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public double getSignificance() { return significance; }
    public void setSignificance(double significance) { this.significance = significance; }
    
    public String getFile() { return file; }
    public void setFile(String file) { this.file = file; }
    
    public double getComplexity() { return complexity; }
    public void setComplexity(double complexity) { this.complexity = complexity; }
    
    public double getChangeFrequency() { return changeFrequency; }
    public void setChangeFrequency(double changeFrequency) { this.changeFrequency = changeFrequency; }
    
    public int getLinesOfCode() { return linesOfCode; }
    public void setLinesOfCode(int linesOfCode) { this.linesOfCode = linesOfCode; }
    
    public List<String> getDependencies() { return dependencies; }
    public void setDependencies(List<String> dependencies) { this.dependencies = dependencies; }
    
    public List<String> getRelatedHotspots() { return relatedHotspots; }
    public void setRelatedHotspots(List<String> relatedHotspots) { this.relatedHotspots = relatedHotspots; }
    
    @Override
    public String toString() {
        return "Hotspot{" +
                "name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", description='" + description + '\'' +
                ", significance=" + significance +
                ", file='" + file + '\'' +
                '}';
    }

	public String[] getRelatedArtifacts() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
}