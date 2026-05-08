package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.*;
import org.junit.Test;
import eu.kalafatic.evolution.controller.orchestration.intent.AtomicIntentAnalysis;
import eu.kalafatic.evolution.controller.orchestration.intent.HybridAtomicIntentClassifier;

public class AtomicIntentClassifierTest {

    @Test
    public void testHeuristicAtomicExamples() {
        assertAtomic("create UserService class");
        assertAtomic("generate LoginController");
        assertAtomic("add Customer entity");
        assertAtomic("write README.md");
        assertAtomic("create docker-compose.yml");
        assertAtomic("add enum StatusType");
    }

    @Test
    public void testHeuristicNonAtomicExamples() {
        assertNotAtomic("redesign authentication architecture");
        assertNotAtomic("optimize persistence layer");
        assertNotAtomic("improve project structure");
        assertNotAtomic("analyze workflow problems");
        assertNotAtomic("refactor entire service layer");
        assertNotAtomic("create java class Test and add many methods");
    }

    private void assertAtomic(String request) {
        AtomicIntentAnalysis analysis = HybridAtomicIntentClassifier.heuristicAnalyze(request);
        assertTrue("Request should be atomic: " + request + " (Signals: " + analysis.getSignals() + ", Confidence: " + analysis.getConfidence() + ")",
                   analysis.isAtomic() && analysis.getConfidence() > 0.6 && !analysis.isRequiresPlanning());
    }

    private void assertNotAtomic(String request) {
        AtomicIntentAnalysis analysis = HybridAtomicIntentClassifier.heuristicAnalyze(request);
        assertFalse("Request should NOT be atomic: " + request + " (Signals: " + analysis.getSignals() + ", Confidence: " + analysis.getConfidence() + ")",
                    analysis.isAtomic() && analysis.getConfidence() > 0.8 && !analysis.isRequiresPlanning());
    }
}
