package eu.kalafatic.evolution.controller.orchestration;

/**
 * Specialized agent for C and C++ development tasks.
 */
public class CppDevAgent extends BaseAiAgent {
    public CppDevAgent() {
        super("CppDev", "CppDev");
        addTool(new FileTool());
        addTool(new CppTool());
        addTool(new GitTool());
        addTool(new ShellTool());
    }

    @Override
    public String process(String taskDescription, TaskContext context, String lastFeedback) throws Exception {
        String prompt = "You are acting as a Senior C/C++ Developer Agent.\n" +
                "Project Context: " + context.getSharedMemory() + "\n";

        if (lastFeedback != null && !lastFeedback.isEmpty()) {
            prompt += "PREVIOUS ATTEMPT FAILED. Feedback: " + lastFeedback + "\nPlease correct your approach.\n";
        }

        prompt += "Current C/C++ Task: " + taskDescription + "\n" +
                "Generate C/C++ source code, headers, Makefiles or CMakeLists.txt content as requested. Provide ONLY the code content for files.";

        context.log("CppDev [" + id + "]: Generating C/C++ code for - " + taskDescription);
        return cleanResponse(aiService.sendRequest(context.getOrchestrator(), prompt, context));
    }
}
