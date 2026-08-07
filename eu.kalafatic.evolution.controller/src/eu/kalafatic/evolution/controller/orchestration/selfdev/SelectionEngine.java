package eu.kalafatic.evolution.controller.orchestration.selfdev;

/**
 * Computes the winner from a set of candidates.
 * Used for auto-approval.
 */
public class SelectionEngine {
    
    /**
     * Compute the winner for auto-approval.
     * Uses the recommended candidate if available, otherwise the highest score.
     * 
     * @param ctx The approval context containing candidates
     * @return The approval decision
     */
    public ApprovalDecision computeWinner(ApprovalContext ctx) {
        BranchVariant winner = ctx.getRecommended();
        
        if (winner == null && ctx.getCandidates() != null && !ctx.getCandidates().isEmpty()) {
            winner = ctx.getCandidates().stream()
                    .max((v1, v2) -> Double.compare(v1.getScore(), v2.getScore()))
                    .orElse(null);
        }
        
        if (winner != null) {
            return ApprovalDecision.autoApprove(winner.getId());
        }
        
        // No winner found - reject
        return ApprovalDecision.reject();
    }
}