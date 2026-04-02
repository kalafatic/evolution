package eu.kalafatic.evolution.controller.orchestration;

/**
 * Specialized agent for general reasoning and conversational tasks.
 */
public class GeneralAgent extends BaseAiAgent {
    public GeneralAgent() {
        super("General", "General");
        addTool(new FileTool());
        addTool(new ShellTool());
    }

    @Override
    public String process(String taskDescription, TaskContext context, String lastFeedback) throws Exception {
        String prompt = "You are acting as a General Assistant Agent.\n" +
                "Context: " + context.getSharedMemory() + "\n";

        if (lastFeedback != null && !lastFeedback.isEmpty()) {
            prompt += "PREVIOUS ATTEMPT FAILED. Feedback: " + lastFeedback + "\nPlease correct your approach.\n";
        }

        prompt += "Current Task: " + taskDescription + "\n" +
                "Respond to the task appropriately based on the context. If it is a greeting, respond politely. If it is a general question, provide a helpful answer.";

        context.log("GeneralAgent [" + id + "]: Processing task - " + taskDescription);
        return cleanResponse(aiService.sendRequest(context.getOrchestrator(), prompt, context));
    }
}
