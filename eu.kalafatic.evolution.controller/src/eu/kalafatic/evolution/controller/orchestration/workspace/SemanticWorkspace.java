package eu.kalafatic.evolution.controller.orchestration.workspace;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Persistent reasoning environment for the orchestration kernel.
 * Maintains semantic context state and tracks active trajectories.
 */
public class SemanticWorkspace {
    private final Map<String, WorkspaceArtifact> artifacts = new ConcurrentHashMap<>();
    private final TrajectoryMemory trajectoryMemory = new TrajectoryMemory();

    // Decay constant
    private static final double DECAY_FACTOR = 0.95;

    public void addArtifact(WorkspaceArtifact artifact) {
        artifacts.put(artifact.getArtifactId(), artifact);
    }

    public WorkspaceArtifact getArtifact(String id) {
        return artifacts.get(id);
    }

    public List<WorkspaceArtifact> getAllArtifacts() {
        return new ArrayList<>(artifacts.values());
    }

    public List<WorkspaceArtifact> findArtifactsByType(String type) {
        return artifacts.values().stream()
                .filter(a -> type.equals(a.getArtifactType()))
                .collect(Collectors.toList());
    }

    public List<WorkspaceArtifact> findArtifactsByTag(String tag) {
        return artifacts.values().stream()
                .filter(a -> a.getSemanticTags().contains(tag))
                .collect(Collectors.toList());
    }

    public TrajectoryMemory getTrajectoryMemory() {
        return trajectoryMemory;
    }

    /**
     * Applies decay mechanics to all artifacts in the workspace.
     */
    public void applyDecay() {
        for (WorkspaceArtifact artifact : artifacts.values()) {
            double currentDecay = artifact.getDecayScore();
            artifact.setDecayScore(currentDecay * DECAY_FACTOR);

            // Confidence also weakens as artifacts age without reinforcement
            artifact.setConfidence(artifact.getConfidence() * DECAY_FACTOR);
        }

        // Remove artifacts that have decayed too much
        artifacts.entrySet().removeIf(entry -> entry.getValue().getDecayScore() < 0.1);
    }

    /**
     * Reinforces an artifact, increasing its relevance and decay score.
     */
    public void reinforceArtifact(String id) {
        WorkspaceArtifact artifact = artifacts.get(id);
        if (artifact != null) {
            artifact.setDecayScore(Math.min(1.0, artifact.getDecayScore() + 0.1));
            artifact.setConfidence(Math.min(1.0, artifact.getConfidence() + 0.05));
        }
    }
}
