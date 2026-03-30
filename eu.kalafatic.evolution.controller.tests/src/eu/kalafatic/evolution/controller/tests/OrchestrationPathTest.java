package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.*;
import java.io.File;
import java.nio.file.Files;
import java.lang.reflect.Method;
import org.junit.Before;
import org.junit.Test;
import eu.kalafatic.evolution.controller.orchestration.EvolutionOrchestrator;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.IAgent;
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
    }

    @Test
    public void testPathSanitization() throws Exception {
        // We want to test the performAction method, but it is private.
        // We can use reflection or test it through a public method if possible.
        // However, EvolutionOrchestrator.execute is complex because it calls AI.

        // Let's use reflection to test performAction directly for 'file' type.
        Method method = EvolutionOrchestrator.class.getDeclaredMethod("performAction", Task.class, IAgent.class, TaskContext.class, String.class);
        method.setAccessible(true);

        Task task = OrchestrationFactory.eINSTANCE.createTask();
        task.setType("file");

        // Mock agent that returns fixed content
        IAgent mockAgent = new IAgent() {
            @Override public String getId() { return "mock"; }
            @Override public String getType() { return "JavaDev"; }
            @Override public java.util.List<eu.kalafatic.evolution.controller.orchestration.ITool> getTools() { return java.util.Collections.emptyList(); }
            @Override public String process(String desc, TaskContext ctx, String feedback) { return "public class Test {}"; }
        };

        String[] absolutePaths = {
            "Write /path/to/file.java",
            "Write C:/path/to/file.java",
            "Write \\path\\to\\file.java",
            "Write D:\\path\\to\\file.java"
        };

        for (String absPath : absolutePaths) {
            task.setName(absPath);
            String result = (String) method.invoke(orchestrator, task, mockAgent, context, null);
            assertTrue("Should succeed for " + absPath + " but got: " + result, result.startsWith("SUCCESS: Wrote file path/to/file.java"));

            File expectedFile = new File(tempDir, "path/to/file.java");
            assertTrue("File should exist: " + expectedFile.getAbsolutePath(), expectedFile.exists());
            expectedFile.delete();
            expectedFile.getParentFile().delete(); // to/
            expectedFile.getParentFile().getParentFile().delete(); // path/
        }
    }
}
