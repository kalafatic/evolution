package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import eu.kalafatic.evolution.controller.memory.MemoryEntry;
import eu.kalafatic.evolution.controller.memory.MemoryImportance;
import eu.kalafatic.evolution.controller.memory.MemoryScope;
import eu.kalafatic.evolution.controller.memory.MemoryService;
import eu.kalafatic.evolution.controller.memory.MemorySource;
import eu.kalafatic.evolution.controller.memory.MemoryType;
import eu.kalafatic.evolution.controller.orchestration.dream.DreamContextRetriever;
import eu.kalafatic.evolution.controller.orchestration.dream.DreamExperienceCandidate;
import eu.kalafatic.evolution.controller.orchestration.dream.DreamTopicSelector;
import eu.kalafatic.evolution.controller.orchestration.selfdev.FailureMemory;
import eu.kalafatic.evolution.controller.orchestration.selfdev.IterationMemoryService;
import eu.kalafatic.evolution.controller.orchestration.selfdev.IterationRecord;

public class DreamExperienceSelectionAndContextTest {

    @Before
    public void setUp() {
        DreamTopicSelector.clearCandidateCooldowns();
        MemoryService.getInstance().clearAll();
    }

    @Test
    public void testCandidateSelectionFromMultipleSourcesAndThreshold() {
        FailureMemory fm = new FailureMemory();
        fm.addFingerprint("NullPointerException in SupervisorClient");
        fm.addFingerprint("NullPointerException in SupervisorClient");

        DreamTopicSelector selector = new DreamTopicSelector();
        DreamTopicSelector.SelectedTopic top = selector.selectBestTopic(fm, null);

        assertNotNull("Severe recurring failure candidate should be selected", top);
        assertTrue(top.getTopic().contains("NullPointerException in SupervisorClient"));
        assertTrue(top.getScore() >= DreamTopicSelector.MINIMUM_SIGNIFICANCE_THRESHOLD);
        assertNotNull(top.getCandidate());
        assertEquals("FailureMemory", top.getCandidate().getSource());
    }

    @Test
    public void testIdleStateWhenNoCandidateExceedsThreshold() {
        FailureMemory fm = new FailureMemory(); // empty
        DreamTopicSelector selector = new DreamTopicSelector();

        DreamTopicSelector.SelectedTopic top = selector.selectBestTopic(fm, null);
        assertNull("Selector should return null (idle) when no candidate meets significance threshold", top);
    }

    @Test
    public void testCandidateCooldownAndDeferral() {
        FailureMemory fm = new FailureMemory();
        fm.addFingerprint("StackOverflowError in CognitiveLoopEngine");

        DreamTopicSelector selector = new DreamTopicSelector();
        DreamTopicSelector.SelectedTopic first = selector.selectBestTopic(fm, null);

        assertNotNull(first);
        assertNotNull(first.getCandidate());

        // Record candidate as analyzed
        DreamTopicSelector.recordCandidateAnalyzed(first.getCandidate().getCandidateId());

        // Select again immediately - score should be penalized due to cooldown
        List<DreamExperienceCandidate> candidates = selector.discoverCandidates(fm, null);
        assertFalse(candidates.isEmpty());
        DreamExperienceCandidate candidateAfterCooldown = candidates.get(0);

        assertTrue("Rationale should indicate candidate is in cooldown", candidateAfterCooldown.getRationale().contains("cooldown"));
        assertTrue("Score in cooldown should be lower than initial score", candidateAfterCooldown.getScore() < first.getScore());
    }

    @Test
    public void testAssociativeContextRetrieval() {
        FailureMemory fm = new FailureMemory();
        fm.addFingerprint("Timeout in OllamaProvider connection");

        MemoryService.getInstance().addEntry(
            "OllamaProvider timeout occurs when local GPU memory is saturated",
            MemoryScope.PROJECT, MemoryType.FACT, MemorySource.SYSTEM, MemoryImportance.CRITICAL
        );

        DreamContextRetriever retriever = new DreamContextRetriever();
        DreamContextRetriever.RetrievedDreamContext context = retriever.retrieveContext("OllamaProvider timeout", null, fm, null);

        assertNotNull(context);
        assertFalse("Observed facts should not be empty", context.getObservedFacts().isEmpty());
        assertFalse("Supporting evidence should contain FailureMemory record", context.getSupportingEvidence().isEmpty());
        assertTrue("Evidence references should track retrieved sources", context.getEvidenceReferences().contains("FailureMemory:Timeout in OllamaProvider connection"));
    }

    @Test
    public void testManualTopicSeedPreservedInContext() {
        DreamContextRetriever retriever = new DreamContextRetriever();
        String userTopic = "Investigate custom dataset preparation memory leak";

        DreamContextRetriever.RetrievedDreamContext context = retriever.retrieveContext(userTopic, null, null, null);

        assertNotNull(context);
        assertEquals("Topic Focus Seed: " + userTopic, context.getObservedFacts().get(0));
        assertFalse("Open questions should be populated for user topic", context.getOpenQuestions().isEmpty());
    }
}
