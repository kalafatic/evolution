package eu.kalafatic.evolution.controller.tests;

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

            File specialDir = new File(tempRoot, "orchestrator/best_practices/special");
            assertTrue("Special directory should exist", specialDir.exists());
            assertTrue("Special directory should be a directory", specialDir.isDirectory());

            File iterativeLoop = new File(specialDir, "iterative_loop.md");
            assertTrue("iterative_loop.md should exist", iterativeLoop.exists());
            String iterativeContent = service.getSpecialContext("iterative_loop.md");
            assertTrue("Content should contain OBSERVE", iterativeContent.contains("OBSERVE"));

            File selfDev = new File(specialDir, "self_development.md");
            assertTrue("self_development.md should exist", selfDev.exists());
            String selfDevContent = service.getSpecialContext("self_development.md");
            assertTrue("Content should contain Autonomous", selfDevContent.contains("Autonomous"));

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
