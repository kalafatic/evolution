package eu.kalafatic.evolution.controller.orchestration;

import org.json.JSONObject;

/**
 * Interface for intent classification before planning.
 */
public interface IIntentClassifier {
    /**
     * Classifies user input into GREETING, CHIT_CHAT, QUESTION, ACTION_REQUEST, AMBIGUOUS, or SYSTEM_COMMAND.
     * @param input The raw user input string.
     * @param context The shared task context.
     * @return A JSON object containing intent, confidence, requires_action, requires_clarification, and reason.
     * @throws Exception if classification fails.
     */
    JSONObject classify(String input, TaskContext context) throws Exception;
}
