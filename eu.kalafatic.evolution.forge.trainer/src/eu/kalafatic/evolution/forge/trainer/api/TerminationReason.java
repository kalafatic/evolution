package eu.kalafatic.evolution.forge.trainer.api;

public enum TerminationReason {
    PLANNED_COMPLETION,
    USER_REQUEST,
    EARLY_STOPPING_CRITERIA,
    THREAD_INTERRUPTED,
    NAN_OR_INF_LOSS,
    NO_TRAINING_SAMPLES,
    HARDWARE_BACKEND_FAILURE,
    EXCEPTION_THROWN
}
