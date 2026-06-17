package eu.kalafatic.evolution.forge.controller.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.lang.reflect.Method;

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

    @Override
    public String importDataset(String sessionId, String path) {
        String result = "Dataset imported: " + path;
        if (datasetService != null) {
            result = datasetService.importDataset(sessionId, path);
        }

        publishEvent(sessionId, "FORGE_DATASET_IMPORTED", path);

        return result;
    }

    private void publishEvent(String sessionId, String typeName, Object payload) {
        try {
            Class<?> sessionManagerClass = Class.forName("eu.kalafatic.evolution.controller.orchestration.SessionManager");
            Method getInstance = sessionManagerClass.getMethod("getInstance");
            Object sm = getInstance.invoke(null);

            Method getSession = sm.getClass().getMethod("getSession", String.class);
            Object session = getSession.invoke(sm, sessionId);

            if (session != null) {
                Method getEventBus = session.getClass().getMethod("getEventBus");
                Object bus = getEventBus.invoke(session);

                if (bus != null) {
                    Class<?> eventTypeClass = Class.forName("eu.kalafatic.evolution.controller.workflow.RuntimeEventType");
                    Object type = Enum.valueOf((Class<Enum>)eventTypeClass, typeName);

                    Class<?> eventClass = Class.forName("eu.kalafatic.evolution.controller.workflow.RuntimeEvent");
                    Object event = eventClass.getConstructor(eventTypeClass, String.class, String.class, Object.class)
                                             .newInstance(type, sessionId, "DatasetController", payload);

                    Method publish = bus.getClass().getMethod("publish", eventClass);
                    publish.invoke(bus, event);
                }
            }
        } catch (Exception e) {
            // Decoupled: fail silently if controller bundle is not present
        }
    }
}
