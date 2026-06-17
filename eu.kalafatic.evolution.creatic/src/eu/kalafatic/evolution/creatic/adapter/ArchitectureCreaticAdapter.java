package eu.kalafatic.evolution.creatic.adapter;

import eu.kalafatic.evolution.creatic.model.ContextGraph;

public class ArchitectureCreaticAdapter {
    public void adapt(ContextGraph graph) {
        try {
            // Event-driven context
            Class<?> sessionManagerClass = Class.forName("eu.kalafatic.evolution.controller.orchestration.SessionManager");
            java.lang.reflect.Method getSmInstance = sessionManagerClass.getMethod("getInstance");
            Object sm = getSmInstance.invoke(null);

            java.lang.reflect.Method getOrch = sm.getClass().getMethod("getOrchestrator");
            Object orch = getOrch.invoke(sm);

            if (orch != null) {
                java.lang.reflect.Method getOrchId = orch.getClass().getMethod("getId");
                String orchId = (String) getOrchId.invoke(orch);

                java.lang.reflect.Method getSession = sm.getClass().getMethod("getSession", String.class);
                Object sessionContainer = getSession.invoke(sm, orchId);

                if (sessionContainer != null) {
                    java.lang.reflect.Method getCollector = sessionContainer.getClass().getMethod("getContextCollector");
                    Object collector = getCollector.invoke(sessionContainer);

                    if (collector != null) {
                        java.lang.reflect.Method getState = collector.getClass().getMethod("getWorkflowState");
                        java.util.Map<String, Object> state = (java.util.Map<String, Object>) getState.invoke(collector);
                        for (java.util.Map.Entry<String, Object> entry : state.entrySet()) {
                            graph.put(entry.getKey(), entry.getValue());
                        }

                        // Propagate structured workflow context to Architecture
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

            if (graph.get("graph.empty") == null) {
                graph.put("graph.empty", true);
            }
        } catch (Exception e) {
            if (graph.get("graph.empty") == null) graph.put("graph.empty", true);
        }
    }
}
