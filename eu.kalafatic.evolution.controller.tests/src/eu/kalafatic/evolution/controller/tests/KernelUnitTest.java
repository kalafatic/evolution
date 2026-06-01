package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.*;
import java.io.File;
import java.nio.file.Files;
import org.junit.Before;
import org.junit.Test;
import eu.kalafatic.evolution.controller.orchestration.*;
import eu.kalafatic.evolution.model.orchestration.*;

public class KernelUnitTest {
    @Test
    public void testInitialization() throws Exception {
        File tempDir = Files.createTempDirectory("kernel-test").toFile();
        Orchestrator orchestrator = OrchestrationFactory.eINSTANCE.createOrchestrator();
        TaskContext context = new TaskContext(orchestrator, tempDir);
        SessionContainer session = SessionManager.getInstance().getOrCreateSession(context.getSessionId());
        IterationManager manager = KernelFactory.create(context, session);
        assertNotNull(manager);
        assertEquals(session, manager.getSessionContainer());
    }
}
