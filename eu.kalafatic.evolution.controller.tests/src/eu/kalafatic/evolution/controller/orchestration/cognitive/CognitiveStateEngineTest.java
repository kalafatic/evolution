package eu.kalafatic.evolution.controller.orchestration.cognitive;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import eu.kalafatic.evolution.controller.orchestration.ContextAssistResult;
import eu.kalafatic.evolution.controller.orchestration.PlatformType;
import eu.kalafatic.evolution.controller.orchestration.PlatformMode;
import eu.kalafatic.evolution.controller.orchestration.AutonomyLevel;

public class CognitiveStateEngineTest {

    private CognitiveStateEngine engine;
    private SessionCognitiveState state;

    @Before
    public void setUp() {
        engine = new CognitiveStateEngine();
        state = new SessionCognitiveState();
    }

    @Test
    public void testChatToCodeTransition() {
        // Initial state is CHAT
        assertEquals(CapabilityType.CHAT, state.getCurrentCapability());

        // Send a coding message
        engine.processInteraction("create a new java class", state, null);

        // Should transition to CODE (weight 3.0, exceeds hysteresis if multiple or high enough,
        // but here it might need more signals depending on HYSTERESIS_THRESHOLD=5.0)
        // Let's send multiple signals to be sure
        engine.processInteraction("fix the bug in the method", state, null);

        assertEquals(CapabilityType.CODE, state.getCurrentCapability());
    }

    @Test
    public void testCodeToArchitectureTransition() {
        state.setCurrentCapability(CapabilityType.CODE);

        engine.processInteraction("analyze the repository architecture", state, null);
        engine.processInteraction("show me the subsystem graph", state, null);

        assertEquals(CapabilityType.ARCHITECTURE, state.getCurrentCapability());
    }

    @Test
    public void testOscillationPrevention() {
        state.setCurrentCapability(CapabilityType.ARCHITECTURE);
        // Set high score for ARCHITECTURE
        state.getCapabilityScores().put(CapabilityType.ARCHITECTURE, 20.0);

        // Single "thanks" should not downgrade to CHAT due to hysteresis (threshold 5.0)
        engine.processInteraction("thanks", state, null);

        assertEquals(CapabilityType.ARCHITECTURE, state.getCurrentCapability());
    }

    @Test
    public void testEvolutionEscalation() {
        engine.processInteraction("evolve the kernel using darwinflow", state, null);
        assertEquals(CapabilityType.EVOLUTION, state.getCurrentCapability());
    }

    @Test
    public void testTrajectoryDetection() {
        engine.processInteraction("analyze architecture", state, null);
        engine.processInteraction("show subsystems", state, null);
        engine.processInteraction("module dependencies", state, null);

        assertTrue(state.getTrajectory().contains(CapabilityType.ARCHITECTURE));
        assertEquals(CognitiveDirection.ANALYZING, state.getCurrentDirection());
    }
}
