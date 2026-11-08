package eu.kalafatic.evolution.controller.orchestration.develop;

/**
 * Task execution lifecycle state machine.
 */
public enum DevelopTaskStatus {
    CREATED,
    QUEUED,
    WAITING_FOR_RESOURCES,
    WAITING_FOR_DEPENDENCIES,
    PREPARING,
    ANALYZING,
    PLANNING,
    CODING,
    BUILDING,
    TESTING,
    FIXING,
    READY_FOR_REVIEW,
    COMMITTING,
    COMPLETED,
    FAILED,
    CANCELLED
}
