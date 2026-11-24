package eu.kalafatic.evolution.view.monitor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Centralised hardware-metrics collector. Periodically samples GPU, CPU and RAM using
 * lightweight readers and delivers immutable {@link HardwareSnapshot}s to a registered
 * consumer. The sampling runs on a dedicated background thread and never touches SWT,
 * so the UI thread is never blocked.
 *
 * <p>Lifecycle: {@link #start()} begins sampling; {@link #stop()} shuts the executor down
 * and releases the readers. Instances are reusable across start/stop cycles.</p>
 */
public final class HardwareMetricsCollector {

    private final long intervalMs;
    private final Consumer<HardwareSnapshot> consumer;

    private final GpuMetricsReader gpuReader = new GpuMetricsReader();
    private final CpuMetricsReader cpuReader = new CpuMetricsReader();
    private final RamMetricsReader ramReader = new RamMetricsReader();

    private volatile ScheduledExecutorService executor;
    private volatile boolean stopped = false;

    public HardwareMetricsCollector(long intervalMs, Consumer<HardwareSnapshot> consumer) {
        this.intervalMs = intervalMs;
        this.consumer = consumer;
    }

    public synchronized void start() {
        if (executor != null) return;
        stopped = false;
        executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "evo-hardware-monitor");
            t.setDaemon(true);
            return t;
        });
        executor.scheduleWithFixedDelay(this::sample, 0, intervalMs, TimeUnit.MILLISECONDS);
    }

    public synchronized void stop() {
        stopped = true;
        ScheduledExecutorService e = executor;
        executor = null;
        if (e != null) {
            e.shutdownNow();
        }
    }

    private void sample() {
        try {
            HardwareSnapshot s = collect();
            if (consumer != null && !stopped) {
                consumer.accept(s);
            }
        } catch (Throwable ignored) {
            // Sampling must never kill the scheduler.
        }
    }

    HardwareSnapshot collect() {
        HardwareSnapshot.Gpu gpu = gpuReader.parse();
        double[] perCore = cpuReader.sampleUtilization();
        double cpuUtil = cpuReader.overallUtilization(perCore);
        long[] ram = ramReader.read();
        long heapUsed = usedHeap();

        boolean gpuAvailable = gpu != null && gpu.available;
        String gpuName = gpu != null ? gpu.name : "No GPU";
        int gpuUtil = gpu != null ? gpu.utilizationPercent : -1;
        long vramUsed = gpu != null ? gpu.vramUsedBytes : 0;
        long vramTotal = gpu != null ? gpu.vramTotalBytes : 0;
        int gpuTemp = gpu != null ? gpu.temperatureCelsius : -1;
        double gpuPower = gpu != null ? gpu.powerWatts : -1;
        double gpuLimit = gpu != null ? gpu.powerLimitWatts : -1;
        String gpuState = gpu != null ? gpu.state : "N/A";

        List<Double> perCoreList = new ArrayList<>();
        for (double u : perCore) perCoreList.add(u);

        return new HardwareSnapshot(gpuAvailable, gpuName, gpuUtil, vramUsed, vramTotal, gpuTemp,
                gpuPower, gpuLimit, gpuState, cudaStatus(), cpuUtil, cpuReader.model(),
                cpuReader.logicalProcessors(), CpuMetricsReader.currentFrequencyMhz(), perCoreList,
                ram[0], ram[1], ram[2], heapUsed);
    }

    private static long usedHeap() {
        Runtime rt = Runtime.getRuntime();
        return rt.totalMemory() - rt.freeMemory();
    }

    private static String cudaStatus() {
        // Report whether the CUDA driver library is loadable; the actual device status is
        // reflected by gpuAvailable (from nvidia-smi). Kept lightweight and non-blocking.
        return new java.io.File("/usr/lib/x86_64-linux-gnu/libcuda.so.1").exists()
                ? "CUDA_DRIVER_PRESENT" : "N/A";
    }
}
