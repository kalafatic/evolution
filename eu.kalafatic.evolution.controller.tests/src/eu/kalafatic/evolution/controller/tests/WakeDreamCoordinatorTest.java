package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import eu.kalafatic.evolution.controller.orchestration.PlatformType;
import eu.kalafatic.evolution.controller.orchestration.dream.DreamCheckpoint;
import eu.kalafatic.evolution.controller.orchestration.dream.WakeDreamCoordinator;
import eu.kalafatic.evolution.controller.orchestration.dream.WakeDreamState;

public class WakeDreamCoordinatorTest {

    private WakeDreamCoordinator coordinator;

    @Before
    public void setUp() {
        coordinator = WakeDreamCoordinator.getInstance();
        coordinator.resetState();
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
    public void testDreamCheckpointSerialization() {
        DreamCheckpoint cp = new DreamCheckpoint("cycle-test");
        cp.setStage("GENERATE_HYPOTHESES");
        cp.getObservedFacts().add("Fact 1");
        cp.getGeneratedHypotheses().add("Hypothesis 1");

        org.json.JSONObject json = cp.toJsonObject();
        assertNotNull(json);

        DreamCheckpoint restored = DreamCheckpoint.fromJsonObject(json);
        assertNotNull(restored);
        assertEquals("cycle-test", restored.getCycleId());
        assertEquals("GENERATE_HYPOTHESES", restored.getStage());
        assertEquals(1, restored.getObservedFacts().size());
        assertEquals("Fact 1", restored.getObservedFacts().get(0));
        assertEquals(1, restored.getGeneratedHypotheses().size());
        assertEquals("Hypothesis 1", restored.getGeneratedHypotheses().get(0));
    }

    @Test
    public void testPlatformTypeDreamMode() {
        assertNotNull(PlatformType.DREAM_MODE);
        assertEquals("DREAM_MODE", PlatformType.DREAM_MODE.name());
    }
}
