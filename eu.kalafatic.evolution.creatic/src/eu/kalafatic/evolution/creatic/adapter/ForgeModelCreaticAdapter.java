package eu.kalafatic.evolution.creatic.adapter;

import eu.kalafatic.evolution.creatic.model.ContextGraph;
import java.lang.reflect.Method;

public class ForgeModelCreaticAdapter {
    public void adapt(ContextGraph graph) {
        try {
            // Safe discovery via reflection to avoid direct coupling to eu.kalafatic.evolution.controller
            Class<?> managerClass = Class.forName("eu.kalafatic.evolution.controller.orchestration.ForgeSessionManager");
            Method getInstance = managerClass.getMethod("getInstance");
            Object manager = getInstance.invoke(null);

            Method getSessions = managerClass.getMethod("getSessions");
            Object sessions = getSessions.invoke(manager);

            if (sessions instanceof java.util.List) {
                java.util.List<?> sessionList = (java.util.List<?>) sessions;
                graph.put("model.exists", !sessionList.isEmpty());

                if (!sessionList.isEmpty()) {
                    Object firstSession = sessionList.get(0);

                    Method getModelType = firstSession.getClass().getMethod("getSelectedModelType");
                    graph.put("model.type", getModelType.invoke(firstSession));

                    Method getStatus = firstSession.getClass().getMethod("getStatus");
                    Object status = getStatus.invoke(firstSession);
                    graph.put("model.trained", "TRAINED".equals(status.toString()) || "IDLE".equals(status.toString()));
                    graph.put("training.active", "TRAINING".equals(status.toString()));
                }
            }
        } catch (Exception e) {
            // Silently fail if classes are not available
            graph.put("model.exists", false);
        }
    }
}
