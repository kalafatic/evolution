package eu.kalafatic.evolution.controller.agents;

import eu.kalafatic.evolution.controller.tools.MavenTool;
import eu.kalafatic.evolution.controller.tools.ShellTool;

/**
 * Specialized agent for testing and validation.
 */
public class TesterAgent extends BaseAiAgent {
    public TesterAgent() {
        super("Tester", "Tester");
        addTool(new MavenTool());
        addTool(new ShellTool());
    }

    @Override
    protected String getAgentInstructions() {
        return "You are acting as a Quality Assurance and Test Engineer Agent.\n" +
               "Generate JUnit tests, or run Maven tests and analyze the output.";
    }
}
