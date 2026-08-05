package eu.kalafatic.evolution.controller.orchestration;

/**
 * Hardened system states for the Evolutionary OS Kernel.
 */
public enum SystemState {
    INIT,
    ANALYZING,
    PLAN_LOCKED,
    EXECUTING,
    VERIFYING,
    CLARIFYING,
    MUTATING,
    EXPORTING,
    AWAITING_BRANCH_SELECTION,
    WAITING_FOR_USER_DECISION,
    DONE,
    FAILED,
    RECOVERING
}
