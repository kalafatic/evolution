package eu.kalafatic.forge.controller.api;

import java.util.List;

public interface DatasetController {

    String importDataset(
            String sessionId,
            String path);

    void attachDataset(
            String sessionId,
            String datasetId);

    void removeDataset(
            String sessionId,
            String datasetId);

    List<DatasetInfo> getDatasets(
            String sessionId);
}
