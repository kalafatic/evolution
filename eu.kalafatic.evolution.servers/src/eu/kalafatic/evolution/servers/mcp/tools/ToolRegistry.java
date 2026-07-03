package eu.kalafatic.evolution.servers.mcp.tools;

import com.fasterxml.jackson.databind.JsonNode;
import eu.kalafatic.evolution.servers.mcp.model.Tool;
import java.util.*;

public class ToolRegistry {
    private final Map<String, Tool> tools = new HashMap<>();
    private final Map<String, ToolExecutor> executors = new HashMap<>();

    public void register(Tool tool, ToolExecutor executor) {
        tools.put(tool.getName(), tool);
        executors.put(tool.getName(), executor);
    }

    public List<Tool> listTools() {
        return new ArrayList<>(tools.values());
    }

    public Object callTool(JsonNode params) throws Exception {
        String name = params.get("name").asText();
        JsonNode arguments = params.get("arguments");

        ToolExecutor executor = executors.get(name);
        if (executor == null) {
            throw new Exception("Tool not found: " + name);
        }

        return executor.execute(arguments);
    }

    public interface ToolExecutor {
        Object execute(JsonNode arguments) throws Exception;
    }
}
