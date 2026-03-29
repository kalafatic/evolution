package eu.kalafatic.evolution.controller.orchestration;

/**
 * Specialized agent for Java development tasks.
 */
public class JavaDevAgent extends BaseAiAgent {
    public JavaDevAgent() {
        super("JavaDev", "JavaDev");
        addTool(new FileTool());
        addTool(new MavenTool());
        addTool(new GitTool());
        addTool(new ShellTool());
    }

    @Override
    public String process(String taskDescription, TaskContext context, String lastFeedback) throws Exception {
        String mcpContext = getMcpContext(context);

        String prompt = "You are acting as a Senior Java Developer Agent.\n" +
                "Project Context: " + context.getSharedMemory() + "\n" +
                mcpContext + "\n";

        if (lastFeedback != null && !lastFeedback.isEmpty()) {
            prompt += "PREVIOUS ATTEMPT FAILED. Feedback: " + lastFeedback + "\nPlease correct your approach.\n";
        }

        prompt += "Current Java Task: " + taskDescription + "\n" +
                "Generate Java source code or Maven POM content as requested. Provide ONLY the code content for files.";

        context.log("JavaDev [" + id + "]: Generating Java code for - " + taskDescription);
        return cleanResponse(sendRequest(context, prompt));
    }
}
