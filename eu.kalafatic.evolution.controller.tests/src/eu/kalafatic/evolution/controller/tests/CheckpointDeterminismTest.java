package eu.kalafatic.evolution.controller.tests;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

import static org.junit.Assert.*;
import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import eu.kalafatic.evolution.controller.orchestration.*;
import eu.kalafatic.evolution.controller.orchestration.selfdev.*;
import eu.kalafatic.evolution.controller.trajectory.Trajectory;
import eu.kalafatic.evolution.model.orchestration.*;

public class CheckpointDeterminismTest {

    private File projectRoot;
    private Orchestrator orchestrator;

    @Before
    public void setUp() throws Exception {
        projectRoot = Files.createTempDirectory("checkpoint-test").toFile();
        new File(projectRoot, "iterations").mkdirs();
        orchestrator = OrchestrationFactory.eINSTANCE.createOrchestrator();
    }

    @Test
    public void testFullStateDeterminism() throws Exception {
        String sessionId = "test-session-" + System.currentTimeMillis();
        TaskContext context = new TaskContext(orchestrator, projectRoot);
        context.setSessionId(sessionId);

        SessionContainer session = SessionManager.getInstance().getOrCreateSession(sessionId);
        IterationManager manager = KernelFactory.create(context, session);
        context.getOrchestrationState().setCurrentPhase("INTENT_EXPANSION");

        // 1. Setup rich state
        EvolutionMemoryGraph graph = session.getEvolutionMemoryGraph();
        graph.recordDimension(new EvolutionDimension("dim1", "desc1", AbstractionLevel.ARCHITECTURE, SemanticDomain.STRUCTURE));
        graph.recordRejection("dim1", "v1", "test-rationale");
        graph.recordEntropy(0.42);
        graph.recordConvergenceReasoning("Converged due to stability");
        EvolutionaryPressureVector pressure = new EvolutionaryPressureVector();
        pressure.ambiguity = 0.8;
        graph.recordGlobalPressure(pressure);

        FailureMemory failure = session.getMemoryService(projectRoot).getFailureMemory();
        failure.addFingerprint("error1");
        failure.recordStrategyFailure("strategy1");
        failure.updateMutationEffectiveness("strategy1", 0.9);

        IterationRecord historyRecord = new IterationRecord();
        historyRecord.setBranchId("hist-1");
        historyRecord.setGoal("hist-goal");
        historyRecord.setResult("SUCCESS");
        session.getMemoryService(projectRoot).saveRecord(historyRecord);
        session.getMemoryService(projectRoot).getArchitectureHotspots().put("src/Debt.java", 5);

        context.getFileChangeTracker().recordChange("src/Test.java", FileChangeTracker.ChangeType.NEW);

        Trajectory traj = new Trajectory("traj1", "goal1");
        traj.setFitnessScore(0.75);
        traj.recordSignal("test-signal", 1.0);
        context.getSemanticWorkspace().getTrajectoryMemory().recordTrajectory(traj);

        // 2. Save Checkpoint
        // Use reflection to call private saveFullCheckpoint() or just call it if it was public.
        // In the provided code it is private. Let's see if we can trigger it or use a public way.
        // I will use reflection for the test.
        java.lang.reflect.Method saveMethod = IterationManager.class.getDeclaredMethod("saveFullCheckpoint");
        saveMethod.setAccessible(true);
        saveMethod.invoke(manager);

        // 3. Restore in a new manager
        TaskContext context2 = new TaskContext(orchestrator, projectRoot);
        context2.setSessionId(sessionId);
        // We need a fresh session container or clear the existing one to prove restoration works.
        // Since SessionManager stores them by ID, we should probably clear the session or use a different ID but same projectRoot.
        // Actually, IterationManager constructor calls restoreStateFromCheckpoint if checkpoint exists.

        SessionManager.getInstance().shutdownSession(sessionId);
        SessionContainer session2 = SessionManager.getInstance().getOrCreateSession(sessionId);
        IterationManager manager2 = KernelFactory.create(context2, session2);

        // 4. Assertions
        EvolutionMemoryGraph graph2 = session2.getEvolutionMemoryGraph();
        assertEquals(1, graph2.getDimensions().size());
        assertEquals("dim1", graph2.getDimensions().get(0).getId());
        assertTrue(graph2.getRejectedBranches().containsKey("dim1"));
        assertEquals("test-rationale", graph2.getRationales().get("v1"));
        assertEquals(0.42, graph2.getEntropyHistory().get(0), 0.001);
        assertEquals("Converged due to stability", graph2.getConvergenceReasoning().get(0));
        assertEquals(0.8, graph2.getGlobalPressureHistory().get(0).ambiguity, 0.001);

        FailureMemory failure2 = session2.getMemoryService(projectRoot).getFailureMemory();
        assertEquals(1, (int) failure2.getFingerprints().get("error1"));
        assertEquals(1, (int) failure2.getStrategyFailures().get("strategy1"));
        assertEquals(0.62, failure2.getMutationEffectiveness("strategy1"), 0.01); // (0.5 * 0.7) + (0.9 * 0.3) = 0.35 + 0.27 = 0.62

        IterationMemoryService mem2 = session2.getMemoryService(projectRoot);
        assertTrue(mem2.getRecords().stream().anyMatch(r -> "hist-1".equals(r.getBranchId())));
        assertEquals(5, (int) mem2.getArchitectureHotspots().get("src/Debt.java"));

        assertEquals(FileChangeTracker.ChangeType.NEW, context2.getFileChangeTracker().getChangeType("src/Test.java"));

        Trajectory traj2 = context2.getSemanticWorkspace().getTrajectoryMemory().getTrajectory("traj1");
        assertNotNull(traj2);
        assertEquals(0.75, traj2.getFitnessScore(), 0.001);
        assertEquals(1, traj2.getSignalHistory().size());
        assertEquals("test-signal", traj2.getSignalHistory().get(0).signalName);
    }
}
