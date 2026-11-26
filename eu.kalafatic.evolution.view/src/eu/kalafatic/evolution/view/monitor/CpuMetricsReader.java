package eu.kalafatic.evolution.view.monitor;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads CPU metrics from Linux procfs ({@code /proc/stat}, {@code /proc/cpuinfo},
 * {@code /proc/loadavg}) and sysfs. CPU utilisation is derived from the delta of
 * jiffies between two samples, so a {@link CpuMetricsReader} instance retains the
 * previous sample's counters. Static inventory (vendor, cores, frequency limits,
 * temperature, load average) is read best-effort and reported as {@code -1}/{@code "Unknown"}
 * when unsupported.
 */
final class CpuMetricsReader {

    private final int logicalProcessors;
    private final String model;
    private long[] prevTotal;
    private long[] prevIdle;

    CpuMetricsReader() {
        this.logicalProcessors = Math.max(1, Runtime.getRuntime().availableProcessors());
        this.model = readCpuModel();
        this.prevTotal = new long[logicalProcessors];
        this.prevIdle = new long[logicalProcessors];
        sampleJiffies(); // initialise baselines
    }

    int logicalProcessors() { return logicalProcessors; }
    String model() { return model; }

    double[] sampleUtilization() {
        long[][] j = sampleJiffies();
        double[] util = new double[logicalProcessors];
        for (int c = 0; c < logicalProcessors; c++) {
            long dTotal = j[0][c] - prevTotal[c];
            long dIdle = j[1][c] - prevIdle[c];
            prevTotal[c] = j[0][c];
            prevIdle[c] = j[1][c];
            util[c] = (dTotal > 0) ? (100.0 * (dTotal - dIdle) / dTotal) : 0.0;
        }
        return util;
    }

    double overallUtilization(double[] perCore) {
        if (perCore == null || perCore.length == 0) return -1;
        double sum = 0;
        for (double u : perCore) sum += u;
        return sum / perCore.length;
    }

    /** Returns [totalJiffies][idleJiffies] per CPU. */
    private long[][] sampleJiffies() {
        long[] total = new long[logicalProcessors];
        long[] idle = new long[logicalProcessors];
        try (BufferedReader r = new BufferedReader(new FileReader("/proc/stat"))) {
            String line;
            while ((line = r.readLine()) != null) {
                if (!line.startsWith("cpu")) continue;
                if (line.startsWith("cpu ")) continue; // aggregate line skipped
                String[] f = line.trim().split("\\s+");
                int cpu = parseCpuIndex(f[0]);
                if (cpu < 0 || cpu >= logicalProcessors) continue;
                long user = lp(f, 1), nice = lp(f, 2), sys = lp(f, 3), idleV = lp(f, 4), iowait = lp(f, 5);
                long irq = lp(f, 6), softirq = lp(f, 7), steal = lp(f, 8);
                long idleAll = idleV + iowait;
                long totalAll = user + nice + sys + idleAll + irq + softirq + steal;
                total[cpu] = totalAll;
                idle[cpu] = idleAll;
            }
        } catch (Exception ignored) {
        }
        return new long[][]{total, idle};
    }

    private static int parseCpuIndex(String cpuToken) {
        try { return Integer.parseInt(cpuToken.substring(3)); } catch (Exception e) { return -1; }
    }
    private static long lp(String[] f, int i) {
        try { return Long.parseLong(f[i]); } catch (Exception e) { return 0; }
    }

    // ------------------------------------------------------------------
    // Static inventory (best-effort, -1 / "Unknown" when unsupported)
    // ------------------------------------------------------------------

    private static String readCpuModel() {
        try (BufferedReader r = new BufferedReader(new FileReader("/proc/cpuinfo"))) {
            String line;
            while ((line = r.readLine()) != null) {
                if (line.startsWith("model name")) {
                    int idx = line.indexOf(':');
                    return idx >= 0 ? line.substring(idx + 1).trim() : "Unknown";
                }
            }
        } catch (Exception ignored) {
        }
        return "Unknown";
    }

    static String vendor() {
        try (BufferedReader r = new BufferedReader(new FileReader("/proc/cpuinfo"))) {
            String line;
            while ((line = r.readLine()) != null) {
                if (line.startsWith("vendor_id")) {
                    int idx = line.indexOf(':');
                    return idx >= 0 ? line.substring(idx + 1).trim() : "Unknown";
                }
            }
        } catch (Exception ignored) {
        }
        return "Unknown";
    }

    static String architecture() {
        String arch = System.getProperty("os.arch", "");
        if (arch == null || arch.isEmpty()) return "Unknown";
        return arch.replace("amd64", "x86_64").replace("aarch64", "arm64");
    }

    /** Physical cores: cores per socket × socket count from {@code /proc/cpuinfo}. */
    static int physicalCores() {
        try (BufferedReader r = new BufferedReader(new FileReader("/proc/cpuinfo"))) {
            String line;
            int coresPerSocket = 0;
            int maxPhysicalId = 0;
            while ((line = r.readLine()) != null) {
                if (line.startsWith("cpu cores")) {
                    coresPerSocket = Math.max(coresPerSocket, intAfterColon(line));
                } else if (line.startsWith("physical id")) {
                    maxPhysicalId = Math.max(maxPhysicalId, intAfterColon(line));
                }
            }
            if (coresPerSocket > 0) {
                return coresPerSocket * (maxPhysicalId + 1);
            }
        } catch (Exception ignored) {
        }
        return 0;
    }

    private static int intAfterColon(String line) {
        try {
            int idx = line.indexOf(':');
            return idx >= 0 ? Integer.parseInt(line.substring(idx + 1).trim()) : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    /** Best-effort current CPU frequency (MHz) from sysfs. */
    static double currentFrequencyMhz() {
        try {
            Path p = Path.of("/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq");
            if (Files.exists(p)) {
                String v = Files.readString(p).trim();
                return Double.parseDouble(v) / 1000.0;
            }
        } catch (Exception ignored) {
        }
        return -1;
    }

    /** Best-effort maximum CPU frequency (MHz) from sysfs. */
    static double maxFrequencyMhz() {
        try {
            Path p = Path.of("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq");
            if (Files.exists(p)) {
                String v = Files.readString(p).trim();
                return Double.parseDouble(v) / 1000.0;
            }
        } catch (Exception ignored) {
        }
        return -1;
    }

    /** Best-effort CPU package temperature (°C) from sysfs thermal zones. */
    static double temperatureCelsius() {
        Path thermalRoot = Path.of("/sys/class/thermal");
        try {
            if (!Files.exists(thermalRoot)) return -1;
            double best = -1;
            try (var stream = Files.list(thermalRoot)) {
                List<Path> zones = stream
                        .filter(p -> p.getFileName().toString().startsWith("thermal_zone"))
                        .sorted()
                        .toList();
                // Prefer x86_pkg_temp-type zones (CPU package sensor).
                for (Path zone : zones) {
                    Path type = zone.resolve("type");
                    String t = Files.exists(type) ? Files.readString(type).trim() : "";
                    if (t.contains("pkg_temp") || t.contains("coretemp") || t.contains("cpu")) {
                        double v = readZoneTemp(zone);
                        if (v > 0) return v;
                    }
                }
                for (Path zone : zones) {
                    double v = readZoneTemp(zone);
                    if (v > 0) {
                        best = v;
                        break;
                    }
                }
            }
            return best;
        } catch (Exception ignored) {
            return -1;
        }
    }

    private static double readZoneTemp(Path zone) {
        try {
            Path temp = zone.resolve("temp");
            if (Files.exists(temp)) {
                return Double.parseDouble(Files.readString(temp).trim()) / 1000.0;
            }
        } catch (Exception ignored) {
        }
        return -1;
    }

    /** 1-minute system load average from {@code /proc/loadavg}. */
    static double loadAverage1m() {
        try {
            String s = Files.readString(Path.of("/proc/loadavg"));
            String[] f = s.trim().split("\\s+");
            return f.length > 0 ? Double.parseDouble(f[0]) : -1;
        } catch (Exception ignored) {
            return -1;
        }
    }
}
