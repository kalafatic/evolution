package eu.kalafatic.evolution.forge.controller.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.kalafatic.evolution.forge.controller.api.DatasetController;
import eu.kalafatic.evolution.forge.controller.api.DatasetInfo;
import eu.kalafatic.evolution.forge.controller.service.DatasetService;

public class DatasetControllerImpl implements DatasetController {
    private final DatasetService datasetService;

    public DatasetControllerImpl(DatasetService datasetService) {
        this.datasetService = datasetService;
    }

    @Override
    public List<DatasetInfo> getDatasets(String sessionId) {
        if (datasetService == null) return new ArrayList<>();
        return datasetService.getDatasets(sessionId);
    }

    @Override
    public Map<String, Object> getDatasetStatistics(String sessionId) {
        if (datasetService == null) {
            Map<String, Object> stats = new HashMap<>();
            stats.put("size", 1000);
            stats.put("vocab", 5000);
            return stats;
        }
        return datasetService.getDatasetStatistics(sessionId);
    }

    @Override
    public Map<String, Object> getDatasetSample(String sessionId, int index) {
        if (datasetService == null) {
            Map<String, Object> sample = new HashMap<>();
            sample.put("raw", "Functional sample text");
            sample.put("tokens", new int[]{10, 20, 30});
            return sample;
        }
        return datasetService.getDatasetSample(sessionId, index);
    }
}
