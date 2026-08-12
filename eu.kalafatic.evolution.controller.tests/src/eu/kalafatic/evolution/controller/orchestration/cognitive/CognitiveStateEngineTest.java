package eu.kalafatic.evolution.controller.orchestration.cognitive;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import java.util.List;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import java.io.File;

public class CognitiveStateEngineTest {

    private CognitiveStateEngine engine;
    private SessionCognitiveState state;
    private TaskContext context;

    @Before
    public void setUp() {
        engine = new CognitiveStateEngine();
        state = new SessionCognitiveState();
        context = new TaskContext(OrchestrationFactory.eINSTANCE.createOrchestrator(), new File("."));
        context.setSessionId("test-session");
    }

    @Test
    public void testChatToCodeTransition() {
        // Initial state is CHAT
        assertEquals(CapabilityType.CHAT, state.getCurrentCapability());

        // Single code signal shouldn't transition immediately due to hysteresis
        engine.processInteraction("java", state, context, null);
        assertEquals(CapabilityType.CHAT, state.getCurrentCapability());

        // Multiple code signals should trigger transition
        engine.processInteraction("class code", state, context, null);
        engine.processInteraction("method code", state, context, null);

        assertEquals(CapabilityType.CODE, state.getCurrentCapability());
    }

    @Test
    public void testCodeToArchitectureTransition() {
        // Set state to CODE
        engine.processInteraction("class code", state, context, null);
        engine.processInteraction("method code", state, context, null);
        engine.processInteraction("compile java code", state, context, null);
        assertEquals(CapabilityType.CODE, state.getCurrentCapability());

        // Architecture signal
        for(int i=0; i<5; i++) engine.processInteraction("architecture repository module dependency graph structure component", state, context, null);

        assertEquals(CapabilityType.ARCHITECTURE, state.getCurrentCapability());
    }

    @Test
    public void testOscillationPrevention() {
        // High ARCHITECTURE score
        for(int i=0; i<10; i++) engine.processInteraction("architecture", state, context, null);
        assertEquals(CapabilityType.ARCHITECTURE, state.getCurrentCapability());

        // Single CHAT signal ("thanks") should NOT downgrade
        engine.processInteraction("thanks", state, context, null);
        assertEquals(CapabilityType.ARCHITECTURE, state.getCurrentCapability());

        // Even a few shouldn't
        engine.processInteraction("hi", state, context, null);
        engine.processInteraction("hello", state, context, null);
        assertEquals(CapabilityType.ARCHITECTURE, state.getCurrentCapability());
    }

    @Test
    public void testEvolutionEscalation() {
        engine.processInteraction("evolve system", state, context, null);
        engine.processInteraction("darwin iteration", state, context, null);
        engine.processInteraction("reality model", state, context, null);

        assertEquals(CapabilityType.EVOLUTION, state.getCurrentCapability());
    }

    @Test
    public void testTrajectoryDetection() {
        engine.processInteraction("hi", state, context, null);
        engine.processInteraction("java", state, context, null);
        engine.processInteraction("architecture", state, context, null);

        List<CapabilityType> trajectory = state.getTrajectory();
        assertTrue(trajectory.size() >= 3);
        assertEquals(CapabilityType.CHAT, trajectory.get(0));

        assertTrue(state.getVelocity() > 0);
        assertEquals(CognitiveDirection.ANALYZING, state.getCurrentDirection());
    }

    @Test
    public void testIntentClassification() {
        engine.processInteraction("Explain Spring Boot", state, context, null);
        assertEquals(SessionIntent.LEARNING, state.getCurrentIntent());

        engine.processInteraction("Fix this exception", state, context, null);
        assertEquals(SessionIntent.TROUBLESHOOTING, state.getCurrentIntent());

        engine.processInteraction("Analyze repository", state, context, null);
        assertEquals(SessionIntent.ANALYZING, state.getCurrentIntent());

        engine.processInteraction("Evolve darwin selection", state, context, null);
        assertEquals(SessionIntent.EVOLVING, state.getCurrentIntent());
    }

    @Test
    public void testForgeModeTransition() {
        // Explicitly set AiMode to FORGE
        context.getOrchestrator().setAiMode(eu.kalafatic.evolution.model.orchestration.AiMode.FORGE);

        // Processing 2 messages ensures history.size() >= 2 to transition direction from STABLE to EVOLVING
        engine.processInteraction("hello", state, context, null);
        engine.processInteraction("world", state, context, null);
        assertEquals(CapabilityType.FORGE, state.getCurrentCapability());
        assertEquals(CognitiveDirection.EVOLVING, state.getCurrentDirection());
    }

    @Test
    public void testCognitiveDepth() {
        assertEquals(1, state.getCognitiveDepth());

        engine.processInteraction("hi", state, context, null);
        assertEquals(1, state.getCognitiveDepth());

        // Escalate to ARCHITECTURE
        for (int i = 0; i < 5; i++) engine.processInteraction("architecture repository", state, context, null);
        assertEquals(CapabilityType.ARCHITECTURE, state.getCurrentCapability());

        // Depth should increase towards target (5 for Architecture)
        assertTrue(state.getCognitiveDepth() > 1);

        // Escalate to EVOLUTION
        for (int i = 0; i < 10; i++) engine.processInteraction("evolve darwin iteration", state, context, null);
        assertEquals(CapabilityType.EVOLUTION, state.getCurrentCapability());
        assertTrue(state.getCognitiveDepth() > 5);
    }
}
