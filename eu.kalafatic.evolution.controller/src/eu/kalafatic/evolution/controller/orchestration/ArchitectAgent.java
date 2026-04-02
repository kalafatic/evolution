package eu.kalafatic.evolution.controller.orchestration;

/**
 * Specialized agent for project structure and design.
 */
public class ArchitectAgent extends BaseAiAgent {
    public ArchitectAgent() {
        super("Architect", "Architect");
        addTool(new FileTool());
        addTool(new ShellTool());
    }

    @Override
    public String process(String taskDescription, TaskContext context, String lastFeedback) throws Exception {
        String prompt = "You are acting as an Architect Agent.\n" +
                "Project Context: " + context.getSharedMemory() + "\n";

        if (lastFeedback != null && !lastFeedback.isEmpty()) {
            prompt += "PREVIOUS ATTEMPT FAILED. Feedback: " + lastFeedback + "\nPlease correct your approach.\n";
        }

        prompt += "Current Architecture Task: " + taskDescription + "\n" +
                "Provide a detailed architecture design or file structure. If you need to create a project structure, output file paths and descriptions.";

        context.log("Architect [" + id + "]: Processing architecture task - " + taskDescription);
        return cleanResponse(aiService.sendRequest(context.getOrchestrator(), prompt, context));
    }
}
