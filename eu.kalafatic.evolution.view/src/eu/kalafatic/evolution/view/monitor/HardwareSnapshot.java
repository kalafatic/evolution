package eu.kalafatic.evolution.view.monitor;

import java.util.ArrayList;
import java.util.List;

/**
 * Immutable snapshot of hardware metrics collected at a single point in time. All fields
 * are nullable / have safe defaults so the UI can render gracefully when a metric is
 * unavailable (e.g. no NVIDIA GPU present).
 */
public final class HardwareSnapshot {

    // ---- GPU ----
    public final boolean gpuAvailable;
    public final String gpuName;
    public final int gpuUtilizationPercent;   // -1 when unknown
    public final long vramUsedBytes;
    public final long vramTotalBytes;
    public final int gpuTemperatureCelsius;   // -1 when unknown
    public final double gpuPowerWatts;        // -1 when unknown
    public final double gpuPowerLimitWatts;   // -1 when unknown
    public final String gpuState;             // e.g. "P0", "P8"
    public final String cudaStatus;           // e.g. "CUDA_DRIVER_VERIFIED", "N/A"

    // ---- CPU ----
    public final double cpuUtilizationPercent; // -1 when unknown
    public final String cpuModel;
    public final int logicalProcessors;        // 0 when unknown
    public final double cpuFrequencyMhz;       // -1 when unknown
    public final List<Double> perCoreUtilization; // may be empty

    // ---- RAM ----
    public final long ramTotalBytes;
    public final long ramUsedBytes;
    public final long ramAvailableBytes;
    public final long processHeapBytes;        // JVM heap used, or -1

    public HardwareSnapshot(boolean gpuAvailable, String gpuName, int gpuUtilizationPercent,
                            long vramUsedBytes, long vramTotalBytes, int gpuTemperatureCelsius,
                            double gpuPowerWatts, double gpuPowerLimitWatts, String gpuState, String cudaStatus,
                            double cpuUtilizationPercent, String cpuModel, int logicalProcessors,
                            double cpuFrequencyMhz, List<Double> perCoreUtilization,
                            long ramTotalBytes, long ramUsedBytes, long ramAvailableBytes, long processHeapBytes) {
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
        this.logicalProcessors = logicalProcessors;
        this.cpuFrequencyMhz = cpuFrequencyMhz;
        this.perCoreUtilization = perCoreUtilization != null ? new ArrayList<>(perCoreUtilization) : new ArrayList<>();
        this.ramTotalBytes = ramTotalBytes;
        this.ramUsedBytes = ramUsedBytes;
        this.ramAvailableBytes = ramAvailableBytes;
        this.processHeapBytes = processHeapBytes;
    }

    public double ramUtilizationPercent() {
        return ramTotalBytes > 0 ? (ramUsedBytes * 100.0 / ramTotalBytes) : -1;
    }

    public double vramUtilizationPercent() {
        return vramTotalBytes > 0 ? (vramUsedBytes * 100.0 / vramTotalBytes) : -1;
    }

    /** A static empty snapshot for initial UI state. */
    public static HardwareSnapshot empty() {
        return new HardwareSnapshot(false, "No GPU", -1, 0, 0, -1, -1, -1, "N/A", "N/A",
                -1, "Unknown", 0, -1, new ArrayList<>(), 0, 0, 0, -1);
    }

    /** Raw GPU metrics parsed from nvidia-smi (used internally by the collector). */
    public static final class Gpu {
        public final boolean available;
        public final String name;
        public final int utilizationPercent;
        public final long vramUsedBytes;
        public final long vramTotalBytes;
        public final int temperatureCelsius;
        public final double powerWatts;
        public final double powerLimitWatts;
        public final String state;

        public Gpu(boolean available, String name, int utilizationPercent, long vramUsedBytes,
                   long vramTotalBytes, int temperatureCelsius, double powerWatts,
                   double powerLimitWatts, String state) {
            this.available = available;
            this.name = name;
            this.utilizationPercent = utilizationPercent;
            this.vramUsedBytes = vramUsedBytes;
            this.vramTotalBytes = vramTotalBytes;
            this.temperatureCelsius = temperatureCelsius;
            this.powerWatts = powerWatts;
            this.powerLimitWatts = powerLimitWatts;
            this.state = state;
        }
    }
}
