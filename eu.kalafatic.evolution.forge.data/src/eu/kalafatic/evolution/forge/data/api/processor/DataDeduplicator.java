package eu.kalafatic.evolution.forge.data.api.processor;

import eu.kalafatic.evolution.forge.data.api.NormalizedSample;

/**
 * Interface for sample deduplication stage in data pipeline.
 */
public interface DataDeduplicator {

    /**
     * Checks if sample is a duplicate of a previously registered sample.
     *
     * @param sample candidate sample
     * @return true if duplicate, false if unique
     */
    boolean isDuplicate(NormalizedSample sample);

    /**
     * Registers a unique sample in deduplication index/fingerprint cache.
     *
     * @param sample unique sample to register
     */
    void register(NormalizedSample sample);

    /**
     * Resets deduplication index.
     */
    void clear();
}
