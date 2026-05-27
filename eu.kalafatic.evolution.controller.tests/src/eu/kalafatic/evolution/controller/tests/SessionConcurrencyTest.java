package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.*;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import eu.kalafatic.evolution.controller.orchestration.*;
import eu.kalafatic.evolution.model.orchestration.*;

public class SessionConcurrencyTest {
    private File tempDir;

    @Before
    public void setUp() throws Exception {
        tempDir = Files.createTempDirectory("concurrency-test").toFile();
        // Clear sessions if possible (OrchestratorServiceImpl doesn't have a clear method,
        // but it's a fresh instance in the OSGi container usually)
    }

    @Test
    public void testConcurrentSessionsIsolation() throws Exception {
        OrchestratorServiceImpl service = OrchestratorServiceImpl.getInstance();

        int sessionCount = 3;
        List<TaskResult> results = new ArrayList<>();

        for (int i = 0; i < sessionCount; i++) {
            TaskRequest request = new TaskRequest("Test request " + i, tempDir);
            String sessionId = "session-concurrency-" + i;
            request.getContext().put("sessionId", sessionId);

            Orchestrator orchestrator = OrchestrationFactory.eINSTANCE.createOrchestrator();
            orchestrator.setId(sessionId);
            orchestrator.setAiChat(OrchestrationFactory.eINSTANCE.createAiChat());
            orchestrator.getAiChat().setPromptInstructions(OrchestrationFactory.eINSTANCE.createPromptInstructions());

            Ollama ollama = OrchestrationFactory.eINSTANCE.createOllama();
            ollama.setUrl("http://localhost:11434");
            ollama.setModel("llama3.2:1b"); // Small model for faster failure if no service
            orchestrator.setOllama(ollama);

            request.getContext().put("orchestrator", orchestrator);

            TaskResult result = service.execute(request);
            results.add(result);
        }

        // Wait for all to finish (or fail due to no LLM)
        long start = System.currentTimeMillis();
        boolean allDone = false;
        while (System.currentTimeMillis() - start < 10000) { // 10s timeout
            allDone = true;
            for (TaskResult r : results) {
                if (r.getStatus() == TaskResult.Status.RUNNING) {
                    allDone = false;
                    break;
                }
            }
            if (allDone) break;
            Thread.sleep(500);
        }

        for (int i = 0; i < sessionCount; i++) {
            TaskResult result = results.get(i);
            String currentSessionId = "session-concurrency-" + i;

            System.out.println("Checking session: " + currentSessionId + " status: " + result.getStatus());

            for (String log : result.getLogs()) {
                // Verify that logs in this session ONLY contain this session's ID
                assertTrue("Log should contain correct sessionId: " + log, log.contains("[" + currentSessionId + "]"));

                for (int j = 0; j < sessionCount; j++) {
                    if (i != j) {
                        String otherSessionId = "session-concurrency-" + j;
                        assertFalse("Cross-session contamination: " + log + " contains " + otherSessionId,
                                   log.contains("[" + otherSessionId + "]"));
                    }
                }
            }
        }
    }
}
