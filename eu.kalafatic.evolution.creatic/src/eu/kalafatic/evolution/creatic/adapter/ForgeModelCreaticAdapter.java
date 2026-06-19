package eu.kalafatic.evolution.creatic.adapter;

import eu.kalafatic.evolution.creatic.model.ContextGraph;
import java.lang.reflect.Method;
import java.util.Map;

public class ForgeModelCreaticAdapter {
    public void adapt(ContextGraph graph) {
        try {
            // Priority 1: Event-driven context from RuntimeContextCollector
            Class<?> sessionManagerClass = Class.forName("eu.kalafatic.evolution.controller.orchestration.SessionManager");
            Method getSmInstance = sessionManagerClass.getMethod("getInstance");
            Object sm = getSmInstance.invoke(null);

            Method getOrch = sm.getClass().getMethod("getOrchestrator");
            Object orch = getOrch.invoke(sm);

            if (orch != null) {
                Method getOrchId = orch.getClass().getMethod("getId");
                String orchId = (String) getOrchId.invoke(orch);

                Method getSession = sm.getClass().getMethod("getSession", String.class);
                Object sessionContainer = getSession.invoke(sm, orchId);

                if (sessionContainer != null) {
                    Method getCollector = sessionContainer.getClass().getMethod("getContextCollector");
                    Object collector = getCollector.invoke(sessionContainer);

                    if (collector != null) {
                        Method getState = collector.getClass().getMethod("getWorkflowState");
                        Map<String, Object> state = (Map<String, Object>) getState.invoke(collector);
                        for (Map.Entry<String, Object> entry : state.entrySet()) {
                            graph.put(entry.getKey(), entry.getValue());
                        }

                        // Ensure structured workflow fields are present
                        String[] structuredFields = {
                            "architecture.status", "dataset.status", "training.status",
                            "evaluation.status", "snapshot.status", "export.status",
                            "deployment.status", "last.action.status", "last.error", "current.goal"
                        };
                        for (String field : structuredFields) {
                            if (!graph.getAll().containsKey(field)) {
                                graph.put(field, state.getOrDefault(field, "UNKNOWN"));
                            }
                        }
                    }
                }
            }

            // Priority 2: Fallback to direct state discovery if needed (e.g., initial load before events)
            if (graph.get("model.exists") == null) {
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

                        try {
                            Method getUiState = firstSession.getClass().getMethod("getModelState");
                            Object modelState = getUiState.invoke(firstSession);
                            Method getHyperparams = modelState.getClass().getMethod("getHyperparameters");
                            String params = (String) getHyperparams.invoke(modelState);
                            if (params != null && params.contains("forge.workflow.status")) {
                                // Extract manually to avoid direct org.json dependency in this module if not available in classpath
                                String marker = "\"forge.workflow.status\":\"";
                                int start = params.indexOf(marker);
                                if (start != -1) {
                                    start += marker.length();
                                    int end = params.indexOf("\"", start);
                                    if (end != -1) {
                                        graph.put("forge.workflow.status", params.substring(start, end));
                                    }
                                }
                            }
                        } catch (Exception e2) {}
                    }
                }
            }
        } catch (Exception e) {
            // Silently fail if classes are not available
            if (graph.get("model.exists") == null) graph.put("model.exists", false);
        }
    }
}
