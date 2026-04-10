package eu.kalafatic.evolution.controller.orchestration;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.controller.orchestration.llm.LlmRouter;
import eu.kalafatic.evolution.controller.orchestration.mcp.McpClient;
import eu.kalafatic.evolution.controller.orchestration.util.DataScrubber;
import eu.kalafatic.evolution.controller.orchestration.selfdev.NeuronContextService;

/**
 * Base AI Agent that wraps existing AI model/chat code.
 */
public abstract class BaseAiAgent implements IAgent {
    protected final String id;
    protected final String type;
    protected final List<ITool> tools = new ArrayList<>();
    protected final LlmRouter llmRouter = new LlmRouter();
    
    protected final AiService aiService = new AiService();
    protected BestPracticesService bestPracticesService;
    protected NeuronContextService neuronContextService;

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

    /**
     * Specific instructions for the agent type.
     * @return Agent-specific instructions.
     */
    protected abstract String getAgentInstructions();

    /**
     * Optional footer instructions for the agent (e.g. JSON format).
     * @return Footer instructions.
     */
    protected String getFooterInstructions() {
        return "Based on the context and the task, provide your response.";
    }

    protected String buildPrompt(String taskDescription, TaskContext context, String lastFeedback) {
        Orchestrator orchestrator = context.getOrchestrator();

        // Initialize services if needed
        if (bestPracticesService == null) {
            bestPracticesService = new BestPracticesService(context.getProjectRoot());
        }
        if (neuronContextService == null) {
            neuronContextService = new NeuronContextService(context.getProjectRoot());
        }

        // 1. Fetch MCP context if enabled
        String mcpContext = "";
        String mcpUrl = orchestrator.getMcpServerUrl();
        if (mcpUrl != null && !mcpUrl.isEmpty()) {
            try {
                McpClient mcpClient = new McpClient(mcpUrl);
                mcpClient.initialize();
                mcpContext = "\nMCP Local Context: " + mcpClient.listResources();
            } catch (Exception e) {
                // Log but continue
            }
        }

        String projectRootPath = context.getProjectRoot() != null ? context.getProjectRoot().getAbsolutePath() : "Unknown";

        StringBuilder sb = new StringBuilder();
        sb.append("You are acting as a ").append(type).append(" Agent.\n");
        sb.append("PROJECT ROOT: ").append(projectRootPath).append("\n");

        String memory = context.getSharedMemory();
        if (memory != null && !memory.isEmpty()) {
            sb.append("\n--- SHARED MEMORY (HISTORY) ---\n");
            sb.append(memory).append("\n");
            sb.append("--- END SHARED MEMORY ---\n");
        }

        if (!mcpContext.isEmpty()) {
            sb.append(mcpContext).append("\n");
        }

        sb.append("\nINSTRUCTIONS:\n").append(getAgentInstructions()).append("\n");

        // Best Practices
        String bp = bestPracticesService.getPractices(type);
        if (bp != null && !bp.isEmpty()) {
            sb.append("\n--- BEST PRACTICES ---\n");
            sb.append(bp).append("\n");
            sb.append("--- END BEST PRACTICES ---\n");
        }

        // Neuron Context
        String nc = neuronContextService.getContextPromptSnippet();
        if (nc != null && !nc.isEmpty()) {
            sb.append(nc).append("\n");
        }

        // External Instruction Files
        List<String> extFiles = context.getInstructionFiles();
        if (extFiles != null && !extFiles.isEmpty()) {
            sb.append("\n--- EXTERNAL INSTRUCTIONS ---\n");
            for (String filePath : extFiles) {
                try {
                    String content = Files.readString(Paths.get(filePath));
                    sb.append("File: ").append(new File(filePath).getName()).append("\n");
                    sb.append(content).append("\n\n");
                } catch (Exception e) {
                    sb.append("Error reading instruction file: ").append(filePath).append(" (").append(e.getMessage()).append(")\n");
                }
            }
            sb.append("--- END EXTERNAL INSTRUCTIONS ---\n");
        }

        if (lastFeedback != null && !lastFeedback.isEmpty()) {
            sb.append("\n--- PREVIOUS ATTEMPT FAILED ---\n");
            sb.append("Feedback: ").append(lastFeedback).append("\n");
            sb.append("Please correct your approach based on this feedback.\n");
        }

        sb.append("\nCURRENT TASK:\n").append(taskDescription).append("\n");

        sb.append("\nFINAL DIRECTIVE:\n").append(getFooterInstructions());

        return sb.toString();
    }

    @Override
    public String process(String taskDescription, TaskContext context, String lastFeedback) throws Exception {
        Orchestrator orchestrator = context.getOrchestrator();
        String prompt = buildPrompt(taskDescription, context, lastFeedback);

        // Data Scrubbing if online
        if (!orchestrator.isOfflineMode()) {
            prompt = DataScrubber.scrub(prompt);
        }

        context.log("Agent [" + id + " (" + type + ")]: Processing task - " + taskDescription);

        // Routing via LlmRouter
        float temperature = 0.7f;
        if (orchestrator.getLlm() != null) {
            temperature = orchestrator.getLlm().getTemperature();
        }

        String proxyUrl = (orchestrator.getAiChat() != null) ? orchestrator.getAiChat().getProxyUrl() : null;
        String response = llmRouter.sendRequest(orchestrator, prompt, temperature, proxyUrl, context);

        return cleanResponse(response);
    }

    protected String cleanResponse(String response) {
        String trimmed = response.trim();
        int firstBackticks = trimmed.indexOf("```");
        if (firstBackticks != -1) {
            int firstNewline = trimmed.indexOf("\n", firstBackticks);
            int lastBackticks = trimmed.lastIndexOf("```");
            if (firstNewline != -1 && lastBackticks > firstNewline) {
                return trimmed.substring(firstNewline + 1, lastBackticks).trim();
            }
        }
        return trimmed;
    }
}
