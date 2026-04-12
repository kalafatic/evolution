package eu.kalafatic.evolution.controller.orchestration;

/**
 * Agent specialized in File operations.
 */
public class FileAgent extends BaseAiAgent {
    public FileAgent() {
        super("File", "File");
        addTool(new FileTool());
    }

    @Override
    protected String getAgentInstructions() {
        return "You are an AI File Agent. Your primary task is to manage files and directories.\n" +
               "Use the FileTool to read, write, or delete files as requested.";
    }
}
