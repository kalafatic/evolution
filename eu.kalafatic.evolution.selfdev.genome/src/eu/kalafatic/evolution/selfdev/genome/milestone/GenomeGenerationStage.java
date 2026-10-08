package eu.kalafatic.evolution.selfdev.genome.milestone;

/**
 * Stages of the Genome generation lifecycle.
 */
public enum GenomeGenerationStage {
    VALIDATING,
    GIT_METADATA,
    SCANNING_FILES,
    LOADING_PREVIOUS_GENOME,
    CALCULATING_CHANGES,
    SCANNING_METADATA,
    DISCOVERING_ARCHITECTURE,
    GENERATING_ARTIFACTS,
    COMMITTING_CURRENT,
    CREATING_HISTORY,
    CREATING_ANALYTICAL_SNAPSHOT,
    COMPLETED,
    FAILED,
    UNCHANGED
}
