package eu.kalafatic.evolution.controller.orchestration;

/**
 * Specialized agent for Java development tasks.
 */
public class JavaDevAgent extends BaseAiAgent {
    public JavaDevAgent() {
        super("JavaDev", "JavaDev");
        addTool(new FileTool());
        addTool(new MavenTool());
        addTool(new GitTool());
        addTool(new ShellTool());
        addTool(new EclipseTool());
    }

    @Override
    protected String getAgentInstructions() {
        return "You are acting as a Senior Java Developer Agent.\n" +
               "Generate Java source code or Maven POM content as requested. Provide ONLY the code content for files.";
    }
}
