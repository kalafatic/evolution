package eu.kalafatic.evolution.controller.tests;

import eu.kalafatic.evolution.controller.hardware.CudaDiagnosticReport;
import eu.kalafatic.evolution.controller.hardware.CudaDiagnosticService;
import eu.kalafatic.evolution.controller.hardware.DiagnosticStage;
import eu.kalafatic.evolution.controller.hardware.DiagnosticStatus;
import eu.kalafatic.evolution.controller.hardware.StageStatus;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests for the centralized CUDA diagnostic service. The Tycho test JVM runs
 * Java 21 while the GPU backend classes are compiled for stable Java 22 FFM,
 * so on this JVM the backend class-loading stage must fail with an explicit
 * {@link UnsupportedClassVersionError} detail and the overall result must be
 * PARTIAL/UNSUPPORTED — never a fabricated PASS. Device detection itself (via
 * nvidia-smi) is exercised wherever a GPU is present.
 */
public class CudaDiagnosticServiceTest {

    private final CudaDiagnosticService service = CudaDiagnosticService.getInstance();

    @Test
    public void testReportHasAllStagesInOrder() {
        CudaDiagnosticReport r = service.run();
        List<DiagnosticStage> stages = r.getStages();
        assertEquals("exactly 12 stages", CudaDiagnosticReport.STAGE_ORDER.size(), stages.size());
        for (int i = 0; i < CudaDiagnosticReport.STAGE_ORDER.size(); i++) {
            assertEquals("stage order must be fixed", CudaDiagnosticReport.STAGE_ORDER.get(i),
                    stages.get(i).id);
            assertNotNull("every stage must have a status", stages.get(i).status);
        }
        assertNotNull(r.executionId);
        assertFalse(r.executionId.isEmpty());
        assertTrue("finish time must not precede start", !r.finishedAt.isBefore(r.startedAt));
        assertTrue(r.totalDurationMs() >= 0);
    }

    @Test
    public void testExecutionIdsAreUniquePerRun() {
        String id1 = service.run().executionId;
        String id2 = service.run().executionId;
        assertFalse("each run must have its own execution id", id1.equals(id2));
    }

    @Test
    public void testJava21BackendClassLoadingFailsExplicitly() {
        int jvmMajor = parseJvmMajor();
        CudaDiagnosticReport r = service.run();
        DiagnosticStage backend = r.stage(CudaDiagnosticReport.STAGE_BACKEND_CLASS_LOADING);
        if (jvmMajor > 0 && jvmMajor < 22 && r.gpuBundlePresent) {
            assertEquals("Java 21 cannot load Java 22 backend classes", StageStatus.FAILED,
                    backend.status);
            assertTrue("failure detail must name the version problem (was: " + backend.detail + ")",
                    backend.detail.contains("UnsupportedClassVersionError")
                            || backend.detail.contains("Java 22"));
            assertFalse("kernel cannot have executed", r.kernelExecuted());
        }
        if (jvmMajor >= 22) {
            // On Java 22+ the stage result depends on the driver, never on guessing.
            assertTrue(backend.status == StageStatus.PASS || backend.status == StageStatus.FAILED
                    || backend.status == StageStatus.UNSUPPORTED);
        }
    }

    @Test
    public void testOverallStatusIsHonest() {
        CudaDiagnosticReport r = service.run();
        assertFalse("a finished report must not be RUNNING",
                r.overallStatus == DiagnosticStatus.RUNNING);
        assertTrue("overall must be one of the defined terminal states",
                r.overallStatus == DiagnosticStatus.PASS
                        || r.overallStatus == DiagnosticStatus.PARTIAL
                        || r.overallStatus == DiagnosticStatus.FAILED
                        || r.overallStatus == DiagnosticStatus.UNSUPPORTED);

        // A GPU can be detected while CUDA still fails — these must never be conflated.
        if (r.kernelExecuted()) {
            assertTrue("kernel success implies numeric verification", r.numericalVerificationPassed);
            assertTrue("kernel success implies cleanup ran", r.cleanupPassed);
            assertEquals("all executed stages passed -> PASS", DiagnosticStatus.PASS,
                    r.overallStatus);
            assertTrue("evidence counters must be recorded", r.kernelLaunchCount > 0);
            assertTrue("memcpy evidence must be recorded", r.memcpyCount > 0);
            assertTrue("device memory must have been allocated", r.deviceMemoryAllocatedBytes > 0);
        } else if (r.gpuDetected && !r.stage(CudaDiagnosticReport.STAGE_BACKEND_CLASS_LOADING).passed()) {
            assertEquals("GPU detected but backend not loadable -> PARTIAL",
                    DiagnosticStatus.PARTIAL, r.overallStatus);
        }
        assertFalse("numerical verification must not pass without kernel execution",
                r.numericalVerificationPassed && !r.kernelExecuted());
    }

    @Test
    public void testDeviceDetectionStageRunsOnGpuMachine() {
        CudaDiagnosticReport r = service.run();
        DiagnosticStage detection = r.stage(CudaDiagnosticReport.STAGE_DEVICE_DETECTION);
        boolean nvidiaSmiPresent = new java.io.File("/usr/bin/nvidia-smi").exists();
        if (nvidiaSmiPresent && r.gpuBundlePresent) {
            assertEquals("device detection must pass where nvidia-smi reports a GPU",
                    StageStatus.PASS, detection.status);
            assertTrue("GPU name must be captured", r.gpuName != null && !r.gpuName.isEmpty());
            assertTrue("VRAM must be captured", r.gpuTotalVramMb > 0);
        } else {
            assertTrue(detection.status == StageStatus.FAILED
                    || detection.status == StageStatus.UNSUPPORTED);
        }
    }

    @Test
    public void testHistoryIsBounded() {
        for (int i = 0; i < CudaDiagnosticService.HISTORY_LIMIT + 2; i++) {
            service.run();
        }
        List<CudaDiagnosticReport> history = service.getHistory();
        assertTrue("history must not exceed the limit (was " + history.size() + ")",
                history.size() <= CudaDiagnosticService.HISTORY_LIMIT);
    }

    @Test
    public void testAsyncStartDoesNotDuplicateRunningTest() throws Exception {
        CudaDiagnosticReport first = service.startAsync();
        CudaDiagnosticReport second = service.startAsync();
        assertEquals("a second start while running must return the same report",
                first.executionId, second.executionId);
        long deadline = System.currentTimeMillis() + 60000;
        while (service.isRunning() && System.currentTimeMillis() < deadline) {
            Thread.sleep(100);
        }
        assertFalse("test must complete within the deadline", service.isRunning());
        CudaDiagnosticReport after = service.startAsync();
        assertFalse("a new test after completion must be a new execution",
                first.executionId.equals(after.executionId));
        deadline = System.currentTimeMillis() + 60000;
        while (service.isRunning() && System.currentTimeMillis() < deadline) {
            Thread.sleep(100);
        }
    }

    @Test
    public void testTextReportIsRenderable() {
        CudaDiagnosticReport r = service.run();
        String text = r.toText();
        assertNotNull(text);
        assertTrue(text.contains("STAGES"));
        assertTrue(text.contains(r.executionId));
        assertTrue(text.contains(r.overallStatus.name()));
    }

    private static int parseJvmMajor() {
        String v = System.getProperty("java.version", "0");
        try {
            String[] parts = v.split("\\.");
            return parts[0].matches("\\d+") ? Integer.parseInt(parts[0]) : Integer.parseInt(parts[1]);
        } catch (RuntimeException e) {
            return 0;
        }
    }
}
