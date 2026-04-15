package eu.kalafatic.evolution.controller.orchestration;

import org.json.JSONObject;

/**
 * Interface for routing decisions based on classified intent.
 */
public interface IPolicyEngine {
    /**
     * Determines the next action (respond directly, clarify, or plan) based on classification.
     * @param classification The intent classification results.
     * @param input The original user input.
     * @param context The shared task context.
     * @return A direct string response if action is blocked, or null if planning is allowed.
     * @throws Exception if evaluation fails.
     */
    String evaluate(JSONObject classification, String input, TaskContext context) throws Exception;
}
