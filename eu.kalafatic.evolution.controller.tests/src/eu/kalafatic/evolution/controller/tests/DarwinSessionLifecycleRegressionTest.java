package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.*;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import eu.kalafatic.evolution.controller.orchestration.AiService;
import eu.kalafatic.evolution.controller.orchestration.Checkpoint;
import eu.kalafatic.evolution.controller.orchestration.IterationManager;
import eu.kalafatic.evolution.controller.orchestration.KernelFactory;
import eu.kalafatic.evolution.controller.orchestration.SessionContainer;
import eu.kalafatic.evolution.controller.orchestration.SessionContext;
import eu.kalafatic.evolution.controller.orchestration.SessionManager;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.cognitive.loop.CognitiveBudget;
import eu.kalafatic.evolution.controller.orchestration.cognitive.loop.CognitiveGoal;
import eu.kalafatic.evolution.controller.orchestration.cognitive.loop.CognitiveLoopEngine;
import eu.kalafatic.evolution.controller.orchestration.cognitive.loop.CognitiveResult;
import eu.kalafatic.evolution.controller.orchestration.cognitive.loop.CognitiveState;
import eu.kalafatic.evolution.controller.orchestration.selfdev.IterationMemoryService;
import eu.kalafatic.evolution.controller.workflow.RuntimeEvent;
import eu.kalafatic.evolution.controller.workflow.RuntimeEventType;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

/**
 * Regression test suite verifying Darwin session lifecycle:
 * CREATE -> INITIALIZE -> EXECUTE -> WAIT -> RESUME -> CANCEL -> COMPLETE/FAIL -> PERSIST -> DISPOSE
 */
public class DarwinSessionLifecycleRegressionTest {

    private File tempDir;

    @Before
    public void setUp() throws Exception {
        tempDir = Files.createTempDirectory("lifecycle-regression-test").toFile();
    }

    @After
    public void tearDown() throws Exception {
        SessionManager.getInstance().shutdownAll();
    }

    @Test
    public void testConcurrentSessionIsolation() throws Exception {
        int count = 5;
        ExecutorService executor = Executors.newFixedThreadPool(count);
        List<Future<Boolean>> futures = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            final String sessionId = "session-iso-" + i;
            futures.add(executor.submit(() -> {
                SessionContainer container = SessionManager.getInstance().getOrCreateSession(sessionId);
                assertNotNull("Session container must exist", container);
                assertEquals(sessionId, container.getSessionId());

                final AtomicInteger eventCount = new AtomicInteger(0);
                container.getEventBus().subscribe(event -> {
                    if (sessionId.equals(event.getSessionId())) {
                        eventCount.incrementAndGet();
                    }
                });

                container.getEventBus().publish(new RuntimeEvent(RuntimeEventType.TASK_STARTED, sessionId, "Test", "task-1"));
                Thread.sleep(150);

                return eventCount.get() >= 1;
            }));
        }

        for (Future<Boolean> f : futures) {
            assertTrue("Each session must isolate events properly", f.get());
        }
        executor.shutdown();
    }

    @Test
    public void testCancellationPropagationAndTimeoutDistinction() throws Exception {
        String sessionId = "session-cancel-test";
        SessionContainer container = SessionManager.getInstance().getOrCreateSession(sessionId);
        TaskContext context = new TaskContext(null, tempDir);
        context.setSessionId(sessionId);
        ((SessionContext) container).setTaskContext(context);

        // Test explicit cancellation
        context.setCancelled(true);
        assertTrue(context.isCancelled());

        CognitiveGoal goal = new CognitiveGoal("Testing cancellation", "GENERAL", null);
        CognitiveLoopEngine engine = new CognitiveLoopEngine(new CognitiveBudget(5, 60000L, 1, 1), null);

        CognitiveResult result = engine.solve(container, context, goal);
        assertNotNull(result);
        assertEquals("Goal must transition to CANCELLED state when cancelled", CognitiveState.CANCELLED, result.getFinalState());

        // Test timeout distinction
        String timeoutSessionId = "session-timeout-test";
        SessionContainer timeoutContainer = SessionManager.getInstance().getOrCreateSession(timeoutSessionId);
        TaskContext timeoutContext = new TaskContext(null, tempDir);
        timeoutContext.setSessionId(timeoutSessionId);

        CognitiveGoal timeoutGoal = new CognitiveGoal("Testing timeout", "UNKNOWN_DOMAIN", null);
        // Budget with 1ms wall clock limit to trigger timeout
        CognitiveLoopEngine timeoutEngine = new CognitiveLoopEngine(new CognitiveBudget(10, 1L, 1, 5), null);

        CognitiveResult timeoutResult = timeoutEngine.solve(timeoutContainer, timeoutContext, timeoutGoal);
        assertNotNull(timeoutResult);
        assertEquals("Goal must transition to TIMEOUT state when budget time is exceeded", CognitiveState.TIMEOUT, timeoutResult.getFinalState());
    }

    @Test
    public void testDisposalPreventsEventEmission() throws Exception {
        String sessionId = "session-disposal-test";
        SessionContainer container = SessionManager.getInstance().getOrCreateSession(sessionId);
        assertFalse(container.isDisposed());

        final List<RuntimeEvent> received = new CopyOnWriteArrayList<>();
        container.getEventBus().subscribe(received::add);

        // Pre-disposal event
        container.getEventBus().publish(new RuntimeEvent(RuntimeEventType.TASK_STARTED, sessionId, "Test", "task-pre"));
        Thread.sleep(150);
        assertEquals(1, received.size());

        // Dispose session
        SessionManager.getInstance().shutdownSession(sessionId);
        assertTrue(container.isDisposed());

        // Post-disposal publication must be rejected and not delivered
        container.getEventBus().publish(new RuntimeEvent(RuntimeEventType.TASK_COMPLETED, sessionId, "Test", "task-post"));
        Thread.sleep(150);
        assertEquals("No new events should be delivered after disposal", 1, received.size());
    }

    @Test
    public void testPersistenceAndRecoveryWithoutInvalidRuntimeObjects() throws Exception {
        String sessionId = "session-persist-test";
        SessionContainer container = SessionManager.getInstance().getOrCreateSession(sessionId);

        Orchestrator orchestrator = OrchestrationFactory.eINSTANCE.createOrchestrator();
        orchestrator.setId(sessionId);

        TaskContext context = new TaskContext(orchestrator, tempDir);
        context.setSessionId(sessionId);
        context.getOrchestrationState().setCurrentPhase("ANALYSIS");
        context.getOrchestrationState().setIterationCount(2);

        AiService mockAi = new AiService() {
            @Override
            public String sendRequest(Orchestrator orch, String prompt, float temperature, String proxyUrl, TaskContext ctx, String forcedModel) throws Exception {
                return "{\"category\": \"TASK\", \"confidence\": 0.9, \"reasoning\": \"Test execution\"}";
            }
        };
        context.setAiService(mockAi);

        IterationMemoryService memoryService = container.getMemoryService(tempDir);
        IterationManager manager = KernelFactory.create("Test Task", context, container, mockAi);
        container.setIterationManager(manager);

        // Save full checkpoint
        manager.saveFullCheckpoint();

        // Verify checkpoint file written to disk
        File checkpointFile = new File(tempDir, "orchestrator/memory/checkpoint_" + sessionId + ".json");
        assertTrue("Checkpoint file must be persisted", checkpointFile.exists());

        // Dispose previous session
        SessionManager.getInstance().shutdownSession(sessionId);
        assertTrue(container.isDisposed());

        // Create new container simulating platform recovery
        SessionContainer recoveredContainer = SessionManager.getInstance().getOrCreateSession(sessionId);
        assertFalse(recoveredContainer.isDisposed());

        TaskContext recoveredContext = new TaskContext(orchestrator, tempDir);
        recoveredContext.setSessionId(sessionId);

        // Checkpoint loading should restore iteration count and phase without reviving old threads
        Checkpoint loaded = memoryService.loadCheckpoint(sessionId);
        assertNotNull("Checkpoint should be recovered", loaded);
        assertEquals(sessionId, loaded.getSessionId());
        assertEquals("ANALYSIS", loaded.getCurrentPhase());
        assertEquals(2, loaded.getIterationCount());
    }

    @Test
    public void testAuthoritativeIterationManagerResolution() throws Exception {
        String sessionId = "session-authoritative-mgr-test";
        SessionContainer session = SessionManager.getInstance().getOrCreateSession(sessionId);

        // Context with null orchestrator
        TaskContext context = new TaskContext(null, tempDir);
        context.setSessionId(sessionId);
        AiService mockAi = new AiService() {
            @Override
            public String sendRequest(Orchestrator orch, String prompt, float temperature, String proxyUrl, TaskContext ctx, String forcedModel) throws Exception {
                return "{\"category\": \"TASK\", \"confidence\": 0.9, \"reasoning\": \"Test resolution\"}";
            }
        };
        context.setAiService(mockAi);

        // Should resolve and create IterationManager cleanly without throwing NPE
        IterationManager manager = KernelFactory.create("Verify manager resolution", context, session);
        assertNotNull(manager);
        session.setIterationManager(manager);

        assertEquals(manager, session.getIterationManager());
        assertNotNull(manager.getDarwinEngine());
        assertEquals(session, manager.getSessionContainer());
    }
}
