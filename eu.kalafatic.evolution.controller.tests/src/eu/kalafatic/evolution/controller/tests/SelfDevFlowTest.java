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
}
