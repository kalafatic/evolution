package eu.kalafatic.evolution.controller.agents;

import eu.kalafatic.evolution.controller.orchestration.TaskContext;

/**
 * Specialized agent for consolidating and deduplicating proposals or clarification requests.
 */
public class ProposalConsolidatorAgent extends BaseAiAgent {

    public ProposalConsolidatorAgent() {
        super("Consolidator", "Consolidator");
    }

    @Override
    protected String getAgentInstructions() {
        return "Goal: Merge multiple proposals into a clear, deduplicated final proposal list for user approval.\n\n" +
               "Input:\n" +
               "* Multiple proposals from different agents (may overlap or duplicate)\n\n" +
               "Tasks:\n" +
               "1. Group similar proposals together\n" +
               "2. Remove duplicates\n" +
               "3. Merge into clear actions\n" +
               "4. Keep it short and concrete\n\n" +
               "Rules:\n" +
               "* No long explanations\n" +
               "* No duplicates\n" +
               "* No agent names\n" +
               "* Focus on decisions, not discussion\n" +
               "* Optional: Add [HIGH / MEDIUM / LOW impact]\n" +
               "* If there are conflicts: clearly highlight them\n";
    }

    @Override
    protected String getFooterInstructions() {
        return "Output format:\n\n" +
               "FINAL PROPOSAL:\n\n" +
               "1. <Action title>\n" +
               "   - key change 1\n" +
               "   - key change 2\n\n" +
               "2. <Action title>\n" +
               "   - key change 1\n";
    }

    public String consolidate(String proposals, TaskContext context) throws Exception {
        return process("PROPOSALS TO CONSOLIDATE:\n\n" + proposals, context, null);
    }
}
