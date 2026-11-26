package eu.kalafatic.evolution.controller.hardware;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Structured result of the Forge GPU training verification test.
 *
 * <p>The test exercises the canonical training pipeline
 * ({@code ForgeDeviceSelector → GpuTrainingAdapter → GpuTrainingEngine → CUDA},
 * or the CPU {@code EvoLlmTrainer}) with a tiny, deterministic, in-memory
 * model and dataset. It records the requested device policy, the selected
 * device, the actual execution backend and independent CUDA execution
 * evidence (kernel launch / memory transfer counters, device memory state,
 * parameter-change verification) so that "the GPU was used" is a proven fact,
 * never an inference from configuration or device detection.</p>
 */
public final class ForgeGpuVerificationReport {

    public final String executionId = UUID.randomUUID().toString();
    public volatile Instant startedAt = Instant.now();
    public volatile Instant finishedAt = startedAt;
    public volatile DiagnosticStatus overallStatus = DiagnosticStatus.RUNNING;

    /** Device policy and selection. */
    public volatile String policyLabel = "";
    public volatile String requestedDevice = "";
    public volatile String selectedDevice = "";
    public volatile boolean cudaAvailable;
    public volatile String selectionNote = "";

    /** True when the test could not run because a Forge training session is active. */
    public volatile boolean blockedByActiveSession;
    public volatile String blockedReason = "";

    /** Diagnostic model configuration. */
    public volatile String modelSummary = "";

    /** Training execution facts. */
    public volatile String actualBackend = "";
    public volatile String actualDevice = "";
    public volatile int trainingStepsExecuted;
    public volatile double initialLoss = Double.NaN;
    public volatile double finalLoss = Double.NaN;
    public volatile List<Double> lossHistory = new ArrayList<>();

    /** Parameter-change verification. */
    public volatile boolean parametersChanged;
    public volatile int parameterChangeCount;
    public volatile double parameterChangeMaxAbs;

    /** Independent CUDA execution evidence (backend instrumentation deltas). */
    public volatile long kernelLaunchDelta;
    public volatile long memcpyDelta;
    public volatile long memAllocDelta;
    public volatile long memFreeDelta;
    public volatile long freeMemBeforeBytes;
    public volatile long freeMemAfterBytes;
    public volatile boolean cleanupVerified;

    public volatile long durationMs;
    public volatile String failureDetail = "";

    public long totalDurationMs() {
        return Math.max(0, finishedAt.toEpochMilli() - startedAt.toEpochMilli());
    }

    public boolean cudaSelected() {
        return "CUDA".equalsIgnoreCase(selectedDevice);
    }

    public boolean cudaActuallyExecuted() {
        return "CUDA".equalsIgnoreCase(actualDevice);
    }

    /** Human-readable, copy-friendly report used by the "View Details" dialogs. */
    public String toText() {
        StringBuilder sb = new StringBuilder();
        sb.append("=========================================================\n");
        sb.append("      EVO FORGE GPU TRAINING VERIFICATION REPORT          \n");
        sb.append("=========================================================\n");
        sb.append("Execution ID   : ").append(executionId).append("\n");
        sb.append("Started        : ").append(startedAt).append("\n");
        sb.append("Finished       : ").append(finishedAt).append("\n");
        sb.append("Duration       : ").append(totalDurationMs()).append(" ms\n");
        sb.append("Overall        : ").append(overallStatus).append("\n\n");

        sb.append("--- DEVICE POLICY & SELECTION ---\n");
        sb.append("Policy         : ").append(policyLabel).append("\n");
        sb.append("Requested      : ").append(requestedDevice).append("\n");
        sb.append("Selected       : ").append(selectedDevice).append("\n");
        sb.append("CUDA available : ").append(cudaAvailable ? "YES" : "NO").append("\n");
        if (selectionNote != null && !selectionNote.isEmpty()) {
            sb.append("Note           : ").append(selectionNote).append("\n");
        }
        if (blockedByActiveSession) {
            sb.append("BLOCKED        : ").append(blockedReason).append("\n");
        }

        sb.append("\n--- TRAINING EXECUTION ---\n");
        sb.append("Model          : ").append(modelSummary).append("\n");
        sb.append("Actual backend : ").append(actualBackend.isEmpty() ? "N/A" : actualBackend).append("\n");
        sb.append("Actual device  : ").append(actualDevice.isEmpty() ? "N/A" : actualDevice).append("\n");
        sb.append("Optimizer steps: ").append(trainingStepsExecuted).append("\n");
        sb.append("Initial loss   : ").append(fmtLoss(initialLoss)).append("\n");
        sb.append("Final loss     : ").append(fmtLoss(finalLoss)).append("\n");
        if (!lossHistory.isEmpty()) {
            sb.append("Loss history   : ").append(lossHistory.size()).append(" epoch(s) — ")
                    .append(lossHistory).append("\n");
        }

        sb.append("\n--- PARAMETER CHANGE VERIFICATION ---\n");
        sb.append("Params changed : ").append(parametersChanged ? "YES" : "NO").append("\n");
        sb.append("Changed params : ").append(parameterChangeCount).append("\n");
        sb.append("Max abs delta  : ").append(String.format("%.3e", parameterChangeMaxAbs)).append("\n");

        sb.append("\n--- CUDA EXECUTION EVIDENCE ---\n");
        sb.append("Kernel launches: ").append(kernelLaunchDelta).append("\n");
        sb.append("Memcpy calls   : ").append(memcpyDelta).append("\n");
        sb.append("Device allocs  : ").append(memAllocDelta).append(" / frees: ").append(memFreeDelta).append("\n");
        sb.append("Free mem before: ").append(freeMemBeforeBytes).append(" bytes\n");
        sb.append("Free mem after : ").append(freeMemAfterBytes).append(" bytes\n");
        sb.append("Cleanup        : ").append(cleanupVerified ? "VERIFIED" : "NOT VERIFIED").append("\n");

        if (!failureDetail.isEmpty()) {
            sb.append("\nFailure detail : ").append(failureDetail).append("\n");
        }
        sb.append("=========================================================\n");
        return sb.toString();
    }

    private static String fmtLoss(double v) {
        return Double.isNaN(v) || Double.isInfinite(v) ? "N/A" : String.format("%.4f", v);
    }
}
