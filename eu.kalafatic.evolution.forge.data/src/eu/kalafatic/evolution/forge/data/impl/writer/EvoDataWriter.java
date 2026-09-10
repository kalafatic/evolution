package eu.kalafatic.evolution.forge.data.impl.writer;

import eu.kalafatic.evolution.forge.data.api.NormalizedSample;
import eu.kalafatic.evolution.forge.data.api.artifact.EvoDatasetArtifact;
import eu.kalafatic.evolution.forge.data.api.source.DatasetSourceConfig;
import eu.kalafatic.evolution.forge.data.api.source.DatasetSourceStats;
import eu.kalafatic.evolution.forge.data.api.writer.TrainingDatasetWriter;

import java.io.File;
import java.util.List;

/**
 * Concrete TrainingDatasetWriter implementation serializing datasets to .evodata artifacts.
 */
public class EvoDataWriter implements TrainingDatasetWriter {

    @Override
    public EvoDatasetArtifact write(File targetFile, List<NormalizedSample> samples, DatasetSourceConfig config, DatasetSourceStats stats, double valSplitRatio) throws Exception {
        EvoDatasetArtifact artifact = new EvoDatasetArtifact(targetFile);
        artifact.save(samples, config, stats, valSplitRatio);
        return artifact;
    }
}
