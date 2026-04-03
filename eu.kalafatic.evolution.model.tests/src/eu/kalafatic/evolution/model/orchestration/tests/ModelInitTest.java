package eu.kalafatic.evolution.model.orchestration.tests;

import eu.kalafatic.evolution.model.orchestration.OrchestrationPackage;
import junit.framework.TestCase;

public class ModelInitTest extends TestCase {
    public void testModelInitialization() {
        assertNotNull("OrchestrationPackage instance should not be null", OrchestrationPackage.eINSTANCE);
        assertEquals("orchestration", OrchestrationPackage.eINSTANCE.getName());
        System.out.println("Model initialization verified successfully: " + OrchestrationPackage.eINSTANCE.getNsURI());
    }
}
