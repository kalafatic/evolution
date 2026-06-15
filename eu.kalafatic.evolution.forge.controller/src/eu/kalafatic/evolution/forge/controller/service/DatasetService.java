package eu.kalafatic.evolution.forge.controller.service;

import eu.kalafatic.evolution.forge.controller.api.DatasetInfo;
import java.util.List;

public interface DatasetService {
    String importDataset(String sessionId, String path);
    void attachDataset(String sessionId, String datasetId);
    void removeDataset(String sessionId, String datasetId);
    List<DatasetInfo> getDatasets(String sessionId);
}
