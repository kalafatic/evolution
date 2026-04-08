package eu.kalafatic.evolution.model.orchestration.tests;

import static org.junit.Assert.*;
import org.junit.Test;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.OrchestrationPackage;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.TestStatus;
import java.io.File;
import java.io.IOException;
import java.util.Collections;

public class TestSavingTest {

    @Test
    public void testTestPersistence() throws IOException {
        // Initialize EMF
        OrchestrationPackage.eINSTANCE.eClass();
        ResourceSet resourceSet = new ResourceSetImpl();
        resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("evo", new XMIResourceFactoryImpl());
        resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("xml", new XMIResourceFactoryImpl());

        // Create temporary file
        File tempFile = File.createTempFile("test-tests", ".evo");
        tempFile.deleteOnExit();
        URI uri = URI.createFileURI(tempFile.getAbsolutePath());

        // 1. Create model and add test
        Resource resource = resourceSet.createResource(uri);
        Orchestrator orchestrator = OrchestrationFactory.eINSTANCE.createOrchestrator();
        orchestrator.setName("Test Orchestrator");
        resource.getContents().add(orchestrator);

        eu.kalafatic.evolution.model.orchestration.Test modelTest = OrchestrationFactory.eINSTANCE.createTest();
        modelTest.setName("Model Test Persistence");
        modelTest.setPath("/path/to/test");
        modelTest.setStatus(TestStatus.PASSED);
        modelTest.setSelected(true);
        orchestrator.getTests().add(modelTest);

        // 2. Save resource - THIS FAILED BEFORE THE FIX
        resource.save(Collections.EMPTY_MAP);

        // 3. Reload and verify
        ResourceSet reloadRS = new ResourceSetImpl();
        reloadRS.getResourceFactoryRegistry().getExtensionToFactoryMap().put("evo", new XMIResourceFactoryImpl());
        reloadRS.getResourceFactoryRegistry().getExtensionToFactoryMap().put("xml", new XMIResourceFactoryImpl());
        Resource reloadedResource = reloadRS.getResource(uri, true);

        assertFalse("Resource should not be empty", reloadedResource.getContents().isEmpty());
        assertTrue("Root should be an Orchestrator", reloadedResource.getContents().get(0) instanceof Orchestrator);

        Orchestrator reloadedOrch = (Orchestrator) reloadedResource.getContents().get(0);
        assertEquals("Should have 1 test", 1, reloadedOrch.getTests().size());

        eu.kalafatic.evolution.model.orchestration.Test reloadedModelTest = reloadedOrch.getTests().get(0);
        assertEquals("Test name should be preserved", "Model Test Persistence", reloadedModelTest.getName());
        assertEquals("Status should be preserved", TestStatus.PASSED, reloadedModelTest.getStatus());
    }
}
