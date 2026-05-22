package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.*;
import java.util.List;
import org.junit.Test;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.selfdev.BranchVariant;
import eu.kalafatic.evolution.controller.orchestration.selfdev.TaskPlanner;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Task;

public class TaskPlannerUnitTest {

    @Test
    public void testGenerateTasksFromVariantWithNullTarget() throws Exception {
        TaskPlanner planner = new TaskPlanner();
        TaskContext context = new TaskContext(OrchestrationFactory.eINSTANCE.createOrchestrator(), null);

        BranchVariant variant = new BranchVariant();
        variant.setStrategy("Test Strategy");

        BranchVariant.Action action = new BranchVariant.Action();
        action.setOperation("WRITE");
        action.setTarget(null); // NULL TARGET
        action.setDomain("file");
        variant.getActions().add(action);

        List<Task> tasks = planner.generateTasksFromVariant(context, variant);

        assertEquals(1, tasks.size());
        assertEquals("WRITE GeneratedArtifact", tasks.get(0).getName());
    }

    @Test
    public void testGenerateTasksFromVariantWithLiteralNullTarget() throws Exception {
        TaskPlanner planner = new TaskPlanner();
        TaskContext context = new TaskContext(OrchestrationFactory.eINSTANCE.createOrchestrator(), null);

        BranchVariant variant = new BranchVariant();
        variant.setStrategy("Test Strategy");

        BranchVariant.Action action = new BranchVariant.Action();
        action.setOperation("WRITE");
        action.setTarget("null"); // LITERAL NULL TARGET
        action.setDomain("file");
        variant.getActions().add(action);

        List<Task> tasks = planner.generateTasksFromVariant(context, variant);

        assertEquals(1, tasks.size());
        assertEquals("WRITE GeneratedArtifact", tasks.get(0).getName());
    }
}
