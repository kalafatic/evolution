package eu.kalafatic.evolution.controller.agents;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

import eu.kalafatic.evolution.controller.tools.FileTool;
import eu.kalafatic.evolution.controller.tools.MavenTool;

/**
 * Specialized agent for Java development tasks.
 */
public class JavaDevAgent extends BaseAiAgent {
    public JavaDevAgent(eu.kalafatic.evolution.controller.orchestration.SessionContainer container) {
        super("JavaDev", "JavaDev", container);
        addTool(new FileTool());
        addTool(new MavenTool());
    }

    @Override
    protected String getAgentInstructions() {
        return "You are a specialized Java Development Agent. Your goal is to write high-quality, maintainable Java code.";
    }
}
