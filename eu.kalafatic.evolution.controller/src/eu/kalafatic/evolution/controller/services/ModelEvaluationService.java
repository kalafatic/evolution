package eu.kalafatic.evolution.controller.services;

import org.json.JSONObject;
import eu.kalafatic.evolution.controller.orchestration.AiService;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.model.orchestration.AIProvider;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

/**
 * Service to evaluate model performance across Analysis, Chat, and Programming categories.
 */
public class ModelEvaluationService {

    private final AiService aiService = new AiService();

    private static final String PROMPT_ANALYZE = "Analyze the pros and cons of microservices vs monolithic architecture. Be concise.";
    private static final String PROMPT_CHAT = "Tell me a joke and then explain why it's funny in a friendly tone.";
    private static final String PROMPT_PROGRAMMING = "Write a Java function to calculate the Nth Fibonacci number using iteration.";

    private static final String EVALUATION_CRITERIA =
            "Evaluate the following AI response on a scale of 1 to 10 for the given category. " +
            "Category: %s. " +
            "Response: %s. " +
            "Criteria: Accuracy, tone, and correctness. " +
            "Output MUST be a valid JSON object with a single field 'rating' (integer 1-10).";

    public void evaluateModel(Orchestrator orchestrator, AIProvider provider, TaskContext context) throws Exception {
        context.log("Starting evaluation for model: " + provider.getName());

        // Save original remote model to restore it later if we are changing it for testing
        String originalRemoteModel = orchestrator.getRemoteModel();
        orchestrator.setRemoteModel(provider.getName());

        try {
            // 1. Analyze
            int analyzeRating = runTest(orchestrator, PROMPT_ANALYZE, "Analysis", context);
            provider.setRatingAnalyze(analyzeRating);
            context.log("Analysis rating: " + analyzeRating);

            // 2. Chat
            int chatRating = runTest(orchestrator, PROMPT_CHAT, "Chat", context);
            provider.setRatingChat(chatRating);
            context.log("Chat rating: " + chatRating);

            // 3. Programming
            int progRating = runTest(orchestrator, PROMPT_PROGRAMMING, "Programming", context);
            provider.setRatingProgramming(progRating);
            context.log("Programming rating: " + progRating);

            // Calculate overall rating (average)
            provider.setRating((analyzeRating + chatRating + progRating) / 3);

        } finally {
            orchestrator.setRemoteModel(originalRemoteModel);
        }
    }

    private int runTest(Orchestrator orchestrator, String testPrompt, String category, TaskContext context) throws Exception {
        String response = aiService.sendRequest(orchestrator, testPrompt, context);

        String evalPrompt = String.format(EVALUATION_CRITERIA, category, response);

        // Use a generic model for evaluation to be objective, or the same model if only one available
        // For simplicity, we use the currently configured "orchestrator" model (which we set to the provider being tested)
        String evalResponse = aiService.sendRequest(orchestrator, evalPrompt, context);

        return parseRating(evalResponse);
    }

    private int parseRating(String evalResponse) {
        try {
            int start = evalResponse.indexOf("{");
            int end = evalResponse.lastIndexOf("}");
            if (start != -1 && end != -1) {
                JSONObject json = new JSONObject(evalResponse.substring(start, end + 1));
                int rating = json.optInt("rating", 5);
                return Math.max(1, Math.min(10, rating));
            }
        } catch (Exception e) {
            // Fallback: try to find a digit in the text
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\b([1-9]|10)\\b").matcher(evalResponse);
            if (m.find()) {
                return Integer.parseInt(m.group(1));
            }
        }
        return 5; // Default middle rating if everything fails
    }
}
