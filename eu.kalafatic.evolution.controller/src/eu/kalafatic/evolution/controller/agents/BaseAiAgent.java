package eu.kalafatic.evolution.controller.agents;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.controller.orchestration.AiService;
import eu.kalafatic.evolution.controller.orchestration.ConversationState;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.intent.ConfirmedRequirements;
import eu.kalafatic.evolution.controller.orchestration.llm.LlmRouter;
import eu.kalafatic.evolution.controller.orchestration.mcp.McpClient;
import eu.kalafatic.evolution.controller.orchestration.attachments.AttachmentInjector;
import eu.kalafatic.evolution.controller.orchestration.util.DataScrubber;
import eu.kalafatic.evolution.controller.orchestration.util.EvolutionConstants;
import eu.kalafatic.evolution.controller.services.BestPracticesService;
import eu.kalafatic.evolution.controller.services.NeuronContextService;
import eu.kalafatic.evolution.controller.tools.ITool;

/**
 * Base AI Agent that wraps existing AI model/chat code.
 */
import eu.kalafatic.evolution.controller.orchestration.IOrchestrationFlow;
import eu.kalafatic.evolution.controller.orchestration.OrchestratorResponse;
import eu.kalafatic.evolution.controller.orchestration.ResultType;
import eu.kalafatic.evolution.controller.orchestration.SystemState;

/**
 * Base AI Agent that wraps existing AI model/chat code.
 */
public abstract class BaseAiAgent implements IAgent, IOrchestrationFlow {
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
        StringBuilder sb = new StringBuilder();
        sb.append("Role: ").append(type).append("\n");
        if (context.getProjectRoot() != null) {
            sb.append("PROJECT ROOT: ").append(context.getProjectRoot().getAbsolutePath()).append("\n\n");
        }

        if (bestPracticesService == null && context.getProjectRoot() != null) {
            bestPracticesService = new BestPracticesService(context.getOrchestrator(), context.getProjectRoot());
        }

        if (bestPracticesService != null) {
            if (context.getOrchestrator().getAiChat() != null && context.getOrchestrator().getAiChat().getPromptInstructions() != null) {
                if (context.getOrchestrator().getAiChat().getPromptInstructions().isIterativeMode()) {
                    sb.append("--- ITERATIVE LOOP CONTEXT ---\n");
                    sb.append(bestPracticesService.getSpecialContext("iterative_loop.md")).append("\n\n");
                }
                if (context.getOrchestrator().getAiChat().getPromptInstructions().isSelfIterativeMode()) {
                    sb.append("--- SELF DEVELOPMENT CONTEXT ---\n");
                    sb.append(bestPracticesService.getSpecialContext("self_development.md")).append("\n\n");
                }
            }
        }

        sb.append("INSTRUCTIONS:\n").append(getAgentInstructions()).append("\n\n");

        ConversationState state = ConversationState.load(context.getSharedMemory(), context.getSessionId());
        ConfirmedRequirements frozen = state.getConfirmedRequirements();
        if (frozen != null) {
            sb.append("### MANDATORY FROZEN REQUIREMENTS (DO NOT DEVIATE) ###\n");
            sb.append(frozen.toString()).append("\n\n");
        }

        if (lastFeedback != null) {
            sb.append("### PREVIOUS FEEDBACK (FAILURE RECOVERY)\n").append(lastFeedback).append("\n\n");
        }

        // Context Authority: All grounding and attachment logic MUST flow through ContextBuilder.
        // Direct injection in agents is deprecated to ensure a single authoritative scoring path.

        sb.append("CURRENT TASK:\n").append(request).append("\n\n");

        String footer = getFooterInstructions();
        if (footer != null) sb.append("FINAL DIRECTIVE:\n").append(footer);

        return sb.toString();
    }

    protected abstract String getAgentInstructions();

    @Override
    public String process(String request, TaskContext context, String lastFeedback) throws Exception {
        String prompt = buildPrompt(request, context, lastFeedback);
        return aiService.sendRequest(context.getOrchestrator(), prompt, context);
    }

    @Override
    public OrchestratorResponse execute(String request, TaskContext context) throws Exception {
        OrchestratorResponse response = new OrchestratorResponse();
        response.setResultType(ResultType.CHAT);

        String result = process(request, context, null);

        ConversationState convState = ConversationState.load(context.getSharedMemory(), context.getSessionId());
        convState.addMessage("Evo: " + result);
        context.getOrchestrator().setSharedMemory(ConversationState.save(context.getSharedMemory(), context.getSessionId(), convState));

        response.setSummary(result);
        response.setContent(result);
        return response;
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
