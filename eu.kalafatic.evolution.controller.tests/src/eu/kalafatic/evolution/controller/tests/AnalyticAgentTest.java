package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.*;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import eu.kalafatic.evolution.controller.agents.AnalyticAgent;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.llm.ILlmProvider;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import java.lang.reflect.Field;

public class AnalyticAgentTest {

    private Orchestrator orchestrator;
    private TaskContext context;
    private MockLlmProvider mockLlm;

    @Before
    public void setUp() {
        orchestrator = OrchestrationFactory.eINSTANCE.createOrchestrator();
        context = new TaskContext(orchestrator, null);
        mockLlm = new MockLlmProvider();
    }

    @Test
    public void testAmbiguousRequest() throws Exception {
        AnalyticAgent agent = new AnalyticAgent();
        injectMockLlm(agent, mockLlm);

        String response = "{\n" +
                "  \"category\": \"CODING\",\n" +
                "  \"objective\": \"Fix a bug\",\n" +
                "  \"isAmbiguous\": true,\n" +
                "  \"missingInformation\": [\"location of the bug\", \"description of the bug\"],\n" +
                "  \"clarificationQuestion\": \"Which bug should I fix and where is it located?\",\n" +
                "  \"refinedPrompt\": \"\"\n" +
                "}";
        mockLlm.setResponse(response);

        JSONObject analysis = agent.analyze("fix bug", context);
        assertTrue(analysis.getBoolean("isAmbiguous"));
        assertEquals("CODING", analysis.getString("category"));
        assertEquals("Which bug should I fix and where is it located?", analysis.getString("clarificationQuestion"));
    }

    @Test
    public void testClearRequest() throws Exception {
        AnalyticAgent agent = new AnalyticAgent();
        injectMockLlm(agent, mockLlm);

        String response = "{\n" +
                "  \"category\": \"CODING\",\n" +
                "  \"objective\": \"Create a Java class\",\n" +
                "  \"isAmbiguous\": false,\n" +
                "  \"missingInformation\": [],\n" +
                "  \"clarificationQuestion\": \"\",\n" +
                "  \"refinedPrompt\": \"Create a Java class named Hello in src/Main.java that prints Hello World\"\n" +
                "}";
        mockLlm.setResponse(response);

        JSONObject analysis = agent.analyze("create src/Main.java class Hello printing Hello World", context);
        assertFalse(analysis.getBoolean("isAmbiguous"));
        assertEquals("CODING", analysis.getString("category"));
        assertTrue(analysis.getString("refinedPrompt").contains("Main.java"));
    }

    private void injectMockLlm(AnalyticAgent agent, ILlmProvider mock) throws Exception {
        Field serviceField = AnalyticAgent.class.getSuperclass().getDeclaredField("aiService");
        serviceField.setAccessible(true);
        Object aiService = serviceField.get(agent);

        Field routerField = aiService.getClass().getDeclaredField("llmRouter");
        routerField.setAccessible(true);
        Object router = routerField.get(aiService);

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
