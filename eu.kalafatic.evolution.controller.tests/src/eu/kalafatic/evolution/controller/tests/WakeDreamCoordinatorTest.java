package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import eu.kalafatic.evolution.controller.orchestration.PlatformType;
import eu.kalafatic.evolution.controller.orchestration.dream.DreamCheckpoint;
import eu.kalafatic.evolution.controller.orchestration.dream.DreamCheckpointStore;
import eu.kalafatic.evolution.controller.orchestration.dream.DreamOrigin;
import eu.kalafatic.evolution.controller.orchestration.dream.DreamTopicSelector;
import eu.kalafatic.evolution.controller.orchestration.dream.WakeDreamCoordinator;
import eu.kalafatic.evolution.controller.orchestration.dream.WakeDreamState;
import eu.kalafatic.evolution.controller.orchestration.selfdev.FailureMemory;

public class WakeDreamCoordinatorTest {

    private WakeDreamCoordinator coordinator;

    @Before
    public void setUp() {
        coordinator = WakeDreamCoordinator.getInstance();
        coordinator.resetState();
        DreamCheckpointStore.clearCheckpoint();
    }

    @Test
    public void testDefaultStateAndEnableToggle() {
        assertNotNull(coordinator.getCurrentState());
        assertFalse(coordinator.isDreamEnabled());

        coordinator.setDreamEnabled(true);
        assertTrue(coordinator.isDreamEnabled());
        assertTrue(coordinator.evaluateDreamEligibility());

        coordinator.setDreamEnabled(false);
        assertFalse(coordinator.isDreamEnabled());
        assertFalse(coordinator.evaluateDreamEligibility());
    }

    @Test
    public void testWakePreemptsDream() {
        coordinator.setDreamEnabled(true);
        assertTrue(coordinator.evaluateDreamEligibility());

        boolean started = coordinator.requestDreamStart("cycle-1");
        assertTrue(started);
        assertEquals(WakeDreamState.DREAM_ACTIVE, coordinator.getCurrentState());

        coordinator.notifyWakeStart("user-request-1");
        assertEquals(WakeDreamState.DREAM_PAUSING, coordinator.getCurrentState());
        assertFalse(coordinator.evaluateDreamEligibility());

        coordinator.confirmDreamPaused("cycle-1");
        assertEquals(WakeDreamState.DREAM_PAUSED, coordinator.getCurrentState());

        coordinator.notifyWakeEnd("user-request-1");
        assertEquals(WakeDreamState.WAKE_IDLE, coordinator.getCurrentState());
        assertTrue(coordinator.evaluateDreamEligibility());
    }

    @Test
    public void testDreamTopicSelectionAndOrigin() {
        FailureMemory fm = new FailureMemory();
        fm.addFingerprint("Build error in TychoEvoRcpBuilder");
        fm.addFingerprint("Build error in TychoEvoRcpBuilder");

        DreamTopicSelector selector = new DreamTopicSelector();
        DreamTopicSelector.SelectedTopic topic = selector.selectBestTopic(fm);

        assertNotNull(topic);
        assertTrue(topic.getTopic().contains("Build error in TychoEvoRcpBuilder"));
        assertTrue(topic.getRationale().contains("2 times"));

        DreamCheckpoint cp = new DreamCheckpoint("cycle-manual");
        cp.setOrigin(DreamOrigin.MANUAL);
        cp.setTopic("Analyze RCP build failure");

        assertEquals(DreamOrigin.MANUAL, cp.getOrigin());
        assertEquals("Analyze RCP build failure", cp.getTopic());
    }

    @Test
    public void testElasticBranchManagement() {
        assertEquals(1, coordinator.getMaxAllowedBranches());
        coordinator.setMaxAllowedBranches(3);
        assertEquals(3, coordinator.getMaxAllowedBranches());

        coordinator.setDreamEnabled(true);
        assertTrue(coordinator.canSpawnBranch());
    }

    @Test
    public void testDreamCheckpointPersistenceStore() {
        DreamCheckpoint cp = new DreamCheckpoint("cycle-persistence-test");
        cp.setOrigin(DreamOrigin.AUTOMATIC);
        cp.setTopic("Test Topic");
        cp.setStage("ANALYZE_RELATIONSHIPS");
        cp.getObservedFacts().add("Fact A");

        DreamCheckpointStore.saveCheckpoint(cp);

        DreamCheckpoint restored = DreamCheckpointStore.loadCheckpoint();
        assertNotNull(restored);
        assertEquals("cycle-persistence-test", restored.getCycleId());
        assertEquals(DreamOrigin.AUTOMATIC, restored.getOrigin());
        assertEquals("Test Topic", restored.getTopic());
        assertEquals("ANALYZE_RELATIONSHIPS", restored.getStage());
        assertEquals(1, restored.getObservedFacts().size());

        DreamCheckpointStore.clearCheckpoint();
    }

    @Test
    public void testPlatformTypeDreamMode() {
        assertNotNull(PlatformType.DREAM_MODE);
        assertEquals("DREAM_MODE", PlatformType.DREAM_MODE.name());
    }
}
