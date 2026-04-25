package eu.kalafatic.evolution.controller.manager;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;

public class NeuronServiceTest {

    private Orchestrator orchestrator;
    private NeuronService service;

    @Before
    public void setUp() {
        orchestrator = OrchestrationFactory.eINSTANCE.createOrchestrator();
        service = NeuronService.getInstance();
    }

    @Test
    public void testCategorizedTraining() {
        // Clear potential state if singleton was dirty
        if (orchestrator.getNeuronAI() != null) {
             orchestrator.getNeuronAI().setTrainingData("{}");
        }

        service.train(orchestrator, "create java class", "coding", 5);
        service.train(orchestrator, "hello world", "chat", 5);

        String[] codingProposals = service.getProposals(orchestrator, "cre", "coding");
        String[] chatProposals = service.getProposals(orchestrator, "hel", "chat");

        // "create java class" -> "create", "java", "class" (tokens) + "create java class" (full phrase)
        // "cre" should match "create" and "create java class"
        assertEquals(2, codingProposals.length);

        // "hello world" -> "hello", "world" (tokens) + "hello world" (full phrase)
        assertEquals(2, chatProposals.length);

        // Check cross-category isolation
        assertEquals(0, service.getProposals(orchestrator, "cre", "chat").length);
    }

    @Test
    public void testWeightedProposals() {
        // Ensure banana is more frequent
        service.train(orchestrator, "apple apple banana", "chat", 5);
        service.train(orchestrator, "banana", "chat", 5);
        service.train(orchestrator, "banana", "chat", 5);

        String[] proposals = service.getProposals(orchestrator, "", "chat");

        // apple freq: 2
        // banana freq: 1 (from first) + 1 + 5 (full sentence) + 1 + 5 (full sentence) = 13

        assertEquals("banana", proposals[0]);
        assertTrue(proposals[1].contains("apple"));
    }

    @Test
    public void testLegacyDataFallback() {
        eu.kalafatic.evolution.model.orchestration.NeuronAI neuronAI = OrchestrationFactory.eINSTANCE.createNeuronAI();
        neuronAI.setTrainingData("[\"legacy-token\"]");
        orchestrator.setNeuronAI(neuronAI);

        String[] proposals = service.getProposals(orchestrator, "leg", "chat");
        assertEquals(1, proposals.length);
        assertEquals("legacy-token", proposals[0]);
    }
}
