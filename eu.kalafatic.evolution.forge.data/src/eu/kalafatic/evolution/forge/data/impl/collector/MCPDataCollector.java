package eu.kalafatic.evolution.forge.data.impl.collector;

import eu.kalafatic.evolution.forge.data.api.collector.CollectionContext;
import eu.kalafatic.evolution.forge.data.api.collector.CollectionResult;
import eu.kalafatic.evolution.forge.data.api.collector.CollectorType;
import eu.kalafatic.evolution.forge.data.api.collector.SourceType;
import eu.kalafatic.evolution.forge.data.api.collector.TrainingDataCollector;
import eu.kalafatic.evolution.forge.data.api.collector.TrainingDataItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Collects knowledge through configured MCP sources/tools.
 * Fails gracefully and reports source unavailable if MCP is not configured.
 */
public class MCPDataCollector extends TrainingDataCollector {

    public interface McpProvider {
        boolean isAvailable();
        List<TrainingDataItem> fetchMcpData(List<String> toolsOrResources);
    }

    private final McpProvider mcpProvider;

    public MCPDataCollector() {
        this(null);
    }

    public MCPDataCollector(McpProvider mcpProvider) {
        this.mcpProvider = mcpProvider;
    }

    @Override
    public CollectorType getType() {
        return CollectorType.MCP;
    }

    @Override
    public String getName() {
        return "MCP Data Collector";
    }

    @Override
    public CollectionResult collect(CollectionContext context) {
        long startTime = System.currentTimeMillis();

        if (mcpProvider == null || !mcpProvider.isAvailable()) {
            return CollectionResult.failure(getType(), "MCP source is unavailable or not configured", System.currentTimeMillis() - startTime);
        }

        try {
            List<String> toolsOrResources = context.getMcpToolsOrResources();
            List<TrainingDataItem> fetched = mcpProvider.fetchMcpData(toolsOrResources);

            if (fetched == null) {
                fetched = new ArrayList<>();
            }

            for (TrainingDataItem item : fetched) {
                item.setSourceType(SourceType.MCP);
                if (!item.getMetadata().containsKey("server")) {
                    item.addMetadata("server", "default-mcp-server");
                }
            }

            return CollectionResult.success(getType(), fetched, System.currentTimeMillis() - startTime);
        } catch (Exception e) {
            return CollectionResult.failure(getType(), "MCP data collection failed: " + e.getMessage(), System.currentTimeMillis() - startTime);
        }
    }
}
