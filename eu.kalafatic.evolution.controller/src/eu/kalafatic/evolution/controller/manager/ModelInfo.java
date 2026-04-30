package eu.kalafatic.evolution.controller.manager;

import eu.kalafatic.evolution.model.orchestration.AIProvider;

/**
 * Unified model information for all LLM providers (local and remote).
 */
public class ModelInfo {
    public enum ModelState { OK, ERR, NA }

    private ModelState state;
    private String stateDescription;
    private String name;
    private boolean local;
    private String pathOrUrl;
    private String token;
    private int rating;
    private int ratingAnalyze;
    private int ratingChat;
    private int ratingProgramming;
    private AIProvider provider;

    public ModelInfo(ModelState state, String name, boolean local, String pathOrUrl, String token) {
        this.state = state;
        this.name = name;
        this.local = local;
        this.pathOrUrl = pathOrUrl;
        this.token = token;
    }

    public boolean isHybrid() {
        if (name != null && name.toLowerCase().endsWith(":cloud")) return true;
        if (!local) {
            // Remote model without token is considered hybrid (ollama based big models - not entirely local)
            if (token == null || token.isEmpty() || "YOUR_API_KEY".equals(token)) {
                return true;
            }
        }
        return false;
    }

    // Getters and Setters
    public ModelState getState() { return state; }
    public void setState(ModelState state) { this.state = state; }

    public String getStateDescription() { return stateDescription; }
    public void setStateDescription(String stateDescription) { this.stateDescription = stateDescription; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public boolean isLocal() { return local; }
    public void setLocal(boolean local) { this.local = local; }

    public String getPathOrUrl() { return pathOrUrl; }
    public void setPathOrUrl(String pathOrUrl) { this.pathOrUrl = pathOrUrl; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }

    public int getRatingAnalyze() { return ratingAnalyze; }
    public void setRatingAnalyze(int ratingAnalyze) { this.ratingAnalyze = ratingAnalyze; }

    public int getRatingChat() { return ratingChat; }
    public void setRatingChat(int ratingChat) { this.ratingChat = ratingChat; }

    public int getRatingProgramming() { return ratingProgramming; }
    public void setRatingProgramming(int ratingProgramming) { this.ratingProgramming = ratingProgramming; }

    public AIProvider getProvider() { return provider; }
    public void setProvider(AIProvider provider) { this.provider = provider; }
}
