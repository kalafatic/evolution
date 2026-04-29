package eu.kalafatic.evolution.controller.agents;

import org.json.JSONObject;
import eu.kalafatic.evolution.controller.parsers.JsonUtils;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;

/**
 * Specialized agent for architectural constraint verification.
 * It ensures that proposed changes align with the DesignModel.
 */
public class ConstraintAgent extends BaseAiAgent {

    public ConstraintAgent() {
        super("Constraint", "Constraint");
    }

    @Override
    protected String getAgentInstructions() {
        return "You are an Architectural Constraint Agent. Your goal is to maintain system health and adherence to the design.\n\n" +
               "CRITERIA:\n" +
               "1. Verify proposed changes against the 'DesignModel' found in SHARED MEMORY.\n" +
               "2. Ensure no forbidden dependencies are introduced.\n" +
               "3. Ensure interface compliance and that components stay within their defined responsibilities.\n" +
               "4. Identify any violations of architectural patterns or best practices defined in the design.";
    }

    @Override
    protected String getFooterInstructions() {
        return "Output MUST be a valid JSON object. Do not include any conversational preamble or follow-up text outside the JSON structure. Schema:\n" +
               "{ \"success\": boolean, \"feedback\": \"Detailed explanation of architectural violations and how to fix them\", \"comment\": \"Brief compliance message\" }";
    }

    /**
     * Evaluates the task output against architectural constraints.
     */
    public JSONObject evaluate(String taskOutput, String taskDescription, TaskContext context) throws Exception {
        String designModel = context.getSharedMemory();
        if (designModel == null || designModel.isEmpty() || !designModel.contains("\"architecture\"")) {
            // No design model to check against, proceed with warning
            context.log("ConstraintAgent: No DesignModel found in shared memory. Skipping deep architectural check.");
            JSONObject success = new JSONObject();
            success.put("success", true);
            success.put("comment", "No architectural constraints defined.");
            return success;
        }

        String prompt = "Evaluate the following task output for architectural compliance:\n" +
                        "TASK: " + taskDescription + "\n" +
                        "OUTPUT: " + taskOutput + "\n\n" +
                        "Does this change violate any constraints defined in the DesignModel?";

        String reviewResult = process(prompt, context, null);
        JSONObject evaluation = JsonUtils.extractJsonObject(reviewResult);

        if (evaluation == null) {
            context.log("ConstraintAgent: Warning - AI response is not a JSON object. Assuming compliance.");
            JSONObject fallback = new JSONObject();
            fallback.put("success", true);
            fallback.put("comment", "Compliance check finished (non-JSON response).");
            return fallback;
        }

        context.log("ConstraintAgent: Compliance for '" + taskDescription + "': success=" + evaluation.optBoolean("success") + ", comment=" + evaluation.optString("comment"));
        return evaluation;
    }
}
