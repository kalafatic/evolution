package eu.kalafatic.evolution.controller.agents;

/**
 * Specialized agent for general reasoning and conversational tasks.
 */
public class GeneralAgent extends BaseAiAgent {
    public GeneralAgent(eu.kalafatic.evolution.controller.orchestration.SessionContainer container) {
        super("General", "General", container);
    }


    @Override
    protected String getAgentInstructions() {
        return "You are acting as a General Assistant Agent.\n" +
               "Respond to the task appropriately based on the context. If it is a greeting, respond politely and briefly. " +
               "If it is a general question, provide a helpful answer.";
    }
}
