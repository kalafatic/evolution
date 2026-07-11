package eu.kalafatic.evolution.controller.tests;

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

    @Test
    public void testProjectManagerPathUtils() {
        // Test static methods on ProjectModelManager
        String codebasePathModel = eu.kalafatic.evolution.controller.manager.ProjectModelManager.getCodebasePath();
        String workspacePathModel = eu.kalafatic.evolution.controller.manager.ProjectModelManager.getWorkspacePath();

        assertNotNull("Codebase path from ProjectModelManager should not be null", codebasePathModel);
        assertNotNull("Workspace path from ProjectModelManager should not be null", workspacePathModel);

        // Test instance methods on ProjectModelManager
        eu.kalafatic.evolution.controller.manager.ProjectModelManager pmm = eu.kalafatic.evolution.controller.manager.ProjectModelManager.getInstance();
        assertNotNull("ProjectModelManager instance should not be null", pmm);
        assertEquals(codebasePathModel, pmm.getCodebaseFolderPath());
        assertEquals(workspacePathModel, pmm.getWorkspaceFolderPath());

        // Test ProjectManager from view using reflection if available in classloader
        try {
            Class<?> viewProjectManagerClass = Class.forName("eu.kalafatic.evolution.view.provider.ProjectManager");

            // Invoke static getCodebasePath()
            Method getCodebasePathMethod = viewProjectManagerClass.getMethod("getCodebasePath");
            String codebasePathView = (String) getCodebasePathMethod.invoke(null);
            assertNotNull("Codebase path from view ProjectManager should not be null", codebasePathView);

            // Invoke static getWorkspacePath()
            Method getWorkspacePathMethod = viewProjectManagerClass.getMethod("getWorkspacePath");
            String workspacePathView = (String) getWorkspacePathMethod.invoke(null);
            assertNotNull("Workspace path from view ProjectManager should not be null", workspacePathView);

            // Invoke instance methods
            Object pmInstance = viewProjectManagerClass.getDeclaredConstructor().newInstance();

            Method getCodebaseFolderPathMethod = viewProjectManagerClass.getMethod("getCodebaseFolderPath");
            assertEquals(codebasePathView, getCodebaseFolderPathMethod.invoke(pmInstance));

            Method getWorkspaceFolderPathMethod = viewProjectManagerClass.getMethod("getWorkspaceFolderPath");
            assertEquals(workspacePathView, getWorkspaceFolderPathMethod.invoke(pmInstance));
        } catch (ClassNotFoundException e) {
            // View bundle not present in classloader (this is normal in standalone controller test runs)
        } catch (Exception e) {
            fail("Failed to test view ProjectManager reflectively: " + e.getLocalizedMessage());
        }
    }
}
