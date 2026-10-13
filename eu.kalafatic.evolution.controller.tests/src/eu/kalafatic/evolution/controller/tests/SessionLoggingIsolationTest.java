package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import eu.kalafatic.evolution.controller.log.Log;
import eu.kalafatic.evolution.controller.orchestration.SessionContainer;
import eu.kalafatic.evolution.controller.orchestration.SessionManager;

public class SessionLoggingIsolationTest {

    private String sessionA;
    private String sessionB;

    @Before
    public void setUp() {
        sessionA = "TestSessionA_" + UUID.randomUUID().toString().substring(0, 8);
        sessionB = "TestSessionB_" + UUID.randomUUID().toString().substring(0, 8);
    }

    @After
    public void tearDown() {
        SessionManager.getInstance().shutdownSession(sessionA);
        SessionManager.getInstance().shutdownSession(sessionB);
    }

    @Test
    public void testConcurrentSessionLoggingIsolation() throws Exception {
        SessionContainer containerA = SessionManager.getInstance().getOrCreateSession(sessionA);
        SessionContainer containerB = SessionManager.getInstance().getOrCreateSession(sessionB);

        assertNotNull(containerA.getSessionLogger());
        assertNotNull(containerB.getSessionLogger());

        File logFileA = containerA.getSessionLogger().getLogFile();
        File logFileB = containerB.getSessionLogger().getLogFile();

        String msgA = "UNIQUE_MESSAGE_FOR_SESSION_A_" + System.currentTimeMillis();
        String msgB = "UNIQUE_MESSAGE_FOR_SESSION_B_" + System.currentTimeMillis();

        Log.log(sessionA, msgA);
        Log.log(sessionB, msgB);

        // Allow file handlers to flush
        containerA.getSessionLogger().log("Flush A");
        containerB.getSessionLogger().log("Flush B");

        assertTrue("Log file A should exist", logFileA.exists());
        assertTrue("Log file B should exist", logFileB.exists());

        List<String> linesA = Files.readAllLines(logFileA.toPath());
        List<String> linesB = Files.readAllLines(logFileB.toPath());

        String contentA = String.join("\n", linesA);
        String contentB = String.join("\n", linesB);

        assertTrue("Session A log file must contain Session A message", contentA.contains(msgA));
        assertFalse("Session A log file must NOT contain Session B message", contentA.contains(msgB));

        assertTrue("Session B log file must contain Session B message", contentB.contains(msgB));
        assertFalse("Session B log file must NOT contain Session A message", contentB.contains(msgA));
    }
}
