package eu.kalafatic.evolution.controller.agents;

import org.json.JSONObject;

import eu.kalafatic.evolution.controller.parsers.JsonUtils;
import eu.kalafatic.evolution.controller.orchestration.PlatformMode;
import eu.kalafatic.evolution.controller.orchestration.PlatformType;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.evolution.EvaluationSignal;
import eu.kalafatic.evolution.controller.orchestration.evolution.SignalSeverity;
import eu.kalafatic.evolution.controller.workflow.RuntimeEvent;
import eu.kalafatic.evolution.controller.workflow.RuntimeEventBus;
import eu.kalafatic.evolution.controller.workflow.RuntimeEventType;

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
               "1. Does the output directly address the goal or request? Stick STRICTLY to the user goal.\n" +
               "2. Is the response helpful, accurate, and complete based on the context? DO NOT hallucinate extra requirements (e.g., unnecessary return statements, extra imports) that weren't requested.\n" +
               "3. For CONVERSATIONAL tasks (like greetings), any polite and relevant response is a SUCCESS.\n" +
               "4. For TECHNICAL/OPERATIONAL tasks (like writing files or running commands), verify that the work was actually performed.\n" +
               "5. If the task was to write a file, check the 'CONTENT:' section to ensure it contains the actual code.\n" +
               "6. Be lenient with conversational agents, but strict with technical agents.";
    }

    @Override
    protected String getFooterInstructions() {
        return "CRITICAL: Output MUST be a single, valid JSON object. Do NOT include any conversational preamble, markdown blocks (```json), or follow-up text. ONLY the JSON object.\n" +
               "Schema:\n" +
               "{ \"success\": boolean, \"feedback\": \"Detailed explanation of why it failed and how to fix it\", \"comment\": \"Brief success message\" }";
    }

    public JSONObject evaluate(String taskOutput, String taskDescription, TaskContext context) throws Exception {
        String modeInfo = "";
        if (context.getPlatformMode() != null && context.getPlatformMode().isAllowSelfModify()) {
            modeInfo = "\nSTRICT EVALUATION: System is in Self-Modification mode. Ensure the change improves system health and stability.\n";
        }
        String reviewResult = process("Evaluation of output: " + taskOutput + " for task: " + taskDescription + modeInfo, context, null);

        // Use robust extraction
        JSONObject evaluation = JsonUtils.extractJsonObject(reviewResult);

        if (evaluation == null) {
            boolean isTechnical = taskDescription.toLowerCase().matches(".*(file|maven|git|shell|test|build|compile).*");

            if (isTechnical) {
                context.log("Reviewer: ERROR - AI response is not a JSON object for TECHNICAL task. Failing by default.");
                JSONObject failure = new JSONObject();
                failure.put("success", false);
                failure.put("feedback", "Reviewer failed to provide a valid JSON evaluation for a technical task. Response was: " + reviewResult);
                return failure;
            } else {
                context.log("Reviewer: Warning - AI response is not a JSON object. Assuming success for non-technical task.");
                JSONObject fallback = new JSONObject();
                fallback.put("success", true);
                fallback.put("comment", "Task completed (non-JSON review).");
                return fallback;
            }
        }

        context.log("Reviewer: Evaluation for '" + taskDescription + "': success=" + evaluation.optBoolean("success") + ", comment=" + evaluation.optString("comment"));

        // Emit Evaluation Signal
        emitSignal(evaluation, taskDescription, context);

        return evaluation;
    }

    private void emitSignal(JSONObject evaluation, String taskDescription, TaskContext context) {
        String variantId = context.getMetadata().getOrDefault("variantId", "unknown").toString();
        boolean success = evaluation.optBoolean("success", false);
        double score = success ? 1.0 : 0.0;
        SignalSeverity severity = success ? SignalSeverity.INFO : SignalSeverity.WARNING;
        String explanation = evaluation.optString("feedback", evaluation.optString("comment", "No explanation"));

        EvaluationSignal signal = new EvaluationSignal(
            variantId,
            "ReviewerAgent",
            score,
            0.8, // confidence estimate
            severity,
            explanation
        );

        RuntimeEventBus.getInstance().publish(new RuntimeEvent(
            RuntimeEventType.EVALUATION_SIGNAL_CREATED,
            context.getSessionId(),
            "ReviewerAgent",
            signal
        ).withMetadata("task", taskDescription));
    }
}
