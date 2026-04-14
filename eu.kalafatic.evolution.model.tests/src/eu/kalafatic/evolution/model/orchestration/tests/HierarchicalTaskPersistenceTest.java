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

public class HierarchicalTaskPersistenceTest {

    @Test
    public void testHierarchicalTaskPersistence() throws IOException {
        // Initialize EMF
        OrchestrationPackage.eINSTANCE.eClass();
        ResourceSet resourceSet = new ResourceSetImpl();
        resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("evo", new XMIResourceFactoryImpl());

        // Create temporary file
        File tempFile = File.createTempFile("test-hierarchy", ".evo");
        tempFile.deleteOnExit();
        URI uri = URI.createFileURI(tempFile.getAbsolutePath());

        // 1. Create model with Plan (Thread) and sub-tasks
        Resource resource = resourceSet.createResource(uri);
        Orchestrator orchestrator = OrchestrationFactory.eINSTANCE.createOrchestrator();
        resource.getContents().add(orchestrator);

        Task plan = OrchestrationFactory.eINSTANCE.createTask();
        plan.setId("P-001");
        plan.setName("Main Plan");
        plan.setStatus(TaskStatus.RUNNING);
        orchestrator.getTasks().add(plan);

        Task subTask1 = OrchestrationFactory.eINSTANCE.createTask();
        subTask1.setName("Sub-task 1");
        subTask1.setStatus(TaskStatus.DONE);
        plan.getSubTasks().add(subTask1);

        Task subTask2 = OrchestrationFactory.eINSTANCE.createTask();
        subTask2.setName("Sub-task 2");
        subTask2.setStatus(TaskStatus.PENDING);
        plan.getSubTasks().add(subTask2);

        // 2. Save resource
        resource.save(Collections.EMPTY_MAP);

        // 3. Reload and verify
        ResourceSet reloadRS = new ResourceSetImpl();
        reloadRS.getResourceFactoryRegistry().getExtensionToFactoryMap().put("evo", new XMIResourceFactoryImpl());
        Resource reloadedResource = reloadRS.getResource(uri, true);

        Orchestrator reloadedOrch = (Orchestrator) reloadedResource.getContents().get(0);
        assertEquals(1, reloadedOrch.getTasks().size());

        Task reloadedPlan = reloadedOrch.getTasks().get(0);
        assertEquals("Main Plan", reloadedPlan.getName());
        assertEquals("P-001", reloadedPlan.getId());
        assertEquals(2, reloadedPlan.getSubTasks().size());

        assertEquals("Sub-task 1", reloadedPlan.getSubTasks().get(0).getName());
        assertEquals(TaskStatus.DONE, reloadedPlan.getSubTasks().get(0).getStatus());
        assertEquals("Sub-task 2", reloadedPlan.getSubTasks().get(1).getName());
        assertEquals(TaskStatus.PENDING, reloadedPlan.getSubTasks().get(1).getStatus());
    }
}
