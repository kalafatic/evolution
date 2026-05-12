package eu.kalafatic.evolution.controller.orchestration.workspace;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import eu.kalafatic.evolution.controller.orchestration.diagnostics.CausalNode;
import eu.kalafatic.evolution.controller.orchestration.diagnostics.CognitiveTrace;
import eu.kalafatic.evolution.controller.workflow.RuntimeEventBus;
import eu.kalafatic.evolution.controller.workflow.RuntimeEvent;
import eu.kalafatic.evolution.controller.workflow.RuntimeEventType;

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

        RuntimeEventBus.getInstance().publish(new RuntimeEvent(
            RuntimeEventType.ARTIFACT_PROMOTED,
            "GLOBAL",
            "SemanticWorkspace",
            artifact.getArtifactId())
            .withMetadata("type", artifact.getArtifactType()));
    }

    public WorkspaceArtifact getArtifact(String id) {
        return artifacts.get(id);
    }

    public List<WorkspaceArtifact> getAllArtifacts() {
        return new ArrayList<>(artifacts.values());
    }

    public List<WorkspaceArtifact> findArtifactsByType(String type) {
        return findArtifactsByType(type, null);
    }

    public List<WorkspaceArtifact> findArtifactsByType(String type, CognitiveTrace trace) {
        List<WorkspaceArtifact> result = artifacts.values().stream()
                .filter(a -> type.equals(a.getArtifactType()))
                .collect(Collectors.toList());

        if (trace != null && !result.isEmpty()) {
            trace.addNode(new CausalNode(
                "workspace-retrieval-" + System.currentTimeMillis(),
                "WORKSPACE_RETRIEVAL",
                "SemanticWorkspace",
                List.of(type),
                result.stream().map(a -> a.getArtifactId()).collect(Collectors.toList()),
                1.0,
                "Retrieved " + result.size() + " artifacts of type: " + type
            ));
        }

        return result;
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
        applyDecay(null);
    }

    public void applyDecay(CognitiveTrace trace) {
        int initialCount = artifacts.size();
        for (WorkspaceArtifact artifact : artifacts.values()) {
            double currentDecay = artifact.getDecayScore();
            artifact.setDecayScore(currentDecay * DECAY_FACTOR);

            // Confidence also weakens as artifacts age without reinforcement
            artifact.setConfidence(artifact.getConfidence() * DECAY_FACTOR);
        }

        // Remove artifacts that have decayed too much
        artifacts.entrySet().removeIf(entry -> entry.getValue().getDecayScore() < 0.1);
        int finalCount = artifacts.size();

        if (trace != null && initialCount != finalCount) {
            trace.addNode(new CausalNode(
                "workspace-decay-" + System.currentTimeMillis(),
                "WORKSPACE_DECAY",
                "SemanticWorkspace",
                List.of("count=" + initialCount),
                List.of("count=" + finalCount),
                1.0,
                "Memory decay applied. Removed " + (initialCount - finalCount) + " stale artifacts."
            ));
        }

        RuntimeEventBus.getInstance().publish(new RuntimeEvent(
            RuntimeEventType.MEMORY_DECAY_APPLIED,
            "GLOBAL",
            "SemanticWorkspace",
            "Pruned " + (initialCount - artifacts.size()) + " stale artifacts."));
    }

    /**
     * Reinforces an artifact, increasing its relevance and decay score.
     */
    public void reinforceArtifact(String id) {
        WorkspaceArtifact artifact = artifacts.get(id);
        if (artifact != null) {
            artifact.setDecayScore(Math.min(1.0, artifact.getDecayScore() + 0.1));
            artifact.setConfidence(Math.min(1.0, artifact.getConfidence() + 0.05));

            RuntimeEventBus.getInstance().publish(new RuntimeEvent(
                RuntimeEventType.TRAJECTORY_STRENGTHENED,
                "GLOBAL",
                "SemanticWorkspace",
                "Reinforced artifact: " + id));
        }
    }
}
