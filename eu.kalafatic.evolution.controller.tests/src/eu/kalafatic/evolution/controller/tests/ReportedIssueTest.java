package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.*;
import org.junit.Test;
import eu.kalafatic.evolution.controller.orchestration.intent.AtomicIntentAnalysis;
import eu.kalafatic.evolution.controller.orchestration.intent.HybridAtomicIntentClassifier;
import eu.kalafatic.evolution.controller.orchestration.IterationManager;

public class ReportedIssueTest {

    @Test
    public void testReportedPrompt() {
        String request = "create java class which can print text";
        AtomicIntentAnalysis analysis = HybridAtomicIntentClassifier.heuristicAnalyze(request);

        // Confidence should be high enough despite the "which" stop word
        assertTrue("Confidence should be >= 0.80 for: " + request + " (Score: " + analysis.getConfidence() + ")",
                   analysis.getConfidence() >= 0.80);

        // It should NOT require planning (it should bypass Darwin)
        assertFalse("Should NOT require planning for: " + request, analysis.isRequiresPlanning());

    }

    @Test
    public void testGenericClassCreation() {
        String request = "create a java class";
        AtomicIntentAnalysis analysis = HybridAtomicIntentClassifier.heuristicAnalyze(request);

        // Even without a name, simple creation of high confidence
        assertTrue("Confidence should be high for: " + request + " (Score: " + analysis.getConfidence() + ")",
                   analysis.getConfidence() >= 0.80);
        assertFalse("Should NOT require planning for: " + request, analysis.isRequiresPlanning());
    }
}
