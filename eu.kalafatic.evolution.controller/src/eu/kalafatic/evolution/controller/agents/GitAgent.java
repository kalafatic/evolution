package eu.kalafatic.evolution.controller.agents;

import eu.kalafatic.evolution.controller.tools.GitTool;

/**
 * Agent specialized in Git version control operations.
 */
public class GitAgent extends BaseAiAgent {
    public GitAgent(eu.kalafatic.evolution.controller.orchestration.SessionContainer container) {
        super("Git", "Git", container);
        addTool(new GitTool());
    }

    @Override
    protected String getAgentInstructions() {
        return "You are an AI Git Agent. You handle version control tasks including branching, committing, and merging.";
    }
}
