package eu.kalafatic.evolution.model.orchestration.tests;

import static org.junit.Assert.*;
import org.junit.Test;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import eu.kalafatic.evolution.model.orchestration.*;
import java.io.File;
import java.io.IOException;
import java.util.Collections;

public class RefactoringPersistenceTest {

    @Test
    public void testNewFeaturesPersistence() throws IOException {
        // Initialize EMF
        OrchestrationPackage.eINSTANCE.eClass();
        ResourceSet resourceSet = new ResourceSetImpl();
        resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("evo", new XMIResourceFactoryImpl());

        // Create temporary file
        File tempFile = File.createTempFile("refactor-test", ".evo");
        tempFile.deleteOnExit();
        URI uri = URI.createFileURI(tempFile.getAbsolutePath());

        // 1. Create model and add new features
        Resource resource = resourceSet.createResource(uri);
        Orchestrator orchestrator = OrchestrationFactory.eINSTANCE.createOrchestrator();
        orchestrator.setName("Refactor Test");
        resource.getContents().add(orchestrator);

        // Add a log element
        LogElement log = OrchestrationFactory.eINSTANCE.createLogElement();
        log.setType("INFO");
        log.setMsg("Test message");
        orchestrator.getLogs().add(log);

        // Add a platform mode
        PlatformMode mode = OrchestrationFactory.eINSTANCE.createPlatformMode();
        mode.setType(PlatformType.DARWIN_MODE);
        mode.setAutonomyLevel(AutonomyLevel.HIGH);
        orchestrator.setActivePlatformMode(mode);

        // 2. Save resource
        resource.save(Collections.EMPTY_MAP);

        // 3. Reload and verify
        ResourceSet reloadRS = new ResourceSetImpl();
        reloadRS.getResourceFactoryRegistry().getExtensionToFactoryMap().put("evo", new XMIResourceFactoryImpl());
        Resource reloadedResource = reloadRS.getResource(uri, true);

        Orchestrator reloadedOrch = (Orchestrator) reloadedResource.getContents().get(0);
        assertEquals("Should have 1 log", 1, reloadedOrch.getLogs().size());
        assertEquals("Log message should be preserved", "Test message", reloadedOrch.getLogs().get(0).getMsg());

        assertNotNull("Active platform mode should not be null", reloadedOrch.getActivePlatformMode());
        assertEquals("Platform type should be DARWIN_MODE", PlatformType.DARWIN_MODE, reloadedOrch.getActivePlatformMode().getType());
        assertEquals("Autonomy level should be HIGH", AutonomyLevel.HIGH, reloadedOrch.getActivePlatformMode().getAutonomyLevel());
    }
}
