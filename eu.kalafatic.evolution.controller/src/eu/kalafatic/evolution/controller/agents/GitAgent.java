package eu.kalafatic.evolution.controller.agents;

import eu.kalafatic.evolution.controller.tools.GitTool;

/**
 * Agent specialized in Git operations.
 */
public class GitAgent extends BaseAiAgent {
    public GitAgent() {
        super("Git", "Git");
        addTool(new GitTool());
    }

    @Override
    protected String getAgentInstructions() {
        return "You are an AI Git Agent. You handle version control tasks.\n" +
               "Use the GitTool to perform operations like clone, commit, push, or branch management.";
    }
}
