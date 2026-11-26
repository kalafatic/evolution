package eu.kalafatic.evolution.view.monitor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Structured result of a one-shot hardware scan. All components are reported
 * independently: CPU, physical RAM, JVM memory and each detected GPU.
 * Unavailable metrics are {@code -1}/{@code "Unknown"}; the scan never
 * fabricates values.
 */
public final class HardwareScanResult {

    public enum ScanStatus {
        /** All components were detected without errors. */
        SUCCESS,
        /** The scan completed but some components were unavailable. */
        PARTIAL,
        /** The scan itself failed. */
        FAILED
    }

    public enum CudaStatus {
        UNKNOWN,
        /** No NVIDIA GPU / nvidia-smi unavailable. */
        NOT_PRESENT,
        /** GPU detected but the CUDA backend classes cannot load on this JVM. */
        UNSUPPORTED_JVM,
        /** GPU detected but the backend could not initialize (driver/lib failure). */
        BACKEND_UNAVAILABLE,
        /** CUDA backend initialized successfully. */
        DETECTED
    }

    /** Static CPU inventory. */
    public static final class CpuInfo {
        public final String vendor;
        public final String model;
        public final String architecture;
        public final int physicalCores;
        public final int logicalProcessors;
        public final double frequencyMhz;
        public final double maxFrequencyMhz;
        public final double temperatureCelsius;
        public final double loadAverage1m;

        public CpuInfo(String vendor, String model, String architecture, int physicalCores,
                       int logicalProcessors, double frequencyMhz, double maxFrequencyMhz,
                       double temperatureCelsius, double loadAverage1m) {
            this.vendor = vendor;
            this.model = model;
            this.architecture = architecture;
            this.physicalCores = physicalCores;
            this.logicalProcessors = logicalProcessors;
            this.frequencyMhz = frequencyMhz;
            this.maxFrequencyMhz = maxFrequencyMhz;
            this.temperatureCelsius = temperatureCelsius;
            this.loadAverage1m = loadAverage1m;
        }
    }

    /** Physical system memory inventory. */
    public static final class RamInfo {
        public final long totalBytes;
        public final long usedBytes;
        public final long availableBytes;
        public final long freeBytes;

        public RamInfo(long totalBytes, long usedBytes, long availableBytes, long freeBytes) {
            this.totalBytes = totalBytes;
            this.usedBytes = usedBytes;
            this.availableBytes = availableBytes;
            this.freeBytes = freeBytes;
        }
    }

    /** JVM memory (distinct from physical RAM). */
    public static final class JvmInfo {
        public final String jvmVersion;
        public final long heapUsedBytes;
        public final long heapMaxBytes;
        public final long nonHeapBytes;

        public JvmInfo(String jvmVersion, long heapUsedBytes, long heapMaxBytes, long nonHeapBytes) {
            this.jvmVersion = jvmVersion;
            this.heapUsedBytes = heapUsedBytes;
            this.heapMaxBytes = heapMaxBytes;
            this.nonHeapBytes = nonHeapBytes;
        }
    }

    /** Per-GPU inventory. */
    public static final class GpuInfo {
        public final int index;
        public final String vendor;
        public final String name;
        public final long totalVramMb;
        public final long usedVramMb;
        public final long freeVramMb;
        public final String driverVersion;
        public final String computeCapability;
        public final int utilizationPercent;
        public final int memCtrlUtilizationPercent;
        public final int temperatureCelsius;
        public final double powerWatts;
        public final double powerLimitWatts;
        public final int smClockMhz;

        public GpuInfo(int index, String vendor, String name, long totalVramMb, long usedVramMb,
                       long freeVramMb, String driverVersion, String computeCapability,
                       int utilizationPercent, int memCtrlUtilizationPercent,
                       int temperatureCelsius, double powerWatts, double powerLimitWatts,
                       int smClockMhz) {
            this.index = index;
            this.vendor = vendor;
            this.name = name;
            this.totalVramMb = totalVramMb;
            this.usedVramMb = usedVramMb;
            this.freeVramMb = freeVramMb;
            this.driverVersion = driverVersion;
            this.computeCapability = computeCapability;
            this.utilizationPercent = utilizationPercent;
            this.memCtrlUtilizationPercent = memCtrlUtilizationPercent;
            this.temperatureCelsius = temperatureCelsius;
            this.powerWatts = powerWatts;
            this.powerLimitWatts = powerLimitWatts;
            this.smClockMhz = smClockMhz;
        }
    }

    public volatile Instant startedAt = Instant.now();
    public volatile Instant finishedAt = startedAt;
    public volatile ScanStatus status = ScanStatus.PARTIAL;
    public volatile CpuInfo cpu;
    public volatile RamInfo ram;
    public volatile JvmInfo jvm;
    public final List<GpuInfo> gpus = new ArrayList<>();
    public volatile CudaStatus cudaStatus = CudaStatus.UNKNOWN;
    public volatile String cudaDetail = "";
    public final List<String> notes = new ArrayList<>();

    public HardwareScanResult() {
    }

    public long totalDurationMs() {
        return Math.max(0, finishedAt.toEpochMilli() - startedAt.toEpochMilli());
    }
}
