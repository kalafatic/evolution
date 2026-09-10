package eu.kalafatic.evolution.forge.data.api.processor;

import eu.kalafatic.evolution.forge.data.api.NormalizedSample;

/**
 * Interface for sample normalization and cleaning stage in data pipeline.
 */
public interface DataNormalizer {

    /**
     * Normalizes and cleans a training sample.
     *
     * @param sample input raw sample
     * @return cleaned sample, or null if unrepairable
     */
    NormalizedSample normalize(NormalizedSample sample);
}
