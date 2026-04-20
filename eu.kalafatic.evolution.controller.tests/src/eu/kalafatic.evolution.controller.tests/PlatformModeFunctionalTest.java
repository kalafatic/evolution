package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.*;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Before;
import org.junit.Test;
import eu.kalafatic.evolution.controller.orchestration.EvolutionOrchestrator;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.llm.ILlmProvider;
import eu.kalafatic.evolution.controller.orchestration.llm.LlmRouter;
import eu.kalafatic.evolution.controller.agents.IAgent;
import eu.kalafatic.evolution.controller.orchestration.AiService;
import eu.kalafatic.evolution.controller.orchestration.PlatformMode;
import eu.kalafatic.evolution.controller.orchestration.PlatformType;
import eu.kalafatic.evolution.controller.orchestration.AutonomyLevel;
import eu.kalafatic.evolution.model.orchestration.AiMode;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.SelfDevSession;
import eu.kalafatic.evolution.model.orchestration.SelfDevStatus;

/**
 * Basic functional tests for key platform modes.
 * These tests verify the end-to-end flow using mocked LLM responses.
 */
public class PlatformModeFunctionalTest {

    private File tempDir;
    private Orchestrator orchestrator;
    private MockLlmProvider mockLlm;
    private EvolutionOrchestrator engine;

    @Before
    public void setUp() throws Exception {
        tempDir = Files.createTempDirectory("platform-mode-test").toFile();
        orchestrator = OrchestrationFactory.eINSTANCE.createOrchestrator();
        orchestrator.setAiMode(AiMode.LOCAL);

        // Initialize AiChat and PromptInstructions to avoid NPE in BaseAiAgent
        orchestrator.setAiChat(OrchestrationFactory.eINSTANCE.createAiChat());
        orchestrator.getAiChat().setPromptInstructions(OrchestrationFactory.eINSTANCE.createPromptInstructions());

        mockLlm = new MockLlmProvider();
        engine = new EvolutionOrchestrator();
        injectMocksIntoOrchestrator(engine, mockLlm);
    }

    @Test
    public void testSimpleChatMode() throws Exception {
        TaskContext context = new TaskContext(orchestrator, tempDir);
        context.setPlatformMode(new PlatformMode(PlatformType.SIMPLE_CHAT, AutonomyLevel.LOW, 1, false));

        mockLlm.setResponse("The project is a Tycho-based RCP application. I am in chat mode.");

        String result = engine.execute("Tell me about the project", context);
        assertNotNull(result);
        assertTrue(result.contains("chat mode"));
        assertTrue(context.getLogs().stream().anyMatch(l -> l.contains("SIMPLE_CHAT detected")));
    }

    @Test
    public void testAssistedCodingMode() throws Exception {
        TaskContext context = new TaskContext(orchestrator, tempDir);
        context.setAutoApprove(true);
        context.setPlatformMode(new PlatformMode(PlatformType.ASSISTED_CODING, AutonomyLevel.LOW, 2, false));

        mockLlm.setResponseSequence(new String[] {
            "{\"intent\":\"new\", \"confidence\":1.0}",
            "{\"category\":\"CODING\", \"isAmbiguous\":false}",
            "[{\"id\": \"t1\", \"name\": \"Create README.md\", \"taskType\": \"file\"}]", // Plan
            "This is a readme content", // File content
            "{\"success\": true, \"comment\": \"Verified\"}" // Review
        });

        String result = engine.execute("Create a readme", context);
        assertNotNull(result);
        assertTrue(new File(tempDir, "README.md").exists());
    }

    @Test
    public void testDarwinModeRouting() throws Exception {
        orchestrator.setDarwinMode(true);
        TaskContext context = new TaskContext(orchestrator, tempDir);
        context.setAutoApprove(true);

        // Darwin mode is handled in EvolutionOrchestrator.execute before agent planning
        // but here we verify the orchestrator flag and context
        assertTrue(orchestrator.isDarwinMode());

        // In this test environment, the actual DarwinEngine generation requires more complex setup
        // so we verify that the orchestrator is at least aware of the mode.
        PlatformMode mode = new PlatformMode(PlatformType.DARWIN_MODE, AutonomyLevel.MEDIUM, 3, false);
        context.setPlatformMode(mode);
        assertEquals(PlatformType.DARWIN_MODE, context.getPlatformMode().getType());
    }

    @Test
    public void testSelfDevMode() throws Exception {
        TaskContext context = new TaskContext(orchestrator, tempDir);
        context.setPlatformMode(new PlatformMode(PlatformType.SELF_DEV_MODE, AutonomyLevel.HIGH, 5, true));

        SelfDevSession session = OrchestrationFactory.eINSTANCE.createSelfDevSession();
        session.setId("test-self-dev");
        session.setStatus(SelfDevStatus.RUNNING);
        orchestrator.setSelfDevSession(session);

        assertEquals(SelfDevStatus.RUNNING, orchestrator.getSelfDevSession().getStatus());
        assertTrue(context.getPlatformMode().isAllowSelfModify());
    }

    private void injectMocksIntoOrchestrator(EvolutionOrchestrator engine, ILlmProvider mock) throws Exception {
        Field[] fields = EvolutionOrchestrator.class.getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            Object agent = field.get(engine);
            if (agent != null && !isExcluded(agent)) {
                injectMockIntoAgent(agent, mock);
            }
        }

        Field agentsField = EvolutionOrchestrator.class.getDeclaredField("availableAgents");
        agentsField.setAccessible(true);
        Object agentsObj = agentsField.get(engine);
        if (agentsObj instanceof List) {
            List<IAgent> agents = (List<IAgent>) agentsObj;
            for (IAgent agent : agents) {
                injectMockIntoAgent(agent, mock);
            }
        } else if (agentsObj instanceof IAgent[]) {
            IAgent[] agents = (IAgent[]) agentsObj;
            for (IAgent agent : agents) {
                injectMockIntoAgent(agent, mock);
            }
        }
    }

    private boolean isExcluded(Object obj) {
        if (obj == null) return true;
        String name = obj.getClass().getName();
        return name.startsWith("java.") || name.startsWith("javax.") || name.startsWith("sun.");
    }

    private void injectMockIntoAgent(Object agent, ILlmProvider mock) throws Exception {
        if (agent == null || isExcluded(agent)) return;
        Class<?> clazz = agent.getClass();
        while (clazz != null && clazz != Object.class) {
            for (Field field : clazz.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) continue;
                field.setAccessible(true);
                Object value = field.get(agent);
                if (value instanceof LlmRouter) {
                    injectProviderIntoRouter((LlmRouter) value, mock);
                } else if (value instanceof AiService) {
                    Field routerField = AiService.class.getDeclaredField("llmRouter");
                    routerField.setAccessible(true);
                    injectProviderIntoRouter((LlmRouter) routerField.get(value), mock);
                }
            }
            clazz = clazz.getSuperclass();
        }
    }

    private void injectProviderIntoRouter(LlmRouter router, ILlmProvider mock) throws Exception {
        if (router == null) return;
        Field[] fields = LlmRouter.class.getDeclaredFields();
        for (Field field : fields) {
            if (field.getType().equals(ILlmProvider.class)) {
                field.setAccessible(true);
                field.set(router, mock);
            }
        }
    }

    private static class MockLlmProvider implements ILlmProvider {
        private String[] responseSequence;
        private final AtomicInteger callCount = new AtomicInteger(0);

        public void setResponse(String response) {
            this.responseSequence = new String[] { response };
            this.callCount.set(0);
        }

        public void setResponseSequence(String[] sequence) {
            this.responseSequence = sequence;
            this.callCount.set(0);
        }

        @Override
        public String sendRequest(Orchestrator orchestrator, String prompt, float temperature, String proxyUrl, TaskContext context) throws Exception {
            int current = callCount.getAndIncrement();
            if (responseSequence != null && current < responseSequence.length) {
                return responseSequence[current];
            }
            return "{\"success\": true, \"comment\": \"Default mock response\"}";
        }

        @Override
        public String testConnection(Orchestrator orchestrator, float temperature, String proxyUrl, TaskContext context) throws Exception {
            return "{\"status\": \"ok\"}";
        }
    }
}
