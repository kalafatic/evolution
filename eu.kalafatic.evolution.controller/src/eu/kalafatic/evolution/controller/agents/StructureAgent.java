package eu.kalafatic.evolution.controller.agents;

import eu.kalafatic.evolution.controller.tools.FileTool;
import eu.kalafatic.evolution.controller.tools.ShellTool;

/**
 * Agent for analyzing project structure.
 */
public class StructureAgent extends BaseAiAgent {
    public StructureAgent(eu.kalafatic.evolution.controller.orchestration.SessionContainer container) {
        super("Structure", "Structure", container);
        addTool(new FileTool());
        addTool(new ShellTool());
    }

    @Override
    protected String getAgentInstructions() {
        return "You are an AI Structure Agent. Your goal is to analyze and report on the project's file and architectural structure.";
    }
}
