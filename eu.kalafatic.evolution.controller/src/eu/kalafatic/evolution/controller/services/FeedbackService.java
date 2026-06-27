package eu.kalafatic.evolution.controller.services;

import org.json.JSONObject;
import eu.kalafatic.evolution.model.orchestration.AIProvider;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.controller.log.Log;

/**
 * Service to handle user feedback, calculate averages, and persist metadata.
 */
public class FeedbackService {

    private static FeedbackService instance;

    public static synchronized FeedbackService getInstance() {
        if (instance == null) {
            instance = new FeedbackService();
        }
        return instance;
    }

    /**
     * Records feedback for the currently active AI provider.
     * @param orchestrator The orchestrator instance.
     * @param category The category (chat, coding, analyze).
     * @param rating The user rating (1-10).
     */
    public void recordFeedback(Orchestrator orchestrator, String category, int rating) {
        if (orchestrator == null) return;
        String remoteModel = orchestrator.getRemoteModel();
        if (remoteModel == null || remoteModel.isEmpty()) return;

        orchestrator.getAiProviders().stream()
            .filter(p -> remoteModel.equals(p.getName()))
            .findFirst()
            .ifPresent(provider -> updateProviderRating(orchestrator, provider, category, rating));
    }

    private void updateProviderRating(Orchestrator orchestrator, AIProvider provider, String category, int rating) {
        try {
            String stateDesc = provider.getStateDescription();
            JSONObject meta = new JSONObject();
            if (stateDesc != null && stateDesc.startsWith("{")) {
                meta = new JSONObject(stateDesc);
            }

            JSONObject stats = meta.optJSONObject("stats");
            if (stats == null) stats = new JSONObject();

            JSONObject catStats = stats.optJSONObject(category);
            if (catStats == null) catStats = new JSONObject();

            int count = catStats.optInt("count", 0);
            double currentAvg = catStats.optDouble("avg", 0.0);

            double newAvg = (currentAvg * count + rating) / (count + 1);
            int newCount = count + 1;

            catStats.put("count", newCount);
            catStats.put("avg", newAvg);
            stats.put(category, catStats);
            meta.put("stats", stats);

            // Persist basic mode info
            meta.put("lastMode", orchestrator.getAiMode().getName());
            meta.put("lastLLM", orchestrator.getRemoteModel());

            provider.setStateDescription(meta.toString());

            // Update model attributes for UI/Legacy compatibility
            int roundedAvg = (int) Math.round(newAvg);
            if ("chat".equalsIgnoreCase(category)) {
                provider.setRatingChat(roundedAvg);
            } else if ("coding".equalsIgnoreCase(category) || "programming".equalsIgnoreCase(category)) {
                provider.setRatingProgramming(roundedAvg);
            } else if ("analyze".equalsIgnoreCase(category)) {
                provider.setRatingAnalyze(roundedAvg);
            }

            // Update overall rating average
            int totalCount = 0;
            double totalWeightedSum = 0;
            java.util.Iterator<String> keys = stats.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                JSONObject s = stats.getJSONObject(key);
                int c = s.getInt("count");
                double a = s.getDouble("avg");
                totalWeightedSum += (a * c);
                totalCount += c;
            }
            if (totalCount > 0) {
                provider.setRating((int) Math.round(totalWeightedSum / totalCount));
            }

        } catch (Exception e) {
            Log.log(this, e);
        }
    }
}
