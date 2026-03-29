package eu.kalafatic.evolution.controller.orchestration;

import java.util.ArrayList;
import java.util.List;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.controller.orchestration.llm.LlmRouter;
import eu.kalafatic.evolution.controller.orchestration.mcp.McpClient;
import eu.kalafatic.evolution.controller.orchestration.util.DataScrubber;

/**
 * Base AI Agent that wraps existing AI model/chat code.
 */
public abstract class BaseAiAgent implements IAgent {
    protected final String id;
    protected final String type;
    protected final List<ITool> tools = new ArrayList<>();
    protected final LlmRouter llmRouter = new LlmRouter();

    public BaseAiAgent(String id, String type) {
        this.id = id;
        this.type = type;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public List<ITool> getTools() {
        return tools;
    }

    public void addTool(ITool tool) {
        tools.add(tool);
    }

    @Override
    public String process(String taskDescription, TaskContext context, String lastFeedback) throws Exception {
        Orchestrator orchestrator = context.getOrchestrator();

        // 1. Fetch MCP context if enabled
        String mcpContext = "";
        String mcpUrl = orchestrator.getMcpServerUrl();
        if (mcpUrl != null && !mcpUrl.isEmpty()) {
            try {
                McpClient mcpClient = new McpClient(mcpUrl);
                mcpClient.initialize();
                // For now, let's just list resources as a proof of concept
                mcpContext = "\nMCP Local Context: " + mcpClient.listResources();
            } catch (Exception e) {
                context.log("MCP Warning: Could not fetch context from " + mcpUrl + ": " + e.getMessage());
            }
        }

        String prompt = "You are acting as a " + type + " Agent.\n" +
                        "Overall Context: " + context.getSharedMemory() + "\n" +
                        mcpContext + "\n";

        if (lastFeedback != null && !lastFeedback.isEmpty()) {
            prompt += "PREVIOUS ATTEMPT FAILED. Feedback: " + lastFeedback + "\nPlease correct your approach.\n";
        }

        prompt += "Current Task: " + taskDescription + "\n" +
                  "Based on the context and the task, provide your response.";

        // 2. Data Scrubbing if online
        if (!orchestrator.isOfflineMode()) {
            prompt = DataScrubber.scrub(prompt);
        }

        context.log("Agent [" + id + " (" + type + ")]: Processing task - " + taskDescription);

        // 3. Routing via LlmRouter
        float temperature = 0.7f;
        if (orchestrator.getLlm() != null) {
            temperature = orchestrator.getLlm().getTemperature();
        }

        String response = llmRouter.sendRequest(orchestrator, prompt, temperature, null);

        // Post-process if necessary
        return cleanResponse(response);
    }

    protected String cleanResponse(String response) {
        if (response.trim().startsWith("```")) {
            int firstNewline = response.indexOf("\n");
            int lastBackticks = response.lastIndexOf("```");
            if (firstNewline != -1 && lastBackticks > firstNewline) {
                return response.substring(firstNewline + 1, lastBackticks).trim();
            }
        }
        return response.trim();
    }
}
