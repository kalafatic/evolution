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
        String prompt = "You are an AI Critic and Code Reviewer. Evaluate the completion of the following task.\n\n" +
                "OVERALL CONTEXT: " + context.getSharedMemory() + "\n" +
                "TASK DESCRIPTION: " + taskDescription + "\n\n" +
                "CRITERIA:\n" +
                "1. Does the output directly address the goal?\n" +
                "2. Is the output technically sound and complete?\n\n" +
                "Output MUST be a valid JSON object. Schema:\n" +
                "{ \"success\": boolean, \"feedback\": \"Detailed explanation of why it failed and how to fix it\", \"comment\": \"Brief success message\" }";

        context.log("Reviewer [" + id + "]: Reviewing task - " + taskDescription);
        return aiService.sendRequest(context.getOrchestrator(), prompt);
    }

    public JSONObject evaluate(String taskOutput, String taskDescription, TaskContext context) throws Exception {
        String reviewResult = process("Evaluation of output: " + taskOutput + " for task: " + taskDescription, context, null);

        int start = reviewResult.indexOf("{");
        int end = reviewResult.lastIndexOf("}");
        if (start == -1 || end == -1 || end <= start) {
            throw new Exception("Reviewer failed to return a valid JSON object. Response: " + reviewResult);
        }
        return new JSONObject(reviewResult.substring(start, end + 1));
    }
}
