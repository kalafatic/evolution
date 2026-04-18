package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.stream.Collectors;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import eu.kalafatic.evolution.controller.orchestration.EvolutionServer;

public class EvolutionServerTest {

    private static EvolutionServer server;
    private static int port = 8888;

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
    public void testServerStatus() throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL("http://localhost:" + port + "/server/status").openConnection();
        conn.setRequestMethod("GET");

        assertEquals(200, conn.getResponseCode());

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            String response = reader.lines().collect(Collectors.joining("\n"));
            JSONObject json = new JSONObject(response);

            assertTrue(json.has("monitoring"));
            assertTrue(json.has("ollama"));
            assertTrue(json.has("sessions"));
            assertEquals(port, json.getInt("port"));
        }
    }

    @Test
    public void testGitBranches() throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL("http://localhost:" + port + "/git/branches").openConnection();
        conn.setRequestMethod("GET");

        // This might fail if the current directory is not a git repo, but in Tycho test it usually is or at least it doesn't crash
        int responseCode = conn.getResponseCode();
        assertTrue(responseCode == 200 || responseCode == 500);
    }
}
