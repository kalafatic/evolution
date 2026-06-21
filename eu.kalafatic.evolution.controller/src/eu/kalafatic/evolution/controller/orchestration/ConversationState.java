package eu.kalafatic.evolution.controller.orchestration;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import eu.kalafatic.evolution.controller.orchestration.cognitive.CapabilityType;
import eu.kalafatic.evolution.controller.orchestration.cognitive.CognitiveDirection;
import eu.kalafatic.evolution.controller.orchestration.cognitive.SessionCognitiveState;
import eu.kalafatic.evolution.controller.orchestration.cognitive.SessionIntent;
import eu.kalafatic.evolution.controller.orchestration.intent.ConfirmedRequirements;

/**
 * Structured conversation state to maintain persistent intent across turns.
 * Supports thread-scoped storage in shared memory.
 */
public class ConversationState {
    private String goal = "";
    private String activeTask = "";
    private String historySummary = "";
    private List<String> lastMessages = new ArrayList<>();
    private List<String> openQuestions = new ArrayList<>();
    private List<String> decisions = new ArrayList<>();
    private List<String> pendingQuestions = new ArrayList<>();
    private List<String> clarificationHistory = new ArrayList<>();
    private boolean isRequirementMet = true;
    private ConfirmedRequirements confirmedRequirements;
    private SessionCognitiveState cognitiveState;
    private JSONObject metadata = new JSONObject();

    public String getGoal() { return goal; }
    public void setGoal(String goal) { this.goal = goal; }

    public String getActiveTask() { return activeTask; }
    public void setActiveTask(String activeTask) { this.activeTask = activeTask; }

    public String getHistorySummary() { return historySummary; }
    public void setHistorySummary(String historySummary) { this.historySummary = historySummary; }

    public List<String> getLastMessages() { return lastMessages; }
    public void addMessage(String message) {
        lastMessages.add(message);
        if (lastMessages.size() > 5) lastMessages.remove(0);
    }

    public List<String> getOpenQuestions() { return openQuestions; }
    public void setOpenQuestions(List<String> questions) { this.openQuestions = questions; }

    public List<String> getDecisions() { return decisions; }
    public void addDecision(String decision) { this.decisions.add(decision); }

    public List<String> getPendingQuestions() { return pendingQuestions; }
    public void setPendingQuestions(List<String> questions) { this.pendingQuestions = questions; }

    public List<String> getClarificationHistory() { return clarificationHistory; }
    public void addClarification(String clarification) { this.clarificationHistory.add(clarification); }

    public boolean isRequirementMet() { return isRequirementMet; }
    public void setRequirementMet(boolean met) { this.isRequirementMet = met; }

    public ConfirmedRequirements getConfirmedRequirements() { return confirmedRequirements; }
    public void setConfirmedRequirements(ConfirmedRequirements reqs) { this.confirmedRequirements = reqs; }

    public SessionCognitiveState getCognitiveState() { return cognitiveState; }
    public void setCognitiveState(SessionCognitiveState cognitiveState) { this.cognitiveState = cognitiveState; }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("goal", goal);
        json.put("active_task", activeTask);
        json.put("history_summary", historySummary);
        json.put("last_messages", new JSONArray(lastMessages));
        json.put("open_questions", new JSONArray(openQuestions));
        json.put("decisions", new JSONArray(decisions));
        json.put("pending_questions", new JSONArray(pendingQuestions));
        json.put("clarification_history", new JSONArray(clarificationHistory));
        json.put("is_requirement_met", isRequirementMet);
        if (confirmedRequirements != null) {
            json.put("confirmed_requirements", confirmedRequirements.toJSON());
        }
        if (cognitiveState != null) {
            JSONObject cog = new JSONObject();
            cog.put("capability", cognitiveState.getCurrentCapability().name());
            cog.put("intent", cognitiveState.getCurrentIntent().name());
            cog.put("direction", cognitiveState.getCurrentDirection().name());
            cog.put("confidence", cognitiveState.getConfidence());
            cog.put("depth", cognitiveState.getCognitiveDepth());
            cog.put("velocity", cognitiveState.getVelocity());
            cog.put("acceleration", cognitiveState.getAcceleration());
            cog.put("dominant_trend", cognitiveState.getDominantTrend().name());
            cog.put("trend_stability", cognitiveState.getTrendStability());

            JSONArray scores = new JSONArray();
            cognitiveState.getCapabilityScores().forEach((k, v) -> {
                JSONObject s = new JSONObject();
                s.put("type", k.name());
                s.put("score", v);
                scores.put(s);
            });
            cog.put("scores", scores);

            JSONArray traj = new JSONArray();
            cognitiveState.getTrajectory().forEach(t -> traj.put(t.name()));
            cog.put("trajectory", traj);

            JSONArray history = new JSONArray();
            cognitiveState.getCapabilityHistory().forEach(s -> {
                JSONObject sig = new JSONObject();
                sig.put("capability", s.getCapability().name());
                sig.put("weight", s.getWeight());
                sig.put("intent", s.getIntent().name());
                sig.put("source", s.getSource());
                history.put(sig);
            });
            cog.put("history", history);

            json.put("cognitive_state", cog);
        }
        json.put("metadata", metadata);
        return json;
    }

    public static ConversationState fromJSON(String jsonStr) {
        ConversationState state = new ConversationState();
        if (jsonStr == null || jsonStr.isEmpty()) return state;
        try {
            JSONObject json = new JSONObject(jsonStr);
            state.setGoal(json.optString("goal", ""));
            state.setActiveTask(json.optString("active_task", ""));
            state.setHistorySummary(json.optString("history_summary", ""));

            state.lastMessages.addAll(eu.kalafatic.evolution.controller.parsers.JsonUtils.toStringList(json.optJSONArray("last_messages")));
            state.openQuestions.addAll(eu.kalafatic.evolution.controller.parsers.JsonUtils.toStringList(json.optJSONArray("open_questions")));
            state.decisions.addAll(eu.kalafatic.evolution.controller.parsers.JsonUtils.toStringList(json.optJSONArray("decisions")));
            state.pendingQuestions.addAll(eu.kalafatic.evolution.controller.parsers.JsonUtils.toStringList(json.optJSONArray("pending_questions")));
            state.clarificationHistory.addAll(eu.kalafatic.evolution.controller.parsers.JsonUtils.toStringList(json.optJSONArray("clarification_history")));

            state.setRequirementMet(json.optBoolean("is_requirement_met", true));
            if (json.has("confirmed_requirements")) {
                state.setConfirmedRequirements(ConfirmedRequirements.fromJSON(json.getJSONObject("confirmed_requirements")));
            }
            if (json.has("cognitive_state")) {
                JSONObject cog = json.getJSONObject("cognitive_state");
                SessionCognitiveState scs = new SessionCognitiveState();
                scs.setCurrentCapability(CapabilityType.valueOf(cog.optString("capability", "CHAT")));
                scs.setCurrentIntent(SessionIntent.valueOf(cog.optString("intent", "LEARNING")));
                scs.setCurrentDirection(CognitiveDirection.valueOf(cog.optString("direction", "STABLE")));
                scs.setConfidence(cog.optDouble("confidence", 1.0));
                scs.setCognitiveDepth(cog.optInt("depth", 1));
                scs.setVelocity(cog.optDouble("velocity", 0.0));
                scs.setAcceleration(cog.optDouble("acceleration", 0.0));
                scs.setDominantTrend(CapabilityType.valueOf(cog.optString("dominant_trend", "CHAT")));
                scs.setTrendStability(cog.optDouble("trend_stability", 1.0));

                JSONArray scores = cog.optJSONArray("scores");
                if (scores != null) {
                    for (int i = 0; i < scores.length(); i++) {
                        JSONObject s = scores.getJSONObject(i);
                        scs.getCapabilityScores().put(CapabilityType.valueOf(s.getString("type")), s.getDouble("score"));
                    }
                }

                JSONArray traj = cog.optJSONArray("trajectory");
                if (traj != null) {
                    List<CapabilityType> trajectoryList = new ArrayList<>();
                    for (int i = 0; i < traj.length(); i++) {
                        trajectoryList.add(CapabilityType.valueOf(traj.getString(i)));
                    }
                    scs.setTrajectory(trajectoryList);
                }

                JSONArray history = cog.optJSONArray("history");
                if (history != null) {
                    for (int i = 0; i < history.length(); i++) {
                        JSONObject sig = history.getJSONObject(i);
                        scs.addSignal(new eu.kalafatic.evolution.controller.orchestration.cognitive.CapabilitySignal(
                            CapabilityType.valueOf(sig.getString("capability")),
                            sig.getDouble("weight"),
                            1.0, // default confidence
                            SessionIntent.valueOf(sig.getString("intent")),
                            null, // no evidence list from JSON
                            sig.getString("source")
                        ));
                    }
                }
                state.setCognitiveState(scs);
            }
            if (json.has("metadata")) {
                state.metadata = json.getJSONObject("metadata");
            }
        } catch (Exception e) {
            // Log error and return empty state
        }
        return state;
    }

    /**
     * Loads the state for a specific thread from shared memory.
     */
    public static ConversationState load(String sharedMemory, String sessionId) {
        if (sharedMemory == null || sharedMemory.isEmpty()) return new ConversationState();
        try {
            JSONObject allStates = new JSONObject(sharedMemory);
            if (allStates.has(sessionId)) {
                return fromJSON(allStates.getJSONObject(sessionId).toString());
            }
        } catch (Exception e) {
            // If sharedMemory is not a JSON object, it might be the old raw format
            // In that case, we return an empty state or try to parse it if it looks like JSON
            if (sharedMemory.trim().startsWith("{")) {
                return fromJSON(sharedMemory);
            }
        }
        return new ConversationState();
    }

    /**
     * Saves the current state for a specific thread into shared memory.
     */
    public static String save(String sharedMemory, String sessionId, ConversationState state) {
        JSONObject allStates;
        try {
            allStates = new JSONObject(sharedMemory);
        } catch (Exception e) {
            allStates = new JSONObject();
        }
        allStates.put(sessionId, state.toJSON());
        return allStates.toString();
    }

    public static String save(String sharedMemory, String sessionId, String key, String value) {
        JSONObject allStates;
        try {
            allStates = new JSONObject(sharedMemory);
        } catch (Exception e) {
            allStates = new JSONObject();
        }

        JSONObject sessionObj = allStates.optJSONObject(sessionId);
        if (sessionObj == null) {
            sessionObj = new JSONObject();
            allStates.put(sessionId, sessionObj);
        }

        JSONObject metadata = sessionObj.optJSONObject("metadata");
        if (metadata == null) {
            metadata = new JSONObject();
            sessionObj.put("metadata", metadata);
        }
        metadata.put(key, value);

        return allStates.toString();
    }

    public String getMetadata(String key) {
        return metadata.optString(key, null);
    }

    public void putMetadata(String key, String value) {
        metadata.put(key, value);
    }
}
