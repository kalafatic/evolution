package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.*;
import java.io.File;
import java.nio.file.Files;
import org.junit.Before;
import org.junit.Test;
import eu.kalafatic.evolution.controller.orchestration.EvolutionOrchestrator;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.Ollama;

public class OrchestrationTest {

    private File tempDir;
    private Orchestrator orchestrator;

    @Before
    public void setUp() throws Exception {
        tempDir = Files.createTempDirectory("evo-test").toFile();
        orchestrator = OrchestrationFactory.eINSTANCE.createOrchestrator();
        orchestrator.setId("test-orch");

        Ollama ollama = OrchestrationFactory.eINSTANCE.createOllama();
        ollama.setUrl("http://localhost:11434");
        ollama.setModel("llama3.2:3b");
        orchestrator.setOllama(ollama);
    }

    @Test
    public void testOrchestratorInitialization() {
        EvolutionOrchestrator engine = new EvolutionOrchestrator();
        assertNotNull(engine);

        TaskContext context = new TaskContext(orchestrator, tempDir);
        assertEquals(orchestrator, context.getOrchestrator());
        assertEquals(tempDir, context.getProjectRoot());
    }

    @Test
    public void testTaskContextState() {
        TaskContext context = new TaskContext(orchestrator, tempDir);
        context.putState("key", "value");
        assertEquals("value", context.getState("key"));

        context.log("test log");
        assertTrue(context.getLogs().contains("test log"));
    }
}
