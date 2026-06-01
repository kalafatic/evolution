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

    public ArchitectureSummarizer(eu.kalafatic.evolution.controller.orchestration.SessionContainer sessionContainer) {
        this.architectAgent = (ArchitectAgent) AgentFactory.createIsolatedAgents(sessionContainer).stream().filter(a -> a.getType().equals(EvolutionConstants.AGENT_ARCHITECT)).findFirst().orElse(null);
    }

    public String summarize(TaskContext context, AiService aiService) throws Exception {
        context.log("[EXPORT] Summarizing system architecture...");

        String instruction = "Perform a deep high-level analysis of the project structure and summarize: " +
                             "1. Main modules (OSGi bundles) and their core responsibilities. " +
                             "2. Key relationships, communication patterns (e.g., state transitions, agent orchestration). " +
                             "3. Core architectural invariants and design patterns (e.g., deterministic kernel, intelligence isolation). " +
                             "4. Current technical debt or areas for improvement relevant to general development. " +
                             "Return the summary in professional Markdown format optimized for a frontier AI model's context window.";

        architectAgent.setAiService(aiService);
        return architectAgent.process(instruction, context, null);
    }
}
