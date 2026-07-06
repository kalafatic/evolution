package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.stream.Collectors;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import eu.kalafatic.evolution.controller.orchestration.EvolutionServer;

public class ResourceServingTest {

    private static EvolutionServer server;
    private static int port = 8889;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        server = new EvolutionServer(port);
        server.startServer();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        if (server != null) {
            server.stopServer();
        }
    }

    @Test
    public void testForgeVizResource() throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL("http://localhost:" + port + "/forge-viz/viz-interactive-demos.js?runtime=SWT").openConnection();
        conn.setRequestMethod("GET");

        assertEquals("Resource should be found", 200, conn.getResponseCode());
        assertTrue("Should be javascript", conn.getContentType().startsWith("application/javascript"));

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            String response = reader.lines().collect(Collectors.joining("\n"));
            assertTrue("Should contain new function name", response.contains("renderInteractiveLlmEvoDemo"));
        }
    }
}
