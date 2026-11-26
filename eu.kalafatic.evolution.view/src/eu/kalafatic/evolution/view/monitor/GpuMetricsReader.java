package eu.kalafatic.evolution.view.monitor;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Reads NVIDIA GPU metrics via the {@code nvidia-smi} CLI. This is a controlled,
 * cache-friendly integration: one short-lived process per sample, parsed defensively so
 * that any missing field or unavailable GPU degrades gracefully to {@code -1}/{@code "N/A"}.
 *
 * <p>Supports multiple GPUs: every row of the query output is parsed into a
 * {@link HardwareSnapshot.Gpu} with its device index.</p>
 */
final class GpuMetricsReader {

    private static final String QUERY =
            "index,name,driver_version,memory.total,memory.used,memory.free,"
                    + "utilization.gpu,utilization.memory,temperature.gpu,power.draw,power.limit,"
                    + "clocks.sm,pstate";

    /** Parses every reported GPU; empty list when no NVIDIA GPU is available. */
    static List<HardwareSnapshot.Gpu> parseAll() {
        ProcessBuilder pb = new ProcessBuilder("nvidia-smi",
                "--query-gpu=" + QUERY, "--format=csv,noheader,nounits");
        pb.redirectErrorStream(true);
        Process p = null;
        try {
            p = pb.start();
            p.waitFor(3, TimeUnit.SECONDS);
            List<HardwareSnapshot.Gpu> gpus = new ArrayList<>();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = r.readLine()) != null) {
                    if (line.trim().isEmpty() || line.toLowerCase().contains("error")
                            || line.toLowerCase().contains("no devices")) {
                        continue;
                    }
                    HardwareSnapshot.Gpu g = parseLine(line);
                    if (g != null) {
                        gpus.add(g);
                    }
                }
            }
            return gpus;
        } catch (Exception e) {
            return new ArrayList<>();
        } finally {
            if (p != null) p.destroy();
        }
    }

    /** Legacy single-GPU accessor (first device). */
    static HardwareSnapshot.Gpu parse() {
        List<HardwareSnapshot.Gpu> all = parseAll();
        return all.isEmpty() ? null : all.get(0);
    }

    private static HardwareSnapshot.Gpu parseLine(String line) {
        String[] f = line.split(",");
        if (f.length < 8) return null;
        int index = parseInt(f[0]);
        String name = f[1].trim();
        String driver = f[2].trim();
        long total = parseLong(f[3]) * 1024 * 1024;   // MiB -> bytes
        long used = parseLong(f[4]) * 1024 * 1024;
        long free = parseLong(f[5]) * 1024 * 1024;
        int util = parseInt(f[6]);
        int memCtrl = parseInt(f[7]);
        int temp = f.length > 8 ? parseInt(f[8]) : -1;
        double power = f.length > 9 ? parseDouble(f[9]) : -1;
        double limit = f.length > 10 ? parseDouble(f[10]) : -1;
        int smClock = f.length > 11 ? parseInt(f[11]) : -1;
        String state = f.length > 12 ? f[12].trim() : "N/A";
        return new HardwareSnapshot.Gpu(index, true, name, driver, util, memCtrl,
                used, total, free, temp, power, limit, smClock, state);
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
