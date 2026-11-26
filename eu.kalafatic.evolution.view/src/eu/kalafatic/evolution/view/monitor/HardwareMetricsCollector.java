package eu.kalafatic.evolution.view.monitor;

import java.lang.management.ManagementFactory;
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

    public synchronized boolean isRunning() {
        return executor != null && !executor.isShutdown();
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

    /**
     * Collects one snapshot synchronously on the calling thread. Serialized so it can
     * safely run concurrently with the scheduled sampling (used by "Refresh now").
     */
    public synchronized HardwareSnapshot collect() {
        List<HardwareSnapshot.Gpu> gpus = gpuReader.parseAll();
        HardwareSnapshot.Gpu first = gpus.isEmpty() ? null : gpus.get(0);

        double[] perCore = cpuReader.sampleUtilization();
        double cpuUtil = cpuReader.overallUtilization(perCore);
        long[] ram = ramReader.read();
        long heapUsed = usedHeap();
        long heapMax = Runtime.getRuntime().maxMemory();
        long nonHeap = ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage().getUsed();

        boolean gpuAvailable = first != null && first.available;
        String gpuName = first != null ? first.name : "No GPU";
        int gpuUtil = first != null ? first.utilizationPercent : -1;
        long vramUsed = first != null ? first.vramUsedBytes : 0;
        long vramTotal = first != null ? first.vramTotalBytes : 0;
        int gpuTemp = first != null ? first.temperatureCelsius : -1;
        double gpuPower = first != null ? first.powerWatts : -1;
        double gpuLimit = first != null ? first.powerLimitWatts : -1;
        String gpuState = first != null ? first.state : "N/A";

        List<Double> perCoreList = new ArrayList<>();
        for (double u : perCore) perCoreList.add(u);

        long ramTotal = ram.length > 0 ? Math.max(0, ram[0]) : 0;
        long ramUsed = ram.length > 1 ? Math.max(0, ram[1]) : 0;
        long ramAvail = ram.length > 2 ? Math.max(0, ram[2]) : 0;
        long ramFree = ram.length > 3 ? Math.max(0, ram[3]) : 0;

        return new HardwareSnapshot(System.currentTimeMillis(), gpus,
                gpuAvailable, gpuName, gpuUtil, vramUsed, vramTotal, gpuTemp,
                gpuPower, gpuLimit, gpuState, cudaStatus(), cpuUtil, cpuReader.model(),
                CpuMetricsReader.vendor(), CpuMetricsReader.physicalCores(),
                cpuReader.logicalProcessors(), CpuMetricsReader.currentFrequencyMhz(),
                CpuMetricsReader.maxFrequencyMhz(), CpuMetricsReader.temperatureCelsius(),
                CpuMetricsReader.loadAverage1m(), perCoreList,
                ramTotal, ramUsed, ramAvail, ramFree, heapUsed, heapMax, nonHeap);
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
