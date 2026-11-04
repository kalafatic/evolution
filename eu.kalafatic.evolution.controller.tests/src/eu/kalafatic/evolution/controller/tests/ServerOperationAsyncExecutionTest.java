package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import eu.kalafatic.evolution.controller.orchestration.EvolutionServer;
import eu.kalafatic.evolution.controller.orchestration.ServerOperation;
import eu.kalafatic.evolution.controller.orchestration.ServerOperationState;
import eu.kalafatic.evolution.controller.orchestration.SessionContainer;
import eu.kalafatic.evolution.controller.orchestration.SessionManager;

public class ServerOperationAsyncExecutionTest {

    private static EvolutionServer server;
    private static final int PORT = 18889;

    @BeforeClass
    public static void setUp() throws Exception {
        server = new EvolutionServer(PORT);
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null) {
            server.stopServer();
        }
    }

    @Test
    public void testServerOperationStateTransitions() {
        ServerOperation op = new ServerOperation("op-test-1", "session-1", "admin", "INFERENCE_TASK");
        assertEquals(ServerOperationState.CREATED, op.getState());
        assertEquals("session-1", op.getSessionId());
        assertEquals("admin", op.getOwnerPrincipal());
        assertNotNull(op.getCreatedAt());

        // CREATED -> QUEUED
        assertTrue(op.transitionTo(ServerOperationState.QUEUED, "Operation queued"));
        assertEquals(ServerOperationState.QUEUED, op.getState());

        // QUEUED -> RUNNING
        assertTrue(op.transitionTo(ServerOperationState.RUNNING, "Operation running"));
        assertEquals(ServerOperationState.RUNNING, op.getState());
        assertNotNull(op.getStartedAt());

        // RUNNING -> WAITING
        assertTrue(op.transitionTo(ServerOperationState.WAITING, "Waiting on user"));
        assertEquals(ServerOperationState.WAITING, op.getState());

        // WAITING -> RUNNING
        assertTrue(op.transitionTo(ServerOperationState.RUNNING, "Resuming execution"));
        assertEquals(ServerOperationState.RUNNING, op.getState());

        // Set progress
        op.setProgress(0.75, "75% done");
        assertEquals(0.75, op.getProgress(), 0.001);
        assertEquals("75% done", op.getStatusMessage());

        // RUNNING -> COMPLETED
        op.complete("Finished successfully", null);
        assertEquals(ServerOperationState.COMPLETED, op.getState());
        assertEquals(1.0, op.getProgress(), 0.001);
        assertNotNull(op.getCompletedAt());
        assertTrue(op.getState().isTerminal());

        // Terminal state must reject further transitions
        try {
            op.transitionTo(ServerOperationState.RUNNING, "Cannot re-run");
            fail("Expected IllegalStateException for transition from terminal COMPLETED state");
        } catch (IllegalStateException e) {
            // Expected
        }
    }

    @Test
    public void testServerOperationFailurePreservesDiagnostics() {
        ServerOperation op = new ServerOperation("op-fail-1", "session-2", "user1", "BUILD");
        op.transitionTo(ServerOperationState.RUNNING, "Building");

        Exception rootCause = new RuntimeException("Compilation failed at line 42");
        op.fail("Build step failure", rootCause);

        assertEquals(ServerOperationState.FAILED, op.getState());
        assertEquals("Build step failure", op.getFailureReason());
        assertSame(rootCause, op.getFailureCause());
        assertNotNull(op.getFailureStackTrace());
        assertTrue(op.getFailureStackTrace().contains("Compilation failed at line 42"));

        JSONObject json = op.toJsonObject();
        assertEquals("op-fail-1", json.getString("operationId"));
        assertEquals("FAILED", json.getString("state"));
        assertTrue(json.getString("failureStackTrace").contains("Compilation failed"));
    }

    @Test
    public void testConcurrentIndependentOperationsInSessionContainer() throws Exception {
        String sid = "test-session-concurrent-" + System.currentTimeMillis();
        SessionContainer container = SessionManager.getInstance().getOrCreateSession(sid);

        ServerOperation op1 = new ServerOperation("op-c-1", sid, "admin", "TASK_A");
        ServerOperation op2 = new ServerOperation("op-c-2", sid, "admin", "TASK_B");

        container.registerOperation(op1);
        container.registerOperation(op2);

        assertEquals(2, container.getOperations().size());
        assertNotNull(SessionManager.getInstance().findOperation("op-c-1"));
        assertNotNull(SessionManager.getInstance().findOperation("op-c-2"));

        CountDownLatch latch = new CountDownLatch(2);

        container.getExecutorService().submit(() -> {
            try {
                op1.transitionTo(ServerOperationState.RUNNING, "Running op1");
                op1.setProgress(0.5, "Working op1");
                Thread.sleep(100);
                op1.complete("Op1 Done", null);
            } catch (Exception e) {
                op1.fail("Op1 error", e);
            } finally {
                latch.countDown();
            }
        });

        container.getExecutorService().submit(() -> {
            try {
                op2.transitionTo(ServerOperationState.RUNNING, "Running op2");
                op2.setProgress(0.5, "Working op2");
                Thread.sleep(150);
                op2.complete("Op2 Done", null);
            } catch (Exception e) {
                op2.fail("Op2 error", e);
            } finally {
                latch.countDown();
            }
        });

        assertTrue("Concurrent operations should complete within timeout", latch.await(2, TimeUnit.SECONDS));
        assertEquals(ServerOperationState.COMPLETED, op1.getState());
        assertEquals(ServerOperationState.COMPLETED, op2.getState());
    }

    @Test
    public void testOperationCancellationReachesRunningTask() throws Exception {
        String sid = "test-session-cancel-" + System.currentTimeMillis();
        SessionContainer container = SessionManager.getInstance().getOrCreateSession(sid);

        ServerOperation op = new ServerOperation("op-cancel-1", sid, "user2", "LONG_JOB");
        container.registerOperation(op);

        CountDownLatch runningLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(1);

        java.util.concurrent.Future<?> future = container.getExecutorService().submit(() -> {
            op.setWorkerThread(Thread.currentThread());
            op.transitionTo(ServerOperationState.RUNNING, "Long job started");
            runningLatch.countDown();
            try {
                Thread.sleep(5000);
                op.complete("Should not complete", null);
            } catch (InterruptedException e) {
                op.markCancelled("Interrupted by cancellation");
            } finally {
                finishLatch.countDown();
            }
        });
        op.setExecutionFuture(future);

        assertTrue("Task should start running", runningLatch.await(2, TimeUnit.SECONDS));
        assertEquals(ServerOperationState.RUNNING, op.getState());

        // Cancel via container
        boolean cancelled = container.cancelOperation("op-cancel-1", "User pressed abort");
        assertTrue(cancelled);
        assertTrue(op.isCancellationRequested());

        assertTrue("Task should acknowledge interruption and finish", finishLatch.await(2, TimeUnit.SECONDS));
        assertEquals(ServerOperationState.CANCELLED, op.getState());
    }

    @Test
    public void testSessionDisposalCleansUpActiveOperations() {
        String sid = "test-session-disposal-" + System.currentTimeMillis();
        SessionContainer container = SessionManager.getInstance().getOrCreateSession(sid);

        ServerOperation op = new ServerOperation("op-disposal-1", sid, "user3", "PERSISTENT_JOB");
        container.registerOperation(op);
        op.transitionTo(ServerOperationState.RUNNING, "Running");

        // Shutdown session
        SessionManager.getInstance().shutdownSession(sid);

        // Operation must have been cancelled to prevent orphaned process
        assertTrue(op.isCancellationRequested());
        assertTrue(op.getState() == ServerOperationState.CANCELLED || op.getState() == ServerOperationState.CANCELLING);
    }

    @Test
    public void testHttpAsyncOperationEndpoints() throws Exception {
        // 1. Submit async task via POST /task
        URL postUrl = new URL("http://localhost:" + PORT + "/task?runtime=SWT");
        HttpURLConnection conn = (HttpURLConnection) postUrl.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json");

        JSONObject postBody = new JSONObject();
        postBody.put("prompt", "Analyze architecture");
        postBody.put("sessionId", "session-http-async-1");

        try (OutputStream os = conn.getOutputStream()) {
            os.write(postBody.toString().getBytes(StandardCharsets.UTF_8));
        }

        assertEquals(200, conn.getResponseCode());
        String responseStr;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            responseStr = br.lines().collect(Collectors.joining("\n"));
        }
        JSONObject resJson = new JSONObject(responseStr);
        assertTrue(resJson.has("operationId"));
        String opId = resJson.getString("operationId");
        assertNotNull(opId);

        // 2. Poll operation status via GET /server/operations/:id
        URL getOpUrl = new URL("http://localhost:" + PORT + "/server/operations/" + opId + "?runtime=SWT");
        HttpURLConnection getOpConn = (HttpURLConnection) getOpUrl.openConnection();
        getOpConn.setRequestMethod("GET");
        assertEquals(200, getOpConn.getResponseCode());

        String opStatusStr;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(getOpConn.getInputStream()))) {
            opStatusStr = br.lines().collect(Collectors.joining("\n"));
        }
        JSONObject opJson = new JSONObject(opStatusStr);
        assertEquals(opId, opJson.getString("operationId"));
        assertEquals("session-http-async-1", opJson.getString("sessionId"));
        assertNotNull(opJson.getString("state"));

        // 3. List operations via GET /server/operations
        URL listUrl = new URL("http://localhost:" + PORT + "/server/operations?sessionId=session-http-async-1&runtime=SWT");
        HttpURLConnection listConn = (HttpURLConnection) listUrl.openConnection();
        listConn.setRequestMethod("GET");
        assertEquals(200, listConn.getResponseCode());

        String listStr;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(listConn.getInputStream()))) {
            listStr = br.lines().collect(Collectors.joining("\n"));
        }
        JSONArray listArr = new JSONArray(listStr);
        assertTrue(listArr.length() >= 1);

        // 4. Cancel operation via POST /server/operations/:id/cancel
        URL cancelUrl = new URL("http://localhost:" + PORT + "/server/operations/" + opId + "/cancel?runtime=SWT");
        HttpURLConnection cancelConn = (HttpURLConnection) cancelUrl.openConnection();
        cancelConn.setRequestMethod("POST");
        cancelConn.setDoOutput(true);
        try (OutputStream os = cancelConn.getOutputStream()) {
            os.write(new JSONObject().put("reason", "Test cancel").toString().getBytes(StandardCharsets.UTF_8));
        }
        assertEquals(200, cancelConn.getResponseCode());
    }
}
