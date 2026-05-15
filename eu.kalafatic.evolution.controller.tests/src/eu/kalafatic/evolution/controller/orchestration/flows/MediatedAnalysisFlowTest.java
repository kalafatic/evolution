package eu.kalafatic.evolution.controller.orchestration;

import static org.junit.Assert.*;
import java.io.File;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import eu.kalafatic.evolution.controller.orchestration.*;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

public class MediatedAnalysisFlowTest {
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void testExecute() throws Exception {
        folder.newFile("pom.xml");

        Orchestrator orchestrator = OrchestrationFactory.eINSTANCE.createOrchestrator();
        TaskContext context = new TaskContext(orchestrator, folder.getRoot());

        MediatedAnalysisFlow flow = new MediatedAnalysisFlow(null, KernelFactory.create(context, null));
        OrchestratorResponse response = flow.execute("Analyze this project", context);

        assertNotNull(response);
        eu.kalafatic.evolution.controller.log.Log.log("SUMMARY_CONTENT: [" + response.getSummary() + "]");
        eu.kalafatic.evolution.controller.log.Log.log("TASKS_COUNT: " + orchestrator.getTasks().size());

        assertNotNull("Response summary is null", response.getSummary());
        assertTrue("Summary should contain expected phrase", response.getSummary().contains("Mediated Context Export Complete"));
        assertTrue("Task count should be at least 7", orchestrator.getTasks().size() >= 7);
    }
}
