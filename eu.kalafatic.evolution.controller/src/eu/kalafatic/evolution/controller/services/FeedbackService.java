package eu.kalafatic.evolution.controller.services;

import org.json.JSONObject;

import eu.kalafatic.evolution.controller.log.Log;
import eu.kalafatic.evolution.model.orchestration.AIProvider;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

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

    /**
     * Records usage for a given AI provider.
     * @param orchestrator The orchestrator instance.
     * @param modelName The name of the model being used.
     */
    public void recordUsage(Orchestrator orchestrator, String modelName) {
        if (orchestrator == null || modelName == null || modelName.isEmpty()) return;

        orchestrator.getAiProviders().stream()
            .filter(p -> modelName.equals(p.getName()))
            .findFirst()
            .ifPresentOrElse(
                provider -> updateProviderUsageAndRating(orchestrator, provider),
                () -> {
                    // Create and add provider if not already present
                    try {
                        AIProvider newProvider = eu.kalafatic.evolution.model.orchestration.OrchestrationFactory.eINSTANCE.createAIProvider();
                        newProvider.setName(modelName);
                        newProvider.setLocal(true);

                        eu.kalafatic.evolution.controller.providers.ProviderConfig config = eu.kalafatic.evolution.controller.providers.AiProviders.PROVIDERS.get(modelName.toLowerCase());
                        if (config != null) {
                            newProvider.setLocal(false);
                            newProvider.setUrl(config.getEndpointUrl());
                            newProvider.setFormat(config.getFormat());
                        } else {
                            String ollamaUrl = (orchestrator.getOllama() != null) ? orchestrator.getOllama().getUrl() : "http://localhost:11434";
                            newProvider.setUrl(ollamaUrl);
                            newProvider.setFormat("ollama");
                        }
                        orchestrator.getAiProviders().add(newProvider);
                        updateProviderUsageAndRating(orchestrator, newProvider);
                    } catch (Exception e) {
                        Log.log(this, e);
                    }
                }
            );
    }

    private void updateProviderUsageAndRating(Orchestrator orchestrator, AIProvider provider) {
        try {
            String stateDesc = provider.getStateDescription();
            JSONObject meta = new JSONObject();
            if (stateDesc != null && stateDesc.startsWith("{")) {
                meta = new JSONObject(stateDesc);
            }

            int usageCount = meta.optInt("usageCount", 0);
            usageCount++;
            meta.put("usageCount", usageCount);

            provider.setStateDescription(meta.toString());

            // Improve ratings: the more used, the higher the ratings
            int currentRating = provider.getRating();
            if (currentRating == 0) {
                currentRating = 50; // default base
            }
            provider.setRating(Math.min(100, currentRating + 1));

            int currentChat = provider.getRatingChat();
            if (currentChat == 0) currentChat = 50;
            provider.setRatingChat(Math.min(100, currentChat + 1));

            int currentProg = provider.getRatingProgramming();
            if (currentProg == 0) currentProg = 50;
            provider.setRatingProgramming(Math.min(100, currentProg + 1));

            int currentAnalyze = provider.getRatingAnalyze();
            if (currentAnalyze == 0) currentAnalyze = 50;
            provider.setRatingAnalyze(Math.min(100, currentAnalyze + 1));

        } catch (Exception e) {
            Log.log(this, e);
        }
    }
}
