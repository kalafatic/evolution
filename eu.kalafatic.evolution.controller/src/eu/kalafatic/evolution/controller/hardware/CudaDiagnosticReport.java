package eu.kalafatic.evolution.controller.hardware;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Structured result of the centralized CUDA diagnostic test.
 *
 * <p>The test exercises EVO's actual CUDA implementation (the
 * {@code eu.kalafatic.evolution.gpu} backend): device detection, driver
 * availability, backend class loading, native initialization, CUDA context,
 * device memory allocation, host-to-device transfer, real NVRTC-compiled
 * kernel execution, device-to-host transfer, numerical verification against an
 * independent CPU reference and resource cleanup. Detection of an NVIDIA GPU
 * or driver is NOT reported as CUDA execution success — only an executed,
 * numerically verified kernel is.</p>
 */
public final class CudaDiagnosticReport {

    public static final String STAGE_DEVICE_DETECTION = "gpu_device_detection";
    public static final String STAGE_DRIVER_AVAILABILITY = "cuda_driver_availability";
    public static final String STAGE_BACKEND_CLASS_LOADING = "backend_class_loading";
    public static final String STAGE_NATIVE_LIBRARY_INIT = "native_library_initialization";
    public static final String STAGE_CONTEXT_INIT = "cuda_context_initialization";
    public static final String STAGE_MEMORY_ALLOCATION = "device_memory_allocation";
    public static final String STAGE_HOST_TO_DEVICE = "host_to_device_transfer";
    public static final String STAGE_KERNEL_EXECUTION = "kernel_execution";
    public static final String STAGE_DEVICE_TO_HOST = "device_to_host_transfer";
    public static final String STAGE_NUMERICAL_VERIFICATION = "numerical_verification";
    public static final String STAGE_MEMORY_CLEANUP = "gpu_memory_cleanup";
    public static final String STAGE_FINAL = "final_result";

    /** Stage IDs in execution order. */
    public static final List<String> STAGE_ORDER = List.of(
            STAGE_DEVICE_DETECTION,
            STAGE_DRIVER_AVAILABILITY,
            STAGE_BACKEND_CLASS_LOADING,
            STAGE_NATIVE_LIBRARY_INIT,
            STAGE_CONTEXT_INIT,
            STAGE_MEMORY_ALLOCATION,
            STAGE_HOST_TO_DEVICE,
            STAGE_KERNEL_EXECUTION,
            STAGE_DEVICE_TO_HOST,
            STAGE_NUMERICAL_VERIFICATION,
            STAGE_MEMORY_CLEANUP,
            STAGE_FINAL);

    public final String executionId = UUID.randomUUID().toString();
    public volatile Instant startedAt = Instant.now();
    public volatile Instant finishedAt = startedAt;
    public volatile DiagnosticStatus overallStatus = DiagnosticStatus.RUNNING;

    /** Detected GPU information (independent of CUDA capability). */
    public volatile boolean gpuDetected;
    public volatile String gpuName = "";
    public volatile long gpuTotalVramMb;
    public volatile String gpuDriverVersion = "";
    public volatile String gpuCudaDriverVersion = "";
    public volatile String computeCapability = "";

    /** JVM / runtime context. */
    public volatile String jvmVersion = System.getProperty("java.version", "");
    public volatile int jvmMajor = parseJvmMajor();
    public volatile boolean gpuBundlePresent;

    /** Evidence from the executed computation. */
    public volatile long deviceMemoryAllocatedBytes;
    public volatile int kernelElementCount;
    public volatile double maxAbsDiff;
    public volatile boolean numericalVerificationPassed;
    public volatile long kernelLaunchCount;
    public volatile long memcpyCount;
    public volatile long memAllocCount;
    public volatile long memFreeCount;
    public volatile long freeMemBeforeBytes;
    public volatile long freeMemAfterBytes;
    public volatile boolean cleanupPassed;
    public volatile String failureDetail = "";

    private final List<DiagnosticStage> stages = new ArrayList<>();

    public CudaDiagnosticReport() {
        for (String id : STAGE_ORDER) {
            stages.add(new DiagnosticStage(id, stageName(id)));
        }
    }

    public List<DiagnosticStage> getStages() {
        return Collections.unmodifiableList(stages);
    }

    public DiagnosticStage stage(String id) {
        for (DiagnosticStage s : stages) {
            if (s.id.equals(id)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown stage id: " + id);
    }

    public long totalDurationMs() {
        return Math.max(0, finishedAt.toEpochMilli() - startedAt.toEpochMilli());
    }

    public boolean kernelExecuted() {
        return stage(STAGE_KERNEL_EXECUTION).passed();
    }

    private static String stageName(String id) {
        switch (id) {
            case STAGE_DEVICE_DETECTION: return "GPU device detection";
            case STAGE_DRIVER_AVAILABILITY: return "CUDA driver availability";
            case STAGE_BACKEND_CLASS_LOADING: return "EVO GPU backend class loading";
            case STAGE_NATIVE_LIBRARY_INIT: return "CUDA native library initialization";
            case STAGE_CONTEXT_INIT: return "CUDA context initialization";
            case STAGE_MEMORY_ALLOCATION: return "Device memory allocation";
            case STAGE_HOST_TO_DEVICE: return "Host-to-device transfer";
            case STAGE_KERNEL_EXECUTION: return "CUDA kernel execution";
            case STAGE_DEVICE_TO_HOST: return "Device-to-host transfer";
            case STAGE_NUMERICAL_VERIFICATION: return "Numerical result verification";
            case STAGE_MEMORY_CLEANUP: return "GPU memory cleanup";
            case STAGE_FINAL: return "Final result";
            default: return id;
        }
    }

    private static int parseJvmMajor() {
        String v = System.getProperty("java.version", "0");
        try {
            String[] parts = v.split("\\.");
            if (parts.length > 0 && parts[0].matches("\\d+")) {
                int major = Integer.parseInt(parts[0]);
                return major > 1 ? major : Integer.parseInt(parts[1]);
            }
        } catch (RuntimeException ignored) {
        }
        return 0;
    }

    /** Human-readable, copy-friendly report used by the "View Details" dialogs. */
    public String toText() {
        StringBuilder sb = new StringBuilder();
        sb.append("=========================================================\n");
        sb.append("        EVO CUDA BACKEND DIAGNOSTIC REPORT                \n");
        sb.append("=========================================================\n");
        sb.append("Execution ID   : ").append(executionId).append("\n");
        sb.append("Started        : ").append(startedAt).append("\n");
        sb.append("Finished       : ").append(finishedAt).append("\n");
        sb.append("Duration       : ").append(totalDurationMs()).append(" ms\n");
        sb.append("JVM            : ").append(jvmVersion).append(" (major ").append(jvmMajor).append(")\n");
        sb.append("Overall        : ").append(overallStatus).append("\n\n");

        sb.append("--- GPU DETECTION ---\n");
        sb.append("GPU detected   : ").append(gpuDetected ? "YES" : "NO").append("\n");
        sb.append("Device         : ").append(gpuName.isEmpty() ? "N/A" : gpuName).append("\n");
        sb.append("Total VRAM     : ").append(gpuTotalVramMb > 0 ? gpuTotalVramMb + " MB" : "N/A").append("\n");
        sb.append("Driver         : ").append(gpuDriverVersion.isEmpty() ? "N/A" : gpuDriverVersion).append("\n");
        sb.append("CUDA driver    : ").append(gpuCudaDriverVersion.isEmpty() ? "N/A" : gpuCudaDriverVersion).append("\n");
        sb.append("Compute cap.   : ").append(computeCapability.isEmpty() ? "N/A" : computeCapability).append("\n\n");

        sb.append("--- EXECUTION EVIDENCE ---\n");
        sb.append("Device mem     : ").append(deviceMemoryAllocatedBytes).append(" bytes\n");
        sb.append("Kernel size    : ").append(kernelElementCount).append(" elements\n");
        sb.append("Kernel launches: ").append(kernelLaunchCount).append("\n");
        sb.append("Memcpy calls   : ").append(memcpyCount).append("\n");
        sb.append("Device allocs  : ").append(memAllocCount).append(" / frees: ").append(memFreeCount).append("\n");
        sb.append("Free mem before: ").append(freeMemBeforeBytes).append(" bytes\n");
        sb.append("Free mem after : ").append(freeMemAfterBytes).append(" bytes\n");
        sb.append("Max abs diff   : ").append(numericalVerificationPassed
                ? String.format("%.3e", maxAbsDiff) : "N/A").append("\n");
        sb.append("Numerical check: ").append(numericalVerificationPassed ? "PASS" : "FAIL").append("\n");
        sb.append("Cleanup        : ").append(cleanupPassed ? "PASS" : "FAIL").append("\n\n");

        sb.append("--- STAGES ---\n");
        for (DiagnosticStage s : stages) {
            sb.append(s).append("\n");
        }
        if (!failureDetail.isEmpty()) {
            sb.append("\nFailure detail : ").append(failureDetail).append("\n");
        }
        sb.append("=========================================================\n");
        return sb.toString();
    }
}
