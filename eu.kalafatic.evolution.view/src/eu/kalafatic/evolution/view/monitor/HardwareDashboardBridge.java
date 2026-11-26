package eu.kalafatic.evolution.view.monitor;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Builds the JSON payloads consumed by the HTML hardware dashboard
 * ({@code hardware-dashboard.html}). Pure Java, no SWT, so the payload
 * structure is unit-testable headlessly. Missing metrics are emitted as
 * {@code -1} and the dashboard renders them as gaps, never as invented values.
 */
public final class HardwareDashboardBridge {

    private HardwareDashboardBridge() {
    }

    /** One live sample for the dashboard's incremental chart update. */
    public static JSONObject toSampleJson(HardwareSnapshot s) {
        JSONObject cpu = new JSONObject();
        cpu.put("util", s.cpuUtilizationPercent);
        cpu.put("freq", s.cpuFrequencyMhz);
        cpu.put("maxFreq", s.cpuMaxFrequencyMhz);
        cpu.put("temp", s.cpuTemperatureCelsius);
        cpu.put("load", s.loadAverage1m);
        cpu.put("model", s.cpuModel);
        cpu.put("vendor", s.cpuVendor);
        cpu.put("physicalCores", s.physicalCores);
        cpu.put("logicalCores", s.logicalProcessors);

        JSONObject ram = new JSONObject();
        ram.put("used", s.ramUsedBytes);
        ram.put("total", s.ramTotalBytes);
        ram.put("available", s.ramAvailableBytes);
        ram.put("free", s.ramFreeBytes);
        ram.put("util", s.ramUtilizationPercent());

        JSONObject jvm = new JSONObject();
        jvm.put("heap", s.processHeapBytes);
        jvm.put("max", s.jvmMaxHeapBytes);
        jvm.put("nonHeap", s.jvmNonHeapBytes);
        jvm.put("util", s.jvmHeapUtilizationPercent());

        JSONArray gpus = new JSONArray();
        for (HardwareSnapshot.Gpu g : s.gpus) {
            JSONObject j = new JSONObject();
            j.put("index", g.index);
            j.put("name", g.name);
            j.put("driver", g.driverVersion);
            j.put("util", g.utilizationPercent);
            j.put("memCtrl", g.memCtrlUtilizationPercent);
            j.put("vramUsed", g.vramUsedBytes);
            j.put("vramTotal", g.vramTotalBytes);
            j.put("vramUtil", g.vramUtilizationPercent());
            j.put("temp", g.temperatureCelsius);
            j.put("power", g.powerWatts);
            j.put("powerLimit", g.powerLimitWatts);
            j.put("smClock", g.smClockMhz);
            j.put("state", g.state);
            gpus.put(j);
        }

        JSONObject root = new JSONObject();
        root.put("ts", s.timestampMs);
        root.put("cpu", cpu);
        root.put("ram", ram);
        root.put("jvm", jvm);
        root.put("gpus", gpus);
        return root;
    }

    /** Hardware-scan summary for the dashboard's inventory section. */
    public static JSONObject toScanJson(HardwareScanResult r) {
        JSONObject root = new JSONObject();
        root.put("startedAt", r.startedAt == null ? "" : r.startedAt.toString());
        root.put("finishedAt", r.finishedAt == null ? "" : r.finishedAt.toString());
        root.put("status", r.status == null ? "UNKNOWN" : r.status.name());
        root.put("durationMs", r.totalDurationMs());
        root.put("cudaStatus", r.cudaStatus == null ? "UNKNOWN" : r.cudaStatus.name());
        root.put("cudaDetail", r.cudaDetail == null ? "" : r.cudaDetail);

        if (r.cpu != null) {
            JSONObject cpu = new JSONObject();
            cpu.put("vendor", r.cpu.vendor);
            cpu.put("model", r.cpu.model);
            cpu.put("arch", r.cpu.architecture);
            cpu.put("physicalCores", r.cpu.physicalCores);
            cpu.put("logicalCores", r.cpu.logicalProcessors);
            cpu.put("freq", r.cpu.frequencyMhz);
            cpu.put("maxFreq", r.cpu.maxFrequencyMhz);
            cpu.put("temp", r.cpu.temperatureCelsius);
            cpu.put("load", r.cpu.loadAverage1m);
            root.put("cpu", cpu);
        }
        if (r.ram != null) {
            JSONObject ram = new JSONObject();
            ram.put("total", r.ram.totalBytes);
            ram.put("used", r.ram.usedBytes);
            ram.put("available", r.ram.availableBytes);
            ram.put("free", r.ram.freeBytes);
            root.put("ram", ram);
        }
        if (r.jvm != null) {
            JSONObject jvm = new JSONObject();
            jvm.put("version", r.jvm.jvmVersion);
            jvm.put("heap", r.jvm.heapUsedBytes);
            jvm.put("max", r.jvm.heapMaxBytes);
            jvm.put("nonHeap", r.jvm.nonHeapBytes);
            root.put("jvm", jvm);
        }
        JSONArray gpus = new JSONArray();
        for (HardwareScanResult.GpuInfo g : r.gpus) {
            JSONObject j = new JSONObject();
            j.put("index", g.index);
            j.put("vendor", g.vendor);
            j.put("name", g.name);
            j.put("totalVramMb", g.totalVramMb);
            j.put("usedVramMb", g.usedVramMb);
            j.put("freeVramMb", g.freeVramMb);
            j.put("driver", g.driverVersion);
            j.put("computeCapability", g.computeCapability);
            j.put("util", g.utilizationPercent);
            j.put("memCtrl", g.memCtrlUtilizationPercent);
            j.put("temp", g.temperatureCelsius);
            j.put("power", g.powerWatts);
            j.put("powerLimit", g.powerLimitWatts);
            j.put("smClock", g.smClockMhz);
            gpus.put(j);
        }
        root.put("gpus", gpus);

        JSONArray notes = new JSONArray();
        for (String n : r.notes) {
            notes.put(n);
        }
        root.put("notes", notes);
        return root;
    }
}
