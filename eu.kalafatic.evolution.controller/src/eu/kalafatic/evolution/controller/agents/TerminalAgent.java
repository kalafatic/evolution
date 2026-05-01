package eu.kalafatic.evolution.controller.agents;

import eu.kalafatic.evolution.controller.orchestration.util.EvolutionConstants;
import eu.kalafatic.evolution.controller.tools.ToolFactory;

/**
 * Agent specialized in Terminal operations.
 */
public class TerminalAgent extends BaseAiAgent {
    public TerminalAgent() {
        super(EvolutionConstants.AGENT_TERMINAL, EvolutionConstants.AGENT_TERMINAL);
        addTool(ToolFactory.getTool(EvolutionConstants.TOOL_SHELL));
    }

    @Override
    protected String getAgentInstructions() {
        return "You are an AI Terminal Agent. You can execute shell commands.\n" +
               "Use the ShellTool to perform actions like running scripts, checking system status, or managing local services.";
    }
}
