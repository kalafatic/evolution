package eu.kalafatic.evolution.forge.data.impl.source;

import eu.kalafatic.evolution.forge.data.api.discovery.DataSourceCandidate;
import eu.kalafatic.evolution.forge.data.api.service.TrainingDataAcquisitionRequest;
import eu.kalafatic.evolution.forge.data.api.source.DatasetSource;
import eu.kalafatic.evolution.forge.data.api.source.TrainingDataSourceAdapter;

/**
 * Adapter for Public Domain Literature and Book candidates.
 */
public class PublicDomainSourceAdapter implements TrainingDataSourceAdapter {

    @Override
    public boolean supports(DataSourceCandidate candidate) {
        return candidate != null && ("PUBLIC_DOMAIN".equalsIgnoreCase(candidate.getProvider()) || "book".equalsIgnoreCase(candidate.getSourceType()));
    }

    @Override
    public DatasetSource createSource(DataSourceCandidate candidate, TrainingDataAcquisitionRequest request) {
        if (candidate.getSource() != null) {
            return candidate.getSource();
        }
        return new LocalDatasetSource("data/books");
    }
}
