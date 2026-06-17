package eu.kalafatic.evolution.creatic.adapter;

import eu.kalafatic.evolution.creatic.model.ContextGraph;
import java.lang.reflect.Method;
import java.util.Map;

public class DarwinCreaticAdapter {
    public void adapt(ContextGraph graph) {
        try {
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

                        // Darwin specific structured fields
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
        } catch (Exception e) {
            // Silently fail
        }
    }
}
