package eu.kalafatic.evolution.controller.agents;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import eu.kalafatic.evolution.controller.orchestration.AiService;
import eu.kalafatic.evolution.controller.orchestration.ConversationState;
import eu.kalafatic.evolution.controller.orchestration.IOrchestrationFlow;
import eu.kalafatic.evolution.controller.orchestration.OrchestratorResponse;
import eu.kalafatic.evolution.controller.orchestration.PlatformType;
import eu.kalafatic.evolution.controller.orchestration.ResultType;
import eu.kalafatic.evolution.controller.orchestration.SessionContainer;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.intent.ConfirmedRequirements;
import eu.kalafatic.evolution.controller.orchestration.llm.LlmRouter;
import eu.kalafatic.evolution.controller.orchestration.util.CodeExtractor;
import eu.kalafatic.evolution.controller.services.BestPracticesService;
import eu.kalafatic.evolution.controller.services.NeuronContextService;
import eu.kalafatic.evolution.controller.tools.ITool;
import eu.kalafatic.evolution.controller.workflow.EvolutionEvent.EELType;

import org.json.JSONArray;
import org.json.JSONObject;


/**
 * Base AI Agent that wraps existing AI model/chat code.
 */
public abstract class BaseAiAgent implements IAgent, IOrchestrationFlow {
    protected final String id;
    protected final String type;
    protected String instructions;
    protected final List<ITool> tools = new ArrayList<>();
    protected final LlmRouter llmRouter = new LlmRouter();
    protected final SessionContainer sessionContainer;
    
    protected AiService aiService = new AiService();
    protected BestPracticesService bestPracticesService;
    protected NeuronContextService neuronContextService;
    

    
    public BaseAiAgent(String id, String type, SessionContainer container) {
        if (container == null) {
            throw new IllegalArgumentException("BaseAiAgent [" + id + "]: SessionContainer cannot be null. Explicit session context injection is mandatory.");
        }
        this.id = id;
        this.type = type;
        this.sessionContainer = container;
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

    public SessionContainer getSessionContainer() {
        return sessionContainer;
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

        BestPracticesService bp = new BestPracticesService(context.getOrchestrator(), context.getProjectRoot());
        String practices = bp.getCombinedPractices();
        if (practices != null && !practices.isEmpty()) {
            sb.append(practices).append("\n\n");
        }

        if (context.getOrchestrator().getAiChat() != null && context.getOrchestrator().getAiChat().getPromptInstructions() != null) {
            if (context.getOrchestrator().getAiChat().getPromptInstructions().isIterativeMode()) {
                sb.append("--- ITERATIVE LOOP CONTEXT ---\n");
                sb.append(bp.getSpecialContext("iterative_loop.md")).append("\n\n");
            }
            if (context.getOrchestrator().getAiChat().getPromptInstructions().isSelfIterativeMode()) {
                sb.append("--- SELF DEVELOPMENT CONTEXT ---\n");
                sb.append(bp.getSpecialContext("self_development.md")).append("\n\n");
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

        // Persist current cognitive state snapshot with history
        eu.kalafatic.evolution.controller.orchestration.SessionContainer session = eu.kalafatic.evolution.controller.orchestration.SessionManager.getInstance().getSession(context.getSessionId());
        if (session != null) {
            convState.setCognitiveState(session.getCognitiveState());
        }

        context.getOrchestrator().setSharedMemory(ConversationState.save(context.getSharedMemory(), context.getSessionId(), convState));

        response.setSummary(result);
        response.setContent(result);
        return response;
    }
    
    protected JSONObject extractJson(String response) {
        if (response == null || response.isEmpty()) {
            return null;
        }
        
        // Try to find JSON in code blocks first
        Pattern codeBlockPattern = Pattern.compile("```(?:json)?\\s*\\n?(.*?)\\n?```", Pattern.DOTALL);
        Matcher matcher = codeBlockPattern.matcher(response);
        if (matcher.find()) {
            String jsonStr = matcher.group(1).trim();
            try {
                return new JSONObject(jsonStr);
            } catch (Exception e) {
                // Fall through to try other methods
            }
        }
        
        // Try to find JSON object directly - more robust pattern
        Pattern jsonPattern = Pattern.compile("\\{[^{}]*(?:\\{[^{}]*\\}[^{}]*)*\\}");
        matcher = jsonPattern.matcher(response);
        if (matcher.find()) {
            String jsonStr = matcher.group();
            try {
                return new JSONObject(jsonStr);
            } catch (Exception e) {
                // Fall through
            }
        }
        
        // Try to parse the whole response as JSON
        try {
            return new JSONObject(response.trim());
        } catch (Exception e) {
            // Fall through
        }
        
        // Last resort: try to extract with a more lenient pattern
        try {
            // Look for anything that looks like key-value pairs
            Pattern kvPattern = Pattern.compile("\"([^\"]+)\"\\s*:\\s*\"([^\"]*)\"");
            matcher = kvPattern.matcher(response);
            if (matcher.find()) {
                JSONObject obj = new JSONObject();
                matcher.reset();
                while (matcher.find()) {
                    obj.put(matcher.group(1), matcher.group(2));
                }
                if (obj.length() > 0) {
                    return obj;
                }
            }
        } catch (Exception e) {
            // Give up
        }
        
        return null;
    }

    protected String getFooterInstructions() { return null; }

    protected String extractContent(String response) {
        return CodeExtractor.extractCode(response);
    }

	public AiService getAiService() {
		return aiService;
	}

}
