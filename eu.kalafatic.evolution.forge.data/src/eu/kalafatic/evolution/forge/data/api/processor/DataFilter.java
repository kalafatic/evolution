package eu.kalafatic.evolution.forge.data.api.processor;

import eu.kalafatic.evolution.forge.data.api.NormalizedSample;

/**
 * Interface for sample quality evaluation and filtering in data pipeline.
 */
public interface DataFilter {

    /**
     * Evaluates if sample meets minimum quality standards.
     *
     * @param sample candidate sample
     * @return true if accepted, false if rejected
     */
    boolean accept(NormalizedSample sample);

    /**
     * Calculates quality score for sample between 0.0 and 1.0.
     *
     * @param sample candidate sample
     * @return quality score
     */
    double calculateQualityScore(NormalizedSample sample);
}
