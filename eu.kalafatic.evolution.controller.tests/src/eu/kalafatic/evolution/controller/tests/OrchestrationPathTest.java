package eu.kalafatic.evolution.controller.tests;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

import static org.junit.Assert.*;
import java.io.File;
import java.nio.file.Files;
import java.lang.reflect.Method;
import org.junit.Before;
import org.junit.Test;
import eu.kalafatic.evolution.controller.agents.IAgent;
import eu.kalafatic.evolution.controller.orchestration.*;
import eu.kalafatic.evolution.model.orchestration.*;

public class OrchestrationPathTest {
    @Test
    public void testPathSanitization() throws Exception {
        EvolutionOrchestrator orchestrator = new EvolutionOrchestrator();
        File tempDir = Files.createTempDirectory("path-test").toFile();
        TaskContext context = new TaskContext(OrchestrationFactory.eINSTANCE.createOrchestrator(), tempDir);

        Method method = EvolutionOrchestrator.class.getDeclaredMethod("applyPatch", Task.class, IAgent.class, TaskContext.class, String.class, String.class);
        method.setAccessible(true);

        Task task = OrchestrationFactory.eINSTANCE.createTask();
        task.setType("file");
        task.setName("Write /path/to/file.java");

        IAgent mockAgent = new IAgent() {
            @Override public SessionContainer getSessionContainer() { return null; }
            @Override public String getId() { return "mock"; }
            @Override public String getType() { return "JavaDev"; }
            @Override public java.util.List<eu.kalafatic.evolution.controller.tools.ITool> getTools() { return java.util.Collections.emptyList(); }
            @Override public String process(String desc, TaskContext ctx, String feedback) { return "public class Test {}"; }
        };

        String result = (String) method.invoke(orchestrator, task, mockAgent, context, null, "public class Test {}");
        assertTrue(result.contains("SUCCESS: Wrote file path/to/file.java"));
    }
}
