package eu.kalafatic.evolution.controller.orchestration.capability.contracts;

import java.util.List;

import eu.kalafatic.evolution.controller.orchestration.diagnostics.CognitiveTrace;
import eu.kalafatic.evolution.controller.orchestration.workspace.TrajectoryMemory;
import eu.kalafatic.evolution.controller.orchestration.workspace.WorkspaceArtifact;

/**
 * Contract for semantic workspace providers.
 */
public interface IWorkspaceContract {
    String ID = "contract.workspace";

    void addArtifact(WorkspaceArtifact artifact);
    List<WorkspaceArtifact> getArtifactsByTag(String tag);
    TrajectoryMemory getTrajectoryMemory();
    void applyDecay(CognitiveTrace trace);
}
