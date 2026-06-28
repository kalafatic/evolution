package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Before;
import eu.kalafatic.evolution.controller.manager.NeuronService;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import java.io.File;

public class NeuronGlobalPersistenceTest {

    @Before
    public void setUp() {
        String testDir = "target/test-supervisor";
        String testFile = testDir + File.separator + "neuron_memory_test.json";
        NeuronService.setGlobalPaths(testDir, testFile);

        File globalFile = new File(testFile);
        if (globalFile.exists()) {
            globalFile.delete();
        }
    }

    @Test
    public void testGlobalPersistence() {
        NeuronService service = NeuronService.getInstance();

        Orchestrator orch1 = OrchestrationFactory.eINSTANCE.createOrchestrator();
        Orchestrator orch2 = OrchestrationFactory.eINSTANCE.createOrchestrator();

        String testPhrase = "Evolutionary kernel specialized in deterministic state transitions";
        service.train(orch1, testPhrase, "coding", 5);

        // Proposals should be available in orch2 even though it wasn't used for training
        String[] proposals = service.getProposals(orch2, "evolutionary", "coding");

        boolean found = false;
        for (String p : proposals) {
            if (p.contains("evolutionary")) {
                found = true;
                break;
            }
        }
        assertTrue("Proposal should be found in second orchestrator instance", found);

        // Proposals should also be available for null orchestrator (global only)
        proposals = service.getProposals(null, "deterministic", "coding");
        found = false;
        for (String p : proposals) {
            if (p.contains("deterministic")) {
                found = true;
                break;
            }
        }
        assertTrue("Proposal should be found in global memory (null orchestrator)", found);
    }
}
