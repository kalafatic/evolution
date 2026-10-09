package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import eu.kalafatic.evolution.controller.memory.MemoryEntry;
import eu.kalafatic.evolution.controller.memory.MemoryImportance;
import eu.kalafatic.evolution.controller.memory.MemoryScope;
import eu.kalafatic.evolution.controller.memory.MemoryService;
import eu.kalafatic.evolution.controller.memory.MemorySource;
import eu.kalafatic.evolution.controller.memory.MemoryType;
import eu.kalafatic.evolution.controller.orchestration.ContextProcessTracker;
import eu.kalafatic.evolution.controller.orchestration.ContextSnapshot;
import eu.kalafatic.evolution.controller.orchestration.ContextualHelpService;
import eu.kalafatic.evolution.controller.orchestration.ContextualHelpService.ContextualHelpResult;
import eu.kalafatic.evolution.controller.workflow.RuntimeEvent;
import eu.kalafatic.evolution.controller.workflow.RuntimeEventBus;
import eu.kalafatic.evolution.controller.workflow.RuntimeEventType;

public class ContextualHelpServiceTest {

    private ContextualHelpService service;
    private ContextProcessTracker tracker;

    @Before
    public void setUp() {
        service = ContextualHelpService.getInstance();
        tracker = ContextProcessTracker.getInstance();
        tracker.clearAll();
    }

    @After
    public void tearDown() {
        tracker.clearAll();
    }

    @Test
    public void testSnapshotResolutionWithEmptySelection() {
        ContextSnapshot snapshot = service.resolveSnapshot("", "", "", "", "TestProject", "", "test-session-1");
        assertNotNull(snapshot);
        assertFalse(snapshot.hasSelection());
        assertFalse(snapshot.hasActiveProcess());
        assertEquals("test-session-1", snapshot.getOwningSessionId());
    }

    @Test
    public void testSnapshotResolutionWithFileSelection() {
        ContextSnapshot snapshot = service.resolveSnapshot(
                "MultiPageEditor",
                "OrchestrationService.java",
                "IFile",
                "eu.kalafatic.evolution.controller/src/OrchestrationService.java",
                "TestProject",
                "",
                "test-session-2"
        );

        assertNotNull(snapshot);
        assertTrue(snapshot.hasSelection());
        assertEquals("OrchestrationService.java", snapshot.getSelectedObjectName());
        assertEquals("IFile", snapshot.getSelectedObjectType());
        assertEquals("MultiPageEditor", snapshot.getActivePartName());
    }

    @Test
    public void testProcessEventTrackingAndCorrelation() {
        RuntimeEventBus bus = new RuntimeEventBus("session-corr-1");
        tracker.registerSessionBus(bus);

        RuntimeEvent startEvent = new RuntimeEvent(
                RuntimeEventType.TASK_STARTED,
                "session-corr-1",
                "test-source",
                "Refactor OrchestrationService"
        );
        startEvent.getMetadata().put("resource", "OrchestrationService.java");
        bus.publish(startEvent);

        ContextProcessTracker.ProcessInfo info = tracker.getProcessState("session-corr-1");
        assertNotNull(info);
        assertEquals("RUNNING", info.getState());

        ContextSnapshot snapshot = service.resolveSnapshot(
                "MultiPageEditor",
                "OrchestrationService.java",
                "IFile",
                "src/OrchestrationService.java",
                "TestProject",
                "",
                "session-corr-1"
        );

        assertTrue(snapshot.hasActiveProcess());
        assertTrue(snapshot.isCorrelatedWithSelection());

        tracker.unregisterSessionBus(bus);
        bus.shutdown();
    }

    @Test
    public void testUnrelatedProcessCorrelation() {
        RuntimeEventBus bus = new RuntimeEventBus("session-unrelated-1");
        tracker.registerSessionBus(bus);

        RuntimeEvent startEvent = new RuntimeEvent(
                RuntimeEventType.FORGE_TRAINING_STARTED,
                "session-unrelated-1",
                "forge-trainer",
                "Training Model Llama-3"
        );
        bus.publish(startEvent);

        ContextSnapshot snapshot = service.resolveSnapshot(
                "EvoNavigator",
                "pom.xml",
                "IFile",
                "pom.xml",
                "TestProject",
                "",
                "session-unrelated-1"
        );

        assertTrue(snapshot.hasActiveProcess());
        assertFalse(snapshot.isCorrelatedWithSelection());

        tracker.unregisterSessionBus(bus);
        bus.shutdown();
    }

    @Test
    public void testMemoryRetrievalScoping() {
        MemoryEntry entry = new MemoryEntry(
                "Always check EMF resource save invariants in MultiPageEditor.",
                MemoryScope.PROJECT,
                MemoryType.RULE,
                MemorySource.USER,
                MemoryImportance.HIGH
        );
        MemoryService.getInstance().addEntry(entry);

        ContextSnapshot snapshot = service.resolveSnapshot(
                "MultiPageEditor",
                "MultiPageEditor.java",
                "IFile",
                "src/MultiPageEditor.java",
                "TestProject",
                "",
                "test-session-mem"
        );

        assertNotNull(snapshot.getRetrievedMemories());
        assertFalse(snapshot.getRetrievedMemories().isEmpty());
        assertTrue(snapshot.getRetrievedMemories().stream()
                .anyMatch(m -> m.getContent().contains("MultiPageEditor")));

        MemoryService.getInstance().deleteEntry(entry.getId());
    }

    @Test
    public void testThreeLevelAssistanceGeneration() {
        ContextSnapshot snapshot = service.resolveSnapshot(
                "MultiPageEditor",
                "ContextualHelpService.java",
                "IFile",
                "src/ContextualHelpService.java",
                "TestProject",
                "",
                "test-session-3lvl"
        );

        ContextualHelpResult result = service.generateAssistance(snapshot);
        assertNotNull(result);
        assertNotNull(result.getSelectionHelp());
        assertNotNull(result.getProcessAwareness());
        assertNotNull(result.getCognitiveAssistance());

        assertTrue(result.getSelectionHelp().contains("ContextualHelpService.java"));
        assertTrue(result.getProcessAwareness().contains("Platform Idle"));
        assertTrue(result.getCognitiveAssistance().contains("Observation"));
    }

    @Test
    public void testFailureDiagnosticAssistance() {
        RuntimeEventBus bus = new RuntimeEventBus("session-failed-1");
        tracker.registerSessionBus(bus);

        RuntimeEvent failEvent = new RuntimeEvent(
                RuntimeEventType.TASK_FAILED,
                "session-failed-1",
                "MavenTool",
                "Compilation error: Symbol not found in class ContextualHelpService"
        );
        bus.publish(failEvent);

        ContextSnapshot snapshot = service.resolveSnapshot(
                "DevelopmentPage",
                "BuildTask",
                "Task",
                "self-dev/run_1/build.log",
                "TestProject",
                "",
                "session-failed-1"
        );

        ContextualHelpResult result = service.generateAssistance(snapshot);
        assertNotNull(result);
        assertTrue(result.getCognitiveAssistance().contains("experienced a failure"));
        assertTrue(result.getCognitiveAssistance().contains("Compilation error"));
        assertTrue(result.getRecommendedActions().contains("Inspect Task Logs"));

        tracker.unregisterSessionBus(bus);
        bus.shutdown();
    }

    @Test
    public void testDebounceAndCancellation() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<ContextualHelpResult> resultRef = new AtomicReference<>();

        // Fire 3 rapid requests; the first 2 should be superseded/cancelled by the 3rd
        service.requestAssistanceDebounced("Part1", "Object1", "Type1", "", "Proj", "", "session-deb-1", 100, res -> {});
        service.requestAssistanceDebounced("Part2", "Object2", "Type2", "", "Proj", "", "session-deb-1", 100, res -> {});
        service.requestAssistanceDebounced("Part3", "Object3", "Type3", "", "Proj", "", "session-deb-1", 100, res -> {
            resultRef.set(res);
            latch.countDown();
        });

        boolean completed = latch.await(2, TimeUnit.SECONDS);
        assertTrue(completed);
        assertNotNull(resultRef.get());
        assertEquals("Object3", resultRef.get().getSnapshot().getSelectedObjectName());
    }
}
