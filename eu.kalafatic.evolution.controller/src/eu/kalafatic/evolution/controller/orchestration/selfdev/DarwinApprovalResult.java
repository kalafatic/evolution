package eu.kalafatic.evolution.controller.orchestration.selfdev;

public class DarwinApprovalResult {
    public enum Action {
        CONTINUE,
        WAIT,
        RETRY,
        REJECT,
        CANCEL
    }

    private final Action action;
    private final String selectedCandidateId;

    public DarwinApprovalResult(Action action, String selectedCandidateId) {
        this.action = action;
        this.selectedCandidateId = selectedCandidateId;
    }

    public Action getAction() { return action; }
    public String getSelectedCandidateId() { return selectedCandidateId; }
}
