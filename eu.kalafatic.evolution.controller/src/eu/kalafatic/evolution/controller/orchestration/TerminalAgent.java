package eu.kalafatic.evolution.controller.orchestration;

/**
 * Agent specialized in Terminal operations.
 */
public class TerminalAgent extends BaseAiAgent {
    public TerminalAgent() {
        super("Terminal", "Terminal");
        addTool(new ShellTool());
    }

    @Override
    protected String getAgentInstructions() {
        return "You are an AI Terminal Agent. You can execute shell commands.\n" +
               "Use the ShellTool to perform actions like running scripts, checking system status, or managing local services.";
    }
}
