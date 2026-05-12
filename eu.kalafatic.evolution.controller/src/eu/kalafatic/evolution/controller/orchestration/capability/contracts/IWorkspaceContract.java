package eu.kalafatic.evolution.controller.orchestration.capability.contracts;

import eu.kalafatic.evolution.controller.orchestration.workspace.WorkspaceArtifact;
import eu.kalafatic.evolution.controller.orchestration.workspace.TrajectoryMemory;
import eu.kalafatic.evolution.controller.orchestration.diagnostics.CognitiveTrace;
import java.util.List;

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
