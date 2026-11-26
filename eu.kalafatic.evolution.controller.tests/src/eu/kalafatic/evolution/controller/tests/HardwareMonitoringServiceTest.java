package eu.kalafatic.evolution.controller.tests;

import eu.kalafatic.evolution.view.monitor.HardwareDashboardBridge;
import eu.kalafatic.evolution.view.monitor.HardwareMonitoringService;
import eu.kalafatic.evolution.view.monitor.HardwareScanResult;
import eu.kalafatic.evolution.view.monitor.HardwareSnapshot;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Test;

import java.io.File;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests for the centralized {@link HardwareMonitoringService}: lifecycle,
 * thread hygiene, bounded history, shared delivery to multiple consumers,
 * scan structure and the dashboard JSON payloads. Runs headless on the Java
 * 21 Tycho test JVM; GPU-related assertions degrade gracefully on machines
 * without nvidia-smi.
 */
public class HardwareMonitoringServiceTest {

    private static final HardwareMonitoringService SERVICE = HardwareMonitoringService.getInstance();

    @After
    public void tearDown() {
        SERVICE.stop();
        SERVICE.setHistoryLimit(HardwareMonitoringService.DEFAULT_HISTORY_LIMIT);
    }

    @Test
    public void testStartStopRepeatedlyWithoutThreadLeak() throws Exception {
        for (int cycle = 0; cycle < 2; cycle++) {
            SERVICE.start();
            assertTrue("monitoring must be running", SERVICE.isRunning());
            SERVICE.stop();
            assertFalse("monitoring must be stopped", SERVICE.isRunning());
        }
        // The daemon sampler must terminate after stop (no leaked thread).
        for (int i = 0; i < 50; i++) {
            if (!isMonitorThreadAlive()) {
                break;
            }
            Thread.sleep(100);
        }
        assertFalse("evo-hardware-monitor thread leaked after stop", isMonitorThreadAlive());
    }

    @Test
    public void testHistoryIsBounded() {
        SERVICE.setHistoryLimit(5);
        for (int i = 0; i < 10; i++) {
            SERVICE.refreshNow();
        }
        List<HardwareSnapshot> history = SERVICE.getHistory();
        assertTrue("history must respect the configured limit (was " + history.size() + ")",
                history.size() <= 5);
        assertNotNull(SERVICE.getLatest());
    }

    @Test
    public void testSamplesAreSharedAcrossConsumers() throws Exception {
        CountDownLatch first = new CountDownLatch(1);
        CountDownLatch second = new CountDownLatch(1);
        AtomicReference<HardwareSnapshot> a = new AtomicReference<>();
        AtomicReference<HardwareSnapshot> b = new AtomicReference<>();
        java.util.function.Consumer<HardwareSnapshot> l1 = s -> { a.set(s); first.countDown(); };
        java.util.function.Consumer<HardwareSnapshot> l2 = s -> { b.set(s); second.countDown(); };
        SERVICE.addListener(l1);
        SERVICE.addListener(l2);
        try {
            SERVICE.refreshNow();
            assertTrue("first consumer must receive the sample", first.await(15, TimeUnit.SECONDS));
            assertTrue("second consumer must receive the sample", second.await(15, TimeUnit.SECONDS));
            assertNotNull(a.get());
            assertNotNull(b.get());
        } finally {
            SERVICE.removeListener(l1);
            SERVICE.removeListener(l2);
        }
    }

    @Test
    public void testSnapshotFieldsAreSane() {
        HardwareSnapshot s = SERVICE.refreshNow();
        assertNotNull(s);
        assertTrue("timestamp must be set", s.timestampMs > 0);
        assertTrue("logical processors must be positive", s.logicalProcessors > 0);
        assertTrue("RAM total must be positive", s.ramTotalBytes > 0);
        assertTrue("CPU utilization must be >= -1", s.cpuUtilizationPercent >= -1);
        assertTrue("JVM max heap must be positive", s.jvmMaxHeapBytes > 0);
        assertTrue("missing metrics must be represented as -1 (freq)",
                s.cpuFrequencyMhz >= -1);
        assertTrue("missing metrics must be represented as -1 (temp)",
                s.cpuTemperatureCelsius >= -1);
        assertTrue("missing metrics must be represented as -1 (load)",
                s.loadAverage1m >= -1);
    }

    @Test
    public void testScanProducesStructuredResult() {
        HardwareScanResult r = SERVICE.runScan();
        assertNotNull(r);
        assertNotNull("scan must report CPU inventory", r.cpu);
        assertTrue("scan must report logical processors", r.cpu.logicalProcessors > 0);
        assertNotNull("scan must report RAM inventory", r.ram);
        assertTrue("scan must report RAM total", r.ram.totalBytes > 0);
        assertNotNull("scan must report JVM inventory", r.jvm);
        assertTrue("scan status must be SUCCESS or PARTIAL",
                r.status == HardwareScanResult.ScanStatus.SUCCESS
                        || r.status == HardwareScanResult.ScanStatus.PARTIAL);
        assertTrue("scan duration must be recorded", r.totalDurationMs() >= 0);
        assertNotNull("CUDA status must be set", r.cudaStatus);
        assertEquals("scan result must be stored", r, SERVICE.getLastScan());
    }

    @Test
    public void testMultiGpuScanMatchesNvidiaSmi() {
        HardwareScanResult r = SERVICE.runScan();
        if (new File("/usr/bin/nvidia-smi").exists()
                && new File("/usr/lib/x86_64-linux-gnu/libcuda.so.1").exists()) {
            assertTrue("GPU inventory must list at least one device on this machine",
                    r.gpus.size() >= 1);
            for (HardwareScanResult.GpuInfo g : r.gpus) {
                assertTrue("GPU name must not be empty", g.name != null && !g.name.isEmpty());
                assertTrue("GPU index must be >= 0", g.index >= 0);
                assertTrue("VRAM total must be positive", g.totalVramMb > 0);
            }
        } else {
            assertEquals("no GPUs expected without nvidia-smi", 0, r.gpus.size());
        }
    }

    @Test
    public void testDashboardSamplePayload() {
        HardwareSnapshot s = SERVICE.refreshNow();
        JSONObject json = HardwareDashboardBridge.toSampleJson(s);
        assertTrue(json.has("ts"));
        assertTrue(json.has("cpu"));
        assertTrue(json.has("ram"));
        assertTrue(json.has("jvm"));
        assertTrue(json.has("gpus"));
        JSONArray gpus = json.getJSONArray("gpus");
        assertEquals("sample GPU count must match snapshot", s.gpus.size(), gpus.length());
        assertTrue("CPU payload must expose utilization", json.getJSONObject("cpu").has("util"));
        assertTrue("JVM payload must expose max heap", json.getJSONObject("jvm").has("max"));
    }

    @Test
    public void testDashboardScanPayload() {
        HardwareScanResult r = SERVICE.runScan();
        JSONObject json = HardwareDashboardBridge.toScanJson(r);
        assertEquals(r.status.name(), json.getString("status"));
        assertEquals(r.cudaStatus.name(), json.getString("cudaStatus"));
        assertTrue(json.has("notes"));
        assertTrue(json.getJSONObject("cpu").getInt("logicalCores") > 0);
        assertTrue(json.getJSONObject("ram").getLong("total") > 0);
    }

    private static boolean isMonitorThreadAlive() {
        for (Thread t : Thread.getAllStackTraces().keySet()) {
            if ("evo-hardware-monitor".equals(t.getName()) && t.isAlive()) {
                return true;
            }
        }
        return false;
    }
}
