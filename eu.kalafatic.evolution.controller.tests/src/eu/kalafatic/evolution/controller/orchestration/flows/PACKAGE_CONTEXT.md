# PACKAGE CONTEXT

## Directory: git/evolution-240526-ok/eu.kalafatic.evolution.controller.tests/src/eu/kalafatic/evolution/controller/orchestration/flows/

## Domain: general

## Components
* `MediatedExportFlowTest.java`: package eu.kalafatic.evolution.controller.orchestration; import static org.junit.Assert.*; import java.io.File; import org.junit.Rule; import org.junit.Test; import org.junit.rules.TemporaryFolder; import eu.kalafatic.evolution.controller.orchestration.*; import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory; import eu.kalafatic.evolution.model.orchestration.Orchestrator; public class MediatedExportFlowTest { @Rule public TemporaryFolder folder = new TemporaryFolder(); @Test public void testExecute() throws Exception { folder.newFile("pom.xml"); Orchestrator orchestrator = OrchestrationFactory.eINSTANCE.createOrchestrator(); TaskContext context = new TaskContext(orchestrator, folder.getRoot()); MediatedExportFlow flow = new MediatedExportFlow(null, KernelFactory.create(context, null)); OrchestratorResponse response = flow.execute("Analyze this project", context); assertNotNull(response);
