package eu.kalafatic.evolution.forge.data.api.discovery;

import java.util.List;

/**
 * Abstraction for remote dataset providers (e.g. Hugging Face).
 */
public interface RemoteDatasetProvider {
    String getProviderName();
    List<DatasetCandidate> search(DatasetSearchRequest request) throws Exception;
    DatasetCandidate getMetadata(String datasetId) throws Exception;
}
