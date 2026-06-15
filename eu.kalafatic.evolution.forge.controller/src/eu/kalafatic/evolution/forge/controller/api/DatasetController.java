package eu.kalafatic.evolution.forge.controller.api;

import java.util.List;
import java.util.Map;

public interface DatasetController {
    List<DatasetInfo> getDatasets(String sessionId);
    Map<String, Object> getDatasetStatistics(String sessionId);
    Map<String, Object> getDatasetSample(String sessionId, int index);
}
