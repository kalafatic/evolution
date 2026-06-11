package eu.kalafatic.evolution.controller.orchestration;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

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
