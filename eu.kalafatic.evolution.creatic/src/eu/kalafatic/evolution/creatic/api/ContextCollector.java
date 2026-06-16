package eu.kalafatic.evolution.creatic.api;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import eu.kalafatic.evolution.creatic.model.ContextGraph;
import eu.kalafatic.evolution.creatic.adapter.ForgeModelCreaticAdapter;
import eu.kalafatic.evolution.creatic.adapter.AIChatCreaticAdapter;
import eu.kalafatic.evolution.creatic.adapter.ArchitectureCreaticAdapter;

public class ContextCollector {

    public ContextGraph collect(String pageId) {
        ContextGraph graph = new ContextGraph(pageId);

        try {
            if (pageId.contains("forge")) {
                new ForgeModelCreaticAdapter().adapt(graph);
            } else if (pageId.contains("chat")) {
                new AIChatCreaticAdapter().adapt(graph);
            } else if (pageId.contains("architecture")) {
                new ArchitectureCreaticAdapter().adapt(graph);
            }
        } catch (Exception e) {
            // Degrade silently
            System.err.println("Error collecting context for " + pageId + ": " + e.getMessage());
        }

        return graph;
    }
}
