package eu.kalafatic.evolution.view.monitor;

import java.util.ArrayList;
import java.util.List;

/**
 * Immutable snapshot of hardware metrics collected at a single point in time. All fields
 * are nullable / have safe defaults so the UI can render gracefully when a metric is
 * unavailable (e.g. no NVIDIA GPU present). Missing numeric metrics are {@code -1} (or
 * {@code 0} where a count is genuinely zero).
 *
 * <p>{@link #gpus} holds the full multi-GPU inventory; the legacy single-GPU fields
 * mirror the first GPU for compatibility with existing consumers.</p>
 */
public final class HardwareSnapshot {

    /** Sampling timestamp (epoch millis). */
    public final long timestampMs;

    // ---- GPU (full inventory) ----
    public final List<Gpu> gpus;

    // ---- GPU (first device mirror, legacy fields) ----
    public final boolean gpuAvailable;
    public final String gpuName;
    public final int gpuUtilizationPercent;   // -1 when unknown
    public final long vramUsedBytes;
    public final long vramTotalBytes;
    public final int gpuTemperatureCelsius;   // -1 when unknown
    public final double gpuPowerWatts;        // -1 when unknown
    public final double gpuPowerLimitWatts;   // -1 when unknown
    public final String gpuState;             // e.g. "P0", "P8"
    public final String cudaStatus;           // e.g. "CUDA_DRIVER_PRESENT", "N/A"

    // ---- CPU ----
    public final double cpuUtilizationPercent; // -1 when unknown
    public final String cpuModel;
    public final String cpuVendor;
    public final int physicalCores;            // 0 when unknown
    public final int logicalProcessors;        // 0 when unknown
    public final double cpuFrequencyMhz;       // -1 when unknown
    public final double cpuMaxFrequencyMhz;    // -1 when unknown
    public final double cpuTemperatureCelsius; // -1 when unknown
    public final double loadAverage1m;         // -1 when unknown
    public final List<Double> perCoreUtilization; // may be empty

    // ---- RAM ----
    public final long ramTotalBytes;
    public final long ramUsedBytes;
    public final long ramAvailableBytes;
    public final long ramFreeBytes;

    // ---- JVM (distinct from physical RAM) ----
    public final long processHeapBytes;        // JVM heap used
    public final long jvmMaxHeapBytes;         // JVM max heap (-Xmx)
    public final long jvmNonHeapBytes;         // JVM non-heap used

    public HardwareSnapshot(long timestampMs,
                            List<Gpu> gpus,
                            boolean gpuAvailable, String gpuName, int gpuUtilizationPercent,
                            long vramUsedBytes, long vramTotalBytes, int gpuTemperatureCelsius,
                            double gpuPowerWatts, double gpuPowerLimitWatts, String gpuState, String cudaStatus,
                            double cpuUtilizationPercent, String cpuModel, String cpuVendor,
                            int physicalCores, int logicalProcessors,
                            double cpuFrequencyMhz, double cpuMaxFrequencyMhz,
                            double cpuTemperatureCelsius, double loadAverage1m,
                            List<Double> perCoreUtilization,
                            long ramTotalBytes, long ramUsedBytes, long ramAvailableBytes, long ramFreeBytes,
                            long processHeapBytes, long jvmMaxHeapBytes, long jvmNonHeapBytes) {
        this.timestampMs = timestampMs;
        this.gpus = gpus != null ? new ArrayList<>(gpus) : new ArrayList<>();
        this.gpuAvailable = gpuAvailable;
        this.gpuName = gpuName;
        this.gpuUtilizationPercent = gpuUtilizationPercent;
        this.vramUsedBytes = vramUsedBytes;
        this.vramTotalBytes = vramTotalBytes;
        this.gpuTemperatureCelsius = gpuTemperatureCelsius;
        this.gpuPowerWatts = gpuPowerWatts;
        this.gpuPowerLimitWatts = gpuPowerLimitWatts;
        this.gpuState = gpuState;
        this.cudaStatus = cudaStatus;
        this.cpuUtilizationPercent = cpuUtilizationPercent;
        this.cpuModel = cpuModel;
        this.cpuVendor = cpuVendor;
        this.physicalCores = physicalCores;
        this.logicalProcessors = logicalProcessors;
        this.cpuFrequencyMhz = cpuFrequencyMhz;
        this.cpuMaxFrequencyMhz = cpuMaxFrequencyMhz;
        this.cpuTemperatureCelsius = cpuTemperatureCelsius;
        this.loadAverage1m = loadAverage1m;
        this.perCoreUtilization = perCoreUtilization != null ? new ArrayList<>(perCoreUtilization) : new ArrayList<>();
        this.ramTotalBytes = ramTotalBytes;
        this.ramUsedBytes = ramUsedBytes;
        this.ramAvailableBytes = ramAvailableBytes;
        this.ramFreeBytes = ramFreeBytes;
        this.processHeapBytes = processHeapBytes;
        this.jvmMaxHeapBytes = jvmMaxHeapBytes;
        this.jvmNonHeapBytes = jvmNonHeapBytes;
    }

    public double ramUtilizationPercent() {
        return ramTotalBytes > 0 ? (ramUsedBytes * 100.0 / ramTotalBytes) : -1;
    }

    public double vramUtilizationPercent() {
        return vramTotalBytes > 0 ? (vramUsedBytes * 100.0 / vramTotalBytes) : -1;
    }

    public double jvmHeapUtilizationPercent() {
        return jvmMaxHeapBytes > 0 ? (processHeapBytes * 100.0 / jvmMaxHeapBytes) : -1;
    }

    /** A static empty snapshot for initial UI state. */
    public static HardwareSnapshot empty() {
        return new HardwareSnapshot(System.currentTimeMillis(), new ArrayList<>(),
                false, "No GPU", -1, 0, 0, -1, -1, -1, "N/A", "N/A",
                -1, "Unknown", "Unknown", 0, 0, -1, -1, -1, -1, new ArrayList<>(),
                0, 0, 0, 0, -1, -1, -1);
    }

    /** Raw GPU metrics parsed from nvidia-smi (used internally by the readers). */
    public static final class Gpu {
        public final int index;                   // device index from nvidia-smi
        public final boolean available;
        public final String name;
        public final String driverVersion;
        public final int utilizationPercent;
        public final int memCtrlUtilizationPercent; // -1 when unknown
        public final long vramUsedBytes;
        public final long vramTotalBytes;
        public final long vramFreeBytes;
        public final int temperatureCelsius;
        public final double powerWatts;
        public final double powerLimitWatts;
        public final int smClockMhz;
        public final String state;

        public Gpu(int index, boolean available, String name, String driverVersion,
                   int utilizationPercent, int memCtrlUtilizationPercent,
                   long vramUsedBytes, long vramTotalBytes, long vramFreeBytes,
                   int temperatureCelsius, double powerWatts, double powerLimitWatts,
                   int smClockMhz, String state) {
            this.index = index;
            this.available = available;
            this.name = name;
            this.driverVersion = driverVersion;
            this.utilizationPercent = utilizationPercent;
            this.memCtrlUtilizationPercent = memCtrlUtilizationPercent;
            this.vramUsedBytes = vramUsedBytes;
            this.vramTotalBytes = vramTotalBytes;
            this.vramFreeBytes = vramFreeBytes;
            this.temperatureCelsius = temperatureCelsius;
            this.powerWatts = powerWatts;
            this.powerLimitWatts = powerLimitWatts;
            this.smClockMhz = smClockMhz;
            this.state = state;
        }

        /** Legacy-compatible constructor (single-GPU layout). */
        public Gpu(boolean available, String name, int utilizationPercent, long vramUsedBytes,
                   long vramTotalBytes, int temperatureCelsius, double powerWatts,
                   double powerLimitWatts, String state) {
            this(0, available, name, "", utilizationPercent, -1, vramUsedBytes, vramTotalBytes,
                    Math.max(0, vramTotalBytes - vramUsedBytes), temperatureCelsius, powerWatts,
                    powerLimitWatts, -1, state);
        }

        public double vramUtilizationPercent() {
            return vramTotalBytes > 0 ? (vramUsedBytes * 100.0 / vramTotalBytes) : -1;
        }
    }
}
