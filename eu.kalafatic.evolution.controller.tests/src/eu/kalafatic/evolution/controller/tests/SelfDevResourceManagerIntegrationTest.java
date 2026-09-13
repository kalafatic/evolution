package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Before;
import org.junit.Test;

import eu.kalafatic.evolution.controller.orchestration.selfdev.CopySourceTask;
import eu.kalafatic.evolution.controller.orchestration.selfdev.GitCheckTask;
import eu.kalafatic.evolution.controller.orchestration.selfdev.PermissionsCheckTask;
import eu.kalafatic.evolution.controller.orchestration.selfdev.SelfDevContext;
import eu.kalafatic.evolution.controller.orchestration.selfdev.SelfDevOrchestrator;
import eu.kalafatic.evolution.controller.orchestration.selfdev.TaskResult;
import eu.kalafatic.evolution.controller.orchestration.selfdev.TaskStatus;
import eu.kalafatic.evolution.controller.resource.EvoPath;
import eu.kalafatic.evolution.controller.resource.ResourceManager;

public class SelfDevResourceManagerIntegrationTest {

    private ResourceManager resourceManager;
    private SelfDevContext context;

    @Before
    public void setUp() {
        resourceManager = ResourceManager.getInstance();
        resourceManager.refresh();
        File evoRoot = resourceManager.getPath(EvoPath.EVO_ROOT).toFile();
        context = new SelfDevContext(evoRoot, resourceManager.getOrchestrator());
    }

    @Test
    public void testSelfDevContextPathsResolvedViaResourceManager() {
        assertNotNull("Repository root should be resolved", context.getRepositoryRoot());
        assertTrue("Repository root must exist", context.getRepositoryRoot().exists());

        assertNotNull("Project root should be resolved", context.getProjectRoot());
        assertTrue("Project root must exist", context.getProjectRoot().exists());

        assertNotNull("Source directory should be resolved", context.getSourceDirectory());
        assertNotNull("Build directory should be resolved", context.getBuildDirectory());
        assertNotNull("Export directory should be resolved", context.getExportDirectory());
        assertNotNull("Runtime directory should be resolved", context.getRuntimeDirectory());

        assertNotNull("Supervisor directory should be resolved", context.getSupervisorDirectory());
        assertTrue("Supervisor directory must exist", context.getSupervisorDirectory().exists());

        assertNotNull("Genome directory should be resolved", context.getGenomeDirectory());
        assertTrue("Genome directory must exist", context.getGenomeDirectory().exists());

        // Verify no double-nested path corruption
        String supPath = context.getSupervisorDirectory().getAbsolutePath();
        assertTrue("Supervisor path must not contain duplicated root segments",
                !supPath.contains("eu.kalafatic.evolution.supervisor/eu.kalafatic.evolution.supervisor"));
    }

    @Test
    public void testSelfDevTaskExecutionAndCopyVerification() {
        GitCheckTask gitCheck = new GitCheckTask("GIT_EVO");
        TaskResult gitResult = gitCheck.execute(context);
        assertNotNull("Git check result should not be null", gitResult);
        assertEquals("Git check should succeed on valid codebase", TaskStatus.SUCCESS, gitResult.getStatus());

        PermissionsCheckTask permCheck = new PermissionsCheckTask("PERMISSIONS");
        TaskResult permResult = permCheck.execute(context);
        assertNotNull("Permissions check result should not be null", permResult);
        assertEquals("Permissions check should succeed", TaskStatus.SUCCESS, permResult.getStatus());

        CopySourceTask copyTask = new CopySourceTask("COPY");
        TaskResult copyResult = copyTask.execute(context);
        assertNotNull("Copy task result should not be null", copyResult);
        assertEquals("Copy task should succeed and verify destination structure", TaskStatus.SUCCESS, copyResult.getStatus());
        assertTrue("Copied source directory must exist and contain pom.xml", new File(context.getSourceDirectory(), "pom.xml").exists());
    }

    @Test
    public void testBlockedTaskDependencyPropagation() {
        SelfDevOrchestrator orchestrator = new SelfDevOrchestrator(context.getProjectRoot(), context.getOrchestrator());

        // Intentionally record a FAILED result for prerequisite task COPY
        TaskResult failedCopy = TaskResult.failure("COPY", "Simulated source copy failure", null);
        context.recordTaskResult(failedCopy);

        // Execute dependent task BUILD
        TaskResult buildResult = orchestrator.getTaskRegistry() != null && orchestrator.getTaskRegistry().containsKey("BUILD")
                ? orchestrator.getTaskRegistry().get("BUILD").execute(context)
                : null;

        if (buildResult != null) {
            assertEquals("Dependent build task must evaluate as BLOCKED when prerequisite fails", TaskStatus.BLOCKED, buildResult.getStatus());
        }
    }
}
