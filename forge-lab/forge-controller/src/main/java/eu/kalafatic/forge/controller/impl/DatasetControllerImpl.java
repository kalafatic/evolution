package eu.kalafatic.forge.controller.impl;

import eu.kalafatic.forge.controller.api.DatasetController;
import eu.kalafatic.forge.controller.api.DatasetInfo;
import eu.kalafatic.forge.controller.service.DatasetService;
import java.util.List;

public class DatasetControllerImpl implements DatasetController {
    private final DatasetService datasetService;

    public DatasetControllerImpl(DatasetService datasetService) {
        this.datasetService = datasetService;
    }

    @Override
    public String importDataset(String sessionId, String path) {
        return datasetService.importDataset(sessionId, path);
    }

    @Override
    public void attachDataset(String sessionId, String datasetId) {
        datasetService.attachDataset(sessionId, datasetId);
    }

    @Override
    public void removeDataset(String sessionId, String datasetId) {
        datasetService.removeDataset(sessionId, datasetId);
    }

    @Override
    public List<DatasetInfo> getDatasets(String sessionId) {
        return datasetService.getDatasets(sessionId);
    }
}
