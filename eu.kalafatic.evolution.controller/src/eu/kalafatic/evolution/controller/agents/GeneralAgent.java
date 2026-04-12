package eu.kalafatic.evolution.controller.agents;

import eu.kalafatic.evolution.controller.tools.FileTool;
import eu.kalafatic.evolution.controller.tools.ShellTool;

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
    protected String getAgentInstructions() {
        return "You are acting as a General Assistant Agent.\n" +
               "Respond to the task appropriately based on the context. If it is a greeting, respond politely. " +
               "If it is a general question, provide a helpful answer.";
    }
}
