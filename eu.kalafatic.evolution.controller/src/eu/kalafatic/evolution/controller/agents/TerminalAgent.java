package eu.kalafatic.evolution.controller.agents;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

import eu.kalafatic.evolution.controller.tools.ShellTool;

/**
 * Agent for general shell and terminal tasks.
 */
public class TerminalAgent extends BaseAiAgent {
    public TerminalAgent(eu.kalafatic.evolution.controller.orchestration.SessionContainer container) {
        super("Terminal", "Terminal", container);
        addTool(new ShellTool());
    }

    @Override
    protected String getAgentInstructions() {
        return "You are an AI Terminal Agent. You perform general shell commands and investigative tasks via the terminal.";
    }
}
