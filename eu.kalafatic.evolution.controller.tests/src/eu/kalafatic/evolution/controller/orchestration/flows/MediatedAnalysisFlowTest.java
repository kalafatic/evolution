package eu.kalafatic.evolution.controller.orchestration.flows;

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
        assertTrue(response.getSummary().contains("Mediated Analysis Complete"));
        assertTrue(orchestrator.getTasks().size() >= 3);
    }
}
