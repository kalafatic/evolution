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
}
