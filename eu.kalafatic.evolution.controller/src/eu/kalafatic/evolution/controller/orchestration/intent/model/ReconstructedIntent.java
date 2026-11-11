package eu.kalafatic.evolution.controller.orchestration.intent.model;

import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

import eu.kalafatic.evolution.controller.orchestration.PlatformType;

/**
 * Reconstructed Intent model representing the complete architectural understanding of user intent,
 * target data, conversational context, constraints, and alternative Darwin execution branches.
 */
public class ReconstructedIntent {

    public enum ActionType {
        QUESTION,
        CONVERSATION,
        PROGRAMMING,
        ANALYSIS,
        MODIFICATION,
        INVESTIGATION,
        CLARIFICATION_NEEDED,
        UNKNOWN
    }

    public enum TargetRole {
        NONE,
        MODIFICATION_TARGET,
        ANALYSIS_ONLY,
        CONTEXTUAL_REFERENCE,
        AMBIGUOUS
    }

    private String intentId;
    private String rawPrompt;
    private ActionType actionType = ActionType.UNKNOWN;
    private String primaryObjective;
    private List<String> secondaryObjectives = new ArrayList<>();
    private String expectedResult;

    // Target information
    private String targetPath;
    private String targetCategory; // e.g. PROJECT, REPOSITORY, FOLDER, AI_MODEL, DATASET, NONE
    private TargetRole targetRole = TargetRole.NONE;
    private String targetDescription;
    private String targetRelationship;

    // Context & Technical Profile
    private String conversationSummary;
    private List<String> technicalConstraints = new ArrayList<>();
    private List<String> requiredCapabilities = new ArrayList<>();
    private List<String> confirmedFacts = new ArrayList<>();
    private List<String> assumptions = new ArrayList<>();
    private List<String> missingInformation = new ArrayList<>();
    private List<String> ambiguities = new ArrayList<>();

    // Darwin Execution Branches
    private List<ExecutionBranch> candidateBranches = new ArrayList<>();
    private ExecutionBranch selectedBranch;
    private PlatformType recommendedExecutionMode;
    private double confidence = 0.5;
    private boolean clarificationRequired = false;
    private String clarificationQuestion;
    private String decisionRationale;

    public ReconstructedIntent() {
        this.intentId = "intent-" + System.currentTimeMillis();
    }

    public ReconstructedIntent(String intentId, String rawPrompt) {
        this.intentId = intentId;
        this.rawPrompt = rawPrompt;
    }

    public String getIntentId() {
        return intentId;
    }

    public void setIntentId(String intentId) {
        this.intentId = intentId;
    }

    public String getRawPrompt() {
        return rawPrompt;
    }

    public void setRawPrompt(String rawPrompt) {
        this.rawPrompt = rawPrompt;
    }

    public ActionType getActionType() {
        return actionType;
    }

    public void setActionType(ActionType actionType) {
        this.actionType = actionType;
    }

    public String getPrimaryObjective() {
        return primaryObjective;
    }

    public void setPrimaryObjective(String primaryObjective) {
        this.primaryObjective = primaryObjective;
    }

    public List<String> getSecondaryObjectives() {
        return secondaryObjectives;
    }

    public void setSecondaryObjectives(List<String> secondaryObjectives) {
        this.secondaryObjectives = secondaryObjectives;
    }

    public String getExpectedResult() {
        return expectedResult;
    }

    public void setExpectedResult(String expectedResult) {
        this.expectedResult = expectedResult;
    }

    public String getTargetPath() {
        return targetPath;
    }

    public void setTargetPath(String targetPath) {
        this.targetPath = targetPath;
    }

    public String getTargetCategory() {
        return targetCategory;
    }

    public void setTargetCategory(String targetCategory) {
        this.targetCategory = targetCategory;
    }

    public TargetRole getTargetRole() {
        return targetRole;
    }

    public void setTargetRole(TargetRole targetRole) {
        this.targetRole = targetRole;
    }

    public String getTargetDescription() {
        return targetDescription;
    }

    public void setTargetDescription(String targetDescription) {
        this.targetDescription = targetDescription;
    }

    public String getTargetRelationship() {
        return targetRelationship;
    }

    public void setTargetRelationship(String targetRelationship) {
        this.targetRelationship = targetRelationship;
    }

    public String getConversationSummary() {
        return conversationSummary;
    }

    public void setConversationSummary(String conversationSummary) {
        this.conversationSummary = conversationSummary;
    }

    public List<String> getTechnicalConstraints() {
        return technicalConstraints;
    }

    public void setTechnicalConstraints(List<String> technicalConstraints) {
        this.technicalConstraints = technicalConstraints;
    }

    public List<String> getRequiredCapabilities() {
        return requiredCapabilities;
    }

    public void setRequiredCapabilities(List<String> requiredCapabilities) {
        this.requiredCapabilities = requiredCapabilities;
    }

    public List<String> getConfirmedFacts() {
        return confirmedFacts;
    }

    public void setConfirmedFacts(List<String> confirmedFacts) {
        this.confirmedFacts = confirmedFacts;
    }

    public List<String> getAssumptions() {
        return assumptions;
    }

    public void setAssumptions(List<String> assumptions) {
        this.assumptions = assumptions;
    }

    public List<String> getMissingInformation() {
        return missingInformation;
    }

    public void setMissingInformation(List<String> missingInformation) {
        this.missingInformation = missingInformation;
    }

    public List<String> getAmbiguities() {
        return ambiguities;
    }

    public void setAmbiguities(List<String> ambiguities) {
        this.ambiguities = ambiguities;
    }

    public List<ExecutionBranch> getCandidateBranches() {
        return candidateBranches;
    }

    public void setCandidateBranches(List<ExecutionBranch> candidateBranches) {
        this.candidateBranches = candidateBranches;
    }

    public ExecutionBranch getSelectedBranch() {
        return selectedBranch;
    }

    public void setSelectedBranch(ExecutionBranch selectedBranch) {
        this.selectedBranch = selectedBranch;
        if (selectedBranch != null) {
            this.recommendedExecutionMode = selectedBranch.getProposedMode();
            selectedBranch.setSelected(true);
        }
    }

    public PlatformType getRecommendedExecutionMode() {
        return recommendedExecutionMode;
    }

    public void setRecommendedExecutionMode(PlatformType recommendedExecutionMode) {
        this.recommendedExecutionMode = recommendedExecutionMode;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = Math.max(0.0, Math.min(1.0, confidence));
    }

    public boolean isClarificationRequired() {
        return clarificationRequired;
    }

    public void setClarificationRequired(boolean clarificationRequired) {
        this.clarificationRequired = clarificationRequired;
    }

    public String getClarificationQuestion() {
        return clarificationQuestion;
    }

    public void setClarificationQuestion(String clarificationQuestion) {
        this.clarificationQuestion = clarificationQuestion;
    }

    public String getDecisionRationale() {
        return decisionRationale;
    }

    public void setDecisionRationale(String decisionRationale) {
        this.decisionRationale = decisionRationale;
    }

    public JSONObject toJsonObject() {
        JSONObject obj = new JSONObject();
        obj.put("intentId", intentId);
        obj.put("rawPrompt", rawPrompt != null ? rawPrompt : "");
        obj.put("actionType", actionType != null ? actionType.name() : ActionType.UNKNOWN.name());
        obj.put("primaryObjective", primaryObjective != null ? primaryObjective : "");
        obj.put("secondaryObjectives", new JSONArray(secondaryObjectives));
        obj.put("expectedResult", expectedResult != null ? expectedResult : "");

        JSONObject targetObj = new JSONObject();
        targetObj.put("path", targetPath != null ? targetPath : "");
        targetObj.put("category", targetCategory != null ? targetCategory : "");
        targetObj.put("role", targetRole != null ? targetRole.name() : TargetRole.NONE.name());
        targetObj.put("description", targetDescription != null ? targetDescription : "");
        targetObj.put("relationship", targetRelationship != null ? targetRelationship : "");
        obj.put("target", targetObj);

        obj.put("conversationSummary", conversationSummary != null ? conversationSummary : "");
        obj.put("technicalConstraints", new JSONArray(technicalConstraints));
        obj.put("requiredCapabilities", new JSONArray(requiredCapabilities));
        obj.put("confirmedFacts", new JSONArray(confirmedFacts));
        obj.put("assumptions", new JSONArray(assumptions));
        obj.put("missingInformation", new JSONArray(missingInformation));
        obj.put("ambiguities", new JSONArray(ambiguities));

        JSONArray branchesArr = new JSONArray();
        for (ExecutionBranch b : candidateBranches) {
            branchesArr.put(b.toJsonObject());
        }
        obj.put("candidateBranches", branchesArr);

        if (selectedBranch != null) {
            obj.put("selectedBranch", selectedBranch.toJsonObject());
        }
        obj.put("recommendedExecutionMode", recommendedExecutionMode != null ? recommendedExecutionMode.name() : "");
        obj.put("confidence", confidence);
        obj.put("clarificationRequired", clarificationRequired);
        obj.put("clarificationQuestion", clarificationQuestion != null ? clarificationQuestion : "");
        obj.put("decisionRationale", decisionRationale != null ? decisionRationale : "");

        return obj;
    }

    public static ReconstructedIntent fromJsonObject(JSONObject obj) {
        if (obj == null) return null;
        ReconstructedIntent intent = new ReconstructedIntent();
        intent.setIntentId(obj.optString("intentId", "intent-" + System.currentTimeMillis()));
        intent.setRawPrompt(obj.optString("rawPrompt", ""));
        String actStr = obj.optString("actionType", null);
        if (actStr != null) {
            try {
                intent.setActionType(ActionType.valueOf(actStr.trim().toUpperCase()));
            } catch (Exception ignored) {}
        }
        intent.setPrimaryObjective(obj.optString("primaryObjective", ""));
        intent.setExpectedResult(obj.optString("expectedResult", ""));

        JSONObject targetObj = obj.optJSONObject("target");
        if (targetObj != null) {
            intent.setTargetPath(targetObj.optString("path", null));
            intent.setTargetCategory(targetObj.optString("category", null));
            String roleStr = targetObj.optString("role", null);
            if (roleStr != null) {
                try {
                    intent.setTargetRole(TargetRole.valueOf(roleStr.trim().toUpperCase()));
                } catch (Exception ignored) {}
            }
            intent.setTargetDescription(targetObj.optString("description", null));
            intent.setTargetRelationship(targetObj.optString("relationship", null));
        }

        intent.setConversationSummary(obj.optString("conversationSummary", ""));
        intent.setConfidence(obj.optDouble("confidence", 0.5));
        intent.setClarificationRequired(obj.optBoolean("clarificationRequired", false));
        intent.setClarificationQuestion(obj.optString("clarificationQuestion", null));
        intent.setDecisionRationale(obj.optString("decisionRationale", null));

        String modeStr = obj.optString("recommendedExecutionMode", null);
        if (modeStr != null) {
            try {
                intent.setRecommendedExecutionMode(PlatformType.valueOf(modeStr.trim().toUpperCase()));
            } catch (Exception ignored) {}
        }

        addJsonStringsToList(obj.optJSONArray("secondaryObjectives"), intent.getSecondaryObjectives());
        addJsonStringsToList(obj.optJSONArray("technicalConstraints"), intent.getTechnicalConstraints());
        addJsonStringsToList(obj.optJSONArray("requiredCapabilities"), intent.getRequiredCapabilities());
        addJsonStringsToList(obj.optJSONArray("confirmedFacts"), intent.getConfirmedFacts());
        addJsonStringsToList(obj.optJSONArray("assumptions"), intent.getAssumptions());
        addJsonStringsToList(obj.optJSONArray("missingInformation"), intent.getMissingInformation());
        addJsonStringsToList(obj.optJSONArray("ambiguities"), intent.getAmbiguities());

        JSONArray branchesArr = obj.optJSONArray("candidateBranches");
        if (branchesArr != null) {
            for (int i = 0; i < branchesArr.length(); i++) {
                JSONObject bObj = branchesArr.optJSONObject(i);
                if (bObj != null) {
                    ExecutionBranch branch = ExecutionBranch.fromJsonObject(bObj);
                    if (branch != null) {
                        intent.getCandidateBranches().add(branch);
                    }
                }
            }
        }

        JSONObject selBranchObj = obj.optJSONObject("selectedBranch");
        if (selBranchObj != null) {
            intent.setSelectedBranch(ExecutionBranch.fromJsonObject(selBranchObj));
        }

        return intent;
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
}
