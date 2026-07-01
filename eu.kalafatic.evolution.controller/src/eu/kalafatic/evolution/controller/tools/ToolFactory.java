package eu.kalafatic.evolution.controller.tools;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import eu.kalafatic.evolution.controller.orchestration.util.EvolutionConstants;

/**
 * Factory and Registry for Tools.
 * Manages tool instances and ensures singletons for stateless tools.
 *
 * @evo:1:1 reason=introduce-tool-factory
 */
public class ToolFactory {

    private static final Map<String, ITool> tools = new ConcurrentHashMap<>();

    static {
        registerDefaultTools();
    }

    private static void registerDefaultTools() {
        registerTool(EvolutionConstants.TOOL_FILE, new FileTool());
        registerTool(EvolutionConstants.TOOL_MAVEN, new MavenTool());
        registerTool(EvolutionConstants.TOOL_GIT, new GitTool());
        registerTool(EvolutionConstants.TOOL_SHELL, new ShellTool());
        registerTool(EvolutionConstants.TOOL_ECLIPSE, new EclipseTool());
        registerTool(EvolutionConstants.TOOL_CPP, new CppTool());
        registerTool(EvolutionConstants.TOOL_DATABASE, new DatabaseTool());
    }

    /**
     * Registers a tool.
     * @param name Name of the tool.
     * @param tool ITool instance.
     */
    public static void registerTool(String name, ITool tool) {
        tools.put(name, tool);
    }

    /**
     * Gets a tool by name.
     * @param name Name of the tool.
     * @return ITool instance or null if not found.
     */
    public static ITool getTool(String name) {
        return tools.get(name);
    }
}
