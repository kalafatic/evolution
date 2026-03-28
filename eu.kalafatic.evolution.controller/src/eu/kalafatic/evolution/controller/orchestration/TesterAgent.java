package eu.kalafatic.evolution.controller.orchestration;

/**
 * Specialized agent for testing and validation.
 */
public class TesterAgent extends BaseAiAgent {
    public TesterAgent() {
        super("Tester", "Tester");
        addTool(new MavenTool());
        addTool(new ShellTool());
    }

    @Override
    public String process(String taskDescription, TaskContext context, String lastFeedback) throws Exception {
        String prompt = "You are acting as a Quality Assurance and Test Engineer Agent.\n" +
                "Project Context: " + context.getSharedMemory() + "\n";

        if (lastFeedback != null && !lastFeedback.isEmpty()) {
            prompt += "PREVIOUS ATTEMPT FAILED. Feedback: " + lastFeedback + "\nPlease correct your approach.\n";
        }

        prompt += "Current Testing Task: " + taskDescription + "\n" +
                "Generate JUnit tests, or run Maven tests and analyze the output.";

        context.log("Tester [" + id + "]: Analyzing testing task - " + taskDescription);
        return cleanResponse(aiService.sendRequest(context.getOrchestrator(), prompt));
    }
}
