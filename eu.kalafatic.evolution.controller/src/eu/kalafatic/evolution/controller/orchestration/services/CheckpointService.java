package eu.kalafatic.evolution.controller.orchestration.services;

import eu.kalafatic.evolution.controller.orchestration.IterationManager;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;

public interface CheckpointService {
    void saveFullCheckpoint(TaskContext context, IterationManager manager);
    void restoreStateFromCheckpoint(TaskContext context, IterationManager manager);
}
