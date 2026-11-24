package eu.kalafatic.evolution.view.monitor;

import java.io.BufferedReader;
import java.io.FileReader;

/**
 * Reads RAM metrics from Linux {@code /proc/meminfo}. Values are reported in bytes.
 */
final class RamMetricsReader {

    static long[] read() {
        long total = -1, available = -1, free = -1, buffers = -1, cached = -1;
        try (BufferedReader r = new BufferedReader(new FileReader("/proc/meminfo"))) {
            String line;
            while ((line = r.readLine()) != null) {
                int colon = line.indexOf(':');
                if (colon < 0) continue;
                String key = line.substring(0, colon).trim();
                long kb = parseKb(line.substring(colon + 1));
                switch (key) {
                    case "MemTotal": total = kb; break;
                    case "MemAvailable": available = kb; break;
                    case "MemFree": free = kb; break;
                    case "Buffers": buffers = kb; break;
                    case "Cached": cached = kb; break;
                }
            }
        } catch (Exception ignored) {
        }
        long used = (total > 0 && available > 0) ? (total - available) : (total > 0 && free > 0 ? total - free : -1);
        return new long[]{total, used, available};
    }

    private static long parseKb(String v) {
        String t = v.trim();
        int idx = 0;
        while (idx < t.length() && (Character.isDigit(t.charAt(idx)))) idx++;
        try {
            long n = Long.parseLong(t.substring(0, idx));
            return n * 1024; // kB -> bytes
        } catch (Exception e) {
            return -1;
        }
    }
}
