package eu.kalafatic.evolution.controller.hardware;

/**
 * Status of one diagnostic stage. A stage is {@link #SKIPPED} when an earlier,
 * required stage failed and executing it would be meaningless or unsafe.
 */
public enum StageStatus {
    PASS,
    FAILED,
    SKIPPED,
    UNSUPPORTED
}
