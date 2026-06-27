package eu.kalafatic.evolution.controller.orchestration.selfdev;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a dimension of implementation choice in the evolutionary search space.
 * For example: "Output Strategy", "Persistence Mode", "Reliability Strategy".
 */
public class EvolutionAxis {
    private String name;
    private String description;
    private List<TrajectoryBlueprint> candidateBlueprints = new ArrayList<>();

    public EvolutionAxis(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<TrajectoryBlueprint> getCandidateBlueprints() {
        return candidateBlueprints;
    }

    public void addBlueprint(TrajectoryBlueprint blueprint) {
        this.candidateBlueprints.add(blueprint);
    }
}
