package eu.kalafatic.evolution.controller.tests.harness;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.sun.net.httpserver.HttpServer;

import eu.kalafatic.evolution.controller.harness.CodingHarnessAdapter;
import eu.kalafatic.evolution.controller.harness.DCodeCodingHarnessAdapter;

/**
 * EVO → DCode harness adapter wire tests (Phase 6, WP-6.5). Stub DCode /api/v1 server;
 * live cross-product verification runs against the real DCode headless runtime.
 */
public class DCodeCodingHarnessAdapterTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private HttpServer stubServer;
    private int stubPort;
    private final AtomicReference<String> stubState = new AtomicReference<>("COMPLETED");
    private final AtomicBoolean cancelReceived = new AtomicBoolean(false);

    @Before
    public void setUp() throws Exception {
        stubState.set("COMPLETED");
        cancelReceived.set(false);
        stubServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        stubServer.createContext("/health", exchange -> respond(exchange, 200,
                "{\"application\":\"DCODE\",\"version\":\"0.1.0\",\"ready\":true,"
                        + "\"status\":\"UP\"}"));
        stubServer.createContext("/api/v1/workspaces", exchange -> {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/api/v1/workspaces")) {
                respond(exchange, 200, "{\"id\":\"ws-stub-1\",\"name\":\"stub\"}");
                return;
            }
            if (path.equals("/api/v1/workspaces/ws-stub-1/repositories")) {
                respond(exchange, 200, "{\"id\":\"ws-stub-1\",\"repositories\":[]}");
                return;
            }
            respond(exchange, 404, "{}");
        });
        stubServer.createContext("/api/v1/sessions", exchange -> {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/api/v1/sessions")) {
                respond(exchange, 200, "{\"id\":\"sess-stub-1\",\"state\":\"CREATED\"}");
                return;
            }
            if (path.equals("/api/v1/sessions/sess-stub-1/start")) {
                respond(exchange, 200, "{\"status\":\"accepted\"}");
                return;
            }
            if (path.equals("/api/v1/sessions/sess-stub-1/cancel")) {
                cancelReceived.set(true);
                respond(exchange, 200, "{\"status\":\"accepted\"}");
                return;
            }
            if (path.equals("/api/v1/sessions/sess-stub-1")) {
                respond(exchange, 200, "{\"id\":\"sess-stub-1\",\"state\":\""
                        + stubState.get() + "\"}");
                return;
            }
            if (path.equals("/api/v1/sessions/sess-stub-1/messages")) {
                respond(exchange, 200,
                        "[{\"role\":\"user\",\"content\":\"task\"},"
                        + "{\"role\":\"assistant\",\"content\":\"stub summary\"}]");
                return;
            }
            if (path.equals("/api/v1/sessions/sess-stub-1/changes")) {
                respond(exchange, 200,
                        "{\"sessionId\":\"sess-stub-1\",\"repositories\":[{\"name\":\"repo\","
                        + "\"changedFiles\":[\"src/Changed.java\"]}]}");
                return;
            }
            respond(exchange, 404, "{}");
        });
        stubServer.start();
        stubPort = stubServer.getAddress().getPort();
    }

    @After
    public void tearDown() {
        stubServer.stop(0);
    }

    private void respond(com.sun.net.httpserver.HttpExchange exchange, int status,
            String body) throws java.io.IOException {
        byte[] payload = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, payload.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(payload);
        }
    }

    private DCodeCodingHarnessAdapter adapter() {
        DCodeCodingHarnessAdapter adapter = new DCodeCodingHarnessAdapter();
        adapter.updateConnectionConfiguration(Map.of(
                "endpoint", "http://127.0.0.1:" + stubPort,
                "timeoutSeconds", "60"));
        return adapter;
    }

    @Test
    public void identityAndCapabilities() {
        DCodeCodingHarnessAdapter adapter = adapter();
        assertEquals("dcode", adapter.getApplicationId());
        assertEquals("DCODE", adapter.getApplicationType());
        assertTrue(adapter.getCapabilities().contains("CODE_IMPLEMENTATION"));
        assertTrue(adapter.getCapabilities().contains("CODE_ANALYSIS"));
    }

    @Test
    public void connectionTestAgainstStub() {
        DCodeCodingHarnessAdapter adapter = adapter();
        assertTrue(adapter.testConnection().success());
    }

    @Test
    public void connectionTestFailsWhenUnreachable() {
        DCodeCodingHarnessAdapter adapter = new DCodeCodingHarnessAdapter();
        adapter.updateConnectionConfiguration(Map.of(
                "endpoint", "http://127.0.0.1:1",
                "timeoutSeconds", "5"));
        assertFalse(adapter.testConnection().success());
    }

    @Test
    public void executeTaskCompletesWithFilesAndLogs() throws Exception {
        java.io.File targetDir = tempFolder.newFolder("dcode-target");
        java.nio.file.Files.writeString(targetDir.toPath().resolve("pom.xml"), "<project/>");
        DCodeCodingHarnessAdapter adapter = adapter();
        List<String> logs = new ArrayList<>();
        CodingHarnessAdapter.HarnessExecutionResult result = adapter.executeTask(
                targetDir, "instructions", "prompt", logs::add, () -> false,
                Duration.ofSeconds(30));
        assertTrue(result.success());
        assertEquals(1, result.modifiedFiles().size());
        assertEquals("src/Changed.java", result.modifiedFiles().get(0));
        assertTrue(result.summary().contains("stub summary"));
        assertTrue(logs.stream().anyMatch(l -> l.contains("stub summary")));
    }

    @Test
    public void waitingForApprovalIsReportedAsCompletedWithPendingReview() throws Exception {
        stubState.set("WAITING_FOR_APPROVAL");
        java.io.File targetDir = tempFolder.newFolder("dcode-target");
        DCodeCodingHarnessAdapter adapter = adapter();
        CodingHarnessAdapter.HarnessExecutionResult result = adapter.executeTask(
                targetDir, "instructions", "prompt", l -> {}, () -> false,
                Duration.ofSeconds(30));
        assertTrue(result.success());
        assertTrue(result.summary().contains("pending review"));
        assertEquals(1, result.modifiedFiles().size());
    }

    @Test
    public void failurePropagates() throws Exception {
        stubState.set("FAILED");
        java.io.File targetDir = tempFolder.newFolder("dcode-target");
        DCodeCodingHarnessAdapter adapter = adapter();
        CodingHarnessAdapter.HarnessExecutionResult result = adapter.executeTask(
                targetDir, "instructions", "prompt", l -> {}, () -> false,
                Duration.ofSeconds(30));
        assertFalse(result.success());
    }

    @Test
    public void cancellationSendsCancelRequest() throws Exception {
        stubState.set("RUNNING");
        java.io.File targetDir = tempFolder.newFolder("dcode-target");
        DCodeCodingHarnessAdapter adapter = adapter();
        AtomicBoolean cancelFlag = new AtomicBoolean(false);
        Thread canceller = new Thread(() -> {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            cancelFlag.set(true);
            stubState.set("CANCELLED");
        });
        canceller.setDaemon(true);
        canceller.start();
        CodingHarnessAdapter.HarnessExecutionResult result = adapter.executeTask(
                targetDir, "instructions", "prompt", l -> {}, cancelFlag::get,
                Duration.ofSeconds(30));
        assertTrue(result.cancelled());
        assertTrue(cancelReceived.get());
    }

    @Test
    public void missingTargetDirIsRejected() {
        DCodeCodingHarnessAdapter adapter = adapter();
        CodingHarnessAdapter.HarnessExecutionResult result = adapter.executeTask(
                tempFolder.getRoot().toPath().resolve("missing").toFile(), "i", "p",
                l -> {}, () -> false, Duration.ofSeconds(30));
        assertFalse(result.success());
        assertTrue(result.errorMessage().contains("does not exist"));
    }

    @Test
    public void configurationMasksToken() {
        DCodeCodingHarnessAdapter adapter = new DCodeCodingHarnessAdapter();
        adapter.updateConnectionConfiguration(Map.of("authToken", "topsecret"));
        Map<String, String> config = adapter.getConnectionConfiguration();
        assertFalse(config.get("authToken").contains("topsecret"));
    }
}
