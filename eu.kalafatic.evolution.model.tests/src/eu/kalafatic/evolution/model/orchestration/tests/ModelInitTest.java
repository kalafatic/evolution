package eu.kalafatic.evolution.model.orchestration.tests;

import eu.kalafatic.evolution.model.orchestration.OrchestrationPackage;
import junit.framework.TestCase;

public class ModelInitTest extends TestCase {
    public void testModelInitialization() {
        assertNotNull("OrchestrationPackage instance should not be null", OrchestrationPackage.eINSTANCE);
        assertEquals("orchestration", OrchestrationPackage.eINSTANCE.getName());
        System.out.println("Model initialization verified successfully: " + OrchestrationPackage.eINSTANCE.getNsURI());
    }

    public void testEclipsePersistence() {
        eu.kalafatic.evolution.model.orchestration.Orchestrator orchestrator = eu.kalafatic.evolution.model.orchestration.OrchestrationFactory.eINSTANCE.createOrchestrator();
        assertNull(orchestrator.getEclipse());

        eu.kalafatic.evolution.model.orchestration.Eclipse eclipse = eu.kalafatic.evolution.model.orchestration.OrchestrationFactory.eINSTANCE.createEclipse();
        eclipse.setWorkspace("/path/to/workspace");
        eclipse.setInstallation("/path/to/eclipse");
        eclipse.setTargetPlatform("Java-21");

        orchestrator.setEclipse(eclipse);

        assertNotNull(orchestrator.getEclipse());
        assertEquals("/path/to/workspace", orchestrator.getEclipse().getWorkspace());
        assertEquals("/path/to/eclipse", orchestrator.getEclipse().getInstallation());
        assertEquals("Java-21", orchestrator.getEclipse().getTargetPlatform());
    }
}
