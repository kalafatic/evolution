package eu.kalafatic.evolution.forge.trainer.impl.llm;

public enum TrainingStatus {
    QUEUED,
    INITIALIZING,
    TOKENIZING,
    BUILDING_DATASET,
    TRAINING,
    VALIDATING,
    SAVING_CHECKPOINT,
    EXPORTING,
    PAUSED,
    RESUMING,
    COMPLETED,
    FAILED,
    CANCELLED
}
