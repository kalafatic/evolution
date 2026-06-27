package eu.kalafatic.evolution.controller.tests;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;
import eu.kalafatic.evolution.controller.orchestration.SessionManager;

import static org.junit.Assert.*;
import org.junit.Test;
import eu.kalafatic.evolution.controller.orchestration.intent.ClarificationPlanner;
import eu.kalafatic.evolution.controller.orchestration.intent.IntentExpansionResult;
import eu.kalafatic.evolution.controller.orchestration.intent.InterpretationState;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.OrchestrationState;
import eu.kalafatic.evolution.controller.orchestration.intent.AtomicIntentAnalysis;
import java.util.HashMap;

public class IntentResolutionTest {

    @Test
    public void testClearIntent() {
        IntentExpansionResult result = new IntentExpansionResult();
        result.setState(InterpretationState.CLEAR);

        ClarificationPlanner planner = new ClarificationPlanner();
        ClarificationPlanner.Strategy strategy = planner.determineStrategy(result, createMockContext());

        assertEquals(ClarificationPlanner.Strategy.AUTO_INFER, strategy);
    }

    @Test
    public void testEvolvableIntent() {
        IntentExpansionResult result = new IntentExpansionResult();
        result.setState(InterpretationState.EVOLVABLE);
        result.getImplementationStrategies().add("Strategy 1");
        result.getImplementationStrategies().add("Strategy 2");

        ClarificationPlanner planner = new ClarificationPlanner();
        ClarificationPlanner.Strategy strategy = planner.determineStrategy(result, createMockContext());

        assertEquals(ClarificationPlanner.Strategy.BRANCH_PARALLEL, strategy);
    }

    @Test
    public void testNeedsClarificationHighRisk() {
        IntentExpansionResult result = new IntentExpansionResult();
        result.setState(InterpretationState.NEEDS_CLARIFICATION);
        result.setDominantConfidence(0.5);
        result.setExecutionRiskScore(0.8);

        ClarificationPlanner planner = new ClarificationPlanner();
        ClarificationPlanner.Strategy strategy = planner.determineStrategy(result, createMockContext());

        assertEquals(ClarificationPlanner.Strategy.CLARIFY_USER, strategy);
    }

    @Test
    public void testNeedsClarificationLowRisk() {
        IntentExpansionResult result = new IntentExpansionResult();
        result.setState(InterpretationState.NEEDS_CLARIFICATION);
        result.setDominantConfidence(0.8);
        result.setExecutionRiskScore(0.3);

        ClarificationPlanner planner = new ClarificationPlanner();
        ClarificationPlanner.Strategy strategy = planner.determineStrategy(result, createMockContext());

        // Should fall back to BRANCH_PARALLEL if risk is low and confidence is high
        assertEquals(ClarificationPlanner.Strategy.BRANCH_PARALLEL, strategy);
    }

    @Test
    public void testBlocked() {
        IntentExpansionResult result = new IntentExpansionResult();
        result.setState(InterpretationState.BLOCKED);
        result.setDominantConfidence(0.4);

        ClarificationPlanner planner = new ClarificationPlanner();
        ClarificationPlanner.Strategy strategy = planner.determineStrategy(result, createMockContext());

        assertEquals(ClarificationPlanner.Strategy.CLARIFY_USER, strategy);
    }

    @Test
    public void testContradictory() {
        IntentExpansionResult result = new IntentExpansionResult();
        result.setState(InterpretationState.CONTRADICTORY);
        result.setDominantConfidence(0.3);

        ClarificationPlanner planner = new ClarificationPlanner();
        ClarificationPlanner.Strategy strategy = planner.determineStrategy(result, createMockContext());

        assertEquals(ClarificationPlanner.Strategy.CLARIFY_USER, strategy);
    }

    @Test
    public void testAtomicOverride() {
        IntentExpansionResult result = new IntentExpansionResult();
        result.setState(InterpretationState.NEEDS_CLARIFICATION);

        TaskContext context = createMockContext();
        AtomicIntentAnalysis atomic = new AtomicIntentAnalysis();
        atomic.setAtomic(true);
        atomic.setConfidence(0.9);
        atomic.setMultiStep(false);
        context.getOrchestrationState().getMetadata().put("atomicAnalysis", atomic);

        ClarificationPlanner planner = new ClarificationPlanner();
        ClarificationPlanner.Strategy strategy = planner.determineStrategy(result, context);

        // High atomic confidence should override NEEDS_CLARIFICATION
        assertEquals(ClarificationPlanner.Strategy.AUTO_INFER, strategy);
    }

    private TaskContext createMockContext() {
        eu.kalafatic.evolution.model.orchestration.Orchestrator orchestrator = eu.kalafatic.evolution.model.orchestration.OrchestrationFactory.eINSTANCE.createOrchestrator();
        TaskContext context = new TaskContext(orchestrator, null);
        // OrchestrationState is already initialized in TaskContext constructor
        return context;
    }
}
