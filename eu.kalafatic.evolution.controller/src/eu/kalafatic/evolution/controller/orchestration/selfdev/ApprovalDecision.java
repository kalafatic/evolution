package eu.kalafatic.evolution.controller.orchestration.selfdev;

/**
 * The decision made by the user or auto-approval.
 */
public class ApprovalDecision {
    private final String selectedVariantId;
    private final boolean approved;
    private final boolean autoApproved;

    public ApprovalDecision(String selectedVariantId, boolean approved, boolean autoApproved) {
        this.selectedVariantId = selectedVariantId;
        this.approved = approved;
        this.autoApproved = autoApproved;
    }

    public String getSelectedVariantId() { return selectedVariantId; }
    public boolean isApproved() { return approved; }
    public boolean isAutoApproved() { return autoApproved; }

    public static ApprovalDecision autoApprove(String variantId) {
        return new ApprovalDecision(variantId, true, true);
    }

    public static ApprovalDecision manualApprove(String variantId) {
        return new ApprovalDecision(variantId, true, false);
    }

    public static ApprovalDecision reject() {
        return new ApprovalDecision(null, false, false);
    }
}