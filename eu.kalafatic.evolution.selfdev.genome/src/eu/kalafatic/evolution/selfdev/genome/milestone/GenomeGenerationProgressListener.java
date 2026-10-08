package eu.kalafatic.evolution.selfdev.genome.milestone;

/**
 * Optional listener for receiving fine-grained incremental progress and stage notifications during Genome generation.
 */
public interface GenomeGenerationProgressListener {

    void onStageStarted(GenomeGenerationStage stage, String message);

    void onProgress(GenomeGenerationStage stage, long current, long total, String message);

    void onStageCompleted(GenomeGenerationStage stage, String message);

    void onError(GenomeGenerationStage stage, String message, Throwable error);
}
