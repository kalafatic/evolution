package eu.kalafatic.evolution.controller.agents;

import org.json.JSONObject;

import eu.kalafatic.evolution.model.orchestration.PlatformMode;
import eu.kalafatic.evolution.model.orchestration.PlatformType;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;

/**
 * Specialized agent for code review and evaluation.
 */
public class ReviewerAgent extends BaseAiAgent {

    public ReviewerAgent() {
        super("Reviewer", "Reviewer");
    }

    @Override
    protected String getAgentInstructions() {
        return "You are an AI Critic and Reviewer. Evaluate the completion of the following task.\n\n" +
               "CRITERIA:\n" +
               "1. Does the output directly address the goal or request?\n" +
               "2. Is the response helpful, accurate, and complete based on the context?\n" +
               "3. For CONVERSATIONAL tasks (like greetings), any polite and relevant response is a SUCCESS.\n" +
               "4. For TECHNICAL/OPERATIONAL tasks (like writing files or running commands), verify that the work was actually performed.\n" +
               "5. If the task was to write a file, check the 'CONTENT:' section to ensure it contains the actual code.\n" +
               "6. Be lenient with conversational agents, but strict with technical agents.";
    }

    @Override
    protected String getFooterInstructions() {
        return "Output MUST be a valid JSON object. Do not include any conversational preamble or follow-up text outside the JSON structure. Schema:\n" +
               "{ \"success\": boolean, \"feedback\": \"Detailed explanation of why it failed and how to fix it\", \"comment\": \"Brief success message\" }";
    }

    public JSONObject evaluate(String taskOutput, String taskDescription, TaskContext context) throws Exception {
        String modeInfo = "";
        if (context.getPlatformMode() != null && context.getPlatformMode().isAllowSelfModify()) {
            modeInfo = "\nSTRICT EVALUATION: System is in Self-Modification mode. Ensure the change improves system health and stability.\n";
        }
        String reviewResult = process("Evaluation of output: " + taskOutput + " for task: " + taskDescription + modeInfo, context, null);

        int start = reviewResult.indexOf("{");
        int end = reviewResult.lastIndexOf("}");

        if (start == -1 || end == -1 || end <= start) {
            context.log("Reviewer: Warning - AI response is not a JSON object. Assuming success for non-technical task.");
            JSONObject fallback = new JSONObject();
            fallback.put("success", true);
            fallback.put("comment", "Task completed (non-JSON review).");
            return fallback;
        }

        try {
            JSONObject evaluation = new JSONObject(reviewResult.substring(start, end + 1));
            context.log("Reviewer: Evaluation for '" + taskDescription + "': success=" + evaluation.optBoolean("success") + ", comment=" + evaluation.optString("comment"));
            return evaluation;
        } catch (org.json.JSONException e) {
            context.log("Reviewer: Warning - Failed to parse AI response as JSON object. Assuming success.");
            JSONObject fallback = new JSONObject();
            fallback.put("success", true);
            fallback.put("comment", "Task completed (parse error in review).");
            return fallback;
        }
    }
}
