package eu.kalafatic.evolution.controller.agents;

import eu.kalafatic.evolution.controller.tools.MavenTool;
import eu.kalafatic.evolution.controller.tools.ShellTool;

/**
 * Agent specialized in Quality Assurance and Linting.
 */
public class QualityAgent extends BaseAiAgent {
    public QualityAgent() {
        super("Quality", "Quality");
        addTool(new MavenTool());
        addTool(new ShellTool());
    }

    @Override
    protected String getAgentInstructions() {
        return "You are an AI Quality Agent. You focus on code quality, compliance, and linting.\n" +
               "Use Checkstyle, Linter tools, or Maven reports to ensure the codebase follows established standards.";
    }
}
