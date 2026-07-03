package eu.kalafatic.evolution.servers.mcp.tools;

import com.fasterxml.jackson.databind.JsonNode;
import eu.kalafatic.evolution.servers.mcp.model.Tool;
import java.util.*;

public class DemoTools {

    public static void registerAll(ToolRegistry registry) {
        // Echo tool
        Map<String, Object> echoSchema = new HashMap<>();
        echoSchema.put("type", "object");
        Map<String, Object> echoProps = new HashMap<>();
        echoProps.put("text", Collections.singletonMap("type", "string"));
        echoSchema.put("properties", echoProps);
        echoSchema.put("required", Collections.singletonList("text"));

        registry.register(new Tool("echo", "Echoes back the input text", echoSchema),
            args -> Collections.singletonMap("text", args.get("text").asText()));

        // Time tool
        registry.register(new Tool("time", "Returns the current system time", new HashMap<>()),
            args -> Collections.singletonMap("currentTime", new Date().toString()));

        // System info tool
        registry.register(new Tool("systemInfo", "Returns system information", new HashMap<>()),
            args -> {
                Map<String, Object> info = new HashMap<>();
                info.put("javaVersion", System.getProperty("java.version"));
                info.put("osName", System.getProperty("os.name"));
                info.put("osArch", System.getProperty("os.arch"));
                info.put("processors", Runtime.getRuntime().availableProcessors());
                info.put("freeMemory", Runtime.getRuntime().freeMemory());
                info.put("totalMemory", Runtime.getRuntime().totalMemory());
                return info;
            });

        // Calculator tool
        Map<String, Object> calcSchema = new HashMap<>();
        calcSchema.put("type", "object");
        Map<String, Object> calcProps = new HashMap<>();
        calcProps.put("operation", Collections.singletonMap("enum", Arrays.asList("add", "subtract", "multiply", "divide")));
        calcProps.put("a", Collections.singletonMap("type", "number"));
        calcProps.put("b", Collections.singletonMap("type", "number"));
        calcSchema.put("properties", calcProps);
        calcSchema.put("required", Arrays.asList("operation", "a", "b"));

        registry.register(new Tool("calculator", "Performs basic arithmetic operations", calcSchema),
            args -> {
                String op = args.get("operation").asText();
                double a = args.get("a").asDouble();
                double b = args.get("b").asDouble();
                double result;
                switch (op) {
                    case "add": result = a + b; break;
                    case "subtract": result = a - b; break;
                    case "multiply": result = a * b; break;
                    case "divide": result = a / b; break;
                    default: throw new Exception("Unknown operation: " + op);
                }
                return Collections.singletonMap("result", result);
            });

        // Project info tool
        registry.register(new Tool("projectInfo", "Returns information about the current MCP server", new HashMap<>()),
            args -> {
                Map<String, Object> info = new HashMap<>();
                info.put("serverName", "EvoMcpServer");
                info.put("version", "1.0.0");
                info.put("capabilities", Arrays.asList("tools", "resources", "prompts"));
                return info;
            });
    }
}
