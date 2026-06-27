package eu.kalafatic.evolution.controller.tests;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;
import eu.kalafatic.evolution.controller.orchestration.SessionManager;

import static org.junit.Assert.*;
import java.io.File;
import java.nio.file.Files;
import org.junit.Test;
import eu.kalafatic.evolution.controller.services.BestPracticesService;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;

public class BestPracticesServiceTest {

    @Test
    public void testInitialization() throws Exception {
        File tempRoot = Files.createTempDirectory("bp-test").toFile();
        try {
            BestPracticesService service = new BestPracticesService(OrchestrationFactory.eINSTANCE.createOrchestrator(), tempRoot);

            // Verify defaults are returned even if files do not exist
            String iterativeContent = service.getSpecialContext("iterative_loop.md");
            assertTrue("Content should contain OBSERVE", iterativeContent.contains("OBSERVE"));

            String selfDevContent = service.getSpecialContext("self_development.md");
            assertTrue("Content should contain Autonomous", selfDevContent.contains("Autonomous"));

            // Verify file-based override
            File specialDir = new File(tempRoot, "orchestrator/best_practices/special");
            specialDir.mkdirs();
            File iterativeLoop = new File(specialDir, "iterative_loop.md");
            String customContent = "CUSTOM LOOP CONTENT";
            Files.write(iterativeLoop.toPath(), customContent.getBytes());

            String overriddenContent = service.getSpecialContext("iterative_loop.md");
            assertEquals("Content should be overridden by file", customContent, overriddenContent);

        } finally {
            deleteDirectory(tempRoot);
        }
    }

    private void deleteDirectory(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) deleteDirectory(f);
                else f.delete();
            }
        }
        dir.delete();
    }
}
