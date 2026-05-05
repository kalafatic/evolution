package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.*;
import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Files;
import org.junit.Before;
import org.junit.Test;

import eu.kalafatic.evolution.controller.agents.IAgent;
import eu.kalafatic.evolution.controller.orchestration.EvolutionOrchestrator;
import eu.kalafatic.evolution.controller.orchestration.SystemState;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.TransitionToken;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.Task;

public class OrchestrationPathTest {

    private EvolutionOrchestrator orchestrator;
    private TaskContext context;
    private File tempDir;

    @Before
    public void setUp() throws Exception {
        orchestrator = new EvolutionOrchestrator();
        tempDir = Files.createTempDirectory("path-test").toFile();
        Orchestrator model = OrchestrationFactory.eINSTANCE.createOrchestrator();
        context = new TaskContext(model, tempDir);

        // Gate Orchestrator for test execution
        context.getStateHolder().applyTransition(new TransitionToken(), SystemState.EXECUTING);
    }

    @Test
    public void testPathSanitization() throws Exception {
        // Test applyPatch directly via reflection since it's the core path handling logic
        Method method = EvolutionOrchestrator.class.getDeclaredMethod("applyPatch", Task.class, IAgent.class, TaskContext.class, String.class, String.class);
        method.setAccessible(true);

        Task task = OrchestrationFactory.eINSTANCE.createTask();
        task.setType("file");

        // Mock agent
        IAgent mockAgent = new IAgent() {
            @Override public String getId() { return "mock"; }
            @Override public String getType() { return "JavaDev"; }
            @Override public java.util.List<eu.kalafatic.evolution.controller.tools.ITool> getTools() { return java.util.Collections.emptyList(); }
            @Override public String process(String desc, TaskContext ctx, String feedback) { return "content"; }
        };

        String[] absolutePaths = {
            "Write /path/to/file.java",
            "Write C:/path/to/file.java",
            "Write \\path\\to\\file.java",
            "Write D:\\path\\to\\file.java"
        };

        for (String absPath : absolutePaths) {
            task.setName(absPath);
            // applyPatch(task, agent, context, feedback, patch)
            String result = (String) method.invoke(orchestrator, task, mockAgent, context, null, "public class Test {}");
            assertTrue("Should succeed for " + absPath + " but got: " + result, result.startsWith("SUCCESS: Wrote file path/to/file.java"));

            File expectedFile = new File(tempDir, "path/to/file.java");
            assertTrue("File should exist: " + expectedFile.getAbsolutePath(), expectedFile.exists());

            // Cleanup
            expectedFile.delete();
            File parent = expectedFile.getParentFile();
            while (parent != null && !parent.equals(tempDir)) {
                parent.delete();
                parent = parent.getParentFile();
            }
        }
    }
}
