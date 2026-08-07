package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.util.List;

/**
 * Context for an approval request.
 * Contains all information needed to compute a winner or present options to the user.
 */
public class ApprovalContext {
    private final String approvalId;
    private final List<BranchVariant> candidates;
    private final BranchVariant recommended;
    private final String goal;
    private final int iteration;
    private final String sessionId;
    private final long timestamp;

    public ApprovalContext(String approvalId, List<BranchVariant> candidates, 
            BranchVariant recommended, String goal, int iteration, String sessionId) {
        this.approvalId = approvalId;
        this.candidates = candidates;
        this.recommended = recommended;
        this.goal = goal;
        this.iteration = iteration;
        this.sessionId = sessionId;
        this.timestamp = System.currentTimeMillis();
    }

    public String getApprovalId() { return approvalId; }
    public List<BranchVariant> getCandidates() { return candidates; }
    public BranchVariant getRecommended() { return recommended; }
    public String getGoal() { return goal; }
    public int getIteration() { return iteration; }
    public String getSessionId() { return sessionId; }
    public long getTimestamp() { return timestamp; }
}