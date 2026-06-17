package eu.kalafatic.evolution.creatic.adapter;

import eu.kalafatic.evolution.creatic.model.ContextGraph;
import java.lang.reflect.Method;

public class AIChatCreaticAdapter {
    public void adapt(ContextGraph graph) {
        try {
            // Event-driven context
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
                        java.util.Map<String, Object> state = (java.util.Map<String, Object>) getState.invoke(collector);
                        for (java.util.Map.Entry<String, Object> entry : state.entrySet()) {
                            graph.put(entry.getKey(), entry.getValue());
                        }
                    }
                }
            }

            // Legacy/Direct discovery fallback
            if (graph.get("darwin.active") == null) {
                Class<?> serviceClass = Class.forName("eu.kalafatic.evolution.controller.orchestration.OrchestratorServiceImpl");
                Method getInstance = serviceClass.getMethod("getInstance");
                Object service = getInstance.invoke(null);

                Method getOrchestrator = serviceClass.getMethod("getOrchestrator");
                Object orchestrator = getOrchestrator.invoke(service);

                if (orchestrator != null) {
                    Method isDarwinMode = orchestrator.getClass().getMethod("isDarwinMode");
                    graph.put("darwin.active", isDarwinMode.invoke(orchestrator));
                }
            }

        } catch (Exception e) {
            if (graph.get("darwin.active") == null) graph.put("darwin.active", false);
        }
    }
}
