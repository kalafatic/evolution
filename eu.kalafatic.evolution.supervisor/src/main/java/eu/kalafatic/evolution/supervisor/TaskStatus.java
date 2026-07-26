package eu.kalafatic.evolution.supervisor;

public enum TaskStatus {
    CREATED,
    READY,
    EVO_RUNNING,
    EVO_COMPLETED,
    EVO_SHUTDOWN,
    BUILDING,
    TESTING,
    RUNNING,
    VERIFYING,
    COMPLETED,
    EVO_FAILED,
    BUILD_FAILED,
    TEST_FAILED,
    RUN_FAILED,
    VERIFICATION_FAILED,
    CANCELLED
}
