package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import eu.kalafatic.evolution.controller.orchestration.selfdev.AbstractSelfDevTask;
import eu.kalafatic.evolution.controller.orchestration.selfdev.ResolvedSelfDevResources;
import eu.kalafatic.evolution.controller.orchestration.selfdev.SelfDevContext;
import eu.kalafatic.evolution.controller.orchestration.selfdev.SelfDevLoopTask;
import eu.kalafatic.evolution.controller.orchestration.selfdev.SelfDevOrchestrator;
import eu.kalafatic.evolution.controller.orchestration.selfdev.SelfDevPreflightResult;
import eu.kalafatic.evolution.controller.orchestration.selfdev.SelfDevTask;
import eu.kalafatic.evolution.controller.orchestration.selfdev.TaskResult;
import eu.kalafatic.evolution.controller.orchestration.selfdev.TaskStatus;
import eu.kalafatic.evolution.controller.resource.ResourceManager;
import eu.kalafatic.evolution.model.orchestration.Git;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

/**
 * WP2 - Task registry, graph validation, preflight gate, cancellation and
 * honest task statuses.
 */
public class SelfDevTaskGraphGateTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private ResourceManager resourceManager;

    @Before
    public void setUp() {
        resourceManager = ResourceManager.getInstance();
        resourceManager.refresh();
    }

    private SelfDevOrchestrator newOrchestrator() {
        return new SelfDevOrchestrator((File) null, resourceManager.getOrchestrator());
    }

    private void installSnapshot(SelfDevContext context) {
        context.setResolvedResources(new ResolvedSelfDevResources(
                context.getRepositoryRoot(), context.getProjectRoot(),
                context.getSourceReactorDirectory(), context.getPreparedReactorDirectory(),
                context.getBuildDirectory(), context.getExportDirectory(),
                context.getRuntimeDirectory(), context.getLogDirectory(),
                context.getSupervisorDirectory(), context.getGenomeDirectory(),
                context.getOs(), context.getWs(), context.getArch(),
                context.getProductId(), context.getLauncher(),
                new File(System.getProperty("java.home"), "bin/java"),
                new File("mvn")));
    }

    // ------------------------------------------------------------------
    // 1. Canonical IDs only - alias collapse + duplicate detection
    // ------------------------------------------------------------------

    @Test
    public void testAliasIdsRemovedAndCanonicalIdsPresent() {
        SelfDevOrchestrator orchestrator = newOrchestrator();
        Map<String, SelfDevTask> registry = orchestrator.getTaskRegistry();
        assertNotNull(registry);

        assertFalse("Alias ID BUILD must be removed", registry.containsKey("BUILD"));
        assertFalse("Alias ID MAVEN must be removed", registry.containsKey("MAVEN"));
        assertFalse("Alias ID EXPORT must be removed", registry.containsKey("EXPORT"));
        assertFalse("Alias ID DEPLOY must be removed", registry.containsKey("DEPLOY"));
        assertTrue(registry.containsKey("BUILD_EVO"));
        assertTrue(registry.containsKey("MAVEN_EVO"));
        assertTrue(registry.containsKey("EXPORT_EVO"));
        assertTrue(registry.containsKey("DEPLOY_EVO"));
        assertEquals("Registry size must be 30 canonical tasks", 30, registry.size());
    }

    @Test
    public void testDuplicateRegistrationRejected() throws Exception {
        SelfDevOrchestrator orchestrator = newOrchestrator();
        SelfDevTask duplicate = new AbstractSelfDevTask("COPY", "Duplicate") {
            @Override
            protected TaskResult run(SelfDevContext context) { return TaskResult.success(id, "dup"); }
        };
        Method register = SelfDevOrchestrator.class.getDeclaredMethod("register", SelfDevTask.class);
        register.setAccessible(true);
        boolean rejected = false;
        try {
            register.invoke(orchestrator, duplicate);
        } catch (java.lang.reflect.InvocationTargetException e) {
            if (e.getCause() instanceof IllegalStateException) {
                rejected = true;
            }
        }
        assertTrue("Duplicate task ID registration must be rejected", rejected);
    }

    // ------------------------------------------------------------------
    // 2. Static DAG validation (cycles, unknown deps, self deps)
    // ------------------------------------------------------------------

    @Test
    public void testProductionGraphHasNoValidationErrors() {
        SelfDevOrchestrator orchestrator = newOrchestrator();
        assertTrue("Production task graph must be valid: " + orchestrator.getGraphValidationErrors(),
                orchestrator.getGraphValidationErrors().isEmpty());
    }

    @Test
    public void testGraphValidationDetectsUnknownAndSelfAndCycles() throws Exception {
        SelfDevOrchestrator orchestrator = newOrchestrator();

        @SuppressWarnings("unchecked")
        Map<String, SelfDevTask> reg = (Map<String, SelfDevTask>) getRegistryField(orchestrator);
        SelfDevTask selfDep = new AbstractSelfDevTask("FAKE_SELF", "Self") {
            @Override protected TaskResult run(SelfDevContext c) { return TaskResult.success(id, "ok"); }
        };
        // addDependency intentionally filters self-dependencies; inject the self-dep
        // directly to verify the graph validator detects it when present.
        Field depsField = AbstractSelfDevTask.class.getDeclaredField("dependencies");
        depsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        java.util.Set<String> selfDeps = (java.util.Set<String>) depsField.get(selfDep);
        selfDeps.add("FAKE_SELF");
        SelfDevTask unknownDep = new AbstractSelfDevTask("FAKE_UNKNOWN", "Unknown") {
            @Override protected TaskResult run(SelfDevContext c) { return TaskResult.success(id, "ok"); }
        };
        unknownDep.addDependency("NOT_REGISTERED_TASK");
        SelfDevTask cycleA = new AbstractSelfDevTask("FAKE_CYCLE_A", "A") {
            @Override protected TaskResult run(SelfDevContext c) { return TaskResult.success(id, "ok"); }
        };
        SelfDevTask cycleB = new AbstractSelfDevTask("FAKE_CYCLE_B", "B") {
            @Override protected TaskResult run(SelfDevContext c) { return TaskResult.success(id, "ok"); }
        };
        cycleA.addDependency("FAKE_CYCLE_B");
        cycleB.addDependency("FAKE_CYCLE_A");
        reg.put("FAKE_SELF", selfDep);
        reg.put("FAKE_UNKNOWN", unknownDep);
        reg.put("FAKE_CYCLE_A", cycleA);
        reg.put("FAKE_CYCLE_B", cycleB);

        Method validate = SelfDevOrchestrator.class.getDeclaredMethod("validateTaskGraph");
        validate.setAccessible(true);
        validate.invoke(orchestrator);

        java.util.List<String> errors = orchestrator.getGraphValidationErrors();
        assertFalse("Graph validation must detect violations", errors.isEmpty());
        assertTrue("Self-dependency not detected in: " + errors, errors.stream().anyMatch(e -> e.contains("Self-dependency") && e.contains("FAKE_SELF")));
        assertTrue("Unknown dependency not detected in: " + errors, errors.stream().anyMatch(e -> e.contains("Unknown dependency") && e.contains("NOT_REGISTERED_TASK")));
        assertTrue("Cycle not detected in: " + errors, errors.stream().anyMatch(e -> e.contains("Cycle detected")));
    }

    private Object getRegistryField(SelfDevOrchestrator orchestrator) throws Exception {
        Field f = SelfDevOrchestrator.class.getDeclaredField("taskRegistry");
        f.setAccessible(true);
        return f.get(orchestrator);
    }

    // ------------------------------------------------------------------
    // 3. Preflight gate (D-39)
    // ------------------------------------------------------------------

    @Test
    public void testPreflightGateBlocksWhenPreflightFails() {
        Orchestrator previous = resourceManager.getOrchestrator();
        try {
            Orchestrator bad = OrchestrationFactory.eINSTANCE.createOrchestrator();
            Git badGit = OrchestrationFactory.eINSTANCE.createGit();
            badGit.setLocalPath(new File(System.getProperty("user.home"), "git/selfdev-gate-nonexistent").getAbsolutePath());
            bad.setGit(badGit);
            resourceManager.setOrchestrator(bad);

            SelfDevOrchestrator orchestrator = newOrchestrator();
            TaskResult res = orchestrator.executeTaskWithDependencies("PERMISSIONS");
            assertEquals("Execution must be BLOCKED by the preflight gate", TaskStatus.BLOCKED, res.getStatus());
            assertTrue(res.getMessage().contains("preflight gate"));
        } finally {
            resourceManager.setOrchestrator(previous);
        }
    }

    @Test
    public void testPreflightGatePassesWithValidSnapshot() throws Exception {
        SelfDevOrchestrator orchestrator = newOrchestrator();
        installSnapshot(orchestrator.getContext());

        SelfDevTask fake = new AbstractSelfDevTask("FAKE_GATE", "Gate") {
            @Override protected TaskResult run(SelfDevContext c) { return TaskResult.success(id, "gated-ok"); }
        };
        @SuppressWarnings("unchecked")
        Map<String, SelfDevTask> reg = (Map<String, SelfDevTask>) getRegistryField(orchestrator);
        reg.put("FAKE_GATE", fake);

        TaskResult res = orchestrator.executeTaskWithDependencies("FAKE_GATE");
        assertTrue("Task must execute when a valid snapshot exists: " + res.getMessage(), res.isSuccess());
    }

    @Test
    public void testSuccessfulPreflightInstallsSnapshot() {
        // Preflight is environment-dependent; verify the contract: when preflight
        // succeeds, the snapshot must be installed on the context.
        SelfDevOrchestrator orchestrator = newOrchestrator();
        SelfDevPreflightResult res = orchestrator.executePreflight();
        if (res.isSuccess()) {
            assertNotNull("Successful preflight must install the resolved snapshot",
                    orchestrator.getContext().getResolvedResources());
        } else {
            assertNull("Failed preflight must not install the resolved snapshot",
                    orchestrator.getContext().getResolvedResources());
        }
    }

    // ------------------------------------------------------------------
    // 4. Cancellation semantics
    // ------------------------------------------------------------------

    @Test
    public void testCancellationDuringExecutionYieldsCancelledStatus() throws Exception {
        SelfDevContext context = newOrchestrator().getContext();
        AbstractSelfDevTask task = new AbstractSelfDevTask("FAKE_CANCEL", "Cancel") {
            @Override
            protected TaskResult run(SelfDevContext c) throws Exception {
                for (int i = 0; i < 400; i++) {
                    checkCancelled();
                    Thread.sleep(10);
                }
                return TaskResult.success(id, "completed");
            }
        };

        AtomicReference<TaskResult> resultRef = new AtomicReference<>();
        Thread worker = new Thread(() -> resultRef.set(task.execute(context)), "selfdev-task-test-cancel");
        worker.start();
        Thread.sleep(120);
        task.cancel();
        worker.join(10000);

        TaskResult result = resultRef.get();
        assertNotNull(result);
        assertEquals("Cancelled task must report CANCELLED", TaskStatus.CANCELLED, result.getStatus());
        assertFalse("Cancelled task must not report success", result.isSuccess());
        assertTrue("CANCELLED must be a terminal status recorded in context",
                context.getTaskResult("FAKE_CANCEL") != null
                && context.getTaskResult("FAKE_CANCEL").getStatus() == TaskStatus.CANCELLED);
    }

    @Test
    public void testSkippedRetainsReason() {
        SelfDevContext context = newOrchestrator().getContext();
        AbstractSelfDevTask task = new AbstractSelfDevTask("FAKE_SKIP", "Skip") {
            @Override
            protected TaskResult run(SelfDevContext c) { return TaskResult.success(id, "never"); }
        };
        task.cancel();
        TaskResult result = task.execute(context);
        assertEquals("Pre-cancelled task must report SKIPPED", TaskStatus.SKIPPED, result.getStatus());
        assertNotNull("SKIPPED must carry an explicit reason", result.getMessage());
        assertFalse("SKIPPED reason must not be empty", result.getMessage().trim().isEmpty());
    }

    // ------------------------------------------------------------------
    // 5. Honest loop-task semantics (bounded ping, no false success)
    // ------------------------------------------------------------------

    @Test
    public void testSelfDevLoopTaskFailsHonestlyWhenUnreachable() throws Exception {
        SelfDevContext context = newOrchestrator().getContext();
        // Debug mode shifts effective ports by +10, moving the check off the
        // possibly-occupied production ports for a deterministic environment.
        context.setDebugMode(true);
        installSnapshot(context);

        SelfDevLoopTask loop = new SelfDevLoopTask("SELF_DEV_LOOP");
        TaskResult res = loop.execute(context);
        assertEquals("Loop ping must fail honestly when nothing is listening", TaskStatus.FAILED, res.getStatus());
        assertTrue(res.getMessage().contains("not responsive"));
    }
}
