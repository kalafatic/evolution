package eu.kalafatic.evolution.controller.agents;

import eu.kalafatic.evolution.controller.tools.FileTool;
import eu.kalafatic.evolution.controller.tools.ShellTool;

/**
 * Specialized agent for project structure and design.
 */
public class ArchitectAgent extends BaseAiAgent {
    public ArchitectAgent() {
        super("Architect", "Architect");
        addTool(new FileTool());
        addTool(new ShellTool());
    }

    /**
     * @evo:21:A reason=design-model-support
     */
    public void updateDesignModel(eu.kalafatic.evolution.controller.orchestration.TaskContext context, String designJson) {
        context.getOrchestrator().setSharedMemory(designJson);
    }

    @Override
    protected String getAgentInstructions() {
        return "You are acting as an Architect Agent.\n" +
               "Analyze the project request and provide a detailed architecture design, resource allocation plan, or file structure.\n" +
               "Your goal is to:\n" +
               "1. Analyze technical requirements and constraints.\n" +
               "2. Propose necessary agents (e.g., JavaDev, Tester, Reviewer) and tools (e.g., Maven, Git) required for the task.\n" +
               "3. Define a clear project structure or execution strategy.\n" +
               "4. ALWAYS provide a JSON representation of the architecture using the following format:\n" +
               "   [PROPOSAL:DESIGN {\"architecture\": {\"name\": \"...\", \"components\": [{\"name\": \"...\", \"type\": \"...\", \"x\": 100, \"y\": 100}], \"relationships\": [{\"from\": \"...\", \"to\": \"...\", \"type\": \"...\"}]}}]\n" +
               "If the request is a simple greeting (e.g., 'hi', 'hello'), just acknowledge it politely and briefly. DO NOT trigger architecture or project planning logic for greetings.\n" +
               "If you need to create a project structure, output file paths and detailed descriptions of each component.";
    }
}
