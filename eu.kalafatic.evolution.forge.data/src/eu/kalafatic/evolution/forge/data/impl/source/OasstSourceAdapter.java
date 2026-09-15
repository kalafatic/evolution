package eu.kalafatic.evolution.forge.data.impl.source;

import eu.kalafatic.evolution.forge.data.api.discovery.DataSourceCandidate;
import eu.kalafatic.evolution.forge.data.api.service.TrainingDataAcquisitionRequest;
import eu.kalafatic.evolution.forge.data.api.source.DatasetSource;
import eu.kalafatic.evolution.forge.data.api.source.TrainingDataSourceAdapter;

import java.io.File;

/**
 * Source adapter for OASST1 dataset candidates.
 */
public class OasstSourceAdapter implements TrainingDataSourceAdapter {

    @Override
    public boolean supports(DataSourceCandidate candidate) {
        if (candidate == null) return false;
        String provider = candidate.getProvider();
        String datasetId = candidate.getDatasetId();
        if ("OASST1".equalsIgnoreCase(provider) || "OASST".equalsIgnoreCase(provider)) {
            return true;
        }
        if (datasetId != null && (datasetId.contains("oasst") || datasetId.contains("OpenAssistant"))) {
            return true;
        }
        return false;
    }

    @Override
    public DatasetSource createSource(DataSourceCandidate candidate, TrainingDataAcquisitionRequest request) throws Exception {
        if (candidate.getSource() != null) {
            return candidate.getSource();
        }
        if (candidate.getDatasetId() != null) {
            File localFile = new File(candidate.getDatasetId());
            if (localFile.exists()) {
                return new OasstDatasetSource(localFile);
            }
        }
        // Default to HuggingFace dataset source for remote OpenAssistant repositories
        String repo = candidate.getDatasetId() != null ? candidate.getDatasetId() : "OpenAssistant/oasst1";
        return new HuggingFaceDatasetSource(repo);
    }
}
