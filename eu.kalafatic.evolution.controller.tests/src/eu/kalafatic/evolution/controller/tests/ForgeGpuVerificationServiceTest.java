package eu.kalafatic.evolution.controller.tests;

import eu.kalafatic.evolution.controller.hardware.DiagnosticStatus;
import eu.kalafatic.evolution.controller.hardware.ForgeGpuVerificationReport;
import eu.kalafatic.evolution.controller.hardware.ForgeGpuVerificationService;
import eu.kalafatic.evolution.controller.orchestration.ForgeSessionManager;
import eu.kalafatic.evolution.forge.model.llm.EvoLlmModel;
import eu.kalafatic.evolution.forge.trainer.device.ForgeDeviceSelector;
import eu.kalafatic.evolution.forge.trainer.impl.llm.EvoLlmTrainer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests for the Forge GPU training verification service. The canonical
 * training path is exercised with an isolated tiny in-memory model:
 * CPU policy must execute on CPU with measurable parameter changes, AUTO must
 * fall back to CPU with an explicit reason when CUDA is unavailable, and
 * explicit CUDA must fail clearly instead of silently running on CPU. The
 * diagnostic must never run while a Forge training session is active and must
 * never touch the user's model, dataset or checkpoints.
 *
 * <p>{@link ForgeSessionManager} is a process-wide singleton shared with the
 * whole suite: {@link #cleanStaleTrainingState()} removes trainer registrations
 * left behind by other tests so the active-session guard is exercised
 * deterministically.</p>
 */
public class ForgeGpuVerificationServiceTest {

    private final ForgeGpuVerificationService service = ForgeGpuVerificationService.getInstance();

    private String originalDeviceProperty;
    private Path originalForgeStateDir;
    private Path tmpStateDir;

    @Before
    public void setUp() throws Exception {
        originalDeviceProperty = System.getProperty("evo.forge.device");
        originalForgeStateDir = ForgeSessionManager.getForgeStateDirectory();
        tmpStateDir = Files.createTempDirectory("evo-forge-diag-test");
        ForgeSessionManager.setForgeStateDirectory(tmpStateDir);
        cleanStaleTrainingState();
    }

    @After
    public void tearDown() {
        cleanStaleTrainingState();
        ForgeSessionManager.setForgeStateDirectory(originalForgeStateDir);
        restoreDevicePolicy();
    }

    /** Completes any trainer registrations left behind by other suite tests. */
    private static void cleanStaleTrainingState() {
        ForgeSessionManager mgr = ForgeSessionManager.getInstance();
        for (int i = 0; i < 100; i++) {
            String active = mgr.getActiveTrainingSessionId();
            if (active == null || active.trim().isEmpty()) {
                return;
            }
            mgr.completeActiveTrainer(active, "COMPLETED");
        }
    }

    @Test
    public void testCpuPolicyExecutesOnCpu() {
        setDevicePolicy("CPU");
        ForgeGpuVerificationReport r = service.run();
        assertEquals("CPU policy must select CPU", "CPU", r.selectedDevice);
        assertEquals("CPU policy must execute the CPU backend", "CPU", r.actualDevice);
        assertTrue("CPU backend name must be reported", r.actualBackend.contains("CPU"));
        assertTrue("parameters must change during real training", r.parametersChanged);
        assertTrue("changed parameter count must be positive", r.parameterChangeCount > 0);
        assertTrue("max abs delta must be positive", r.parameterChangeMaxAbs > 0);
        assertFalse("loss history must not be empty", r.lossHistory.isEmpty());
        assertTrue("optimizer steps must be recorded", r.trainingStepsExecuted > 0);
        assertEquals("CPU policy with CPU execution is PASS", DiagnosticStatus.PASS,
                r.overallStatus);
        assertFalse("CPU run must not be blocked", r.blockedByActiveSession);
    }

    @Test
    public void testAutoFallsBackToCpuWhenCudaUnavailable() {
        setDevicePolicy("AUTO");
        boolean cudaAvailable = ForgeDeviceSelector.isCudaAvailable();
        ForgeGpuVerificationReport r = service.run();
        assertEquals("AUTO policy must be captured", "AUTO", r.policyLabel);
        if (!cudaAvailable) {
            assertEquals("AUTO must fall back to CPU when CUDA is unavailable", "CPU",
                    r.selectedDevice);
            assertNotNull("fallback reason must be explicit", r.selectionNote);
            assertFalse("fallback reason must not be empty", r.selectionNote.isEmpty());
            assertEquals("AUTO fallback executes the CPU backend", "CPU", r.actualDevice);
            assertTrue(r.parametersChanged);
            assertEquals("AUTO + CPU fallback is PASS (correct policy behavior)",
                    DiagnosticStatus.PASS, r.overallStatus);
        } else {
            // CUDA actually available: the run must select CUDA and prove it.
            assertEquals("CUDA", r.selectedDevice);
            assertTrue("CUDA run must be one of PASS/PARTIAL",
                    r.overallStatus == DiagnosticStatus.PASS
                            || r.overallStatus == DiagnosticStatus.PARTIAL);
            if (r.cudaActuallyExecuted()) {
                assertTrue("parameters must change", r.parametersChanged);
                assertFalse(r.lossHistory.isEmpty());
            }
        }
    }

    @Test
    public void testExplicitCudaFailsClearlyWhenUnavailable() {
        setDevicePolicy("CUDA");
        boolean cudaAvailable = ForgeDeviceSelector.isCudaAvailable();
        ForgeGpuVerificationReport r = service.run();
        assertEquals("CUDA policy must be captured", "CUDA", r.policyLabel);
        if (!cudaAvailable) {
            assertEquals("explicit CUDA must fail, not silently run on CPU",
                    DiagnosticStatus.FAILED, r.overallStatus);
            assertTrue("failure detail must explain the CUDA unavailability (was: "
                            + r.failureDetail + ")",
                    r.failureDetail.contains("CUDA"));
            assertEquals("no training may execute for explicit CUDA without CUDA", "none",
                    r.actualDevice);
        } else {
            assertEquals("CUDA", r.selectedDevice);
            assertTrue(r.overallStatus == DiagnosticStatus.PASS
                    || r.overallStatus == DiagnosticStatus.PARTIAL);
            assertTrue("CUDA execution must be proven by the backend",
                    r.cudaActuallyExecuted());
        }
    }

    @Test
    public void testDiagnosticRefusesToRunWhileTrainingIsActive() {
        setDevicePolicy("CPU");
        EvoLlmModel tiny = new EvoLlmModel(64, 32, 4, 1, 64, 32);
        EvoLlmTrainer trainer = new EvoLlmTrainer(tiny);
        ForgeSessionManager.getInstance().registerActiveTrainer("gpu-diag-test-session", trainer);
        try {
            String active = ForgeSessionManager.getInstance().getActiveTrainingSessionId();
            assertNotNull("an active trainer must be reported", active);
            ForgeGpuVerificationReport r = service.run();
            assertEquals("verification must not run during an active session",
                    DiagnosticStatus.NOT_TESTED, r.overallStatus);
            assertTrue("blocked flag must be set", r.blockedByActiveSession);
            assertTrue("blocked reason must name the session",
                    r.blockedReason.contains("gpu-diag-test-session"));
        } finally {
            ForgeSessionManager.getInstance().completeActiveTrainer("gpu-diag-test-session", "COMPLETED");
        }
    }

    @Test
    public void testReportStructureAndHistoryBounds() {
        setDevicePolicy("CPU");
        for (int i = 0; i < ForgeGpuVerificationService.HISTORY_LIMIT + 2; i++) {
            service.run();
        }
        assertTrue("history must be bounded",
                service.getHistory().size() <= ForgeGpuVerificationService.HISTORY_LIMIT);
        ForgeGpuVerificationReport r = service.getCurrent();
        assertNotNull(r);
        assertFalse(r.executionId.isEmpty());
        assertTrue(r.totalDurationMs() >= 0);
        assertNotNull(r.toText());
        assertTrue(r.toText().contains("PARAMETER CHANGE VERIFICATION"));
    }

    private void setDevicePolicy(String policy) {
        System.setProperty("evo.forge.device", policy);
    }

    private void restoreDevicePolicy() {
        if (originalDeviceProperty == null) {
            System.clearProperty("evo.forge.device");
        } else {
            System.setProperty("evo.forge.device", originalDeviceProperty);
        }
    }
}
