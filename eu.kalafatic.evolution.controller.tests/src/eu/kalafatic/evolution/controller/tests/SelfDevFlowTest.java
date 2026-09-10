package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.*;
import java.io.File;
import java.lang.reflect.Method;
import org.junit.Test;
import eu.kalafatic.evolution.controller.orchestration.selfdev.SelfDevBootstrapController;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;

public class SelfDevFlowTest {
    @Test
    public void testCheckGenome() throws Exception {
        File projectRoot = new File(".").getAbsoluteFile();
        System.out.println("Project Root in Test: " + projectRoot.getAbsolutePath());
        
        Orchestrator orchestrator = OrchestrationFactory.eINSTANCE.createOrchestrator();
        SelfDevBootstrapController controller = new SelfDevBootstrapController(projectRoot, orchestrator);
        
        Method checkGenomeMethod = SelfDevBootstrapController.class.getDeclaredMethod("checkGenome");
        checkGenomeMethod.setAccessible(true);
        
        String result = (String) checkGenomeMethod.invoke(controller);
        System.out.println("Result of checkGenome: " + result);
        assertNotNull(result);
        assertFalse(result.startsWith("ERROR"));
    }

    @Test
    public void testCompileSupervisorModule() throws Exception {
        File projectRoot = new File(".").getAbsoluteFile();
        Orchestrator orchestrator = OrchestrationFactory.eINSTANCE.createOrchestrator();
        SelfDevBootstrapController controller = new SelfDevBootstrapController(projectRoot, orchestrator);

        Method findSupervisorDirMethod = SelfDevBootstrapController.class.getDeclaredMethod("findSupervisorDir");
        findSupervisorDirMethod.setAccessible(true);
        File supervisorDir = (File) findSupervisorDirMethod.invoke(controller);
        assertNotNull(supervisorDir);
        assertTrue(supervisorDir.exists());

        Method compileSupervisorModuleMethod = SelfDevBootstrapController.class.getDeclaredMethod("compileSupervisorModule", File.class);
        compileSupervisorModuleMethod.setAccessible(true);

        String result = (String) compileSupervisorModuleMethod.invoke(controller, supervisorDir);
        System.out.println("Result of compileSupervisorModule: " + result);
        assertNotNull(result);
        assertEquals("SUCCESS", result);
    }

    @Test
    public void testCheckSourceSnapshotIntegrity() throws Exception {
        File projectRoot = new File(".").getAbsoluteFile();
        Orchestrator orchestrator = OrchestrationFactory.eINSTANCE.createOrchestrator();
        SelfDevBootstrapController controller = new SelfDevBootstrapController(projectRoot, orchestrator);

        // Test with current repository path - should pass integrity check
        String result = controller.checkSourceSnapshotIntegrity(projectRoot);
        assertNull("Snapshot integrity check should pass for root repo", result);

        // Test with incomplete folder - should fail with clear error message
        File tempDir = new File(System.getProperty("java.io.tmpdir"), "incomplete_snapshot_test_" + System.currentTimeMillis());
        tempDir.mkdirs();
        try {
            String incompleteResult = controller.checkSourceSnapshotIntegrity(tempDir);
            assertNotNull(incompleteResult);
            assertTrue(incompleteResult.contains("SelfDev source snapshot is incomplete"));
            assertTrue(incompleteResult.contains("expected Forge target package"));
        } finally {
            tempDir.delete();
        }
    }
}
