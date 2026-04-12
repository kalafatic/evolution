package eu.kalafatic.evolution.controller.agents;

import eu.kalafatic.evolution.controller.tools.FileTool;
import eu.kalafatic.evolution.controller.tools.ShellTool;

/**
 * Agent specialized in Project Structure and Analysis.
 */
public class StructureAgent extends BaseAiAgent {
    public StructureAgent() {
        super("Structure", "Structure");
        addTool(new ShellTool());
        addTool(new FileTool());
    }

    @Override
    protected String getAgentInstructions() {
        return "You are an AI Structure Agent. Your goal is to analyze the project's directory structure and codebase organization.\n" +
               "Use tools like ls-tree or file reading to understand and report on how the project is built.";
    }
}
