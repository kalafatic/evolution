package eu.kalafatic.evolution.creatic.adapter;

import eu.kalafatic.evolution.creatic.model.ContextGraph;

public class ArchitectureCreaticAdapter {
    public void adapt(ContextGraph graph) {
        try {
            Class<?> rendererClass = Class.forName("eu.kalafatic.evolution.controller.orchestration.design.DesignRenderer");
            // DesignRenderer is often instantiated per request, but we can look at static/singleton state if available
            // For now, let's detect if we are in architecture mode based on pageId (already handled by collector)
            graph.put("graph.empty", true); // Placeholder until we can reliably read graph state
        } catch (Exception e) {
            graph.put("graph.empty", true);
        }
    }
}
