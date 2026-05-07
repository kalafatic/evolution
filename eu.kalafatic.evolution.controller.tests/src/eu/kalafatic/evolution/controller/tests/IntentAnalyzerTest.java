package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import eu.kalafatic.evolution.controller.orchestration.AiService;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.intent.IntentAnalysisResult;
import eu.kalafatic.evolution.controller.orchestration.intent.IntentAnalyzer;
import eu.kalafatic.evolution.controller.orchestration.llm.ILlmProvider;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import org.junit.After;

public class IntentAnalyzerTest {

    private Orchestrator orchestrator;
    private TaskContext context;
    private MockLlmProvider mockLlm;
    private File tempRoot;
    private AiService aiService;

    @Before
    public void setUp() throws Exception {
        tempRoot = Files.createTempDirectory("intent-test").toFile();
        orchestrator = OrchestrationFactory.eINSTANCE.createOrchestrator();
        context = new TaskContext(orchestrator, tempRoot);
        mockLlm = new MockLlmProvider();
        aiService = new AiService();
        injectMockLlm(aiService, mockLlm);
    }

    @After
    public void tearDown() {
        if (tempRoot != null && tempRoot.exists()) {
            deleteDirectory(tempRoot);
        }
    }

    private void deleteDirectory(File directory) {
        File[] allContents = directory.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        directory.delete();
    }

    @Test
    public void testClearIntent() throws Exception {
        String response = "{\n" +
                "  \"goal\": \"Create a REST API with Spring Boot\",\n" +
                "  \"language\": \"Java\",\n" +
                "  \"framework\": \"Spring Boot\",\n" +
                "  \"targetPlatform\": \"Web\",\n" +
                "  \"expectedOutput\": \"Working REST controller\",\n" +
                "  \"constraints\": [\"use Maven\"],\n" +
                "  \"missingInformation\": [],\n" +
                "  \"ambiguities\": [],\n" +
                "  \"confidenceScore\": 0.95\n" +
                "}";
        mockLlm.setResponse(response);

        IntentAnalyzer analyzer = new IntentAnalyzer(aiService);
        IntentAnalysisResult result = analyzer.analyze("create spring boot rest api in java using maven", context);

        assertEquals("Create a REST API with Spring Boot", result.getGoal());
        assertEquals("Java", result.getLanguage());
        assertEquals("Spring Boot", result.getFramework());
        assertEquals(0.95, result.getConfidenceScore(), 0.001);
        assertFalse(result.isAmbiguous());
    }

    @Test
    public void testAmbiguousIntent() throws Exception {
        String response = "{\n" +
                "  \"goal\": \"Implement feature\",\n" +
                "  \"language\": \"unknown\",\n" +
                "  \"framework\": \"unknown\",\n" +
                "  \"targetPlatform\": \"unknown\",\n" +
                "  \"expectedOutput\": \"unknown\",\n" +
                "  \"constraints\": [],\n" +
                "  \"missingInformation\": [\n" +
                "    { \"field\": \"language\", \"description\": \"Programming language not specified\" }\n" +
                "  ],\n" +
                "  \"ambiguities\": [\n" +
                "    { \"part\": \"feature\", \"reason\": \"Vague description of the feature\" }\n" +
                "  ],\n" +
                "  \"confidenceScore\": 0.3\n" +
                "}";
        mockLlm.setResponse(response);

        IntentAnalyzer analyzer = new IntentAnalyzer(aiService);
        IntentAnalysisResult result = analyzer.analyze("implement a new feature", context);

        assertTrue(result.isAmbiguous());
        assertEquals(1, result.getMissingInformation().size());
        assertEquals(1, result.getAmbiguities().size());
        assertEquals(0.3, result.getConfidenceScore(), 0.001);
    }

    private void injectMockLlm(AiService service, MockLlmProvider mock) throws Exception {
        Field routerField = service.getClass().getDeclaredField("llmRouter");
        routerField.setAccessible(true);
        Object router = routerField.get(service);

        Field ollamaField = router.getClass().getDeclaredField("ollamaProvider");
        ollamaField.setAccessible(true);
        ollamaField.set(router, mock);
    }

    private static class MockLlmProvider implements ILlmProvider {
        private String response;

        public void setResponse(String response) {
            this.response = response;
        }

        @Override
        public String sendRequest(Orchestrator orchestrator, String prompt, float temperature, String proxyUrl, TaskContext context) throws Exception {
            return response;
        }
    }
}
