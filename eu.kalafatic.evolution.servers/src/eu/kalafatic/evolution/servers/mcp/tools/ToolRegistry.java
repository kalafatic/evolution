package eu.kalafatic.evolution.servers.mcp.tools;

import com.fasterxml.jackson.databind.JsonNode;
import eu.kalafatic.evolution.servers.mcp.model.McpExecutionContext;
import eu.kalafatic.evolution.servers.mcp.model.Tool;
import java.util.*;

public class ToolRegistry {
    private final Map<String, Tool> tools = new LinkedHashMap<>();
    private final Map<String, ToolExecutor> executors = new LinkedHashMap<>();

    public void register(Tool tool, ToolExecutor executor) {
        tools.put(tool.getName(), tool);
        executors.put(tool.getName(), executor);
    }

    public List<Tool> listTools() {
        return new ArrayList<>(tools.values());
    }

    public Tool getTool(String name) {
        return tools.get(name);
    }

    public boolean hasTool(String name) {
        return tools.containsKey(name);
    }

    public Object callTool(JsonNode params) throws Exception {
        return callTool(params, McpExecutionContext.system());
    }

    public Object callTool(JsonNode params, McpExecutionContext context) throws Exception {
        if (params == null || !params.has("name")) {
            throw new IllegalArgumentException("Invalid params: missing tool 'name'");
        }
        String name = params.get("name").asText();
        JsonNode arguments = params.get("arguments");

        Tool tool = tools.get(name);
        ToolExecutor executor = executors.get(name);
        if (tool == null || executor == null) {
            throw new NoSuchElementException("Tool not found: " + name);
        }

        // Validate schema and parameters
        validateArguments(tool, arguments);

        if (context != null) {
            context.checkCancelledOrTimedOut();
        }

        return executor.execute(arguments, context);
    }

    /**
     * Validates input arguments against declared tool JSON schema.
     */
    public void validateArguments(Tool tool, JsonNode arguments) throws IllegalArgumentException {
        Map<String, Object> schema = tool.getInputSchema();
        if (schema == null || schema.isEmpty()) {
            return; // No strict schema defined
        }

        // Check required fields
        Object requiredObj = schema.get("required");
        if (requiredObj instanceof Iterable<?>) {
            for (Object req : (Iterable<?>) requiredObj) {
                String fieldName = req.toString();
                if (arguments == null || !arguments.has(fieldName) || arguments.get(fieldName).isNull()) {
                    throw new IllegalArgumentException("Missing required parameter: '" + fieldName + "' for tool: " + tool.getName());
                }
            }
        }

        // Check properties and basic types
        Object propsObj = schema.get("properties");
        if (propsObj instanceof Map<?, ?> && arguments != null && arguments.isObject()) {
            Map<?, ?> propsMap = (Map<?, ?>) propsObj;
            Iterator<String> fieldNames = arguments.fieldNames();
            while (fieldNames.hasNext()) {
                String field = fieldNames.next();
                Object propSpecObj = propsMap.get(field);
                if (propSpecObj instanceof Map<?, ?>) {
                    Map<?, ?> propSpec = (Map<?, ?>) propSpecObj;
                    JsonNode val = arguments.get(field);
                    String expectedType = (String) propSpec.get("type");
                    if (expectedType != null && val != null && !val.isNull()) {
                        validateFieldType(field, val, expectedType);
                    }
                    Object enumObj = propSpec.get("enum");
                    if (enumObj instanceof Iterable<?>) {
                        boolean match = false;
                        String actualVal = val.asText();
                        for (Object opt : (Iterable<?>) enumObj) {
                            if (opt.toString().equals(actualVal)) {
                                match = true;
                                break;
                            }
                        }
                        if (!match) {
                            throw new IllegalArgumentException("Parameter '" + field + "' value '" + actualVal + "' is not in allowed enum options: " + enumObj);
                        }
                    }
                }
            }
        }
    }

    private void validateFieldType(String fieldName, JsonNode value, String expectedType) throws IllegalArgumentException {
        switch (expectedType.toLowerCase()) {
            case "string":
                if (!value.isTextual()) {
                    throw new IllegalArgumentException("Parameter '" + fieldName + "' must be a string, got " + value.getNodeType());
                }
                break;
            case "number":
                if (!value.isNumber()) {
                    throw new IllegalArgumentException("Parameter '" + fieldName + "' must be a number, got " + value.getNodeType());
                }
                break;
            case "integer":
                if (!value.isIntegralNumber()) {
                    throw new IllegalArgumentException("Parameter '" + fieldName + "' must be an integer, got " + value.getNodeType());
                }
                break;
            case "boolean":
                if (!value.isBoolean()) {
                    throw new IllegalArgumentException("Parameter '" + fieldName + "' must be a boolean, got " + value.getNodeType());
                }
                break;
            case "array":
                if (!value.isArray()) {
                    throw new IllegalArgumentException("Parameter '" + fieldName + "' must be an array, got " + value.getNodeType());
                }
                break;
            case "object":
                if (!value.isObject()) {
                    throw new IllegalArgumentException("Parameter '" + fieldName + "' must be an object, got " + value.getNodeType());
                }
                break;
            default:
                break;
        }
    }

    @FunctionalInterface
    public interface ToolExecutor {
        Object execute(JsonNode arguments, McpExecutionContext context) throws Exception;

        default Object execute(JsonNode arguments) throws Exception {
            return execute(arguments, McpExecutionContext.system());
        }
    }
}
