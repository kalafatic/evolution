package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import eu.kalafatic.evolution.controller.orchestration.AiService;
import eu.kalafatic.evolution.controller.orchestration.DarwinFlow;
import eu.kalafatic.evolution.controller.orchestration.IterationManager;
import eu.kalafatic.evolution.controller.orchestration.KernelFacade;
import eu.kalafatic.evolution.controller.orchestration.KernelFactory;
import eu.kalafatic.evolution.controller.orchestration.OrchestratorResponse;
import eu.kalafatic.evolution.controller.orchestration.PlatformMode;
import eu.kalafatic.evolution.controller.orchestration.PlatformType;
import eu.kalafatic.evolution.controller.orchestration.ResultType;
import eu.kalafatic.evolution.controller.orchestration.SessionContainer;
import eu.kalafatic.evolution.controller.orchestration.SessionManager;
import eu.kalafatic.evolution.controller.orchestration.SystemState;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.TaskRequest;
import eu.kalafatic.evolution.controller.orchestration.VariantExecutionContext;
import eu.kalafatic.evolution.controller.orchestration.cognitive.loop.CognitiveGoal;
import eu.kalafatic.evolution.controller.orchestration.cognitive.loop.CognitiveLoopEngine;
import eu.kalafatic.evolution.controller.orchestration.cognitive.loop.CognitiveObservation;
import eu.kalafatic.evolution.controller.orchestration.cognitive.loop.WorldState;
import eu.kalafatic.evolution.controller.orchestration.AutonomyLevel;
import eu.kalafatic.evolution.controller.orchestration.selfdev.ADarwinEngine;
import eu.kalafatic.evolution.controller.orchestration.selfdev.BranchVariant;
import eu.kalafatic.evolution.controller.orchestration.selfdev.ChatEngine;
import eu.kalafatic.evolution.controller.orchestration.selfdev.CodingEngine;
import eu.kalafatic.evolution.controller.orchestration.selfdev.CodingEngine2;
import eu.kalafatic.evolution.controller.orchestration.selfdev.DarwinEngine;
import eu.kalafatic.evolution.controller.orchestration.selfdev.DarwinEngineFactory;
import eu.kalafatic.evolution.controller.orchestration.selfdev.DarwinEngineRegistry;
import eu.kalafatic.evolution.controller.orchestration.selfdev.EvolutionaryPressureVector;
import eu.kalafatic.evolution.controller.orchestration.selfdev.IterationMemoryService;
import eu.kalafatic.evolution.controller.orchestration.selfdev.LLMDarwinEngine;
import eu.kalafatic.evolution.controller.orchestration.selfdev.MediatedEngine;
import eu.kalafatic.evolution.controller.orchestration.selfdev.SystemStateSignalProvider;
import eu.kalafatic.evolution.controller.orchestration.selfdev.TaskPlanner;
import eu.kalafatic.evolution.model.orchestration.EvaluationResult;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.SelfDevDecision;

public class DarwinExecutionLifecycleTest {

    private File tempDir;
    private String sessionId;
    private SessionContainer session;
    private Orchestrator orchestrator;
    private TaskContext context;

    @Before
    public void setUp() throws Exception {
        tempDir = Files.createTempDirectory("darwin-exec-test").toFile();
        sessionId = "darwin-test-" + System.currentTimeMillis();
        session = SessionManager.getInstance().getOrCreateSession(sessionId);

        orchestrator = OrchestrationFactory.eINSTANCE.createOrchestrator();
        orchestrator.setId(sessionId);

        context = new TaskContext(orchestrator, tempDir);
        context.setSessionId(sessionId);
        context.getMetadata().put("testMode", true);
        context.getMetadata().put("sessionContext", session);
    }

    @After
    public void tearDown() {
        SessionManager.getInstance().shutdownSession(sessionId);
        if (tempDir != null && tempDir.exists()) {
            deleteRecursive(tempDir);
        }
    }

    private void deleteRecursive(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) deleteRecursive(f);
        }
        dir.delete();
    }

    /**
     * T01: Mode resolution without unnecessary LLM intent escalation.
     * When explicit forced technical mode is set, it must bypass intent analysis and create the corresponding engine.
     */
    @Test
    public void testT01_ExplicitForcedModeResolution() throws Exception {
        context.setPlatformMode(new PlatformMode(PlatformType.FORGE, AutonomyLevel.MEDIUM, 3, false));
        IterationManager manager = KernelFactory.create("Train model", context, session);
        assertNotNull(manager);
        assertNotNull(manager.getDarwinEngine());
        assertTrue("Engine should be LLMDarwinEngine for FORGE mode", manager.getDarwinEngine() instanceof LLMDarwinEngine);
        assertEquals(PlatformType.FORGE, manager.getDarwinEngine().getPlatformType());

        context.setPlatformMode(new PlatformMode(PlatformType.HYBRID_MANUAL_EXPORT, AutonomyLevel.LOW, 1, false));
        IterationManager mediatedManager = KernelFactory.create("Export reality", context, session);
        assertNotNull(mediatedManager.getDarwinEngine());
        assertTrue("Engine should be MediatedEngine for HYBRID_MANUAL_EXPORT", mediatedManager.getDarwinEngine() instanceof MediatedEngine);
    }

    /**
     * T02: Single authoritative engine and IterationManager instance per session.
     */
    @Test
    public void testT02_SingleAuthoritativeEnginePerSession() throws Exception {
        KernelFacade facade = new KernelFacade();
        TaskRequest request = new TaskRequest("Test request", tempDir);
        facade.handle(request, context);

        IterationManager manager1 = session.getIterationManager();
        assertNotNull("IterationManager must be registered on session", manager1);
        ADarwinEngine engine1 = DarwinEngineRegistry.get(sessionId);
        assertNotNull("ADarwinEngine must be registered in DarwinEngineRegistry", engine1);
        assertEquals(manager1.getDarwinEngine(), engine1);

        // Second request in same session reuses existing manager
        facade.handle(new TaskRequest("Second request", tempDir), context);
        assertEquals("IterationManager must remain authoritative and single per session", manager1, session.getIterationManager());
    }

    /**
     * T03: Inference and Iteration failure propagation.
     * A non-successful EvaluationResult must result in ResultType.ERROR and SystemState.FAILED, never fake CHAT or DONE.
     */
    @Test
    public void testT03_FailurePropagationNeverFakeSuccess() throws Exception {
        IterationMemoryService memoryService = new IterationMemoryService(tempDir);
        SystemStateSignalProvider stateProvider = new SystemStateSignalProvider(tempDir, context);

        // Subclass engine that simulates failure
        ADarwinEngine failingEngine = new ADarwinEngine(context, memoryService, stateProvider, PlatformType.ASSISTED_CODING) {
            @Override
            public EvaluationResult runDarwinIteration(TaskContext ctx, IterationManager mgr) throws Exception {
                EvaluationResult res = OrchestrationFactory.eINSTANCE.createEvaluationResult();
                res.setSuccess(false);
                res.getErrors().add("Simulated compilation/reality failure");
                res.setDecision(SelfDevDecision.STOP);
                return res;
            }
        };

        IterationManager manager = KernelFactory.create(failingEngine, context, session, new AiService());
        session.setIterationManager(manager);

        OrchestratorResponse response = failingEngine.orchestrateEvolution(new TaskRequest("Failing evolution", tempDir), manager);
        assertNotNull(response);
        assertEquals("ResultType must be ERROR on failed evolution", ResultType.ERROR, response.getResultType());
        assertEquals("State must transition to FAILED", SystemState.FAILED, context.getStateHolder().getState());
        assertTrue("Summary must report failure", response.getSummary().contains("failed"));
    }

    /**
     * T04: Initialization failure propagation.
     * When critical exceptions occur in evolution, error response must be returned and state marked FAILED.
     */
    @Test
    public void testT04_ExceptionPropagation() {
        IterationMemoryService memoryService = new IterationMemoryService(tempDir);
        SystemStateSignalProvider stateProvider = new SystemStateSignalProvider(tempDir, context);

        ADarwinEngine throwingEngine = new ADarwinEngine(context, memoryService, stateProvider, PlatformType.ASSISTED_CODING) {
            @Override
            public EvaluationResult runDarwinIteration(TaskContext ctx, IterationManager mgr) throws Exception {
                throw new IllegalStateException("Simulated critical kernel exception");
            }
        };

        IterationManager manager;
        try {
            manager = KernelFactory.create(throwingEngine, context, session, new AiService());
            session.setIterationManager(manager);
            OrchestratorResponse resp = throwingEngine.orchestrateEvolution(new TaskRequest("Throwing task", tempDir), manager);
            assertEquals("ResultType must be ERROR", ResultType.ERROR, resp.getResultType());
            assertEquals("State must be FAILED", SystemState.FAILED, context.getStateHolder().getState());
        } catch (Exception e) {
            assertTrue("Exception message preserved", e.getMessage().contains("Simulated critical kernel exception"));
            assertEquals("State must be FAILED", SystemState.FAILED, context.getStateHolder().getState());
        }
    }

    /**
     * T05: Engine registry lifecycle: clean unregistration on session shutdown.
     */
    @Test
    public void testT05_RegistryLifecycleAndCleanUnregister() throws Exception {
        String testSid = "reg-test-" + System.currentTimeMillis();
        SessionContainer testSession = SessionManager.getInstance().getOrCreateSession(testSid);
        TaskContext testCtx = new TaskContext(orchestrator, tempDir);
        testCtx.setSessionId(testSid);
        testCtx.getMetadata().put("testMode", true);

        IterationManager mgr = KernelFactory.create("Test", testCtx, testSession);
        testSession.setIterationManager(mgr);

        assertTrue("DarwinEngineRegistry must register active engine", DarwinEngineRegistry.hasEngine(testSid));
        assertNotNull(DarwinEngineRegistry.get(testSid));

        // Shutdown session
        SessionManager.getInstance().shutdownSession(testSid);
        assertFalse("DarwinEngineRegistry must unregister engine on session shutdown", DarwinEngineRegistry.hasEngine(testSid));
    }

    /**
     * T06: CognitiveLoopEngine Darwin execution using authoritative session IterationManager without orphaned engines.
     */
    @Test
    public void testT06_CognitiveLoopAuthoritativeDarwinExecution() {
        CognitiveLoopEngine loopEngine = new CognitiveLoopEngine(3, null);
        CognitiveGoal goal = new CognitiveGoal("Refactor Architecture", "CODING", Map.of());

        // Invoke solve via reflection of executeRealDarwinSearch or solve directly
        java.lang.reflect.Method m = null;
        try {
            m = CognitiveLoopEngine.class.getDeclaredMethod("executeRealDarwinSearch",
                    SessionContainer.class, TaskContext.class, CognitiveGoal.class, WorldState.class, List.class);
            m.setAccessible(true);
            CognitiveObservation obs = (CognitiveObservation) m.invoke(loopEngine, session, context, goal, new WorldState(sessionId), Collections.emptyList());
            assertNotNull(obs);
            assertNotNull("Authoritative IterationManager should be initialized", session.getIterationManager());
            ADarwinEngine engine = DarwinEngineRegistry.get(sessionId);
            assertNotNull("Authoritative engine should be registered in DarwinEngineRegistry", engine);
            assertEquals(session.getIterationManager().getDarwinEngine(), engine);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * T07: DarwinFlow execution delegation.
     * DarwinFlow must delegate evaluateVariantParallel to the authoritative ADarwinEngine.
     */
    @Test
    public void testT07_DarwinFlowDelegatesToAuthoritativeEngine() throws Exception {
        IterationManager manager = KernelFactory.create("Test delegation", context, session);
        session.setIterationManager(manager);

        DarwinFlow flow = new DarwinFlow(new AiService(), manager);
        BranchVariant variant = new BranchVariant();
        variant.setId("v-test-1");
        variant.setStrategy("Test strategy");

        TaskPlanner planner = new TaskPlanner(session);
        EvolutionaryPressureVector pressure = new EvolutionaryPressureVector();

        VariantExecutionContext result = flow.evaluateVariantParallel(variant, planner, context, null, pressure);
        assertNotNull(result);
        assertEquals("v-test-1", result.getVariantId());
        assertTrue("Variant evaluated via authoritative engine", variant.isSuccess());
    }

    /**
     * T08: Concurrent sessions isolation.
     * Two separate sessions have independent SessionContainers, IterationManagers, and ADarwinEngine registrations.
     */
    @Test
    public void testT08_ConcurrentSessionsIsolation() throws Exception {
        String sidA = "session-A-" + System.currentTimeMillis();
        String sidB = "session-B-" + System.currentTimeMillis();

        SessionContainer sessionA = SessionManager.getInstance().getOrCreateSession(sidA);
        SessionContainer sessionB = SessionManager.getInstance().getOrCreateSession(sidB);

        TaskContext ctxA = new TaskContext(orchestrator, tempDir);
        ctxA.setSessionId(sidA);
        ctxA.getMetadata().put("testMode", true);

        TaskContext ctxB = new TaskContext(orchestrator, tempDir);
        ctxB.setSessionId(sidB);
        ctxB.getMetadata().put("testMode", true);

        IterationManager mgrA = KernelFactory.create("Task A", ctxA, sessionA);
        IterationManager mgrB = KernelFactory.create("Task B", ctxB, sessionB);

        sessionA.setIterationManager(mgrA);
        sessionB.setIterationManager(mgrB);

        ADarwinEngine engineA = DarwinEngineRegistry.get(sidA);
        ADarwinEngine engineB = DarwinEngineRegistry.get(sidB);

        assertNotNull(engineA);
        assertNotNull(engineB);
        assertTrue("Engines must be distinct objects across sessions", engineA != engineB);
        assertTrue("IterationManagers must be distinct across sessions", mgrA != mgrB);

        SessionManager.getInstance().shutdownSession(sidA);
        assertFalse(DarwinEngineRegistry.hasEngine(sidA));
        assertTrue(DarwinEngineRegistry.hasEngine(sidB));

        SessionManager.getInstance().shutdownSession(sidB);
        assertFalse(DarwinEngineRegistry.hasEngine(sidB));
    }

    /**
     * T09: Cancellation transitions state to FAILED and stops execution.
     */
    @Test
    public void testT09_CancellationStopsExecution() throws Exception {
        IterationManager manager = KernelFactory.create("Test cancel", context, session);
        session.setIterationManager(manager);

        context.setCancelled(true);
        OrchestratorResponse resp = manager.handle(new TaskRequest("Should cancel", tempDir));
        assertNotNull(resp);
        assertEquals("State must transition to FAILED upon cancellation", SystemState.FAILED, context.getStateHolder().getState());
    }

    /**
     * T10: CodingEngine2 backwards compatibility delegation check.
     */
    @Test
    public void testT10_CodingEngine2Compatibility() {
        IterationMemoryService memoryService = new IterationMemoryService(tempDir);
        SystemStateSignalProvider stateProvider = new SystemStateSignalProvider(tempDir, context);

        CodingEngine2 engine2 = new CodingEngine2(context, memoryService, stateProvider);
        assertTrue("CodingEngine2 must be instance of CodingEngine", engine2 instanceof CodingEngine);
        assertTrue("CodingEngine2 must be instance of ADarwinEngine", engine2 instanceof ADarwinEngine);
        assertEquals(PlatformType.ASSISTED_CODING, engine2.getPlatformType());
    }
}
