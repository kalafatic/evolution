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
    MUTATING,
    DONE,
    FAILED,
    RECOVERING
}
