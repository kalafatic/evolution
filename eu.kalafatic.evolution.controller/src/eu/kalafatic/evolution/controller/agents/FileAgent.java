package eu.kalafatic.evolution.controller.agents;

import eu.kalafatic.evolution.controller.orchestration.util.EvolutionConstants;
import eu.kalafatic.evolution.controller.tools.ToolFactory;

/**
 * Agent specialized in File operations.
 */
public class FileAgent extends BaseAiAgent {
    public FileAgent() {
        super("File", "File");
    }

    public FileAgent(eu.kalafatic.evolution.controller.orchestration.SessionContainer container) {
        super("File", "File", container);
        addTool(ToolFactory.getTool(EvolutionConstants.TOOL_FILE));
    }

    @Override
    protected String getAgentInstructions() {
        return "You are an AI File Agent. Your primary task is to generate full content for files.\n" +
               "CRITICAL: Return ONLY the content for the file, preferably in a single markdown code block (e.g. ```java ... ```).\n" +
               "Do NOT attempt to use tool commands like 'WRITE' or explain what you are doing. Your output will be processed directly as the file content.";
    }
}
