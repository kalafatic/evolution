package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.*;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import eu.kalafatic.evolution.controller.orchestration.PlannerAgent;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.llm.ILlmProvider;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.Task;
import java.lang.reflect.Field;

public class PlannerCategorizationTest {

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
    public void testCreateFileCategorization() throws Exception {
        PlannerAgent planner = new PlannerAgent();
        injectMockLlm(planner, mockLlm);

        // This response mimics a miscategorization (what happened in the bug report)
        String wrongResponse = "[ { \"id\": \"task1\", \"name\": \"General: Respond to greeting\", \"description\": \"Politely acknowledge the user's greeting.\", \"taskType\": \"llm\", \"approvalRequired\": false, \"loopToTaskId\": \"none\" } ]";
        mockLlm.setResponse(wrongResponse);

        List<Task> tasks = planner.plan("create file a", context);

        // This confirms the failure state if the planner is not improved
        assertEquals(1, tasks.size());
        assertEquals("llm", tasks.get(0).getType());
        assertEquals("General: Respond to greeting", tasks.get(0).getName());
    }

    @Test
    public void testImprovedCategorization() throws Exception {
        PlannerAgent planner = new PlannerAgent();
        injectMockLlm(planner, mockLlm);

        // This response mimics what we expect from the improved prompt
        String betterResponse = "[ { \"id\": \"t1\", \"name\": \"General: Clarify file creation\", \"description\": \"Ask where 'a' should be created...\", \"taskType\": \"llm\", \"approvalRequired\": false } ]";
        mockLlm.setResponse(betterResponse);

        List<Task> tasks = planner.plan("create file a", context);

        assertEquals(1, tasks.size());
        assertEquals("llm", tasks.get(0).getType());
        assertTrue(tasks.get(0).getName().contains("Clarify"));
    }

    private void injectMockLlm(PlannerAgent agent, ILlmProvider mock) throws Exception {
        Field serviceField = PlannerAgent.class.getSuperclass().getDeclaredField("aiService");
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
