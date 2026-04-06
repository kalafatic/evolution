package eu.kalafatic.evolution.controller.orchestration;

/**
 * Specialized agent for project structure and design.
 */
public class ArchitectAgent extends BaseAiAgent {
    public ArchitectAgent() {
        super("Architect", "Architect");
        addTool(new FileTool());
        addTool(new ShellTool());
    }

    @Override
    protected String getAgentInstructions() {
        return "You are acting as an Architect Agent.\n" +
               "Provide a detailed architecture design or file structure. If you need to create a project structure, output file paths and descriptions.";
    }
}
