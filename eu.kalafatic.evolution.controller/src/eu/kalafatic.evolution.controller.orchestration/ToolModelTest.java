package eu.kalafatic.evolution.controller.orchestration;

import java.io.File;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;

public class ToolModelTest {
    public static void main(String[] args) {
        try {
            Orchestrator orchestrator = OrchestrationFactory.eINSTANCE.createOrchestrator();
            orchestrator.setGit(OrchestrationFactory.eINSTANCE.createGit());
            orchestrator.setMaven(OrchestrationFactory.eINSTANCE.createMaven());
            orchestrator.setFileConfig(OrchestrationFactory.eINSTANCE.createFileConfig());
            orchestrator.setDatabase(OrchestrationFactory.eINSTANCE.createDatabase());

            System.out.println("Testing model persistence attributes...");

            orchestrator.getGit().setTestStatus("SUCCESS");
            if (!"SUCCESS".equals(orchestrator.getGit().getTestStatus())) throw new Exception("Git status failed");

            orchestrator.getMaven().setTestStatus("FAILED");
            if (!"FAILED".equals(orchestrator.getMaven().getTestStatus())) throw new Exception("Maven status failed");

            orchestrator.getFileConfig().setTestStatus("SUCCESS");
            if (!"SUCCESS".equals(orchestrator.getFileConfig().getTestStatus())) throw new Exception("File status failed");

            orchestrator.getDatabase().setTestStatus("SUCCESS");
            if (!"SUCCESS".equals(orchestrator.getDatabase().getTestStatus())) throw new Exception("DB status failed");

            System.out.println("All status attributes working OK in EMF models.");
            System.out.println("MODEL TESTS PASSED");
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
