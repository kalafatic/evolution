package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.ConversationState;
import eu.kalafatic.evolution.controller.orchestration.intent.ClarificationManager;
import eu.kalafatic.evolution.controller.orchestration.intent.IntentAnalysisResult;
import eu.kalafatic.evolution.controller.orchestration.intent.MissingRequirement;
import eu.kalafatic.evolution.controller.orchestration.intent.Ambiguity;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import java.io.File;
import java.nio.file.Files;

public class ClarificationManagerTest {

    private ClarificationManager manager;
    private IntentAnalysisResult result;
    private TaskContext context;
    private Orchestrator orchestrator;

    @Before
    public void setUp() throws Exception {
        manager = new ClarificationManager();
        result = new IntentAnalysisResult();
        orchestrator = OrchestrationFactory.eINSTANCE.createOrchestrator();
        context = new TaskContext(orchestrator, new File("."));
    }

    @Test
    public void testShouldClarifyLowConfidence() {
        result.setConfidenceScore(0.5);
        assertTrue(manager.shouldClarify(result));
    }

    @Test
    public void testShouldClarifyAmbiguity() {
        result.setConfidenceScore(0.9);
        result.getAmbiguities().add(new Ambiguity("test", "reason"));
        assertTrue(manager.shouldClarify(result));
    }

    @Test
    public void testShouldNotClarify() {
        result.setConfidenceScore(0.9);
        assertFalse(manager.shouldClarify(result));
    }

    @Test
    public void testGenerateClarificationQuestionMissingInfo() {
        result.getMissingInformation().add(new MissingRequirement("language", "Not specified"));
        String question = manager.generateClarificationQuestion(result, context);
        assertTrue(question.contains("missing some information about 'language'"));
        assertTrue(question.contains("Not specified"));
    }

    @Test
    public void testGenerateClarificationQuestionFromLlm() {
        result.setClarificationQuestion("Specified by LLM");
        String question = manager.generateClarificationQuestion(result, context);
        assertEquals("Specified by LLM", question);
    }

    @Test
    public void testGenerateClarificationQuestionContradiction() {
        result.getContradictions().add("Conflicting languages");
        String question = manager.generateClarificationQuestion(result, context);
        assertTrue(question.contains("noticed some contradictions"));
        assertTrue(question.contains("Conflicting languages"));
    }

    @Test
    public void testUpdateState() {
        ConversationState state = new ConversationState();
        result.getMissingInformation().add(new MissingRequirement("framework", "missing"));
        String question = "What framework?";

        manager.updateState(state, result, question);

        assertFalse(state.isRequirementMet());
        assertEquals(1, state.getPendingQuestions().size());
        assertTrue(state.getPendingQuestions().get(0).contains("framework"));
        assertTrue(state.getLastMessages().get(0).contains(question));
    }
}
