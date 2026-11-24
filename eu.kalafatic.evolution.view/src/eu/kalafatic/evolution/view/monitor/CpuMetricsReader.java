package eu.kalafatic.evolution.view.monitor;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads CPU metrics from Linux procfs ({@code /proc/stat}, {@code /proc/cpuinfo}).
 * CPU utilisation is derived from the delta of jiffies between two samples, so a
 * {@link CpuMetricsReader} instance retains the previous sample's counters.
 */
final class CpuMetricsReader {

    private final int logicalProcessors;
    private final String model;
    private long[] prevTotal;
    private long[] prevIdle;

    CpuMetricsReader() {
        this.logicalProcessors = Runtime.getRuntime().availableProcessors();
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
}
