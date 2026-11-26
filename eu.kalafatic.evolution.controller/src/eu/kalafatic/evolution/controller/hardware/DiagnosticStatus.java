package eu.kalafatic.evolution.controller.hardware;

/**
 * Overall result of a hardware or training diagnostic execution.
 *
 * <p>These states are deliberately independent: a GPU can be detected while
 * CUDA execution still fails, and CUDA can be selected by configuration while
 * training actually executes on CPU. Each state maps to a single observable
 * fact about the executed test, never to a guessed capability.</p>
 */
public enum DiagnosticStatus {

    /** The test has not been executed yet. */
    NOT_TESTED,

    /** The test is currently executing on the diagnostic worker thread. */
    RUNNING,

    /** All executed stages succeeded and the numerical/evidence checks passed. */
    PASS,

    /**
     * The test executed but some stages failed or required evidence could not
     * be collected (e.g. CUDA kernels executed but could not be independently
     * counted).
     */
    PARTIAL,

    /** The test executed and a required stage or the overall verification failed. */
    FAILED,

    /**
     * The capability under test is not supported on this machine/JVM
     * (no GPU, no driver, unsupported JVM version, missing bundle).
     */
    UNSUPPORTED
}
