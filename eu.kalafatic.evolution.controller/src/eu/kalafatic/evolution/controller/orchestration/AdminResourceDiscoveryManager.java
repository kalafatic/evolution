package eu.kalafatic.evolution.controller.orchestration;

import java.util.logging.Logger;

import eu.kalafatic.evolution.controller.tools.ToolFactory;
import eu.kalafatic.evolution.servers.service.AuthorizationService;

public class AdminResourceDiscoveryManager {
    private static final Logger LOGGER = Logger.getLogger(AdminResourceDiscoveryManager.class.getName());

    private static AdminResourceDiscoveryManager instance;

    public static synchronized AdminResourceDiscoveryManager getInstance() {
        if (instance == null) {
            instance = new AdminResourceDiscoveryManager();
        }
        return instance;
    }

    private AdminResourceDiscoveryManager() {}

    /**
     * Discovers and registers existing system resources (MCP tools, Agents, Darwin modes, Processes) into Administration Resource Inventory.
     */
    public void discoverAndSyncResources() {
        AuthorizationService authService = AuthorizationService.getInstance();

        // 1. Discover MCP Tools from ToolFactory
        try {
            for (String toolName : ToolFactory.getRegisteredTools()) {
                authService.registerResource(
                    "MCP_TOOL",
                    toolName,
                    "Tool: " + toolName,
                    "Discovered system tool " + toolName,
                    true,
                    "{\"type\":\"tool\",\"source\":\"ToolFactory\"}"
                );
            }
        } catch (Exception e) {
            LOGGER.warning("Error discovering MCP tools: " + e.getMessage());
        }

        // 2. Discover Agents & Darwin Modes
        try {
            String[][] agentsAndModes = {
                {"AGENT", "RealityDiscoveryAgent", "Reality Discovery Agent", "Multi-modal architecture analysis agent"},
                {"AGENT", "ForgeTrainingEstimationAgent", "Training Estimation Agent", "Hardware and performance estimation agent"},
                {"DARWIN_MODE", "simple_chat", "Simple Chat", "Direct AI chat interaction mode"},
                {"DARWIN_MODE", "assisted_coding", "Assisted Coding", "Interactive code completion and modification mode"},
                {"DARWIN_MODE", "self_dev", "Self Development", "Autonomous self-iterative development mode"},
                {"DARWIN_MODE", "mediated", "Mediated Mode", "Non-intrusive cognitive design analysis mode"},
                {"DARWIN_MODE", "intent", "Intent Reconstruction", "General purpose target intent reconstruction mode"}
            };

            for (String[] am : agentsAndModes) {
                authService.registerResource(
                    am[0],
                    am[1],
                    am[2],
                    am[3],
                    true,
                    "{\"type\":\"" + am[0].toLowerCase() + "\",\"status\":\"active\"}"
                );
            }
        } catch (Exception e) {
            LOGGER.warning("Error discovering Agents & Modes: " + e.getMessage());
        }

        // 3. Discover Processes & Services
        try {
            String[][] processes = {
                {"PROCESS", "START_EVO_SUPERVISOR", "Start Supervisor Process", "Launches the EVO Supervisor runtime process"},
                {"PROCESS", "CREATE_EVO_PROJECT", "Create EVO Project", "Initializes new project structure and server workspace"},
                {"PROCESS", "CREATE_INFERENCE_TASK", "Inference Task Execution", "Submits and processes AI inference tasks"},
                {"PROCESS", "ANALYTIC_CHAT_VALIDATION", "Chat Validation Task", "Validates conversational memory and analytic context"}
            };

            for (String[] p : processes) {
                authService.registerResource(
                    p[0],
                    p[1],
                    p[2],
                    p[3],
                    true,
                    "{\"type\":\"process\",\"status\":\"registered\"}"
                );
            }
        } catch (Exception e) {
            LOGGER.warning("Error discovering Processes: " + e.getMessage());
        }

        LOGGER.info("Resource discovery and synchronization completed.");
    }
}
