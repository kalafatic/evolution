package eu.kalafatic.evolution.controller.tools;

import java.util.Map;
import java.util.Set;
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
        registerTool(DatasetAcquisitionTool.NAME, new DatasetAcquisitionTool());

        // Eclipse / Java Development Tools
        registerTool(eu.kalafatic.evolution.controller.tools.java.JavaDevelopmentTools.InspectClassTool.NAME, new eu.kalafatic.evolution.controller.tools.java.JavaDevelopmentTools.InspectClassTool());
        registerTool(eu.kalafatic.evolution.controller.tools.java.JavaDevelopmentTools.InspectMethodTool.NAME, new eu.kalafatic.evolution.controller.tools.java.JavaDevelopmentTools.InspectMethodTool());
        registerTool(eu.kalafatic.evolution.controller.tools.java.JavaDevelopmentTools.FindReferencesTool.NAME, new eu.kalafatic.evolution.controller.tools.java.JavaDevelopmentTools.FindReferencesTool());
        registerTool(eu.kalafatic.evolution.controller.tools.java.JavaDevelopmentTools.FindCallersTool.NAME, new eu.kalafatic.evolution.controller.tools.java.JavaDevelopmentTools.FindCallersTool());
        registerTool(eu.kalafatic.evolution.controller.tools.java.JavaDevelopmentTools.FindImplementationsTool.NAME, new eu.kalafatic.evolution.controller.tools.java.JavaDevelopmentTools.FindImplementationsTool());
        registerTool(eu.kalafatic.evolution.controller.tools.java.JavaDevelopmentTools.GetDiagnosticsTool.NAME, new eu.kalafatic.evolution.controller.tools.java.JavaDevelopmentTools.GetDiagnosticsTool());
        registerTool(eu.kalafatic.evolution.controller.tools.java.JavaDevelopmentTools.GetProjectStructureTool.NAME, new eu.kalafatic.evolution.controller.tools.java.JavaDevelopmentTools.GetProjectStructureTool());
        registerTool(eu.kalafatic.evolution.controller.tools.java.JavaDevelopmentTools.GetDependenciesTool.NAME, new eu.kalafatic.evolution.controller.tools.java.JavaDevelopmentTools.GetDependenciesTool());
        registerTool(eu.kalafatic.evolution.controller.tools.java.JavaDevelopmentTools.FormatTool.NAME, new eu.kalafatic.evolution.controller.tools.java.JavaDevelopmentTools.FormatTool());

        // Connected coding harnesses (Phase 3, WP-3.8/WP-3.9): Darwin selects external coding
        // agents (e.g. OpenCode) through the connected-application registry.
        registerTool(CodingHarnessTool.NAME, new CodingHarnessTool());

        // Register built-in connected application adapters
        try {
            eu.kalafatic.evolution.controller.harness.ConnectedApplicationRegistry registry =
                    eu.kalafatic.evolution.controller.harness.ConnectedApplicationRegistry.getInstance();
            registry.registerAdapter(new eu.kalafatic.evolution.controller.harness.OpenCodeCodingHarnessAdapter());
            registry.registerAdapter(new eu.kalafatic.evolution.controller.harness.DStudioCodingHarnessAdapter());
            // Phase 6: DCode is the default family coding harness.
            registry.registerAdapter(new eu.kalafatic.evolution.controller.harness.DCodeCodingHarnessAdapter());
            if (registry.getSelectedApplicationId().isEmpty()) {
                registry.selectApplication(
                        eu.kalafatic.evolution.controller.harness.DCodeCodingHarnessAdapter.APPLICATION_ID);
            }
        } catch (Throwable ignored) {}
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

    /**
     * Returns the set of registered tool names.
     */
    public static Set<String> getRegisteredTools() {
        return tools.keySet();
    }
}
