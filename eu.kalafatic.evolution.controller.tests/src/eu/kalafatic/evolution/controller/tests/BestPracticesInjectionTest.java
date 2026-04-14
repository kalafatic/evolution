package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.*;
import java.io.File;
import java.nio.file.Files;
import org.junit.Before;
import org.junit.Test;

import eu.kalafatic.evolution.controller.agents.BaseAiAgent;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

public class BestPracticesInjectionTest {

    private File tempRoot;
    private Orchestrator orchestrator;
    private TaskContext context;

    @Before
    public void setUp() throws Exception {
        tempRoot = Files.createTempDirectory("bp-inject-test").toFile();
        orchestrator = OrchestrationFactory.eINSTANCE.createOrchestrator();
        context = new TaskContext(orchestrator, tempRoot);
    }

    @Test
    public void testIterativeLoopInjection() throws Exception {
        orchestrator.setIterativeMode(true);
        orchestrator.setSelfIterativeMode(false);

        TestAgent agent = new TestAgent();
        String prompt = agent.getPrompt("test task", context);

        assertTrue("Prompt should contain ITERATIVE LOOP CONTEXT", prompt.contains("--- ITERATIVE LOOP CONTEXT ---"));
        assertTrue("Prompt should contain OBSERVE", prompt.contains("OBSERVE"));
        assertFalse("Prompt should NOT contain SELF DEVELOPMENT CONTEXT", prompt.contains("--- SELF DEVELOPMENT CONTEXT ---"));
    }

    @Test
    public void testSelfDevelopmentInjection() throws Exception {
        orchestrator.setIterativeMode(false);
        orchestrator.setSelfIterativeMode(true);

        TestAgent agent = new TestAgent();
        String prompt = agent.getPrompt("test task", context);

        assertTrue("Prompt should contain SELF DEVELOPMENT CONTEXT", prompt.contains("--- SELF DEVELOPMENT CONTEXT ---"));
        assertTrue("Prompt should contain Autonomous", prompt.contains("Autonomous"));
        assertFalse("Prompt should NOT contain ITERATIVE LOOP CONTEXT", prompt.contains("--- ITERATIVE LOOP CONTEXT ---"));
    }

    @Test
    public void testBothInjections() throws Exception {
        orchestrator.setIterativeMode(true);
        orchestrator.setSelfIterativeMode(true);

        TestAgent agent = new TestAgent();
        String prompt = agent.getPrompt("test task", context);

        assertTrue("Prompt should contain ITERATIVE LOOP CONTEXT", prompt.contains("--- ITERATIVE LOOP CONTEXT ---"));
        assertTrue("Prompt should contain SELF DEVELOPMENT CONTEXT", prompt.contains("--- SELF DEVELOPMENT CONTEXT ---"));
    }

    @Test
    public void testIterativeContextContent() throws Exception {
        orchestrator.setIterativeMode(true);
        TestAgent agent = new TestAgent();
        String prompt = agent.getPrompt("test task", context);

        assertTrue("Prompt should contain OBSERVE", prompt.contains("OBSERVE"));
        assertTrue("Prompt should contain ANALYZE", prompt.contains("ANALYZE"));
        assertTrue("Prompt should contain PLAN", prompt.contains("PLAN"));
        assertTrue("Prompt should contain TEST", prompt.contains("TEST"));
    }

    private static class TestAgent extends BaseAiAgent {
        public TestAgent() {
            super("test", "Test");
        }
        @Override
        protected String getAgentInstructions() {
            return "Test instructions";
        }
        public String getPrompt(String task, TaskContext ctx) {
            return buildPrompt(task, ctx, null);
        }
    }
}
