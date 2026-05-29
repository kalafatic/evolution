package eu.kalafatic.evolution.controller.agents;

import eu.kalafatic.evolution.controller.orchestration.util.EvolutionConstants;
import eu.kalafatic.evolution.controller.tools.ToolFactory;

/**
 * Agent specialized in Git operations.
 */
public class GitAgent extends BaseAiAgent {
    public GitAgent() {
        super("Git", "Git");
    }

    public GitAgent(eu.kalafatic.evolution.controller.orchestration.SessionContainer container) {
        super("Git", "Git", container);
        addTool(ToolFactory.getTool(EvolutionConstants.TOOL_GIT));
    }

    @Override
    protected String getAgentInstructions() {
        return "You are an AI Git Agent. You handle version control tasks.\n" +
               "Use the GitTool to perform operations like clone, commit, push, or branch management.";
    }
}
