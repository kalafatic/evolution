package eu.kalafatic.evolution.forge.data.impl.source;

import eu.kalafatic.evolution.forge.data.api.discovery.DataSourceCandidate;
import eu.kalafatic.evolution.forge.data.api.service.TrainingDataAcquisitionRequest;
import eu.kalafatic.evolution.forge.data.api.source.DatasetSource;
import eu.kalafatic.evolution.forge.data.api.source.TrainingDataSourceAdapter;

/**
 * Adapter for HuggingFace dataset candidates.
 */
public class HuggingFaceSourceAdapter implements TrainingDataSourceAdapter {

    @Override
    public boolean supports(DataSourceCandidate candidate) {
        return candidate != null && ("HUGGING_FACE".equalsIgnoreCase(candidate.getProvider()) || "HuggingFace".equalsIgnoreCase(candidate.getProvider()));
    }

    @Override
    public DatasetSource createSource(DataSourceCandidate candidate, TrainingDataAcquisitionRequest request) {
        if (candidate.getSource() != null) {
            return candidate.getSource();
        }
        return new HuggingFaceDatasetSource(candidate.getDatasetId());
    }
}
