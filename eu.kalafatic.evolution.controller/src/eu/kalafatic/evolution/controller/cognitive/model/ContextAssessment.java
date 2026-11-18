package eu.kalafatic.evolution.controller.cognitive.model;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Quality assessment attached to every reconstructed ContextPackage.
 */
public class ContextAssessment {
    private String contextId;
    private double quality;
    private double completeness;
    private List<String> conflicts = new ArrayList<>();
    private List<String> staleItems = new ArrayList<>();
    private double budgetUtilization;
    private List<String> warnings = new ArrayList<>();

    public String getContextId() { return contextId; }
    public void setContextId(String contextId) { this.contextId = contextId; }
    public double getQuality() { return quality; }
    public void setQuality(double quality) { this.quality = quality; }
    public double getCompleteness() { return completeness; }
    public void setCompleteness(double completeness) { this.completeness = completeness; }
    public List<String> getConflicts() { return conflicts; }
    public void setConflicts(List<String> conflicts) { this.conflicts = conflicts != null ? conflicts : new ArrayList<>(); }
    public List<String> getStaleItems() { return staleItems; }
    public void setStaleItems(List<String> staleItems) { this.staleItems = staleItems != null ? staleItems : new ArrayList<>(); }
    public double getBudgetUtilization() { return budgetUtilization; }
    public void setBudgetUtilization(double budgetUtilization) { this.budgetUtilization = budgetUtilization; }
    public List<String> getWarnings() { return warnings; }
    public void setWarnings(List<String> warnings) { this.warnings = warnings != null ? warnings : new ArrayList<>(); }

    public JSONObject toJsonObject() {
        JSONObject json = new JSONObject();
        if (contextId != null) json.put("contextId", contextId);
        json.put("quality", quality);
        json.put("completeness", completeness);
        JSONArray conflictArr = new JSONArray();
        for (String c : conflicts) conflictArr.put(c);
        json.put("conflicts", conflictArr);
        JSONArray staleArr = new JSONArray();
        for (String s : staleItems) staleArr.put(s);
        json.put("staleItems", staleArr);
        json.put("budgetUtilization", budgetUtilization);
        JSONArray warnArr = new JSONArray();
        for (String w : warnings) warnArr.put(w);
        json.put("warnings", warnArr);
        return json;
    }

    public static ContextAssessment fromJsonObject(JSONObject json) {
        if (json == null) return null;
        ContextAssessment a = new ContextAssessment();
        if (json.has("contextId")) a.setContextId(json.getString("contextId"));
        if (json.has("quality")) a.setQuality(json.getDouble("quality"));
        if (json.has("completeness")) a.setCompleteness(json.getDouble("completeness"));
        if (json.has("conflicts")) {
            JSONArray arr = json.getJSONArray("conflicts");
            List<String> list = new ArrayList<>();
            for (int i = 0; i < arr.length(); i++) list.add(arr.getString(i));
            a.setConflicts(list);
        }
        if (json.has("staleItems")) {
            JSONArray arr = json.getJSONArray("staleItems");
            List<String> list = new ArrayList<>();
            for (int i = 0; i < arr.length(); i++) list.add(arr.getString(i));
            a.setStaleItems(list);
        }
        if (json.has("budgetUtilization")) a.setBudgetUtilization(json.getDouble("budgetUtilization"));
        if (json.has("warnings")) {
            JSONArray arr = json.getJSONArray("warnings");
            List<String> list = new ArrayList<>();
            for (int i = 0; i < arr.length(); i++) list.add(arr.getString(i));
            a.setWarnings(list);
        }
        return a;
    }
}
