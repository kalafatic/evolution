package eu.kalafatic.evolution.controller.workflow;

public enum RuntimeEventType {
    MODE_CHANGED,
    TASK_STARTED,
    TASK_COMPLETED,
    TASK_FAILED,
    ITERATION_STARTED,
    ITERATION_COMPLETED,
    SUPERVISOR_STATUS_CHANGED,
    DEPLOYMENT_STATUS_CHANGED,
    EXPORT_READY,
    REASONING_STEP
}
