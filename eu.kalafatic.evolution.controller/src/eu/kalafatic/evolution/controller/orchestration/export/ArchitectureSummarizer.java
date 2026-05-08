package eu.kalafatic.evolution.controller.orchestration.export;

import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.AiService;
import eu.kalafatic.evolution.controller.agents.ArchitectAgent;
import eu.kalafatic.evolution.controller.agents.AgentFactory;
import eu.kalafatic.evolution.controller.orchestration.util.EvolutionConstants;

/**
 * Generates a high-level description of system modules and relationships.
 */
public class ArchitectureSummarizer {
    private final ArchitectAgent architectAgent;

    public ArchitectureSummarizer() {
        this.architectAgent = (ArchitectAgent) AgentFactory.getAgent(EvolutionConstants.AGENT_ARCHITECT);
    }

    public String summarize(TaskContext context, AiService aiService) throws Exception {
        context.log("[EXPORT] Summarizing system architecture...");

        String instruction = "Perform a high-level analysis of the project structure and summarize: " +
                             "1. Main modules and their responsibilities. " +
                             "2. Key relationships and communication patterns. " +
                             "3. Any architectural constraints or notable design patterns. " +
                             "Return the summary in Markdown format.";

        architectAgent.setAiService(aiService);
        return architectAgent.process(instruction, context, null);
    }
}
