package eu.kalafatic.evolution.controller.agents;

import eu.kalafatic.evolution.controller.tools.EclipseTool;
import eu.kalafatic.evolution.controller.tools.FileTool;
import eu.kalafatic.evolution.controller.tools.GitTool;
import eu.kalafatic.evolution.controller.tools.MavenTool;
import eu.kalafatic.evolution.controller.tools.ShellTool;

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
               "Generate Java source code or Maven POM content as requested.\n" +
               "Evo-Way: When generating a plan for a coding task, you should include the full code implementation in the 'implementation' field of the JSON response if the task is straightforward. This allows for immediate execution without a second reasoning step.\n" +
               "IMPORTANT: Always look at the SHARED MEMORY for overall requirements and context to ensure your code matches the user's intent.";
    }

    @Override
    protected String getFooterInstructions() {
        return "Provide ONLY the code content for files, without any conversational preamble or markdown backticks unless specifically required for a file's format.";
    }
}
