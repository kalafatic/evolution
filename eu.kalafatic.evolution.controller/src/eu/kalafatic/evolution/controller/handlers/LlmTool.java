package eu.kalafatic.evolution.controller.handlers;

import org.eclipse.core.resources.IProject;
import eu.kalafatic.evolution.model.orchestration.Agent;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.Task;

public class LlmTool implements ITaskTool {
    @Override
    public boolean canHandle(String taskType) {
        // Default tool
        return "llm".equalsIgnoreCase(taskType) || taskType == null;
    }

    @Override
    public String execute(Orchestrator orchestrator, IProject project, Agent agent, Task task, String context, String lastFeedback, OrchestrationCommandHandler handler) throws Exception {
        String agentType = (agent != null) ? agent.getType() : "general assistant";
        String prompt = "You are acting as a " + agentType + ".\n" +
                "Context: " + context + "\n";
        if (lastFeedback != null) {
            prompt += "PREVIOUS ATTEMPT FAILED. Feedback: " + lastFeedback + "\nPlease correct your approach.\n";
        }
        prompt += "Your task: " + task.getName() + "\n" +
                "Provide your response below:";

        return handler.sendRequest(orchestrator, prompt, (orchestrator.getAiChat() != null) ? orchestrator.getAiChat().getProxyUrl() : null);
    }
}
