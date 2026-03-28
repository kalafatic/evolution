package eu.kalafatic.evolution.controller.orchestration;

import java.util.ArrayList;
import java.util.List;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

/**
 * Base AI Agent that wraps existing AI model/chat code.
 */
public abstract class BaseAiAgent implements IAgent {
    protected final String id;
    protected final String type;
    protected final List<ITool> tools = new ArrayList<>();
    protected final AiService aiService = new AiService();

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
        String prompt = "You are acting as a " + type + " Agent.\n" +
                        "Overall Context: " + context.getSharedMemory() + "\n";

        if (lastFeedback != null && !lastFeedback.isEmpty()) {
            prompt += "PREVIOUS ATTEMPT FAILED. Feedback: " + lastFeedback + "\nPlease correct your approach.\n";
        }

        prompt += "Current Task: " + taskDescription + "\n" +
                  "Based on the context and the task, provide your response.";

        context.log("Agent [" + id + " (" + type + ")]: Processing task - " + taskDescription);
        String response = aiService.sendRequest(context.getOrchestrator(), prompt);

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
