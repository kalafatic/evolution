package eu.kalafatic.evolution.controller.agents;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

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
    private final ReviewerAgent reviewer;
    private final ConstraintAgent constraintAgent;

    public ValidatorAgent(eu.kalafatic.evolution.controller.orchestration.SessionContainer container) {
        super("Validator", "Validator", container);
        this.reviewer = new ReviewerAgent(container);
        this.constraintAgent = new ConstraintAgent(container);
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
