package eu.kalafatic.evolution.controller.orchestration.dream;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Persistable snapshot of a DREAM cycle's state with JSON serialization support.
 */
public class DreamCheckpoint implements Serializable {

    private static final long serialVersionUID = 1L;

    private String cycleId;
    private DreamOrigin origin = DreamOrigin.MANUAL;
    private String topic;
    private String topicSelectionRationale;
    private String stage; // SELECT_EXPERIENCES, ANALYZE_RELATIONSHIPS, GENERATE_HYPOTHESES, VERIFY_HYPOTHESES, CONSOLIDATE_RESULTS, COMPLETE
    private long startTime;
    private long lastUpdated;
    private int consumedIterations;
    private int maxIterations;
    private long maxDurationMs;

    private List<String> selectedExperienceIds = new ArrayList<>();
    private List<String> observedFacts = new ArrayList<>();
    private List<String> candidateRelationships = new ArrayList<>();
    private List<String> generatedHypotheses = new ArrayList<>();
    private List<String> supportingEvidence = new ArrayList<>();
    private List<String> contradictoryEvidence = new ArrayList<>();
    private List<String> unresolvedQuestions = new ArrayList<>();
    private List<String> verifiedResults = new ArrayList<>();

    public DreamCheckpoint() {
    }

    public DreamCheckpoint(String cycleId) {
        this.cycleId = cycleId;
        this.stage = "SELECT_EXPERIENCES";
        this.startTime = System.currentTimeMillis();
        this.lastUpdated = System.currentTimeMillis();
        this.consumedIterations = 0;
        this.maxIterations = 3;
        this.maxDurationMs = 60000L;
    }

    public JSONObject toJsonObject() {
        JSONObject json = new JSONObject();
        json.put("cycleId", cycleId);
        json.put("origin", origin != null ? origin.name() : DreamOrigin.MANUAL.name());
        json.put("topic", topic);
        json.put("topicSelectionRationale", topicSelectionRationale);
        json.put("stage", stage);
        json.put("startTime", startTime);
        json.put("lastUpdated", lastUpdated);
        json.put("consumedIterations", consumedIterations);
        json.put("maxIterations", maxIterations);
        json.put("maxDurationMs", maxDurationMs);

        json.put("selectedExperienceIds", new JSONArray(selectedExperienceIds));
        json.put("observedFacts", new JSONArray(observedFacts));
        json.put("candidateRelationships", new JSONArray(candidateRelationships));
        json.put("generatedHypotheses", new JSONArray(generatedHypotheses));
        json.put("supportingEvidence", new JSONArray(supportingEvidence));
        json.put("contradictoryEvidence", new JSONArray(contradictoryEvidence));
        json.put("unresolvedQuestions", new JSONArray(unresolvedQuestions));
        json.put("verifiedResults", new JSONArray(verifiedResults));

        return json;
    }

    public static DreamCheckpoint fromJsonObject(JSONObject json) {
        if (json == null) return null;
        DreamCheckpoint cp = new DreamCheckpoint();
        cp.setCycleId(json.optString("cycleId"));
        String origStr = json.optString("origin", DreamOrigin.MANUAL.name());
        try {
            cp.setOrigin(DreamOrigin.valueOf(origStr));
        } catch (Exception e) {
            cp.setOrigin(DreamOrigin.MANUAL);
        }
        cp.setTopic(json.optString("topic"));
        cp.setTopicSelectionRationale(json.optString("topicSelectionRationale"));
        cp.setStage(json.optString("stage", "SELECT_EXPERIENCES"));
        cp.setStartTime(json.optLong("startTime", System.currentTimeMillis()));
        cp.setLastUpdated(json.optLong("lastUpdated", System.currentTimeMillis()));
        cp.setConsumedIterations(json.optInt("consumedIterations", 0));
        cp.setMaxIterations(json.optInt("maxIterations", 3));
        cp.setMaxDurationMs(json.optLong("maxDurationMs", 60000L));

        populateList(cp.getSelectedExperienceIds(), json.optJSONArray("selectedExperienceIds"));
        populateList(cp.getObservedFacts(), json.optJSONArray("observedFacts"));
        populateList(cp.getCandidateRelationships(), json.optJSONArray("candidateRelationships"));
        populateList(cp.getGeneratedHypotheses(), json.optJSONArray("generatedHypotheses"));
        populateList(cp.getSupportingEvidence(), json.optJSONArray("supportingEvidence"));
        populateList(cp.getContradictoryEvidence(), json.optJSONArray("contradictoryEvidence"));
        populateList(cp.getUnresolvedQuestions(), json.optJSONArray("unresolvedQuestions"));
        populateList(cp.getVerifiedResults(), json.optJSONArray("verifiedResults"));

        return cp;
    }

    private static void populateList(List<String> target, JSONArray arr) {
        if (arr != null) {
            for (int i = 0; i < arr.length(); i++) {
                target.add(arr.optString(i));
            }
        }
    }

    public String getCycleId() {
        return cycleId;
    }

    public void setCycleId(String cycleId) {
        this.cycleId = cycleId;
    }

    public DreamOrigin getOrigin() {
        return origin;
    }

    public void setOrigin(DreamOrigin origin) {
        this.origin = origin;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getTopicSelectionRationale() {
        return topicSelectionRationale;
    }

    public void setTopicSelectionRationale(String topicSelectionRationale) {
        this.topicSelectionRationale = topicSelectionRationale;
    }

    public String getStage() {
        return stage;
    }

    public void setStage(String stage) {
        this.stage = stage;
        this.lastUpdated = System.currentTimeMillis();
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public int getConsumedIterations() {
        return consumedIterations;
    }

    public void setConsumedIterations(int consumedIterations) {
        this.consumedIterations = consumedIterations;
    }

    public int getMaxIterations() {
        return maxIterations;
    }

    public void setMaxIterations(int maxIterations) {
        this.maxIterations = maxIterations;
    }

    public long getMaxDurationMs() {
        return maxDurationMs;
    }

    public void setMaxDurationMs(long maxDurationMs) {
        this.maxDurationMs = maxDurationMs;
    }

    public List<String> getSelectedExperienceIds() {
        return selectedExperienceIds;
    }

    public List<String> getObservedFacts() {
        return observedFacts;
    }

    public List<String> getCandidateRelationships() {
        return candidateRelationships;
    }

    public List<String> getGeneratedHypotheses() {
        return generatedHypotheses;
    }

    public List<String> getSupportingEvidence() {
        return supportingEvidence;
    }

    public List<String> getContradictoryEvidence() {
        return contradictoryEvidence;
    }

    public List<String> getUnresolvedQuestions() {
        return unresolvedQuestions;
    }

    public List<String> getVerifiedResults() {
        return verifiedResults;
    }
}
