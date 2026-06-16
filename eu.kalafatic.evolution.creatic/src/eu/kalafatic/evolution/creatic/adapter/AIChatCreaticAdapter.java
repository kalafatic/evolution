package eu.kalafatic.evolution.creatic.adapter;

import eu.kalafatic.evolution.creatic.model.ContextGraph;
import java.lang.reflect.Method;

public class AIChatCreaticAdapter {
    public void adapt(ContextGraph graph) {
        try {
            Class<?> serviceClass = Class.forName("eu.kalafatic.evolution.controller.orchestration.OrchestratorServiceImpl");
            Method getInstance = serviceClass.getMethod("getInstance");
            Object service = getInstance.invoke(null);

            Method getOrchestrator = serviceClass.getMethod("getOrchestrator");
            Object orchestrator = getOrchestrator.invoke(service);

            if (orchestrator != null) {
                Method isDarwinMode = orchestrator.getClass().getMethod("isDarwinMode");
                graph.put("darwin.active", isDarwinMode.invoke(orchestrator));

                Method getTasks = orchestrator.getClass().getMethod("getTasks");
                Object tasks = getTasks.invoke(orchestrator);
                if (tasks instanceof java.util.List) {
                    graph.put("system.busy", !((java.util.List<?>) tasks).isEmpty());
                }
            }

            // Check if chat is empty
            Class<?> convControllerClass = Class.forName("eu.kalafatic.evolution.controller.orchestration.ConversationOutputController");
            Method getConvInstance = convControllerClass.getMethod("getInstance");
            Object convController = getConvInstance.invoke(null);
            Method getHistory = convControllerClass.getMethod("getSessionHistory", String.class);
            Object history = getHistory.invoke(convController, "Default");
            if (history instanceof java.util.List) {
                graph.put("chat.empty", ((java.util.List<?>) history).isEmpty());
            }

        } catch (Exception e) {
            graph.put("darwin.active", false);
        }
    }
}
