package eu.kalafatic.evolution.view.monitor;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

/**
 * Reads NVIDIA GPU metrics via the {@code nvidia-smi} CLI. This is a controlled,
 * cache-friendly integration: one short-lived process per sample, parsed defensively so
 * that any missing field or unavailable GPU degrades gracefully to {@code -1}/{@code "N/A"}.
 */
final class GpuMetricsReader {

    private static final String QUERY =
            "name,utilization.gpu,memory.used,memory.total,temperature.gpu,power.draw,power.limit,pstate";

    static HardwareSnapshot.Gpu parse() {
        ProcessBuilder pb = new ProcessBuilder("nvidia-smi",
                "--query-gpu=" + QUERY, "--format=csv,noheader,nounits");
        pb.redirectErrorStream(true);
        Process p = null;
        try {
            p = pb.start();
            p.waitFor(3, TimeUnit.SECONDS);
            String line;
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                line = r.readLine();
            }
            if (line == null || line.trim().isEmpty() || line.toLowerCase().contains("error")
                    || line.toLowerCase().contains("no devices")) {
                return null;
            }
            String[] f = line.split(",");
            if (f.length < 8) return null;
            String name = f[0].trim();
            int util = parseInt(f[1]);
            long used = parseLong(f[2]) * 1024 * 1024;   // MiB -> bytes
            long total = parseLong(f[3]) * 1024 * 1024;
            int temp = parseInt(f[4]);
            double power = parseDouble(f[5]);
            double limit = parseDouble(f[6]);
            String state = f[7].trim();
            return new HardwareSnapshot.Gpu(true, name, util, used, total, temp, power, limit, state);
        } catch (Exception e) {
            return null;
        } finally {
            if (p != null) p.destroy();
        }
    }

    static int parseInt(String s) {
        try { return (int) Double.parseDouble(s.trim()); } catch (Exception e) { return -1; }
    }
    static long parseLong(String s) {
        try { return (long) Double.parseDouble(s.trim()); } catch (Exception e) { return -1; }
    }
    static double parseDouble(String s) {
        try { return Double.parseDouble(s.trim()); } catch (Exception e) { return -1; }
    }
}
