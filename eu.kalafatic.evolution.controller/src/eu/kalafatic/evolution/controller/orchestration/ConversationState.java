package eu.kalafatic.evolution.controller.orchestration;

import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

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

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("goal", goal);
        json.put("active_task", activeTask);
        json.put("history_summary", historySummary);
        json.put("last_messages", new JSONArray(lastMessages));
        json.put("open_questions", new JSONArray(openQuestions));
        json.put("decisions", new JSONArray(decisions));
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

            JSONArray msgs = json.optJSONArray("last_messages");
            if (msgs != null) {
                for (int i = 0; i < msgs.length(); i++) state.lastMessages.add(msgs.getString(i));
            }

            JSONArray ques = json.optJSONArray("open_questions");
            if (ques != null) {
                for (int i = 0; i < ques.length(); i++) state.openQuestions.add(ques.getString(i));
            }

            JSONArray decs = json.optJSONArray("decisions");
            if (decs != null) {
                for (int i = 0; i < decs.length(); i++) state.decisions.add(decs.getString(i));
            }
        } catch (Exception e) {
            // Log error and return empty state
        }
        return state;
    }

    /**
     * Loads the state for a specific thread from shared memory.
     */
    public static ConversationState load(String sharedMemory, String threadId) {
        if (sharedMemory == null || sharedMemory.isEmpty()) return new ConversationState();
        try {
            JSONObject allStates = new JSONObject(sharedMemory);
            if (allStates.has(threadId)) {
                return fromJSON(allStates.getJSONObject(threadId).toString());
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
    public static String save(String sharedMemory, String threadId, ConversationState state) {
        JSONObject allStates;
        try {
            allStates = new JSONObject(sharedMemory);
        } catch (Exception e) {
            allStates = new JSONObject();
        }
        allStates.put(threadId, state.toJSON());
        return allStates.toString();
    }
}
