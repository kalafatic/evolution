package eu.kalafatic.evolution.controller.agents;

import org.json.JSONObject;
import eu.kalafatic.evolution.controller.orchestration.ChangeUnit;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;

/**
 * Unified Validator role that merges responsibilities of ReviewerAgent and ConstraintAgent.
 * Delegates internally to preserve existing implementations.
 *
 * @evo:21:A reason=unified-validator-role
 */
public class ValidatorAgent extends BaseAiAgent {
    private final ReviewerAgent reviewer = new ReviewerAgent();
    private final ConstraintAgent constraintAgent = new ConstraintAgent();

    public ValidatorAgent() {
        super("Validator", "Validator");
    }

    @Override
    public void setAiService(eu.kalafatic.evolution.controller.orchestration.AiService aiService) {
        super.setAiService(aiService);
        reviewer.setAiService(aiService);
        constraintAgent.setAiService(aiService);
    }

    @Override
    protected String getAgentInstructions() {
        return "You are a unified Validator. Your goal is to ensure that task outputs are both correct AND follow architectural guardrails.";
    }

    public JSONObject evaluate(ChangeUnit change, String taskName, TaskContext context) throws Exception {
        context.log("Validator: Starting unified evaluation for " + taskName);
        String result = change.getPatch();

        // 1. Review completion
        JSONObject reviewEval = reviewer.evaluate(result, taskName, context);
        if (!reviewEval.optBoolean("success", false)) {
            context.log("Validator: Reviewer failed. Reason: " + reviewEval.optString("feedback"));
            return reviewEval;
        }

        // 2. Verify constraints
        JSONObject constraintEval = constraintAgent.evaluate(result, taskName, context);
        if (!constraintEval.optBoolean("success", false)) {
            context.log("Validator: Constraint violation detected. Reason: " + constraintEval.optString("feedback"));
            return constraintEval;
        }

        context.log("Validator: Both review and constraints passed.");
        return reviewEval; // return success with comment
    }
}
