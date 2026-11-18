package eu.kalafatic.evolution.controller.cognitive.model;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Result of context reconstruction: the platform context contract (schema 1.0).
 * Every section carries provenance, confidence and freshness so that consumers
 * can distinguish confirmed facts from historical or inferred information.
 */
public class ContextPackage {
    public static final String SCHEMA_VERSION = "1.0";

    private String contextId;
    private String schemaVersion = SCHEMA_VERSION;
    private JSONObject userContext;
    private JSONObject sessionContext;
    private JSONObject projectContext;
    private List<ContextItem> memories = new ArrayList<>();
    private List<JSONObject> decisions = new ArrayList<>();
    private List<JSONObject> knownIssues = new ArrayList<>();
    private List<ContextItem> procedures = new ArrayList<>();
    private JSONObject continuity;
    private List<ContextSource> sources = new ArrayList<>();
    private double confidence;
    private long freshness;
    private int tokenEstimate;
    private int budgetTokens;
    private List<String> warnings = new ArrayList<>();
    private List<String> missingInfo = new ArrayList<>();
    private ContextAssessment assessment;

    public String getContextId() { return contextId; }
    public void setContextId(String contextId) { this.contextId = contextId; }
    public String getSchemaVersion() { return schemaVersion; }
    public void setSchemaVersion(String schemaVersion) { this.schemaVersion = schemaVersion; }
    public JSONObject getUserContext() { return userContext; }
    public void setUserContext(JSONObject userContext) { this.userContext = userContext; }
    public JSONObject getSessionContext() { return sessionContext; }
    public void setSessionContext(JSONObject sessionContext) { this.sessionContext = sessionContext; }
    public JSONObject getProjectContext() { return projectContext; }
    public void setProjectContext(JSONObject projectContext) { this.projectContext = projectContext; }
    public List<ContextItem> getMemories() { return memories; }
    public void setMemories(List<ContextItem> memories) { this.memories = memories != null ? memories : new ArrayList<>(); }
    public List<JSONObject> getDecisions() { return decisions; }
    public void setDecisions(List<JSONObject> decisions) { this.decisions = decisions != null ? decisions : new ArrayList<>(); }
    public List<JSONObject> getKnownIssues() { return knownIssues; }
    public void setKnownIssues(List<JSONObject> knownIssues) { this.knownIssues = knownIssues != null ? knownIssues : new ArrayList<>(); }
    public List<ContextItem> getProcedures() { return procedures; }
    public void setProcedures(List<ContextItem> procedures) { this.procedures = procedures != null ? procedures : new ArrayList<>(); }
    public JSONObject getContinuity() { return continuity; }
    public void setContinuity(JSONObject continuity) { this.continuity = continuity; }
    public List<ContextSource> getSources() { return sources; }
    public void setSources(List<ContextSource> sources) { this.sources = sources != null ? sources : new ArrayList<>(); }
    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }
    public long getFreshness() { return freshness; }
    public void setFreshness(long freshness) { this.freshness = freshness; }
    public int getTokenEstimate() { return tokenEstimate; }
    public void setTokenEstimate(int tokenEstimate) { this.tokenEstimate = tokenEstimate; }
    public int getBudgetTokens() { return budgetTokens; }
    public void setBudgetTokens(int budgetTokens) { this.budgetTokens = budgetTokens; }
    public List<String> getWarnings() { return warnings; }
    public void setWarnings(List<String> warnings) { this.warnings = warnings != null ? warnings : new ArrayList<>(); }
    public List<String> getMissingInfo() { return missingInfo; }
    public void setMissingInfo(List<String> missingInfo) { this.missingInfo = missingInfo != null ? missingInfo : new ArrayList<>(); }
    public ContextAssessment getAssessment() { return assessment; }
    public void setAssessment(ContextAssessment assessment) { this.assessment = assessment; }

    public void addWarning(String warning) {
        if (warnings == null) warnings = new ArrayList<>();
        warnings.add(warning);
    }

    public void addMissing(String missing) {
        if (missingInfo == null) missingInfo = new ArrayList<>();
        missingInfo.add(missing);
    }

    public JSONObject toJsonObject() {
        JSONObject json = new JSONObject();
        json.put("contextId", contextId);
        json.put("schemaVersion", schemaVersion != null ? schemaVersion : SCHEMA_VERSION);
        if (userContext != null) json.put("userContext", userContext);
        if (sessionContext != null) json.put("sessionContext", sessionContext);
        if (projectContext != null) json.put("projectContext", projectContext);
        JSONArray memoryArr = new JSONArray();
        for (ContextItem item : memories) memoryArr.put(item.toJsonObject());
        json.put("memories", memoryArr);
        JSONArray decisionArr = new JSONArray();
        for (JSONObject d : decisions) decisionArr.put(d);
        json.put("decisions", decisionArr);
        JSONArray issueArr = new JSONArray();
        for (JSONObject i : knownIssues) issueArr.put(i);
        json.put("knownIssues", issueArr);
        JSONArray procedureArr = new JSONArray();
        for (ContextItem item : procedures) procedureArr.put(item.toJsonObject());
        json.put("procedures", procedureArr);
        if (continuity != null) json.put("continuity", continuity);
        JSONArray sourceArr = new JSONArray();
        for (ContextSource s : sources) sourceArr.put(s.toJsonObject());
        json.put("sources", sourceArr);
        json.put("confidence", confidence);
        json.put("freshness", freshness);
        json.put("tokenEstimate", tokenEstimate);
        json.put("budgetTokens", budgetTokens);
        JSONArray warnArr = new JSONArray();
        for (String w : warnings) warnArr.put(w);
        json.put("warnings", warnArr);
        JSONArray missingArr = new JSONArray();
        for (String m : missingInfo) missingArr.put(m);
        json.put("missingInfo", missingArr);
        if (assessment != null) json.put("assessment", assessment.toJsonObject());
        return json;
    }

    public static ContextPackage fromJsonObject(JSONObject json) {
        if (json == null) return null;
        ContextPackage pkg = new ContextPackage();
        if (json.has("contextId")) pkg.setContextId(json.getString("contextId"));
        if (json.has("schemaVersion")) pkg.setSchemaVersion(json.getString("schemaVersion"));
        if (json.has("userContext")) pkg.setUserContext(json.getJSONObject("userContext"));
        if (json.has("sessionContext")) pkg.setSessionContext(json.getJSONObject("sessionContext"));
        if (json.has("projectContext")) pkg.setProjectContext(json.getJSONObject("projectContext"));
        if (json.has("memories")) {
            JSONArray arr = json.getJSONArray("memories");
            List<ContextItem> items = new ArrayList<>();
            for (int i = 0; i < arr.length(); i++) {
                ContextItem item = ContextItem.fromJsonObject(arr.optJSONObject(i));
                if (item != null) items.add(item);
            }
            pkg.setMemories(items);
        }
        if (json.has("decisions")) {
            JSONArray arr = json.getJSONArray("decisions");
            List<JSONObject> list = new ArrayList<>();
            for (int i = 0; i < arr.length(); i++) list.add(arr.getJSONObject(i));
            pkg.setDecisions(list);
        }
        if (json.has("knownIssues")) {
            JSONArray arr = json.getJSONArray("knownIssues");
            List<JSONObject> list = new ArrayList<>();
            for (int i = 0; i < arr.length(); i++) list.add(arr.getJSONObject(i));
            pkg.setKnownIssues(list);
        }
        if (json.has("procedures")) {
            JSONArray arr = json.getJSONArray("procedures");
            List<ContextItem> items = new ArrayList<>();
            for (int i = 0; i < arr.length(); i++) {
                ContextItem item = ContextItem.fromJsonObject(arr.optJSONObject(i));
                if (item != null) items.add(item);
            }
            pkg.setProcedures(items);
        }
        if (json.has("continuity")) pkg.setContinuity(json.getJSONObject("continuity"));
        if (json.has("sources")) {
            JSONArray arr = json.getJSONArray("sources");
            List<ContextSource> sources = new ArrayList<>();
            for (int i = 0; i < arr.length(); i++) {
                ContextSource s = ContextSource.fromJsonObject(arr.optJSONObject(i));
                if (s != null) sources.add(s);
            }
            pkg.setSources(sources);
        }
        if (json.has("confidence")) pkg.setConfidence(json.getDouble("confidence"));
        if (json.has("freshness")) pkg.setFreshness(json.getLong("freshness"));
        if (json.has("tokenEstimate")) pkg.setTokenEstimate(json.getInt("tokenEstimate"));
        if (json.has("budgetTokens")) pkg.setBudgetTokens(json.getInt("budgetTokens"));
        if (json.has("warnings")) {
            JSONArray arr = json.getJSONArray("warnings");
            List<String> list = new ArrayList<>();
            for (int i = 0; i < arr.length(); i++) list.add(arr.getString(i));
            pkg.setWarnings(list);
        }
        if (json.has("missingInfo")) {
            JSONArray arr = json.getJSONArray("missingInfo");
            List<String> list = new ArrayList<>();
            for (int i = 0; i < arr.length(); i++) list.add(arr.getString(i));
            pkg.setMissingInfo(list);
        }
        if (json.has("assessment")) pkg.setAssessment(ContextAssessment.fromJsonObject(json.getJSONObject("assessment")));
        return pkg;
    }
}
