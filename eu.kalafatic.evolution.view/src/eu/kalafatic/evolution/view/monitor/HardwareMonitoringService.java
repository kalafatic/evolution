package eu.kalafatic.evolution.view.monitor;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Centralized hardware monitoring service — the single authoritative source
 * for CPU / RAM / GPU metrics, the hardware inventory and the monitoring
 * lifecycle. All hardware-panel consumers (Settings Hardware panel, Forge
 * page, AI Commander Forge panel) share this one instance: there is exactly
 * one sampling thread regardless of how many UI components are open.
 *
 * <p>Lifecycle and threading:</p>
 * <ul>
 *   <li>{@link #start()}/{@link #stop()} manage the single sampling collector.</li>
 *   <li>Samples are appended to a bounded history (default 300) and pushed to
 *       registered listeners on the sampling thread; listeners marshal to SWT
 *       themselves ({@code Display.asyncExec}).</li>
 *   <li>{@link #refreshNow()} collects one snapshot synchronously.</li>
 *   <li>{@link #scan(Consumer)} runs the one-shot hardware scan on a dedicated
 *       background thread and reports progress through the callback.</li>
 * </ul>
 *
 * <p>The service never touches SWT and is fully testable headless.</p>
 */
public final class HardwareMonitoringService {

    public static final int DEFAULT_HISTORY_LIMIT = 300;
    public static final long DEFAULT_INTERVAL_MS = 1000;

    private static final HardwareMonitoringService INSTANCE = new HardwareMonitoringService();

    private final List<Consumer<HardwareSnapshot>> listeners = new CopyOnWriteArrayList<>();
    private final Deque<HardwareSnapshot> history = new ArrayDeque<>();
    private final AtomicBoolean scanning = new AtomicBoolean(false);

    private volatile HardwareMetricsCollector collector;
    private volatile long intervalMs = DEFAULT_INTERVAL_MS;
    private volatile int historyLimit = DEFAULT_HISTORY_LIMIT;
    private volatile HardwareSnapshot latest = HardwareSnapshot.empty();
    private volatile HardwareScanResult lastScan;

    private HardwareMonitoringService() {
    }

    public static HardwareMonitoringService getInstance() {
        return INSTANCE;
    }

    // ------------------------------------------------------------------
    // Monitoring lifecycle
    // ------------------------------------------------------------------
    public synchronized void start() {
        if (collector != null && collector.isRunning()) {
            return;
        }
        collector = new HardwareMetricsCollector(intervalMs, this::onSample);
        collector.start();
    }

    public synchronized void stop() {
        HardwareMetricsCollector c = collector;
        collector = null;
        if (c != null) {
            c.stop();
        }
    }

    public synchronized boolean isRunning() {
        return collector != null && collector.isRunning();
    }

    public synchronized long getIntervalMs() {
        return intervalMs;
    }

    /** Changes the sampling interval; takes effect on the next {@link #start()}. */
    public synchronized void setIntervalMs(long intervalMs) {
        this.intervalMs = Math.max(200, intervalMs);
        boolean wasRunning = isRunning();
        stop();
        if (wasRunning) {
            start();
        }
    }

    /** The most recent snapshot (never null). */
    public HardwareSnapshot getLatest() {
        return latest;
    }

    /** Copy of the bounded history (oldest first). */
    public synchronized List<HardwareSnapshot> getHistory() {
        return new ArrayList<>(history);
    }

    public synchronized int getHistoryLimit() {
        return historyLimit;
    }

    /** Configures the bounded history window (applies to subsequent samples). */
    public synchronized void setHistoryLimit(int limit) {
        this.historyLimit = Math.max(1, limit);
        while (history.size() > historyLimit) {
            history.removeFirst();
        }
    }

    /**
     * Collects one snapshot synchronously and delivers it to the shared
     * history and listeners. Safe to call while monitoring is running
     * (collection is serialized).
     */
    public HardwareSnapshot refreshNow() {
        HardwareMetricsCollector c = collector;
        if (c == null) {
            // No active collector: create a transient one for a single sample.
            c = new HardwareMetricsCollector(intervalMs, this::onSample);
        }
        HardwareSnapshot s = c.collect();
        onSample(s);
        return s;
    }

    public void addListener(Consumer<HardwareSnapshot> listener) {
        listeners.add(listener);
    }

    public void removeListener(Consumer<HardwareSnapshot> listener) {
        listeners.remove(listener);
    }

    private void onSample(HardwareSnapshot s) {
        latest = s;
        synchronized (this) {
            history.addLast(s);
            while (history.size() > historyLimit) {
                history.removeFirst();
            }
        }
        for (Consumer<HardwareSnapshot> l : listeners) {
            try {
                l.accept(s);
            } catch (Throwable ignored) {
            }
        }
    }

    // ------------------------------------------------------------------
    // One-shot hardware scan
    // ------------------------------------------------------------------
    /** True while a scan is executing. */
    public boolean isScanning() {
        return scanning.get();
    }

    /** Result of the last scan, or null. */
    public HardwareScanResult getLastScan() {
        return lastScan;
    }

    /**
     * Runs the full hardware scan asynchronously on a dedicated background
     * thread. {@code progress} receives human-readable stage descriptions on
     * the scan thread. The result is stored as {@link #getLastScan()} and
     * returned to the completion callback. Repeated scans are allowed;
     * concurrent scans are serialized.
     */
    public void scan(Consumer<String> progress, Consumer<HardwareScanResult> onDone) {
        if (!scanning.compareAndSet(false, true)) {
            // A scan is already running; report its progress through onDone when it completes.
            HardwareScanResult current = lastScan;
            if (onDone != null) {
                onDone.accept(current);
            }
            return;
        }
        ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "evo-hardware-scan");
            t.setDaemon(true);
            return t;
        });
        executor.submit(() -> {
            HardwareScanResult result = new HardwareScanResult();
            try {
                result = executeScan(progress);
                lastScan = result;
            } catch (Throwable t) {
                result = new HardwareScanResult();
                result.status = HardwareScanResult.ScanStatus.FAILED;
                result.notes.add("Scan failed: " + t);
                lastScan = result;
            } finally {
                result.finishedAt = java.time.Instant.now();
                scanning.set(false);
                executor.shutdown();
                if (onDone != null) {
                    try {
                        onDone.accept(result);
                    } catch (Throwable ignored) {
                    }
                }
            }
        });
    }

    /**
     * Executes the full scan synchronously on the calling thread.
     * Intended for tests and tooling; UI code must use {@link #scan(Consumer, Consumer)}.
     */
    public HardwareScanResult runScan() {
        HardwareScanResult result = executeScan(msg -> {
        });
        result.finishedAt = java.time.Instant.now();
        lastScan = result;
        return result;
    }

    private void progress(Consumer<String> callback, String message) {
        if (callback != null) {
            try {
                callback.accept(message);
            } catch (Throwable ignored) {
            }
        }
    }

    private HardwareScanResult executeScan(Consumer<String> progress) {
        HardwareScanResult result = new HardwareScanResult();
        result.startedAt = java.time.Instant.now();
        boolean partial = false;

        // 1. CPU resources.
        this.progress(progress, "Detecting CPU resources…");
        double freq = CpuMetricsReader.currentFrequencyMhz();
        double maxFreq = CpuMetricsReader.maxFrequencyMhz();
        double temp = CpuMetricsReader.temperatureCelsius();
        double load = CpuMetricsReader.loadAverage1m();
        int physical = CpuMetricsReader.physicalCores();
        int logical = Runtime.getRuntime().availableProcessors();
        result.cpu = new HardwareScanResult.CpuInfo(CpuMetricsReader.vendor(),
                new CpuMetricsReader().model(), CpuMetricsReader.architecture(),
                physical, logical, freq, maxFreq, temp, load);
        if (physical <= 0 || logical <= 0) {
            partial = true;
            result.notes.add("CPU core count could not be fully resolved.");
        }

        // 2. Physical memory.
        this.progress(progress, "Detecting physical memory…");
        long[] ram = new RamMetricsReader().read();
        result.ram = new HardwareScanResult.RamInfo(
                Math.max(0, ram[0]), Math.max(0, ram[1]), Math.max(0, ram[2]), Math.max(0, ram[3]));
        if (result.ram.totalBytes <= 0) {
            partial = true;
            result.notes.add("Physical memory could not be read.");
        }

        // 3. JVM memory (distinct from physical RAM).
        Runtime rt = Runtime.getRuntime();
        result.jvm = new HardwareScanResult.JvmInfo(
                System.getProperty("java.version", "Unknown"),
                rt.totalMemory() - rt.freeMemory(), rt.maxMemory(),
                java.lang.management.ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage().getUsed());

        // 4. GPU devices.
        this.progress(progress, "Detecting GPU devices…");
        List<HardwareSnapshot.Gpu> gpus = new GpuMetricsReader().parseAll();
        if (gpus.isEmpty()) {
            result.cudaStatus = HardwareScanResult.CudaStatus.NOT_PRESENT;
            result.notes.add("No NVIDIA GPU reported by nvidia-smi.");
        } else {
            for (HardwareSnapshot.Gpu g : gpus) {
                result.gpus.add(new HardwareScanResult.GpuInfo(g.index, "NVIDIA", g.name,
                        g.vramTotalBytes / (1024 * 1024), g.vramUsedBytes / (1024 * 1024),
                        g.vramFreeBytes / (1024 * 1024), g.driverVersion, "",
                        g.utilizationPercent, g.memCtrlUtilizationPercent, g.temperatureCelsius,
                        g.powerWatts, g.powerLimitWatts, g.smClockMhz));
            }
        }

        // 5. CUDA availability (authoritative probe from the controller
        // diagnostic service — no duplicated CUDA detection here).
        this.progress(progress, "Detecting CUDA availability…");
        try {
            eu.kalafatic.evolution.controller.hardware.CudaDiagnosticService.CudaCapability cap =
                    eu.kalafatic.evolution.controller.hardware.CudaDiagnosticService.getInstance()
                            .probeCudaCapability();
            result.cudaDetail = cap.detail;
            if (cap.backendInitialized) {
                result.cudaStatus = HardwareScanResult.CudaStatus.DETECTED;
            } else if (cap.gpuDetected && cap.backendLoadable) {
                result.cudaStatus = HardwareScanResult.CudaStatus.BACKEND_UNAVAILABLE;
            } else if (cap.gpuDetected) {
                result.cudaStatus = HardwareScanResult.CudaStatus.UNSUPPORTED_JVM;
            } else if (cap.gpuBundlePresent) {
                result.cudaStatus = HardwareScanResult.CudaStatus.NOT_PRESENT;
            } else {
                result.cudaStatus = HardwareScanResult.CudaStatus.UNKNOWN;
            }
            // Enrich GPU inventory with the detected compute capability.
            if (!cap.computeCapability.isEmpty()) {
                for (int i = 0; i < result.gpus.size(); i++) {
                    HardwareScanResult.GpuInfo g = result.gpus.get(i);
                    result.gpus.set(i, new HardwareScanResult.GpuInfo(g.index, g.vendor, g.name,
                            g.totalVramMb, g.usedVramMb, g.freeVramMb, g.driverVersion,
                            cap.computeCapability, g.utilizationPercent,
                            g.memCtrlUtilizationPercent, g.temperatureCelsius,
                            g.powerWatts, g.powerLimitWatts, g.smClockMhz));
                }
            }
            if (cap.gpuDetected && !cap.backendInitialized) {
                partial = true;
                if (!cap.detail.isEmpty()) {
                    result.notes.add(cap.detail);
                }
            }
        } catch (Throwable t) {
            partial = true;
            result.cudaStatus = HardwareScanResult.CudaStatus.UNKNOWN;
            result.cudaDetail = "CUDA probe failed: " + t;
            result.notes.add("CUDA probe failed: " + t);
        }

        this.progress(progress, "Completing scan…");
        result.status = partial
                ? HardwareScanResult.ScanStatus.PARTIAL
                : HardwareScanResult.ScanStatus.SUCCESS;
        result.finishedAt = java.time.Instant.now();
        return result;
    }
}
