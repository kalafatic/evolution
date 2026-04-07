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
import eu.kalafatic.evolution.model.orchestration.Task;
import eu.kalafatic.evolution.model.orchestration.TaskStatus;
import java.io.File;
import java.io.IOException;
import java.util.Collections;

public class TaskSavingTest {

    @Test
    public void testTaskPersistence() throws IOException {
        // Initialize EMF
        OrchestrationPackage.eINSTANCE.eClass();
        ResourceSet resourceSet = new ResourceSetImpl();
        resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("evo", new XMIResourceFactoryImpl());
        resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("xml", new XMIResourceFactoryImpl());

        // Create temporary file
        File tempFile = File.createTempFile("test-tasks", ".evo");
        tempFile.deleteOnExit();
        URI uri = URI.createFileURI(tempFile.getAbsolutePath());

        // 1. Create model and add task
        Resource resource = resourceSet.createResource(uri);
        Orchestrator orchestrator = OrchestrationFactory.eINSTANCE.createOrchestrator();
        orchestrator.setName("Test Orchestrator");
        resource.getContents().add(orchestrator);

        Task task = OrchestrationFactory.eINSTANCE.createTask();
        task.setName("Test Task Persistence");
        task.setScheduledTime("14:30");
        task.setStatus(TaskStatus.PENDING);
        task.setSelected(true);
        orchestrator.getTasks().add(task);

        // 2. Save resource
        resource.save(Collections.EMPTY_MAP);

        // 3. Reload and verify
        ResourceSet reloadRS = new ResourceSetImpl();
        reloadRS.getResourceFactoryRegistry().getExtensionToFactoryMap().put("evo", new XMIResourceFactoryImpl());
        reloadRS.getResourceFactoryRegistry().getExtensionToFactoryMap().put("xml", new XMIResourceFactoryImpl());
        Resource reloadedResource = reloadRS.getResource(uri, true);

        assertFalse("Resource should not be empty", reloadedResource.getContents().isEmpty());
        assertTrue("Root should be an Orchestrator", reloadedResource.getContents().get(0) instanceof Orchestrator);

        Orchestrator reloadedOrch = (Orchestrator) reloadedResource.getContents().get(0);
        assertEquals("Orchestrator name should be preserved", "Test Orchestrator", reloadedOrch.getName());
        assertEquals("Should have 1 task", 1, reloadedOrch.getTasks().size());

        Task reloadedTask = reloadedOrch.getTasks().get(0);
        assertEquals("Task name should be preserved", "Test Task Persistence", reloadedTask.getName());
        assertEquals("Scheduled time should be preserved", "14:30", reloadedTask.getScheduledTime());
        assertEquals("Status should be preserved", TaskStatus.PENDING, reloadedTask.getStatus());
        assertTrue("Selection should be preserved", reloadedTask.isSelected());
    }
}
