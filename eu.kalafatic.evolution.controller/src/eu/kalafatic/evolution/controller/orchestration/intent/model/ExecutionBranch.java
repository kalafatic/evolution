package eu.kalafatic.evolution.controller.orchestration.intent.model;

import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

import eu.kalafatic.evolution.controller.orchestration.PlatformType;

/**
 * Represents a candidate Darwin execution branch generated during Intent Reconstruction.
 * Contains technical and strategic assessment of an alternative execution direction.
 */
public class ExecutionBranch {

    private String id;
    private String name;
    private String interpretation;
    private PlatformType proposedMode;
    private String expectedResult;
    private List<String> requiredCapabilities = new ArrayList<>();
    private List<String> targetRequirements = new ArrayList<>();
    private List<String> advantages = new ArrayList<>();
    private List<String> limitations = new ArrayList<>();
    private List<String> risks = new ArrayList<>();
    private double confidence = 0.5;
    private boolean selected = false;
    private String selectionRationale;

    public ExecutionBranch() {
    }

    public ExecutionBranch(String id, String name, PlatformType proposedMode, String interpretation) {
        this.id = id;
        this.name = name;
        this.proposedMode = proposedMode;
        this.interpretation = interpretation;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getInterpretation() {
        return interpretation;
    }

    public void setInterpretation(String interpretation) {
        this.interpretation = interpretation;
    }

    public PlatformType getProposedMode() {
        return proposedMode;
    }

    public void setProposedMode(PlatformType proposedMode) {
        this.proposedMode = proposedMode;
    }

    public String getExpectedResult() {
        return expectedResult;
    }

    public void setExpectedResult(String expectedResult) {
        this.expectedResult = expectedResult;
    }

    public List<String> getRequiredCapabilities() {
        return requiredCapabilities;
    }

    public void setRequiredCapabilities(List<String> requiredCapabilities) {
        this.requiredCapabilities = requiredCapabilities;
    }

    public List<String> getTargetRequirements() {
        return targetRequirements;
    }

    public void setTargetRequirements(List<String> targetRequirements) {
        this.targetRequirements = targetRequirements;
    }

    public List<String> getAdvantages() {
        return advantages;
    }

    public void setAdvantages(List<String> advantages) {
        this.advantages = advantages;
    }

    public List<String> getLimitations() {
        return limitations;
    }

    public void setLimitations(List<String> limitations) {
        this.limitations = limitations;
    }

    public List<String> getRisks() {
        return risks;
    }

    public void setRisks(List<String> risks) {
        this.risks = risks;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = Math.max(0.0, Math.min(1.0, confidence));
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public String getSelectionRationale() {
        return selectionRationale;
    }

    public void setSelectionRationale(String selectionRationale) {
        this.selectionRationale = selectionRationale;
    }

    public JSONObject toJsonObject() {
        JSONObject obj = new JSONObject();
        obj.put("id", id != null ? id : "");
        obj.put("name", name != null ? name : "");
        obj.put("interpretation", interpretation != null ? interpretation : "");
        obj.put("proposedMode", proposedMode != null ? proposedMode.name() : "");
        obj.put("expectedResult", expectedResult != null ? expectedResult : "");
        obj.put("requiredCapabilities", new JSONArray(requiredCapabilities));
        obj.put("targetRequirements", new JSONArray(targetRequirements));
        obj.put("advantages", new JSONArray(advantages));
        obj.put("limitations", new JSONArray(limitations));
        obj.put("risks", new JSONArray(risks));
        obj.put("confidence", confidence);
        obj.put("selected", selected);
        obj.put("selectionRationale", selectionRationale != null ? selectionRationale : "");
        return obj;
    }

    public static ExecutionBranch fromJsonObject(JSONObject obj) {
        if (obj == null) return null;
        ExecutionBranch branch = new ExecutionBranch();
        branch.setId(obj.optString("id", "branch-" + System.currentTimeMillis()));
        branch.setName(obj.optString("name", "Branch"));
        branch.setInterpretation(obj.optString("interpretation", ""));
        String modeStr = obj.optString("proposedMode", null);
        if (modeStr != null) {
            try {
                branch.setProposedMode(PlatformType.valueOf(modeStr.trim().toUpperCase()));
            } catch (Exception ignored) {}
        }
        branch.setExpectedResult(obj.optString("expectedResult", ""));
        branch.setConfidence(obj.optDouble("confidence", 0.5));
        branch.setSelected(obj.optBoolean("selected", false));
        branch.setSelectionRationale(obj.optString("selectionRationale", ""));

        addJsonStringsToList(obj.optJSONArray("requiredCapabilities"), branch.getRequiredCapabilities());
        addJsonStringsToList(obj.optJSONArray("targetRequirements"), branch.getTargetRequirements());
        addJsonStringsToList(obj.optJSONArray("advantages"), branch.getAdvantages());
        addJsonStringsToList(obj.optJSONArray("limitations"), branch.getLimitations());
        addJsonStringsToList(obj.optJSONArray("risks"), branch.getRisks());

        return branch;
    }

    private static void addJsonStringsToList(JSONArray arr, List<String> list) {
        if (arr == null) return;
        for (int i = 0; i < arr.length(); i++) {
            String val = arr.optString(i, null);
            if (val != null && !val.trim().isEmpty()) {
                list.add(val.trim());
            }
        }
    }

    @Override
    public String toString() {
        return "ExecutionBranch{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", proposedMode=" + proposedMode +
                ", confidence=" + confidence +
                ", selected=" + selected +
                '}';
    }
}
