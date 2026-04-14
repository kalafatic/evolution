package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;

public class TaskContextAutoApproveTest {

    @Test
    public void testAutoApproveFlag() {
        Orchestrator orchestrator = OrchestrationFactory.eINSTANCE.createOrchestrator();
        TaskContext context = new TaskContext(orchestrator, null);

        assertFalse("Initial autoApprove should be false", context.isAutoApprove());

        context.setAutoApprove(true);
        assertTrue("autoApprove should be true after setting it", context.isAutoApprove());

        context.setAutoApprove(false);
        assertFalse("autoApprove should be false after setting it back", context.isAutoApprove());
    }
}
