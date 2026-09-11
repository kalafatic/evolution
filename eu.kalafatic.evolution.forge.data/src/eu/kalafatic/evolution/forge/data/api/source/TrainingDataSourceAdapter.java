package eu.kalafatic.evolution.forge.data.api.source;

import eu.kalafatic.evolution.forge.data.api.discovery.DataSourceCandidate;
import eu.kalafatic.evolution.forge.data.api.service.TrainingDataAcquisitionRequest;

/**
 * Common polymorphic interface for source adapters acquiring dataset candidates.
 */
public interface TrainingDataSourceAdapter {

    /**
     * Checks if this adapter supports the given candidate source.
     */
    boolean supports(DataSourceCandidate candidate);

    /**
     * Creates an initialized DatasetSource instance corresponding to the candidate.
     */
    DatasetSource createSource(DataSourceCandidate candidate, TrainingDataAcquisitionRequest request) throws Exception;
}
