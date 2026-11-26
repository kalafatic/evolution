package eu.kalafatic.evolution.controller.hardware;

import eu.kalafatic.evolution.forge.trainer.device.ForgeDeviceSelector;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Centralized CUDA diagnostic service — the single authoritative source for
 * CUDA backend testing used by the Settings Hardware panel, the Forge page and
 * the AI Commander Forge panel.
 *
 * <p>The test exercises EVO's real CUDA implementation ({@code CudaDevice} in
 * the {@code eu.kalafatic.evolution.gpu} bundle): a small deterministic
 * {@code saxpy} kernel is compiled with NVRTC, executed on the device and
 * verified against an independently computed CPU reference. GPU/driver
 * detection alone is never reported as CUDA success.</p>
 *
 * <p>Access to the GPU bundle is reflective and goes through the
 * forge.trainer class loader, because the GPU classes are compiled for stable
 * Java 22 FFM while this bundle targets Java 21 (Tycho 4.0.5 limitation). On a
 * Java 21 JVM the backend classes fail to load with
 * {@link UnsupportedClassVersionError}; the service captures that explicitly
 * as a failed stage instead of misreporting CUDA as unavailable or silently
 * swallowing the error. On Java 22+ with a compatible NVIDIA GPU the full
 * pipeline executes.</p>
 *
 * <p>Lifecycle: results are kept in a bounded in-memory history (last 5);
 * listeners are notified on start and completion. If a test is already
 * running, {@link #startAsync()} returns the running report instead of
 * launching a duplicate.</p>
 */
public final class CudaDiagnosticService {

    public static final int HISTORY_LIMIT = 5;

    private static final int KERNEL_N = 4096;

    /** Small deterministic kernel: out[i] = a*x[i] + y[i]. */
    private static final String SAXPY_SOURCE = """
            extern "C" __global__ void saxpy(float* out, const float* x, const float* y, int n, float a) {
                int i = blockIdx.x * blockDim.x + threadIdx.x;
                if (i < n) out[i] = a * x[i] + y[i];
            }
            """;

    private static final CudaDiagnosticService INSTANCE = new CudaDiagnosticService();

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicReference<CudaDiagnosticReport> current =
            new AtomicReference<>(null);
    private final Deque<CudaDiagnosticReport> history = new ArrayDeque<>();
    private final List<Consumer<CudaDiagnosticReport>> listeners =
            new CopyOnWriteArrayList<>();

    private CudaDiagnosticService() {
    }

    public static CudaDiagnosticService getInstance() {
        return INSTANCE;
    }

    /** True while a diagnostic test is executing. */
    public boolean isRunning() {
        return running.get();
    }

    /** The report being executed, or the last completed one when idle. */
    public CudaDiagnosticReport getCurrent() {
        return current.get();
    }

    /** Bounded history of completed runs (newest first). */
    public synchronized List<CudaDiagnosticReport> getHistory() {
        return new ArrayList<>(history);
    }

    public void addListener(Consumer<CudaDiagnosticReport> listener) {
        listeners.add(listener);
    }

    public void removeListener(Consumer<CudaDiagnosticReport> listener) {
        listeners.remove(listener);
    }

    /**
     * Starts the diagnostic on the shared worker thread. If a test is already
     * running, the current (running) report is returned and no duplicate test
     * is launched. Never blocks.
     */
    public CudaDiagnosticReport startAsync() {
        if (!running.compareAndSet(false, true)) {
            return current.get();
        }
        final CudaDiagnosticReport report = new CudaDiagnosticReport();
        current.set(report);
        DiagnosticWorker.submit(() -> {
            try {
                execute(report);
            } finally {
                running.set(false);
                notifyListeners(report);
                synchronized (this) {
                    history.addFirst(report);
                    while (history.size() > HISTORY_LIMIT) {
                        history.removeLast();
                    }
                }
            }
        });
        return report;
    }

    /**
     * Executes the full stage pipeline synchronously on the calling thread.
     * Intended for tests and tooling; UI code must use {@link #startAsync()}.
     */
    public CudaDiagnosticReport run() {
        CudaDiagnosticReport report = new CudaDiagnosticReport();
        current.set(report);
        execute(report);
        notifyListeners(report);
        synchronized (this) {
            history.addFirst(report);
            while (history.size() > HISTORY_LIMIT) {
                history.removeLast();
            }
        }
        return report;
    }

    /**
     * Structured CUDA capability snapshot used by the hardware scan (and any
     * other consumer that needs detection without a full diagnostic run).
     */
    public static final class CudaCapability {
        public final boolean gpuBundlePresent;
        public final boolean gpuDetected;
        public final String gpuName;
        public final String driverVersion;
        public final boolean backendLoadable;
        public final boolean backendInitialized;
        public final String computeCapability;
        public final String detail;

        public CudaCapability(boolean gpuBundlePresent, boolean gpuDetected, String gpuName,
                              String driverVersion, boolean backendLoadable,
                              boolean backendInitialized, String computeCapability,
                              String detail) {
            this.gpuBundlePresent = gpuBundlePresent;
            this.gpuDetected = gpuDetected;
            this.gpuName = gpuName;
            this.driverVersion = driverVersion;
            this.backendLoadable = backendLoadable;
            this.backendInitialized = backendInitialized;
            this.computeCapability = computeCapability;
            this.detail = detail;
        }
    }

    /**
     * Lightweight CUDA detection used by the hardware scan: GPU device
     * detection (existing {@code NvidiaGpuDetector}), backend class
     * loadability on the current JVM and actual backend initialization
     * ({@code CudaDevice.tryCreate()}). No kernel is executed here — the full
     * 12-stage diagnostic does that.
     */
    public CudaCapability probeCudaCapability() {
        ClassLoader loader = ForgeDeviceSelector.class.getClassLoader();
        boolean bundlePresent = false;
        try {
            Class.forName("eu.kalafatic.evolution.gpu.diag.NvidiaGpuDetector", false, loader);
            bundlePresent = true;
        } catch (Throwable ignored) {
        }

        boolean detected = false;
        String name = "";
        String driver = "";
        String detail = "";
        if (bundlePresent) {
            try {
                Class<?> detectorClass = Class.forName(
                        "eu.kalafatic.evolution.gpu.diag.NvidiaGpuDetector", true, loader);
                Object detector = detectorClass.getDeclaredConstructor().newInstance();
                Object info = detectorClass.getMethod("detectGpu").invoke(detector);
                detected = (Boolean) info.getClass().getMethod("isAvailable").invoke(info);
                if (detected) {
                    name = String.valueOf(info.getClass().getMethod("getDeviceName").invoke(info));
                    driver = String.valueOf(info.getClass().getMethod("getDriverVersion").invoke(info));
                }
            } catch (Throwable t) {
                detail = "GPU detection failed: " + describe(t);
            }
        } else {
            detail = "GPU bundle not installed";
        }

        boolean loadable = false;
        boolean initialized = false;
        String computeCap = "";
        if (bundlePresent) {
            try {
                Class.forName("eu.kalafatic.evolution.gpu.cuda.CudaDevice", true, loader);
                Class.forName("eu.kalafatic.evolution.gpu.trainer.GpuTrainingEngine", true, loader);
                loadable = true;
            } catch (UnsupportedClassVersionError e) {
                detail = (detail.isEmpty() ? "" : detail + "; ")
                        + "Backend requires Java 22+ (current JVM "
                        + System.getProperty("java.version", "unknown") + ")";
            } catch (Throwable t) {
                detail = (detail.isEmpty() ? "" : detail + "; ")
                        + "Backend class loading failed: " + describe(t);
            }
            if (loadable) {
                try {
                    Class<?> cudaDevice = Class.forName(
                            "eu.kalafatic.evolution.gpu.cuda.CudaDevice", true, loader);
                    Object device = cudaDevice.getMethod("tryCreate").invoke(null);
                    initialized = device != null;
                    if (initialized) {
                        computeCap = cudaDevice.getMethod("getComputeCapabilityMajor").invoke(device)
                                + "." + cudaDevice.getMethod("getComputeCapabilityMinor").invoke(device);
                    } else {
                        detail = (detail.isEmpty() ? "" : detail + "; ")
                                + "CUDA device could not be initialized (driver/GPU unavailable)";
                    }
                } catch (Throwable t) {
                    detail = (detail.isEmpty() ? "" : detail + "; ")
                            + "CUDA initialization failed: " + describe(t);
                }
            }
        }
        return new CudaCapability(bundlePresent, detected, name, driver, loadable,
                initialized, computeCap, detail);
    }

    private void notifyListeners(CudaDiagnosticReport report) {
        for (Consumer<CudaDiagnosticReport> l : listeners) {
            try {
                l.accept(report);
            } catch (Throwable ignored) {
                // A broken UI listener must never break the worker thread.
            }
        }
    }

    // ------------------------------------------------------------------
    // Stage execution
    // ------------------------------------------------------------------
    private void execute(CudaDiagnosticReport r) {
        r.startedAt = Instant.now();

        ClassLoader gpuLoader = ForgeDeviceSelector.class.getClassLoader();
        r.gpuBundlePresent = probeBundle(gpuLoader);

        stage(r, CudaDiagnosticReport.STAGE_DEVICE_DETECTION, s -> detectGpu(r, gpuLoader, s));
        stage(r, CudaDiagnosticReport.STAGE_DRIVER_AVAILABILITY, s -> verifyDriver(r, gpuLoader, s));

        boolean backendLoaded = Boolean.TRUE.equals(stageValue(r,
                CudaDiagnosticReport.STAGE_BACKEND_CLASS_LOADING,
                s -> loadBackendClasses(gpuLoader, s)));

        Object device = null;
        if (backendLoaded) {
            device = stageValue(r, CudaDiagnosticReport.STAGE_NATIVE_LIBRARY_INIT,
                    s -> initializeNativeDevice(gpuLoader, s));
        }

        // Later stages depend on a live device.
        if (device != null) {
            runKernelPipeline(r, gpuLoader, device);
        }

        finish(r);
    }

    private boolean probeBundle(ClassLoader loader) {
        try {
            Class.forName("eu.kalafatic.evolution.gpu.diag.NvidiaGpuDetector", false, loader);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private void detectGpu(CudaDiagnosticReport r, ClassLoader loader, DiagnosticStage s) {
        if (!r.gpuBundlePresent) {
            s.status = StageStatus.UNSUPPORTED;
            s.detail = "GPU detection bundle (eu.kalafatic.evolution.gpu) is not installed";
            return;
        }
        try {
            Class<?> detectorClass = Class.forName(
                    "eu.kalafatic.evolution.gpu.diag.NvidiaGpuDetector", true, loader);
            Object detector = detectorClass.getDeclaredConstructor().newInstance();
            Object info = detectorClass.getMethod("detectGpu").invoke(detector);
            r.gpuDetected = (Boolean) info.getClass().getMethod("isAvailable").invoke(info);
            if (r.gpuDetected) {
                r.gpuName = String.valueOf(info.getClass().getMethod("getDeviceName").invoke(info));
                r.gpuTotalVramMb = (Long) info.getClass().getMethod("getTotalVramMb").invoke(info);
                r.gpuDriverVersion = String.valueOf(info.getClass().getMethod("getDriverVersion").invoke(info));
                r.gpuCudaDriverVersion = String.valueOf(info.getClass().getMethod("getCudaDriverVersion").invoke(info));
                s.status = StageStatus.PASS;
                s.detail = "Detected " + r.gpuName + " (" + r.gpuTotalVramMb + " MB, driver "
                        + r.gpuDriverVersion + ")";
            } else {
                s.status = StageStatus.FAILED;
                List<?> errors = (List<?>) info.getClass().getMethod("getConfigErrors").invoke(info);
                s.detail = "No NVIDIA GPU detected: " + errors;
            }
        } catch (Throwable t) {
            s.status = StageStatus.FAILED;
            s.detail = "GPU detection failed: " + describe(t);
        }
    }

    private void verifyDriver(CudaDiagnosticReport r, ClassLoader loader, DiagnosticStage s) {
        if (!r.gpuBundlePresent) {
            s.status = StageStatus.UNSUPPORTED;
            s.detail = "CUDA verifier bundle is not installed";
            return;
        }
        try {
            Class<?> verifierClass = Class.forName(
                    "eu.kalafatic.evolution.gpu.diag.CudaComputeVerifier", true, loader);
            Object verifier = verifierClass.getDeclaredConstructor().newInstance();
            Object info = verifierClass.getMethod("verifyCudaCompute").invoke(verifier);
            boolean driverFound = (Boolean) info.getClass().getMethod("isDriverLibraryFound").invoke(info);
            boolean runtimeFound = (Boolean) info.getClass().getMethod("isRuntimeLibraryFound").invoke(info);
            String deviceType = String.valueOf(info.getClass().getMethod("getComputeDeviceType").invoke(info));
            if (driverFound) {
                s.status = StageStatus.PASS;
                s.detail = "Driver library found; compute device type: " + deviceType
                        + (runtimeFound ? "; runtime library found" : "; runtime library NOT found");
            } else {
                s.status = StageStatus.FAILED;
                List<?> errors = (List<?>) info.getClass().getMethod("getErrorMessages").invoke(info);
                s.detail = "CUDA driver library not found: " + errors;
            }
        } catch (Throwable t) {
            s.status = StageStatus.FAILED;
            s.detail = "Driver verification failed: " + describe(t);
        }
    }

    private boolean loadBackendClasses(ClassLoader loader, DiagnosticStage s) {
        try {
            Class.forName("eu.kalafatic.evolution.gpu.cuda.CudaDevice", true, loader);
            Class.forName("eu.kalafatic.evolution.gpu.trainer.GpuTrainingEngine", true, loader);
            s.status = StageStatus.PASS;
            s.detail = "CudaDevice and GpuTrainingEngine loaded on JVM "
                    + System.getProperty("java.version", "unknown");
            return true;
        } catch (UnsupportedClassVersionError e) {
            s.status = StageStatus.FAILED;
            s.detail = "GPU backend is compiled for stable Java 22 FFM; current JVM ("
                    + System.getProperty("java.version", "unknown")
                    + ") cannot load the classes: " + e.getMessage()
                    + " — CPU-only operation remains fully functional";
            return false;
        } catch (Throwable t) {
            s.status = StageStatus.FAILED;
            s.detail = "GPU backend class loading failed: " + describe(t);
            return false;
        }
    }

    private Object initializeNativeDevice(ClassLoader loader, DiagnosticStage s) {
        try {
            Class<?> cudaDevice = Class.forName(
                    "eu.kalafatic.evolution.gpu.cuda.CudaDevice", true, loader);
            Method tryCreate = cudaDevice.getMethod("tryCreate");
            Object device = tryCreate.invoke(null);
            if (device == null) {
                s.status = StageStatus.FAILED;
                s.detail = "CudaDevice.tryCreate() returned null — no usable CUDA device "
                        + "(missing driver, no GPU or NVRTC unavailable)";
                return null;
            }
            s.status = StageStatus.PASS;
            s.detail = "CUDA driver initialized (cuInit) and native libraries loaded";
            return device;
        } catch (Throwable t) {
            s.status = StageStatus.FAILED;
            s.detail = "Native library initialization failed: " + describe(t);
            return null;
        }
    }

    private void runKernelPipeline(CudaDiagnosticReport r, ClassLoader loader, Object device) {
        try {
            Class<?> cudaDevice = device.getClass();
            Method memGetInfo = cudaDevice.getMethod("memGetInfo");
            Method alloc = cudaDevice.getMethod("alloc", long.class);
            Method free = cudaDevice.getMethod("free", long.class);
            Method memcpyHtoD = cudaDevice.getMethod("memcpyHtoD", long.class, float[].class);
            Method memcpyDtoH = cudaDevice.getMethod("memcpyDtoH", float[].class, long.class);
            Method compile = cudaDevice.getMethod("compile", String.class, String.class);
            Method synchronize = cudaDevice.getMethod("synchronize");

            // Context stage: a live context must answer cuMemGetInfo.
            long[] mem = (long[]) stageValue(r, CudaDiagnosticReport.STAGE_CONTEXT_INIT, s -> {
                try {
                    long[] m = (long[]) memGetInfo.invoke(device);
                    s.status = StageStatus.PASS;
                    s.detail = "CUDA context usable (free=" + m[0] + ", total=" + m[1] + " bytes)";
                    return m;
                } catch (Throwable t) {
                    s.status = StageStatus.FAILED;
                    s.detail = "cuMemGetInfo failed: " + describe(t);
                    return null;
                }
            });
            if (mem == null) {
                finish(r);
                return;
            }
            r.freeMemBeforeBytes = mem[0];

            // Independent CPU reference.
            float a = 2.5f;
            float[] x = new float[KERNEL_N];
            float[] y = new float[KERNEL_N];
            float[] expected = new float[KERNEL_N];
            java.util.Random rnd = new java.util.Random(1234L);
            for (int i = 0; i < KERNEL_N; i++) {
                x[i] = rnd.nextFloat() * 10.0f - 5.0f;
                y[i] = rnd.nextFloat() * 10.0f - 5.0f;
                expected[i] = a * x[i] + y[i];
            }

            final long[] devPtrs = new long[3]; // x, y, out
            stage(r, CudaDiagnosticReport.STAGE_MEMORY_ALLOCATION, s -> {
                try {
                    devPtrs[0] = (Long) alloc.invoke(device, (long) KERNEL_N * 4);
                    devPtrs[1] = (Long) alloc.invoke(device, (long) KERNEL_N * 4);
                    devPtrs[2] = (Long) alloc.invoke(device, (long) KERNEL_N * 4);
                    r.deviceMemoryAllocatedBytes = 3L * KERNEL_N * 4;
                    s.status = StageStatus.PASS;
                    s.detail = "Allocated 3 × " + (KERNEL_N * 4) + " bytes of device memory";
                } catch (Throwable t) {
                    s.status = StageStatus.FAILED;
                    s.detail = "cuMemAlloc failed: " + describe(t);
                }
            });
            if (!r.stage(CudaDiagnosticReport.STAGE_MEMORY_ALLOCATION).passed()) {
                finish(r);
                return;
            }

            stage(r, CudaDiagnosticReport.STAGE_HOST_TO_DEVICE, s -> {
                try {
                    memcpyHtoD.invoke(device, devPtrs[0], x);
                    memcpyHtoD.invoke(device, devPtrs[1], y);
                    s.status = StageStatus.PASS;
                    s.detail = "Transferred " + (2L * KERNEL_N * 4) + " bytes host → device";
                } catch (Throwable t) {
                    s.status = StageStatus.FAILED;
                    s.detail = "cuMemcpyHtoD failed: " + describe(t);
                }
            });
            if (!r.stage(CudaDiagnosticReport.STAGE_HOST_TO_DEVICE).passed()) {
                finish(r);
                return;
            }

            stage(r, CudaDiagnosticReport.STAGE_KERNEL_EXECUTION, s -> {
                try {
                    Object module = compile.invoke(device, "evo_diag_saxpy", SAXPY_SOURCE);
                    Method function = module.getClass().getMethod("function", String.class);
                    Object kernel = function.invoke(module, "saxpy");
                    Method launch = kernel.getClass().getMethod("launch", int.class, List.class);
                    launch.invoke(kernel, KERNEL_N, List.of(devPtrs[2], devPtrs[0], devPtrs[1],
                            KERNEL_N, a));
                    synchronize.invoke(device);
                    r.kernelElementCount = KERNEL_N;
                    s.status = StageStatus.PASS;
                    s.detail = "NVRTC-compiled saxpy kernel executed over " + KERNEL_N
                            + " elements and synchronized";
                } catch (Throwable t) {
                    s.status = StageStatus.FAILED;
                    s.detail = "Kernel execution failed: " + describe(t);
                }
            });
            if (!r.stage(CudaDiagnosticReport.STAGE_KERNEL_EXECUTION).passed()) {
                finish(r);
                return;
            }

            final float[] out = new float[KERNEL_N];
            stage(r, CudaDiagnosticReport.STAGE_DEVICE_TO_HOST, s -> {
                try {
                    memcpyDtoH.invoke(device, out, devPtrs[2]);
                    s.status = StageStatus.PASS;
                    s.detail = "Transferred " + (KERNEL_N * 4) + " bytes device → host";
                } catch (Throwable t) {
                    s.status = StageStatus.FAILED;
                    s.detail = "cuMemcpyDtoH failed: " + describe(t);
                }
            });
            if (!r.stage(CudaDiagnosticReport.STAGE_DEVICE_TO_HOST).passed()) {
                finish(r);
                return;
            }

            stage(r, CudaDiagnosticReport.STAGE_NUMERICAL_VERIFICATION, s -> {
                double maxAbs = 0.0;
                for (int i = 0; i < KERNEL_N; i++) {
                    maxAbs = Math.max(maxAbs, Math.abs(out[i] - expected[i]));
                }
                r.maxAbsDiff = maxAbs;
                r.numericalVerificationPassed = maxAbs < 1e-4;
                if (r.numericalVerificationPassed) {
                    s.status = StageStatus.PASS;
                    s.detail = String.format("GPU result matches CPU reference (max |diff| = %.3e)", maxAbs);
                } else {
                    s.status = StageStatus.FAILED;
                    s.detail = String.format("GPU result deviates from CPU reference (max |diff| = %.3e)",
                            maxAbs);
                }
            });

            stage(r, CudaDiagnosticReport.STAGE_MEMORY_CLEANUP, s -> {
                try {
                    free.invoke(device, devPtrs[2]);
                    free.invoke(device, devPtrs[1]);
                    free.invoke(device, devPtrs[0]);
                    long[] after = (long[]) memGetInfo.invoke(device);
                    r.freeMemAfterBytes = after[0];
                    long leaked = r.freeMemBeforeBytes - after[0];
                    r.cleanupPassed = leaked < 64L * 1024 * 1024;
                    s.status = r.cleanupPassed ? StageStatus.PASS : StageStatus.FAILED;
                    s.detail = "Released 3 device buffers; free memory delta = "
                            + leaked + " bytes (before " + r.freeMemBeforeBytes
                            + ", after " + after[0] + ")";
                } catch (Throwable t) {
                    s.status = StageStatus.FAILED;
                    s.detail = "Cleanup failed: " + describe(t);
                }
            });

            // Execution evidence from the instrumented backend.
            try {
                r.kernelLaunchCount = (Long) cudaDevice.getMethod("getKernelLaunchCount").invoke(device);
                r.memcpyCount = (Long) cudaDevice.getMethod("getMemcpyCount").invoke(device);
                r.memAllocCount = (Long) cudaDevice.getMethod("getMemAllocCount").invoke(device);
                r.memFreeCount = (Long) cudaDevice.getMethod("getMemFreeCount").invoke(device);
            } catch (Throwable t) {
                r.failureDetail = "Backend instrumentation unavailable: " + describe(t);
            }
            try {
                r.computeCapability = cudaDevice.getMethod("getComputeCapabilityMajor").invoke(device)
                        + "." + cudaDevice.getMethod("getComputeCapabilityMinor").invoke(device);
            } catch (Throwable ignored) {
            }
        } catch (Throwable t) {
            // Defensive: stage pipeline setup itself failed.
            r.failureDetail = "Diagnostic pipeline error: " + describe(t);
        }
    }

    private void finish(CudaDiagnosticReport r) {
        // Mark every stage that never ran as SKIPPED with an explicit note.
        for (DiagnosticStage s : r.getStages()) {
            if (s.status == StageStatus.SKIPPED && s.detail.isEmpty()) {
                s.detail = "Not executed";
            }
        }
        DiagnosticStage finalStage = r.stage(CudaDiagnosticReport.STAGE_FINAL);
        if (finalStage.status == StageStatus.SKIPPED) {
            finalStage.status = StageStatus.PASS;
            finalStage.detail = "Result aggregation";
        }
        r.overallStatus = computeOverall(r);
        r.finishedAt = Instant.now();
    }

    private DiagnosticStatus computeOverall(CudaDiagnosticReport r) {
        boolean deviceDetected = r.stage(CudaDiagnosticReport.STAGE_DEVICE_DETECTION).passed();
        boolean backendLoaded = r.stage(CudaDiagnosticReport.STAGE_BACKEND_CLASS_LOADING).passed();
        boolean kernelPassed = r.stage(CudaDiagnosticReport.STAGE_KERNEL_EXECUTION).passed();
        boolean numericPassed = r.stage(CudaDiagnosticReport.STAGE_NUMERICAL_VERIFICATION).passed();
        boolean cleanupPassed = r.stage(CudaDiagnosticReport.STAGE_MEMORY_CLEANUP).passed();

        if (!deviceDetected) {
            // No NVIDIA GPU (or nvidia-smi/driver unavailable): CUDA is not
            // supported on this machine, regardless of the JVM.
            return DiagnosticStatus.UNSUPPORTED;
        }
        if (!backendLoaded) {
            // GPU present but EVO's backend cannot load (unsupported JVM
            // version, missing bundle, ...): detection succeeded, CUDA could
            // not be exercised.
            return DiagnosticStatus.PARTIAL;
        }
        if (!kernelPassed) {
            return DiagnosticStatus.FAILED;
        }
        if (!numericPassed) {
            return DiagnosticStatus.FAILED;
        }
        if (!cleanupPassed) {
            return DiagnosticStatus.PARTIAL;
        }
        return DiagnosticStatus.PASS;
    }

    /** Runs one stage, records timestamps/duration and passes detail through. */
    private static void stage(CudaDiagnosticReport r, String id, Consumer<DiagnosticStage> body) {
        DiagnosticStage s = r.stage(id);
        s.startedAt = Instant.now();
        try {
            body.accept(s);
        } catch (Throwable t) {
            s.status = StageStatus.FAILED;
            if (s.detail.isEmpty()) {
                s.detail = describe(t);
            }
        }
        s.finishedAt = Instant.now();
        s.durationMs = Math.max(0, s.finishedAt.toEpochMilli() - s.startedAt.toEpochMilli());
    }

    private static <T> T stageValue(CudaDiagnosticReport r, String id,
                                    java.util.function.Function<DiagnosticStage, T> body) {
        DiagnosticStage s = r.stage(id);
        s.startedAt = Instant.now();
        try {
            return body.apply(s);
        } catch (Throwable t) {
            s.status = StageStatus.FAILED;
            if (s.detail.isEmpty()) {
                s.detail = describe(t);
            }
            return null;
        } finally {
            s.finishedAt = Instant.now();
            s.durationMs = Math.max(0, s.finishedAt.toEpochMilli() - s.startedAt.toEpochMilli());
        }
    }

    private static String describe(Throwable t) {
        String msg = t.getMessage();
        String cls = t.getClass().getSimpleName();
        return msg == null || msg.isEmpty() ? cls : cls + ": " + msg;
    }
}
