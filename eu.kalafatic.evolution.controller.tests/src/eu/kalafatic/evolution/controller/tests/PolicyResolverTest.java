package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import eu.kalafatic.evolution.controller.orchestration.behavior.BitState;
import eu.kalafatic.evolution.controller.orchestration.behavior.ExecutionPolicy;
import eu.kalafatic.evolution.controller.orchestration.behavior.PolicyResolver;

public class PolicyResolverTest {

    @Test
    public void testMediatedDarwinRule() {
        PolicyResolver resolver = new PolicyResolver();

        // Encode MEDIATED mode + DARWIN reasoning
        long state = BitState.encode(BitState.MODE_MEDIATED, BitState.SUPERVISION_AUTO, BitState.INTERACTION_CONTINUOUS, BitState.REASONING_DARWIN);

        ExecutionPolicy policy = resolver.resolve(state);

        assertEquals(ExecutionPolicy.ExecutionMode.MEDIATED, policy.getExecutionMode());
        assertEquals(ExecutionPolicy.ReasoningStrategy.DARWIN, policy.getReasoningStrategy());

        // Rule should have changed supervision to HYBRID and interaction to STEP
        assertEquals(ExecutionPolicy.SupervisionLevel.HYBRID, policy.getSupervisionLevel());
        assertEquals(ExecutionPolicy.InteractionMode.STEP, policy.getInteractionMode());
        assertTrue(policy.getConstraints().stream().anyMatch(c -> c.contains("MEDIATED_DARWIN_GUARD")));
    }

    @Test
    public void testExploratoryRule() {
        PolicyResolver resolver = new PolicyResolver();

        long state = BitState.encode(BitState.MODE_LOCAL, BitState.SUPERVISION_AUTO, BitState.INTERACTION_CONTINUOUS, BitState.REASONING_EXPLORATORY);

        ExecutionPolicy policy = resolver.resolve(state);

        assertEquals(0.9, policy.getExplorationLevel(), 0.001);
    }

    @Test
    public void testConservativeRule() {
        PolicyResolver resolver = new PolicyResolver();

        long state = BitState.encode(BitState.MODE_LOCAL, BitState.SUPERVISION_AUTO, BitState.INTERACTION_CONTINUOUS, BitState.REASONING_CONSERVATIVE);

        ExecutionPolicy policy = resolver.resolve(state);

        assertEquals(0.2, policy.getExplorationLevel(), 0.001);
        assertEquals(ExecutionPolicy.InteractionMode.STEP, policy.getInteractionMode());
        assertTrue(policy.getConstraints().stream().anyMatch(c -> c.contains("CONSERVATIVE_SAFETY")));
    }

    @Test
    public void testNormalizationMediated() {
        PolicyResolver resolver = new PolicyResolver();

        // MEDIATED mode + ATOMIC reasoning (no specific rule in registry, but normalization should trigger)
        long state = BitState.encode(BitState.MODE_MEDIATED, BitState.SUPERVISION_AUTO, BitState.INTERACTION_CONTINUOUS, BitState.REASONING_ATOMIC);

        ExecutionPolicy policy = resolver.resolve(state);

        assertEquals(ExecutionPolicy.ExecutionMode.MEDIATED, policy.getExecutionMode());
        // Normalization should force MANUAL supervision if AUTO was provided
        assertEquals(ExecutionPolicy.SupervisionLevel.MANUAL, policy.getSupervisionLevel());
    }
}
