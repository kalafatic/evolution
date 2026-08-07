package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

import eu.kalafatic.evolution.controller.orchestration.IterationManager;
import eu.kalafatic.evolution.controller.orchestration.SessionContainer;
import eu.kalafatic.evolution.controller.orchestration.SystemState;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.TransitionToken;
import eu.kalafatic.evolution.controller.workflow.RuntimeEvent;
import eu.kalafatic.evolution.controller.workflow.RuntimeEventType;
import eu.kalafatic.evolution.model.orchestration.PromptInstructions;

/**
 * Central approval service that manages all approval checkpoints.
 * This is the ONLY place where approval logic lives.
 */
public class ApprovalService {
    
    private final Map<String, PendingApproval> pendingApprovals = new ConcurrentHashMap<>();
    private final SelectionEngine selectionEngine = new SelectionEngine();
    private final SessionContainer sessionContainer;
    
    public ApprovalService(SessionContainer sessionContainer) {
        this.sessionContainer = sessionContainer;
    }
    
    /**
     * The main approval checkpoint.
     * Called by the Darwin engine when it needs approval.
     */
    public ApprovalDecision awaitApproval(ApprovalContext ctx, TaskContext context, 
            IterationManager iterationManager) 
            throws PauseExecutionException {
        
        PromptInstructions instructions = null;
        if (context.getOrchestrator() != null && context.getOrchestrator().getAiChat() != null) {
            instructions = context.getOrchestrator().getAiChat().getPromptInstructions();
        }
        
        // ============================================================
        // AUTO MODE: Compute winner and return immediately
        // ============================================================
        if (instructions != null && instructions.isAutoApprove()) {
            context.log("[APPROVAL] Auto-approve enabled. Computing winner...");
            ApprovalDecision decision = selectionEngine.computeWinner(ctx);
            context.log("[APPROVAL] Auto-approved: " + decision.getSelectedVariantId());
            return decision;
        }
        
        // ============================================================
        // MANUAL MODE: Pause and wait for user
        // ============================================================
        context.log("[APPROVAL] Manual mode. Waiting for user approval...");
        
        // Transition to waiting state
        iterationManager.transition(SystemState.WAITING_FOR_USER_DECISION, context);
        
        // Register pending approval
        PendingApproval pending = new PendingApproval(ctx, context);
        pendingApprovals.put(ctx.getApprovalId(), pending);
        
        // Store in context for UI to find
        context.getMetadata().put("pending_approval_id", ctx.getApprovalId());
        context.getMetadata().put("pending_candidates", ctx.getCandidates());
        context.getMetadata().put("recommended_candidate", ctx.getRecommended());
        context.getMetadata().put("iterationManager", iterationManager);
        
        // Emit branches to UI
        emitBranchesToUI(context, ctx);
        
        // THROW to pause the execution
        throw new PauseExecutionException(ctx.getApprovalId());
    }
    
    /**
     * Resume a pending approval with a decision.
     * Called by the UI when the user makes a decision or auto-approve is toggled.
     */
    public boolean resumeApproval(String approvalId, ApprovalDecision decision) {
        PendingApproval pending = pendingApprovals.remove(approvalId);
        if (pending == null) {
            return false;
        }
        
        pending.complete(decision);
        
        // Resume the iteration
        TaskContext context = pending.getTaskContext();
        IterationManager iterationManager = 
                (IterationManager) context.getMetadata().get("iterationManager");
        
        if (iterationManager != null) {
            // Transition to EXECUTING to resume
            TransitionToken token = context.getTransitionToken();
            if (token == null) {
                token = new TransitionToken("DarwinEngine-Resume-" + System.currentTimeMillis());
                context.setTransitionToken(token);
            }
            context.getStateHolder().applyTransition(token, SystemState.EXECUTING);
            
            // Store the decision for the engine to pick up
            context.getMetadata().put("resume_manual_id", decision.getSelectedVariantId());
            context.getMetadata().remove("pending_approval_id");
        }
        
        return true;
    }
    
    /**
     * Resume all pending approvals when auto-approve is enabled.
     * Called when the user toggles the Auto-Approve checkbox.
     */
    public void resumeAllPendingApprovals() {
        // Use a copy of the keys to avoid ConcurrentModificationException
        for (String approvalId : pendingApprovals.keySet().toArray(new String[0])) {
            PendingApproval pending = pendingApprovals.get(approvalId);
            if (pending != null) {
                ApprovalContext ctx = pending.getContext();
                ApprovalDecision decision = selectionEngine.computeWinner(ctx);
                resumeApproval(approvalId, decision);
            }
        }
    }
    
    /**
     * Check if there are any pending approvals.
     */
    public boolean hasPendingApprovals() {
        return !pendingApprovals.isEmpty();
    }
    
    /**
     * Get a pending approval by ID.
     */
    public PendingApproval getPendingApproval(String approvalId) {
        return pendingApprovals.get(approvalId);
    }
    
    /**
     * Get the number of pending approvals.
     */
    public int getPendingCount() {
        return pendingApprovals.size();
    }
    
    /**
     * Emit branches to the UI.
     */
    private void emitBranchesToUI(TaskContext context, ApprovalContext ctx) {
        StringBuilder outcomeBuilder = new StringBuilder("[DARWIN_BRANCHES] ");
        outcomeBuilder.append("\nIteration ").append(ctx.getIteration()).append("\n");
        
        org.json.JSONObject json = new org.json.JSONObject();
        json.put("iteration", ctx.getIteration());
        json.put("approval_id", ctx.getApprovalId());
        org.json.JSONArray variantsArr = new org.json.JSONArray();
        for (BranchVariant v : ctx.getCandidates()) {
            org.json.JSONObject vObj = new org.json.JSONObject();
            vObj.put("id", v.getId());
            vObj.put("strategy", v.getStrategy());
            vObj.put("score", v.getScore());
            vObj.put("survival_argument", v.getSurvivalArgument());
            vObj.put("tradeoffs", v.getTradeoffs());
            vObj.put("status", v.getActivationState().name());
            variantsArr.put(vObj);
        }
        json.put("variants", variantsArr);
        if (ctx.getRecommended() != null) {
            json.put("recommended", ctx.getRecommended().getId());
        }
        outcomeBuilder.append("[DECISION:MANUAL] ");
        outcomeBuilder.append(json.toString());
        context.log(outcomeBuilder.toString());
        
        // Use sessionContainer to publish event
        if (sessionContainer != null && sessionContainer.getEventBus() != null) {
            try {
                RuntimeEventType approvalType = RuntimeEventType.valueOf("APPROVAL_REQUIRED");
                sessionContainer.getEventBus().publish(
                        new RuntimeEvent(
                                approvalType,
                                context.getSessionId(), 
                                "DarwinEngine", 
                                json.toString()));
            } catch (IllegalArgumentException e) {
                // Fallback: use FLOW_PAUSED
                sessionContainer.getEventBus().publish(
                        new RuntimeEvent(
                                RuntimeEventType.FLOW_PAUSED,
                                context.getSessionId(), 
                                "DarwinEngine", 
                                json.toString()));
            }
        }
    }
    
    /**
     * Pending approval record.
     */
    public static class PendingApproval {
        private final ApprovalContext context;
        private final TaskContext taskContext;
        private ApprovalDecision decision;
        private boolean completed;
        
        public PendingApproval(ApprovalContext context, TaskContext taskContext) {
            this.context = context;
            this.taskContext = taskContext;
            this.completed = false;
        }
        
        public ApprovalContext getContext() { return context; }
        public TaskContext getTaskContext() { return taskContext; }
        public ApprovalDecision getDecision() { return decision; }
        public boolean isCompleted() { return completed; }
        
        public void complete(ApprovalDecision decision) {
            this.decision = decision;
            this.completed = true;
        }
    }
}