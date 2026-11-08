package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import eu.kalafatic.evolution.controller.orchestration.EvolutionServer;
import eu.kalafatic.evolution.controller.orchestration.mutation.MutationSession;
import eu.kalafatic.evolution.controller.orchestration.mutation.MutationSessionManager;
import eu.kalafatic.evolution.controller.vcs.GitVersionControlProvider;

/**
 * End-to-end integration test demonstrating the full local Git workflow for Mutation agent:
 * Create session -> select repository -> request modification -> agent investigates ->
 * agent proposes changes -> diff review -> approval -> build/test -> correction -> explicit commit.
 */
public class MutationIntegrationTest {

    private static EvolutionServer server;
    private static final int TEST_PORT = 28080;
    private static final String BASE_URL = "http://localhost:" + TEST_PORT;

    private File tempRepo;
    private GitVersionControlProvider gitProvider;
    private MutationSessionManager sessionManager;

    @BeforeClass
    public static void startServer() throws Exception {
        try {
            server = new EvolutionServer(TEST_PORT);
            server.startServer();
        } catch (Exception e) {
            // Server might already be running or port busy, fallback to existing instance
        }
    }

    @AfterClass
    public static void stopServer() throws Exception {
        if (server != null) {
            try {
                server.stopServer();
            } catch (Exception ignored) {}
        }
    }

    @Before
    public void setUp() throws Exception {
        sessionManager = MutationSessionManager.getInstance();
        sessionManager.clearAllSessions();

        tempRepo = Files.createTempDirectory("evo-mutation-repo-test").toFile();
        gitProvider = new GitVersionControlProvider();

        // Initialize local git repo using GitManager
        eu.kalafatic.evolution.controller.orchestration.selfdev.GitManager gm =
                new eu.kalafatic.evolution.controller.orchestration.selfdev.GitManager(tempRepo);
        gm.ensureInitialCommit();

        File mathFile = new File(tempRepo, "MathService.java");
        Files.writeString(mathFile.toPath(), "public class MathService { public int multiply(int a, int b) { return a * b; } }");

        File pomFile = new File(tempRepo, "pom.xml");
        Files.writeString(pomFile.toPath(), "<project><modelVersion>4.0.0</modelVersion><groupId>test</groupId><artifactId>mutation-test</artifactId><version>1.0</version></project>");

        gitProvider.commitChanges(tempRepo, "Initial project structure");
    }

    @After
    public void tearDown() throws Exception {
        sessionManager.clearAllSessions();
        if (tempRepo != null && tempRepo.exists()) {
            deleteDirectory(tempRepo);
        }
    }

    private void deleteDirectory(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) deleteDirectory(f);
        }
        dir.delete();
    }

    private JSONObject postJson(String endpoint, JSONObject payload) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(BASE_URL + endpoint).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(payload.toString().getBytes(StandardCharsets.UTF_8));
        }

        int code = conn.getResponseCode();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream(), StandardCharsets.UTF_8))) {
            String resp = reader.lines().collect(Collectors.joining("\n"));
            return new JSONObject(resp.isEmpty() ? "{}" : resp);
        }
    }

    private JSONObject getJson(String endpoint) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(BASE_URL + endpoint).openConnection();
        conn.setRequestMethod("GET");

        int code = conn.getResponseCode();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream(), StandardCharsets.UTF_8))) {
            String resp = reader.lines().collect(Collectors.joining("\n"));
            if (resp.startsWith("[")) {
                JSONObject wrapped = new JSONObject();
                wrapped.put("array", new JSONArray(resp));
                return wrapped;
            }
            return new JSONObject(resp.isEmpty() ? "{}" : resp);
        }
    }

    @Test
    public void testEndToEndLocalGitWorkflow() throws Exception {
        // Step 1: Create session & select repository
        String sessionId = "mutation-test-" + System.currentTimeMillis();
        MutationSession session = sessionManager.createSession(tempRepo.getAbsolutePath(), "main", sessionId);
        assertNotNull(session);
        assertEquals(sessionId, session.getSessionId());
        assertEquals("main", session.getBranch());
        assertEquals(MutationSession.Status.CREATED, session.getStatus());
        assertTrue(session.getWorkspaceDir().exists());

        // Verify repository-agnostic project detection
        String arch = session.detectProjectArchitecture();
        assertTrue(arch.contains("Maven"));

        // Step 2: Request modification / Agent investigates
        session.setStatus(MutationSession.Status.INVESTIGATING);
        session.addLog("Agent investigating MathService multiply logic");
        assertEquals(MutationSession.Status.INVESTIGATING, session.getStatus());

        // Step 3: Agent proposes changes (sensitive change held for approval)
        String updatedContent = "public class MathService {\n" +
                "    public int multiply(int a, int b) { return a * b; }\n" +
                "    public int divide(int a, int b) { if (b == 0) throw new IllegalArgumentException(\"Zero divisor\"); return a / b; }\n" +
                "}";

        MutationSession.FileModification proposedMod = new MutationSession.FileModification("MathService.java", "MODIFY", updatedContent);
        session.setProposedChanges(Collections.singletonList(proposedMod));

        assertEquals(MutationSession.Status.WAITING_APPROVAL, session.getStatus());
        assertFalse(session.isApproved());
        assertEquals(1, session.getProposedChanges().size());

        // Step 4: First-class Diff review prior to application
        // Unapproved changes must not touch workspace yet
        File workspaceFile = new File(session.getWorkspaceDir(), "MathService.java");
        assertTrue(workspaceFile.exists());
        String initialContent = Files.readString(workspaceFile.toPath());
        assertFalse(initialContent.contains("divide"));

        // Step 5: Explicit approval before applying sensitive changes
        session.setApproved(true);
        assertTrue(session.isApproved());
        assertEquals(MutationSession.Status.APPROVED, session.getStatus());

        session.applyProposedChanges();

        // Verify workspace is updated
        String appliedContent = Files.readString(workspaceFile.toPath());
        assertTrue(appliedContent.contains("divide"));

        // Step 6: Diff review after application
        String diff = session.getDiff();
        assertNotNull(diff);
        assertTrue(diff.contains("+    public int divide"));

        List<String> changedFiles = session.getChangedFiles();
        assertFalse(changedFiles.isEmpty());

        // Step 7: Build & test execution
        String buildOutput = session.executeBuildAndTest();
        assertNotNull(buildOutput);
        assertNotNull(session.getLastBuildStatus());

        // Step 8: Explicit commit with audit trail
        String commitMsg = "feat: add divide operation to MathService";
        session.commit(commitMsg);

        assertEquals(MutationSession.Status.COMMITTED, session.getStatus());

        // Step 9: Verify clean working directory after commit
        List<String> remainingChanges = session.getChangedFiles();
        assertTrue(remainingChanges.isEmpty());

        // Verify audit log entries
        List<String> auditLogs = session.getAuditLogs();
        assertTrue(auditLogs.stream().anyMatch(l -> l.contains("Explicit Git commit executed")));
    }

    @Test
    public void testMutationServerEndpoints() throws Exception {
        // Test GET /mutation web workstation page
        HttpURLConnection conn = (HttpURLConnection) new URL(BASE_URL + "/mutation").openConnection();
        conn.setRequestMethod("GET");
        assertEquals(200, conn.getResponseCode());

        String html;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            html = reader.lines().collect(Collectors.joining("\n"));
        }
        assertTrue(html.contains("EVO Mutation"));
        assertTrue(html.contains("evo-global-nav"));

        // Test POST /mutation/session
        String testSid = "api-mutation-" + System.currentTimeMillis();
        JSONObject createReq = new JSONObject();
        createReq.put("repoUrl", tempRepo.getAbsolutePath());
        createReq.put("branch", "main");
        createReq.put("sessionId", testSid);

        JSONObject createRes = postJson("/mutation/session", createReq);
        assertEquals("ok", createRes.optString("status"));
        assertEquals(testSid, createRes.optString("sessionId"));

        // Test GET /mutation/session/:id
        JSONObject sessionRes = getJson("/mutation/session/" + testSid);
        assertEquals(testSid, sessionRes.optString("sessionId"));
        assertEquals("CREATED", sessionRes.optString("status"));

        // Test POST /mutation/approve
        JSONObject approveReq = new JSONObject();
        approveReq.put("sessionId", testSid);
        approveReq.put("approve", true);
        JSONObject approveRes = postJson("/mutation/approve", approveReq);
        assertEquals("ok", approveRes.optString("status"));
        assertTrue(approveRes.optBoolean("approved"));

        // Test POST /mutation/cancel
        JSONObject cancelReq = new JSONObject();
        cancelReq.put("sessionId", testSid);
        JSONObject cancelRes = postJson("/mutation/cancel", cancelReq);
        assertEquals("ok", cancelRes.optString("status"));

        JSONObject updatedSession = getJson("/mutation/session/" + testSid);
        assertTrue(updatedSession.optBoolean("isCancelled"));
        assertEquals("CANCELLED", updatedSession.optString("status"));
    }
}
