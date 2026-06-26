package eu.kalafatic.evolution.controller.orchestration.services;

import eu.kalafatic.evolution.controller.orchestration.IterationManager;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.OrchestrationState;
import eu.kalafatic.evolution.controller.orchestration.Checkpoint;
import eu.kalafatic.evolution.controller.orchestration.FileChangeTracker;
import eu.kalafatic.evolution.controller.orchestration.selfdev.StateSnapshot;
import eu.kalafatic.evolution.controller.orchestration.selfdev.IterationRecord;
import eu.kalafatic.evolution.controller.trajectory.Trajectory;
import eu.kalafatic.evolution.controller.workflow.RuntimeEvent;
import eu.kalafatic.evolution.controller.workflow.RuntimeEventType;
import eu.kalafatic.evolution.controller.workflow.RuntimeEventBus;
import java.util.Map;

public class DefaultCheckpointService implements CheckpointService {

    @Override
    public void saveFullCheckpoint(TaskContext context, IterationManager manager) {
        if (manager.getMemoryService() == null) return;

        OrchestrationState state = context.getOrchestrationState();
        Checkpoint cp = new Checkpoint();
        cp.setSessionId(context.getSessionId());
        cp.setCurrentPhase(state.getCurrentPhase());
        cp.setRawInput(state.getRawInput());
        cp.setIterationCount(state.getIterationCount());
        cp.setMetadata(state.getMetadata());
        cp.setChangedFiles(context.getFileChangeTracker().getChangedFiles());
        cp.setActiveLineage(manager.getMemoryService().getActiveLineage());
        cp.setCurrentIterationId(manager.getCurrentIterationModel() != null ? manager.getCurrentIterationModel().getId() : state.getCurrentIterationId());
        cp.setArtifacts(context.getSemanticWorkspace().getAllArtifacts());
        cp.setCognitiveTraceNodes(state.getCognitiveTrace().getNodes());
        cp.setRejectedBranches(manager.getMemoryService().getEvolutionGraph().getRejectedBranches());
        cp.setRationales(manager.getMemoryService().getEvolutionGraph().getRationales());
        cp.setEntropyHistory(manager.getMemoryService().getEvolutionGraph().getEntropyHistory());
        cp.setDimensions(manager.getMemoryService().getEvolutionGraph().getDimensions());
        cp.setConvergenceReasoning(manager.getMemoryService().getEvolutionGraph().getConvergenceReasoning());
        cp.setGlobalPressureHistory(manager.getMemoryService().getEvolutionGraph().getGlobalPressureHistory());

        cp.setFailureFingerprints(manager.getMemoryService().getFailureMemory().getFingerprints());
        cp.setStrategyFailures(manager.getMemoryService().getFailureMemory().getStrategyFailures());
        cp.setMutationEffectiveness(manager.getMemoryService().getFailureMemory().getMutationEffectiveness());

        cp.setAllRecords(manager.getMemoryService().getRecords());
        cp.setArchitectureHotspots(manager.getMemoryService().getArchitectureHotspots());

        cp.setTrajectories(context.getSemanticWorkspace().getTrajectoryMemory().getTrajectories());

        Object lastSnapshot = state.getMetadata().get("lastSnapshot");
        if (lastSnapshot instanceof StateSnapshot) {
            cp.setLastSnapshot((StateSnapshot) lastSnapshot);
        }

        manager.getMemoryService().saveCheckpoint(cp);
    }

    @Override
    public void restoreStateFromCheckpoint(TaskContext context, IterationManager manager) {
        // Logic moved from restoreStateFromCheckpoint
    }
}
