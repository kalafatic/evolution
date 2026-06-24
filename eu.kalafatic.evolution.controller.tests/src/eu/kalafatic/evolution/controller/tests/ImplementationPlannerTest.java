package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.*;
import org.json.JSONObject;
import org.junit.Test;
import eu.kalafatic.evolution.controller.orchestration.selfdev.BranchVariant;
import eu.kalafatic.evolution.controller.orchestration.selfdev.ImplementationPlanner;
import eu.kalafatic.evolution.model.orchestration.Task;
import java.util.List;

public class ImplementationPlannerTest {

    @Test
    public void testPlanTasks() {
        ImplementationPlanner planner = new ImplementationPlanner();
        BranchVariant variant = new BranchVariant();
        variant.setStrategy("Test Strategy");

        BranchVariant.Action action = new BranchVariant.Action();
        action.setDomain("file");
        action.setOperation("WRITE");
        action.setTarget("Test.java");
        action.setDescription("Test Description");
        action.setImplementation("public class Test {}");
        variant.getActions().add(action);

        List<Task> tasks = planner.planTasks(variant);

        assertEquals(1, tasks.size());
        Task task = tasks.get(0);
        assertEquals("WRITE Test.java", task.getName());
        assertEquals("file", task.getType());
        assertEquals("public class Test {}", task.getResponse());
    }

    @Test
    public void testValidate() {
        ImplementationPlanner planner = new ImplementationPlanner();
        BranchVariant variant = new BranchVariant();

        // Invalid variant (missing everything)
        assertFalse(planner.validate(variant));

        variant.setStrategy("Valid Strategy long enough");
        variant.setSemanticAnchor("Valid Anchor long enough");

        BranchVariant.Action action = new BranchVariant.Action();
        action.setDomain("file");
        action.setOperation("WRITE");
        action.setTarget("Test.java");
        action.setDescription("Test Description");
        action.setImplementation("public class Test {}");
        variant.getActions().add(action);

        assertTrue(planner.validate(variant));
    }

    @Test
    public void testValidateProhibitedTarget() {
        ImplementationPlanner planner = new ImplementationPlanner();
        BranchVariant variant = new BranchVariant();
        variant.setStrategy("Valid Strategy long enough");
        variant.setSemanticAnchor("Valid Anchor long enough");

        BranchVariant.Action action = new BranchVariant.Action();
        action.setDomain("file");
        action.setOperation("WRITE");
        action.setTarget(".");
        action.setDescription("Test Description");
        action.setImplementation("public class Test {}");
        variant.getActions().add(action);

        assertFalse(planner.validate(variant));
    }
}
