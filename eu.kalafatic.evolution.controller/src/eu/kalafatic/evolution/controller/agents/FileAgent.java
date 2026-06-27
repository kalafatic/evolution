package eu.kalafatic.evolution.controller.agents;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

import eu.kalafatic.evolution.controller.tools.FileTool;

/**
 * Agent specialized in file system operations.
 */
public class FileAgent extends BaseAiAgent {
    public FileAgent(eu.kalafatic.evolution.controller.orchestration.SessionContainer container) {
        super("File", "File", container);
        addTool(new FileTool());
    }

    @Override
    protected String getAgentInstructions() {
        return "You are an AI File Agent. You specialize in reading, writing, and managing files.";
    }
}
