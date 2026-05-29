package eu.kalafatic.evolution.controller.orchestration.intent;

import eu.kalafatic.evolution.controller.agents.BaseAiAgent;
import eu.kalafatic.evolution.controller.orchestration.AiService;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.utils.semantic.EvolutionComponent;
import eu.kalafatic.utils.semantic.EvolutionaryImpact;
import eu.kalafatic.utils.semantic.Stability;

/**
 * Engine dedicated to resolving missing intent and grounding the task.
 */
@EvolutionComponent(
    domain = "intent",
    role = "clarification-authority",
    purpose = "Separates intent resolution from the evolutionary mutation loop",
    stability = Stability.STABLE,
    evolutionaryImpact = EvolutionaryImpact.MEDIUM
)
public class IntentClarificationEngine extends BaseAiAgent {

    public IntentClarificationEngine(AiService aiService) {
        super("IntentClarificationEngine", "IntentClarificationEngine");
        setAiService(aiService);
    }

    @Override
    protected String getAgentInstructions() {
        return "Role: Intent Clarification Engine.\n" +
               "Purpose: Resolve missing information and grounding requirements.\n" +
               "Focus: Intent understanding, not architectural evolution.";
    }

    public IntentExpansionResult clarify(String prompt, TaskContext context) throws Exception {
        context.log("[CLARIFICATION] Resolving initial intent grounding for: " + prompt);

        // This leverages the existing expansion logic but scoped strictly to clarification
        IntentExpansionEngine expansionEngine = new IntentExpansionEngine();
        expansionEngine.setAiService(this.aiService);

        return expansionEngine.expand(prompt, context);
    }
}
