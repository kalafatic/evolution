package eu.kalafatic.evolution.controller.kernel;

import eu.kalafatic.evolution.controller.orchestration.selfdev.IterationMemoryService;
import eu.kalafatic.evolution.controller.trajectory.Trajectory;
import eu.kalafatic.evolution.controller.trajectory.TrajectoryAnalysisRecord;

public class DefaultTrajectoryEngine implements TrajectoryEngine {
    private final IterationMemoryService memoryService;

    public DefaultTrajectoryEngine(IterationMemoryService memoryService) {
        this.memoryService = memoryService;
    }

    @Override
    public void recordTrajectory(Trajectory trajectory) {
        if (memoryService != null && memoryService.getTrajectoryMemory() != null) {
            memoryService.getTrajectoryMemory().recordTrajectory(trajectory);
        }
    }

    @Override
    public void saveTrajectoryAnalysis(TrajectoryAnalysisRecord record) {
        if (memoryService != null) {
            memoryService.saveTrajectoryAnalysis(record);
        }
    }

    @Override
    public void flush() {
        if (memoryService != null) {
            memoryService.flush();
        }
    }
}
