package eu.kalafatic.evolution.forge.trainer.impl.llm;

@FunctionalInterface
public interface TrainingProgressListener {
    void onProgress(TrainingProgress progress);
}
