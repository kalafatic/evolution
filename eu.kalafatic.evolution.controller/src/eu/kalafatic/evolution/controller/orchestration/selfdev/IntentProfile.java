package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.util.ArrayList;
import java.util.List;

public class IntentProfile {
    public String primaryGoal;
    public String complexity;
    public String domain;
    public String artifactType;
    public boolean requiresFramework;
    public String abstractionLevel;
    public double ambiguityScore;
    public List<String> keyFeatures = new ArrayList<>();
    public List<String> avoidances = new ArrayList<>();
    public String userSkillLevel;
    
    @Override
    public String toString() {
        return "IntentProfile{" +
                "primaryGoal='" + primaryGoal + '\'' +
                ", complexity='" + complexity + '\'' +
                ", domain='" + domain + '\'' +
                ", artifactType='" + artifactType + '\'' +
                ", requiresFramework=" + requiresFramework +
                ", abstractionLevel='" + abstractionLevel + '\'' +
                ", ambiguityScore=" + ambiguityScore +
                ", keyFeatures=" + keyFeatures +
                ", avoidances=" + avoidances +
                ", userSkillLevel='" + userSkillLevel + '\'' +
                '}';
    }
}

