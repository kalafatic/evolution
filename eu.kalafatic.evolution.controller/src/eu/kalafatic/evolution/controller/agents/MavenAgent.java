package eu.kalafatic.evolution.controller.agents;

import eu.kalafatic.evolution.controller.orchestration.util.EvolutionConstants;
import eu.kalafatic.evolution.controller.tools.ToolFactory;

/**
 * Agent specialized in Maven operations.
 */
public class MavenAgent extends BaseAiAgent {
    public MavenAgent() {
        super("Maven", "Maven");
    }

    public MavenAgent(eu.kalafatic.evolution.controller.orchestration.SessionContainer container) {
        super("Maven", "Maven", container);
        addTool(ToolFactory.getTool(EvolutionConstants.TOOL_MAVEN));
    }

    @Override
    protected String getAgentInstructions() {
        return "You are an AI Maven Agent. You manage Java project builds.\n" +
               "Use the MavenTool to run goals like clean, compile, test, or install.";
    }
}
