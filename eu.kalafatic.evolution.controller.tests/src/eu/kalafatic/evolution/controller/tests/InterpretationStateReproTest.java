package eu.kalafatic.evolution.controller.tests;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;
import eu.kalafatic.evolution.controller.orchestration.SessionManager;

import static org.junit.Assert.*;
import org.junit.Test;
import eu.kalafatic.evolution.controller.orchestration.intent.InterpretationState;
import eu.kalafatic.evolution.controller.orchestration.intent.IntentExpansionEngine;

public class InterpretationStateReproTest {

    @Test
    public void testReproIssueFixed() {
        // This is what happened in IntentExpansionEngine
        String stateStr = "CLEAR | NEEDS_CLARIFICATION | BLOCKED | CONTRADICTORY";

        // Now it should resolve to CLEAR instead of throwing exception
        InterpretationState state = IntentExpansionEngine.parseState(stateStr, null);
        assertEquals(InterpretationState.CLEAR, state);
    }

    @Test
    public void testNoisyBlocked() {
        String stateStr = "BLOCKED | CONTRADICTORY";
        InterpretationState state = IntentExpansionEngine.parseState(stateStr, null);
        assertEquals(InterpretationState.BLOCKED, state);
    }

    @Test
    public void testExactMatch() {
        String stateStr = "NEEDS_CLARIFICATION";
        InterpretationState state = IntentExpansionEngine.parseState(stateStr, null);
        assertEquals(InterpretationState.NEEDS_CLARIFICATION, state);
    }
}
