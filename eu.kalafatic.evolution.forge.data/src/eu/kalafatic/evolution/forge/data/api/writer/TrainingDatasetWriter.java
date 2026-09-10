package eu.kalafatic.evolution.forge.data.api.writer;

import eu.kalafatic.evolution.forge.data.api.NormalizedSample;
import eu.kalafatic.evolution.forge.data.api.artifact.EvoDatasetArtifact;
import eu.kalafatic.evolution.forge.data.api.source.DatasetSourceConfig;
import eu.kalafatic.evolution.forge.data.api.source.DatasetSourceStats;

import java.io.File;
import java.util.List;

/**
 * Interface for serializing training datasets into persistence artifacts (such as .evodata ZIP archives).
 */
public interface TrainingDatasetWriter {

    /**
     * Writes dataset samples and metadata into a target artifact file.
     *
     * @param targetFile target output file (.evodata)
     * @param samples normalized training samples
     * @param config source configuration
     * @param stats size accounting stats
     * @param valSplitRatio validation split ratio
     * @return initialized EvoDatasetArtifact
     * @throws Exception on serialization failure
     */
    EvoDatasetArtifact write(File targetFile, List<NormalizedSample> samples, DatasetSourceConfig config, DatasetSourceStats stats, double valSplitRatio) throws Exception;
}
