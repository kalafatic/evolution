package eu.kalafatic.evolution.controller.agents;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.controller.orchestration.AiService;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.llm.LlmRouter;
import eu.kalafatic.evolution.controller.orchestration.mcp.McpClient;
import eu.kalafatic.evolution.controller.orchestration.util.DataScrubber;
import eu.kalafatic.evolution.controller.orchestration.util.EvolutionConstants;
import eu.kalafatic.evolution.controller.services.BestPracticesService;
import eu.kalafatic.evolution.controller.services.NeuronContextService;
import eu.kalafatic.evolution.controller.tools.ITool;
import eu.kalafatic.evolution.controller.orchestration.ContextBuilder;

/**
 * Base AI Agent that wraps existing AI model/chat code.
 */
public abstract class BaseAiAgent implements IAgent {
    protected final String id;
    protected final String type;
    protected final List<ITool> tools = new ArrayList<>();
    protected final LlmRouter llmRouter = new LlmRouter();
    
    protected AiService aiService = new AiService();
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
        if (tool != null) {
            this.tools.add(tool);
        }
    }

    public void setAiService(AiService aiService) {
        this.aiService = aiService;
    }

    protected String buildPrompt(String request, TaskContext context, String lastFeedback) {
        return ContextBuilder.buildStrategicPrompt(type, getAgentInstructions(), getFooterInstructions(), request, context, lastFeedback);
    }

    protected abstract String getAgentInstructions();

    @Override
    public String process(String request, TaskContext context, String lastFeedback) throws Exception {
        String prompt = buildPrompt(request, context, lastFeedback);
        return aiService.sendRequest(context.getOrchestrator(), prompt, context);
    }

    protected String getFooterInstructions() { return null; }

    protected String extractContent(String response) {
        if (response == null) return "";
        String trimmed = response.trim();
        if (trimmed.startsWith("```")) {
            int firstBackticks = trimmed.indexOf("```");
            int firstNewline = trimmed.indexOf("\n", firstBackticks);
            int lastBackticks = trimmed.lastIndexOf("```");
            if (firstNewline != -1 && lastBackticks > firstNewline) {
                return trimmed.substring(firstNewline + 1, lastBackticks).trim();
            }
        }
        return trimmed;
    }
}
