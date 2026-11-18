package eu.kalafatic.evolution.controller.cognitive.model;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Request for context reconstruction. Identity fields (applicationId,
 * userId) are hints only - the authoritative principal comes from the
 * authenticated MCP token or the internal caller. Access scopes limit which
 * data sources may contribute.
 */
public class ContextRequest {
    public static final String SCHEMA_VERSION = "1.0";

    private String applicationId;
    private String userId;
    private String project;
    private String sessionId;
    private String taskId;
    private String intent;
    private int budgetTokens = 12000;
    private List<String> categories = new ArrayList<>();
    private long timeWindowFrom;
    private long timeWindowTo;
    private List<String> accessScope = new ArrayList<>();

    public String getApplicationId() { return applicationId; }
    public void setApplicationId(String applicationId) { this.applicationId = applicationId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getProject() { return project; }
    public void setProject(String project) { this.project = project; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }
    public String getIntent() { return intent; }
    public void setIntent(String intent) { this.intent = intent; }
    public int getBudgetTokens() { return budgetTokens; }
    public void setBudgetTokens(int budgetTokens) { this.budgetTokens = budgetTokens; }
    public List<String> getCategories() { return categories; }
    public void setCategories(List<String> categories) { this.categories = categories != null ? categories : new ArrayList<>(); }
    public long getTimeWindowFrom() { return timeWindowFrom; }
    public void setTimeWindowFrom(long timeWindowFrom) { this.timeWindowFrom = timeWindowFrom; }
    public long getTimeWindowTo() { return timeWindowTo; }
    public void setTimeWindowTo(long timeWindowTo) { this.timeWindowTo = timeWindowTo; }
    public List<String> getAccessScope() { return accessScope; }
    public void setAccessScope(List<String> accessScope) { this.accessScope = accessScope != null ? accessScope : new ArrayList<>(); }

    public boolean wantsCategory(String category) {
        if (categories == null || categories.isEmpty()) {
            return true;
        }
        for (String c : categories) {
            if (c != null && c.equalsIgnoreCase(category)) {
                return true;
            }
        }
        return false;
    }

    public JSONObject toJsonObject() {
        JSONObject json = new JSONObject();
        json.put("schemaVersion", SCHEMA_VERSION);
        if (applicationId != null) json.put("applicationId", applicationId);
        if (userId != null) json.put("userId", userId);
        if (project != null) json.put("project", project);
        if (sessionId != null) json.put("sessionId", sessionId);
        if (taskId != null) json.put("taskId", taskId);
        if (intent != null) json.put("intent", intent);
        json.put("budgetTokens", budgetTokens);
        if (!categories.isEmpty()) {
            JSONArray arr = new JSONArray();
            for (String c : categories) arr.put(c);
            json.put("categories", arr);
        }
        if (timeWindowFrom > 0 || timeWindowTo > 0) {
            JSONObject window = new JSONObject();
            window.put("from", timeWindowFrom);
            window.put("to", timeWindowTo);
            json.put("timeWindow", window);
        }
        if (!accessScope.isEmpty()) {
            JSONArray arr = new JSONArray();
            for (String s : accessScope) arr.put(s);
            json.put("accessScope", arr);
        }
        return json;
    }

    public static ContextRequest fromJsonObject(JSONObject json) {
        if (json == null) return null;
        ContextRequest req = new ContextRequest();
        if (json.has("applicationId")) req.setApplicationId(json.optString("applicationId", null));
        if (json.has("userId")) req.setUserId(json.optString("userId", null));
        if (json.has("project")) req.setProject(json.optString("project", null));
        if (json.has("sessionId")) req.setSessionId(json.optString("sessionId", null));
        if (json.has("taskId")) req.setTaskId(json.optString("taskId", null));
        if (json.has("intent")) req.setIntent(json.optString("intent", null));
        if (json.has("budgetTokens")) req.setBudgetTokens(json.optInt("budgetTokens", 12000));
        if (json.has("categories")) {
            JSONArray arr = json.getJSONArray("categories");
            List<String> categories = new ArrayList<>();
            for (int i = 0; i < arr.length(); i++) categories.add(arr.getString(i));
            req.setCategories(categories);
        }
        if (json.has("timeWindow")) {
            JSONObject window = json.getJSONObject("timeWindow");
            if (window.has("from")) req.setTimeWindowFrom(window.optLong("from", 0));
            if (window.has("to")) req.setTimeWindowTo(window.optLong("to", 0));
        }
        if (json.has("accessScope")) {
            JSONArray arr = json.getJSONArray("accessScope");
            List<String> scopes = new ArrayList<>();
            for (int i = 0; i < arr.length(); i++) scopes.add(arr.getString(i));
            req.setAccessScope(scopes);
        }
        return req;
    }
}
