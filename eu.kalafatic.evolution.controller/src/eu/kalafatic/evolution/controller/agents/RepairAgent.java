package eu.kalafatic.evolution.controller.agents;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

import eu.kalafatic.evolution.controller.tools.FileTool;

/**
 * Specialized agent for fixing code and configuration errors.
 */
public class RepairAgent extends BaseAiAgent {
    public RepairAgent(eu.kalafatic.evolution.controller.orchestration.SessionContainer container) {
        super("Repair", "Repair", container);
        addTool(new FileTool());
    }

    @Override
    protected String getAgentInstructions() {
        return "You are a specialized Code Repair Agent. Your goal is to identify and fix bugs, compilation errors, and configuration issues.";
    }
}
