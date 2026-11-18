package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import eu.kalafatic.evolution.controller.orchestration.selfdev.SelfDevBootstrapController;
import eu.kalafatic.evolution.controller.orchestration.selfdev.SelfDevContext;
import eu.kalafatic.evolution.controller.orchestration.selfdev.SelfDevIterationLedger;
import eu.kalafatic.evolution.controller.orchestration.selfdev.SelfDevLoopGuard;
import eu.kalafatic.evolution.controller.orchestration.selfdev.SelfDevOrchestrator;
import eu.kalafatic.evolution.controller.resource.ResourceManager;

/**
 * WP8 - Darwin iteration loop tests: bounded execution, termination reasons,
 * persistent iteration ledger, single controlled entry point.
 */
public class SelfDevIterationLoopTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private ResourceManager resourceManager;
    private final List<File> createdRunDirs = new ArrayList<>();

    @Before
    public void setUp() {
        resourceManager = ResourceManager.getInstance();
        resourceManager.refresh();
    }

    @After
    public void tearDown() {
        for (File dir : createdRunDirs) {
            try {
                deleteRecursively(dir);
            } catch (Exception ignored) {}
        }
        createdRunDirs.clear();
    }

    private void deleteRecursively(File f) throws Exception {
        if (f.isDirectory()) {
            File[] children = f.listFiles();
            if (children != null) {
                for (File c : children) {
                    deleteRecursively(c);
                }
            }
        }
        Files.deleteIfExists(f.toPath());
    }

    private SelfDevContext newContext() {
        SelfDevContext context = new SelfDevContext(null, resourceManager.getOrchestrator());
        createdRunDirs.add(context.getBuildDirectory().getParentFile());
        return context;
    }

    // ------------------------------------------------------------------
    // Ledger
    // ------------------------------------------------------------------

    @Test
    public void testLedgerRoundTripAndPersistence() {
        SelfDevContext context = newContext();
        SelfDevIterationLedger ledger = new SelfDevIterationLedger(context);
        ledger.record("iter-1", SelfDevIterationLedger.Stage.OBJECTIVE_RECEIVED, "SUCCESS", null, "build feature X");
        ledger.record("iter-1", SelfDevIterationLedger.Stage.BUILD, "SUCCESS", "BUILD_EVO", "mvn ok");
        ledger.record("iter-1", SelfDevIterationLedger.Stage.COMMIT_PREPARED, "PREPARED", null, "eligible");

        assertNotNull(ledger.getLedgerFile());
        assertTrue("Ledger must be persisted", ledger.getLedgerFile().exists());

        List<SelfDevIterationLedger.Entry> entries = ledger.readEntries();
        assertEquals(3, entries.size());
        assertEquals(SelfDevIterationLedger.Stage.OBJECTIVE_RECEIVED, entries.get(0).getStage());
        assertEquals(SelfDevIterationLedger.Stage.BUILD, entries.get(1).getStage());
        assertEquals("BUILD_EVO", entries.get(1).getTaskId());
        assertEquals(SelfDevIterationLedger.Stage.COMMIT_PREPARED, entries.get(2).getStage());
    }

    // ------------------------------------------------------------------
    // Loop guard
    // ------------------------------------------------------------------

    @Test
    public void testGuardDefaultsBoundNonPositiveMaxIterations() {
        SelfDevLoopGuard guard = new SelfDevLoopGuard(0, -1, -1, -1, -1);
        assertEquals("maxIterations<=0 must resolve to the default bound (D-56)",
                SelfDevLoopGuard.DEFAULT_MAX_ITERATIONS, guard.getMaxIterations());
        assertEquals(SelfDevLoopGuard.TerminationReason.NONE, guard.evaluate(false));
    }

    @Test
    public void testGuardTerminatesAtMaxIterations() {
        SelfDevLoopGuard guard = new SelfDevLoopGuard(3, -1, -1, -1, -1);
        guard.recordIterationOutcome(true, null, true);
        guard.recordIterationOutcome(true, null, true);
        guard.recordIterationOutcome(true, null, true);
        assertEquals(SelfDevLoopGuard.TerminationReason.MAX_ITERATIONS_REACHED, guard.evaluate(false));
    }

    @Test
    public void testGuardDetectsRepeatedIdenticalFailures() {
        SelfDevLoopGuard guard = SelfDevLoopGuard.defaults();
        guard.recordIterationOutcome(false, "mvn:BUILD FAILURE:compilation", true);
        guard.recordIterationOutcome(false, "mvn:BUILD FAILURE:compilation", true);
        guard.recordIterationOutcome(false, "mvn:BUILD FAILURE:compilation", true);
        assertEquals(SelfDevLoopGuard.TerminationReason.REPEATED_IDENTICAL_FAILURES, guard.evaluate(false));
    }

    @Test
    public void testGuardDetectsNoChangeIterations() {
        SelfDevLoopGuard guard = SelfDevLoopGuard.defaults();
        guard.recordIterationOutcome(true, null, false);
        guard.recordIterationOutcome(true, null, false);
        assertEquals(SelfDevLoopGuard.TerminationReason.NO_CHANGE_ITERATIONS, guard.evaluate(false));
    }

    @Test
    public void testGuardCancellation() {
        SelfDevLoopGuard guard = SelfDevLoopGuard.defaults();
        guard.cancel();
        assertEquals(SelfDevLoopGuard.TerminationReason.CANCELLED, guard.evaluate(false));
    }

    @Test
    public void testGuardTerminalStage() {
        SelfDevLoopGuard guard = SelfDevLoopGuard.defaults();
        assertEquals(SelfDevLoopGuard.TerminationReason.TERMINAL_STAGE, guard.evaluate(true));
    }

    @Test
    public void testGuardWallClock() throws InterruptedException {
        SelfDevLoopGuard guard = new SelfDevLoopGuard(-1, 150, -1, -1, -1);
        Thread.sleep(250);
        assertEquals(SelfDevLoopGuard.TerminationReason.WALL_CLOCK_EXCEEDED, guard.evaluate(false));
    }

    @Test
    public void testGuardRecoveryResetsFailureFingerprint() {
        SelfDevLoopGuard guard = new SelfDevLoopGuard(10, -1, -1, -1, -1);
        guard.recordIterationOutcome(false, "X", true);
        guard.recordIterationOutcome(false, "X", true);
        guard.recordIterationOutcome(true, null, true); // success resets the chain
        guard.recordIterationOutcome(false, "X", true);
        guard.recordIterationOutcome(false, "X", true);
        assertEquals("Success must reset the identical-failure chain",
                SelfDevLoopGuard.TerminationReason.NONE, guard.evaluate(false));
    }

    // ------------------------------------------------------------------
    // Single controlled entry point
    // ------------------------------------------------------------------

    @Test
    public void testStartIterationRecordsLedgerAndDelegates() {
        SelfDevContext context = newContext();
        SelfDevOrchestrator orchestrator = new SelfDevOrchestrator(context, null, null);
        SelfDevBootstrapController controller = new SelfDevBootstrapController(orchestrator);

        org.json.JSONObject result = controller.startIteration("Improve resource resolution", null);
        assertNotNull(result);
        assertEquals(context.getRunId(), result.optString("runId"));
        assertNotNull(result.optString("iterationId"));
        assertNotNull(result.optString("submissionStatus"));

        SelfDevIterationLedger ledger = new SelfDevIterationLedger(context);
        List<SelfDevIterationLedger.Entry> entries = ledger.readEntries();
        assertFalse("Ledger must contain iteration stages", entries.isEmpty());
        boolean objective = entries.stream().anyMatch(e -> e.getStage() == SelfDevIterationLedger.Stage.OBJECTIVE_RECEIVED);
        boolean analysis = entries.stream().anyMatch(e -> e.getStage() == SelfDevIterationLedger.Stage.ANALYSIS);
        boolean prepared = entries.stream().anyMatch(e -> e.getStage() == SelfDevIterationLedger.Stage.RUN_PREPARED);
        assertTrue(objective && analysis && prepared);
    }
}
