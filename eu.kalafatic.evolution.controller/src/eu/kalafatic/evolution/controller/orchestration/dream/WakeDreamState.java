package eu.kalafatic.evolution.controller.orchestration.dream;

/**
 * Operating states for WAKE / DREAM coordination.
 */
public enum WakeDreamState {
    WAKE_IDLE,
    WAKE_ACTIVE,
    DREAM_PENDING,
    DREAM_ACTIVE,
    DREAM_PAUSING,
    DREAM_PAUSED,
    DREAM_COMPLETING,
    DREAM_FAILED
}
