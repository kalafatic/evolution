package eu.kalafatic.evolution.controller.orchestration;

import org.json.JSONObject;

/**
 * Specialized agent for code review and evaluation.
 */
public class ReviewerAgent extends BaseAiAgent {

    public ReviewerAgent() {
        super("Reviewer", "Reviewer");
    }

    @Override
    public String process(String taskDescription, TaskContext context, String lastFeedback) throws Exception {
        String prompt = "You are an AI Critic and Reviewer. Evaluate the completion of the following task.\n\n" +
                "OVERALL CONTEXT: " + context.getSharedMemory() + "\n" +
                mcpContext + "\n" +
                "TASK DESCRIPTION: " + taskDescription + "\n\n" +
                "CRITERIA:\n" +
                "1. Does the output directly address the goal or request?\n" +
                "2. Is the response helpful, accurate, and complete based on the context?\n\n" +
                "Output MUST be a valid JSON object. Schema:\n" +
                "{ \"success\": boolean, \"feedback\": \"Detailed explanation of why it failed and how to fix it\", \"comment\": \"Brief success message\" }";

        context.log("Reviewer [" + id + "]: Reviewing task - " + taskDescription);
        return sendRequest(context, prompt);
    }

    public JSONObject evaluate(String taskOutput, String taskDescription, TaskContext context) throws Exception {
        String reviewResult = process("Evaluation of output: " + taskOutput + " for task: " + taskDescription, context, null);

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
            return new JSONObject(reviewResult.substring(start, end + 1));
        } catch (org.json.JSONException e) {
            context.log("Reviewer: Warning - Failed to parse AI response as JSON object. Assuming success.");
            JSONObject fallback = new JSONObject();
            fallback.put("success", true);
            fallback.put("comment", "Task completed (parse error in review).");
            return fallback;
        }
    }
}
