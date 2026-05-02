package eu.kalafatic.evolution.controller.agents;

import eu.kalafatic.evolution.controller.orchestration.util.EvolutionConstants;
import eu.kalafatic.evolution.controller.tools.ToolFactory;

/**
 * Specialized agent for general reasoning and conversational tasks.
 */
public class GeneralAgent extends BaseAiAgent {
    public GeneralAgent() {
        super(EvolutionConstants.AGENT_GENERAL, EvolutionConstants.AGENT_GENERAL);
        addTool(ToolFactory.getTool(EvolutionConstants.TOOL_FILE));
        addTool(ToolFactory.getTool(EvolutionConstants.TOOL_SHELL));
    }

    @Override
    protected String getAgentInstructions() {
        return "You are acting as a General Assistant Agent.\n" +
               "Respond to the task appropriately based on the context. If it is a greeting, respond politely and briefly. " +
               "If it is a general question, provide a helpful answer.";
    }
}
