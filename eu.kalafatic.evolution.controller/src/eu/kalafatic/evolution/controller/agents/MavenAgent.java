package eu.kalafatic.evolution.controller.agents;

import eu.kalafatic.evolution.controller.tools.MavenTool;

/**
 * Agent specialized in Maven operations.
 */
public class MavenAgent extends BaseAiAgent {
    public MavenAgent() {
        super("Maven", "Maven");
        addTool(new MavenTool());
    }

    @Override
    protected String getAgentInstructions() {
        return "You are an AI Maven Agent. You manage Java project builds.\n" +
               "Use the MavenTool to run goals like clean, compile, test, or install.";
    }
}
