package eu.kalafatic.evolution.controller.tests.harness;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.sun.net.httpserver.HttpServer;

import eu.kalafatic.evolution.controller.harness.CodingHarnessAdapter;
import eu.kalafatic.evolution.controller.harness.DStudioCodingHarnessAdapter;

/**
 * Tests for the EVO → DStudio TEIP 1.1 connector (Phase 4, WP-4.10). Uses a stub DStudio
 * TEIP server to verify the wire behavior of the adapter without a real DStudio runtime;
 * live cross-product verification happens in the Phase 4 end-to-end suite.
 */
public class DStudioCodingHarnessAdapterTest {

    private HttpServer stubServer;
    private int stubPort;
    private final AtomicReference<String> stubStatus = new AtomicReference<>("COMPLETED");
    private final List<String> stubModifiedFiles = new CopyOnWriteArrayList<>();
    private final AtomicBoolean cancelReceived = new AtomicBoolean(false);
    private final AtomicReference<String> receivedInstruction =
            new AtomicReference<>("");

    @Before
    public void setUp() throws Exception {
        stubModifiedFiles.clear();
        stubModifiedFiles.add("src/Changed.java");
        stubStatus.set("COMPLETED");
        cancelReceived.set(false);
        receivedInstruction.set("");
        stubServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        stubServer.createContext("/teip/v1/health", exchange -> {
            respond(exchange, 200,
                    "{\"protocol\":\"TEIP\",\"version\":\"1.1\",\"status\":\"UP\","
                    + "\"application\":\"DSTUDIO\",\"ready\":true}");
        });
        stubServer.createContext("/teip/v1/executions", exchange -> {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/teip/v1/executions") && "POST".equals(
                    exchange.getRequestMethod())) {
                String body = new String(exchange.getRequestBody().readAllBytes(),
                        StandardCharsets.UTF_8);
                receivedInstruction.set(body.contains("instruction") ? body : body);
                respond(exchange, 202,
                        "{\"protocol\":\"TEIP\",\"version\":\"1.1\","
                        + "\"executionId\":\"exec-stub-1\",\"status\":\"QUEUED\","
                        + "\"links\":{}}");
                return;
            }
            respond(exchange, 404, "{}");
        });
        stubServer.createContext("/teip/v1/executions/exec-stub-1", exchange -> {
            respond(exchange, 200,
                    "{\"protocol\":\"TEIP\",\"version\":\"1.1\","
                    + "\"executionId\":\"exec-stub-1\",\"status\":\""
                    + stubStatus.get() + "\",\"progress\":1.0}");
        });
        stubServer.createContext("/teip/v1/executions/exec-stub-1/events", exchange -> {
            respond(exchange, 200,
                    "{\"protocol\":\"TEIP\",\"version\":\"1.1\","
                    + "\"executionId\":\"exec-stub-1\","
                    + "\"events\":[{\"eventId\":\"evt-1\",\"executionId\":\"exec-stub-1\","
                    + "\"sequence\":1,\"type\":\"EXECUTION_LOG\","
                    + "\"message\":\"stub log line\"}],"
                    + "\"hasMore\":false,\"nextSequence\":2}");
        });
        stubServer.createContext("/teip/v1/executions/exec-stub-1/result", exchange -> {
            respond(exchange, 200,
                    "{\"protocol\":\"TEIP\",\"version\":\"1.1\","
                    + "\"executionId\":\"exec-stub-1\",\"status\":\"COMPLETED\","
                    + "\"summary\":\"stub summary\",\"patch\":\"diff --git\","
                    + "\"modifiedFiles\":[\"src/Changed.java\"]}");
        });
        stubServer.createContext("/teip/v1/executions/exec-stub-1/cancel", exchange -> {
            cancelReceived.set(true);
            respond(exchange, 200,
                    "{\"protocol\":\"TEIP\",\"version\":\"1.1\","
                    + "\"executionId\":\"exec-stub-1\",\"status\":\"CANCELLED\","
                    + "\"message\":\"cancelled\"}");
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

    private DStudioCodingHarnessAdapter adapter() {
        DStudioCodingHarnessAdapter adapter = new DStudioCodingHarnessAdapter();
        adapter.updateConnectionConfiguration(Map.of(
                "endpoint", "http://127.0.0.1:" + stubPort,
                "timeoutSeconds", "60"));
        return adapter;
    }

    @Test
    public void testIdentityAndCapabilities() {
        DStudioCodingHarnessAdapter adapter = adapter();
        assertEquals("dstudio", adapter.getApplicationId());
        assertEquals("DSTUDIO", adapter.getApplicationType());
        assertTrue(adapter.getCapabilities().contains("CODE_IMPLEMENTATION"));
    }

    @Test
    public void testConnectionTestAgainstStub() {
        DStudioCodingHarnessAdapter adapter = adapter();
        CodingHarnessAdapter.ConnectionTestResult result = adapter.testConnection();
        assertTrue(result.message(), result.success());
    }

    @Test
    public void testConnectionTestFailsWhenUnreachable() {
        DStudioCodingHarnessAdapter adapter = new DStudioCodingHarnessAdapter();
        adapter.updateConnectionConfiguration(Map.of(
                "endpoint", "http://127.0.0.1:1",
                "timeoutSeconds", "5"));
        assertFalse(adapter.testConnection().success());
    }

    @Test
    public void testExecuteTaskCompletesWithFiles() throws Exception {
        DStudioCodingHarnessAdapter adapter = adapter();
        File target = new File(System.getProperty("java.io.tmpdir"));
        List<String> logs = new ArrayList<>();
        CodingHarnessAdapter.HarnessExecutionResult result = adapter.executeTask(
                target, "Do something", "Do something", logs::add, () -> false,
                Duration.ofSeconds(30));
        assertTrue(result.errorMessage(), result.success());
        assertEquals(1, result.modifiedFiles().size());
        assertEquals("src/Changed.java", result.modifiedFiles().get(0));
        assertTrue(result.summary().contains("stub summary"));
        assertTrue(logs.stream().anyMatch(l -> l.contains("stub log line")));
        assertTrue(receivedInstruction.get().contains("Do something"));
        assertTrue(receivedInstruction.get().contains("workingDirectory"));
    }

    @Test
    public void testFailurePropagates() throws Exception {
        stubStatus.set("FAILED");
        DStudioCodingHarnessAdapter adapter = adapter();
        File target = new File(System.getProperty("java.io.tmpdir"));
        CodingHarnessAdapter.HarnessExecutionResult result = adapter.executeTask(
                target, "Do something", "Do something", l -> {}, () -> false,
                Duration.ofSeconds(30));
        assertFalse(result.success());
    }

    @Test
    public void testCancellationSendsCancelRequest() throws Exception {
        stubStatus.set("RUNNING");
        DStudioCodingHarnessAdapter adapter = adapter();
        File target = new File(System.getProperty("java.io.tmpdir"));
        AtomicBoolean cancelFlag = new AtomicBoolean(false);
        Thread canceller = new Thread(() -> {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            cancelFlag.set(true);
            stubStatus.set("CANCELLED");
        });
        canceller.setDaemon(true);
        canceller.start();
        CodingHarnessAdapter.HarnessExecutionResult result = adapter.executeTask(
                target, "Do something", "Do something", l -> {}, cancelFlag::get,
                Duration.ofSeconds(30));
        assertTrue(result.cancelled());
        assertTrue(cancelReceived.get());
    }

    @Test
    public void testConfigurationMasksToken() {
        DStudioCodingHarnessAdapter adapter = new DStudioCodingHarnessAdapter();
        adapter.updateConnectionConfiguration(Map.of("authToken", "topsecret"));
        Map<String, String> config = adapter.getConnectionConfiguration();
        assertFalse(config.get("authToken").contains("topsecret"));
    }
}
