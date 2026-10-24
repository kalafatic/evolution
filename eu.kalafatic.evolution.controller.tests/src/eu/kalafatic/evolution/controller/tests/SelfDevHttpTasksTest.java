package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import eu.kalafatic.evolution.controller.orchestration.selfdev.AnalyticChatValidationTask;
import eu.kalafatic.evolution.controller.orchestration.selfdev.CreateEvoProjectTask;
import eu.kalafatic.evolution.controller.orchestration.selfdev.CreateInferenceTask;
import eu.kalafatic.evolution.controller.orchestration.selfdev.SelfDevContext;
import eu.kalafatic.evolution.controller.orchestration.selfdev.SelfDevHttpUtils;
import eu.kalafatic.evolution.controller.orchestration.selfdev.TaskResult;
import eu.kalafatic.evolution.controller.orchestration.selfdev.TaskStatus;
import eu.kalafatic.evolution.controller.resource.ResourceManager;

public class SelfDevHttpTasksTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private HttpServer server;
    private int serverPort;
    private File mockRepoRoot;
    private SelfDevContext context;

    @Before
    public void setUp() throws Exception {
        // Create ephemeral HTTP server
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.setExecutor(null);
        server.start();
        serverPort = server.getAddress().getPort();

        ResourceManager.getInstance().setServerPort(serverPort);
        ResourceManager.getInstance().setSupervisorPort(serverPort);

        mockRepoRoot = tempFolder.newFolder("mockEvoRepo");
        File rootPom = new File(mockRepoRoot, "pom.xml");
        try (FileWriter fw = new FileWriter(rootPom)) {
            fw.write("<project><modelVersion>4.0.0</modelVersion><groupId>eu.kalafatic.evolution</groupId><artifactId>eu.kalafatic.evolution.aggregator</artifactId><version>2.6.5-SNAPSHOT</version><packaging>pom</packaging></project>");
        }

        File controllerModelPkg = new File(mockRepoRoot, "eu.kalafatic.evolution.controller/src/eu/kalafatic/evolution/forge/model/llm");
        controllerModelPkg.mkdirs();

        File supervisorPkg = new File(mockRepoRoot, "eu.kalafatic.evolution.supervisor/src/main/java/eu/kalafatic/evolution/supervisor");
        supervisorPkg.mkdirs();

        context = new SelfDevContext(mockRepoRoot, null);
    }

    @After
    public void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    public void testProjectCreationSuccess() throws Exception {
        server.createContext("/server/project/create", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) {
                try {
                    byte[] resp = "{\"status\":\"OK\",\"project\":\"SelfDevProject\"}".getBytes();
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, resp.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(resp);
                    }
                } catch (Exception ignored) {
                }
            }
        });

        CreateEvoProjectTask task = new CreateEvoProjectTask("CREATE_EVO_PROJECT");
        TaskResult result = task.execute(context);

        assertNotNull(result);
        assertEquals(TaskStatus.SUCCESS, result.getStatus());
        assertTrue(result.getMessage().contains("SelfDevProject"));
        assertEquals("SelfDevProject", result.getDiagnostics().get("projectName"));
        assertEquals(String.valueOf(serverPort), result.getDiagnostics().get("port"));
    }

    @Test
    public void testProjectCreationAlternativeSuccess() throws Exception {
        server.createContext("/server/project/create", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) {
                try {
                    byte[] resp = "{\"status\":\"OK\",\"projectName\":\"SelfDevProject\"}".getBytes();
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, resp.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(resp);
                    }
                } catch (Exception ignored) {
                }
            }
        });

        CreateEvoProjectTask task = new CreateEvoProjectTask("CREATE_EVO_PROJECT");
        TaskResult result = task.execute(context);

        assertNotNull(result);
        assertEquals(TaskStatus.SUCCESS, result.getStatus());
    }

    @Test
    public void testProjectCreationHttp500WithBody() throws Exception {
        server.createContext("/server/project/create", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) {
                try {
                    byte[] resp = "{\"status\":\"ERROR\",\"error\":\"Disk full\"}".getBytes();
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(500, resp.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(resp);
                    }
                } catch (Exception ignored) {
                }
            }
        });

        CreateEvoProjectTask task = new CreateEvoProjectTask("CREATE_EVO_PROJECT");
        TaskResult result = task.execute(context);

        assertNotNull(result);
        assertEquals(TaskStatus.FAILED, result.getStatus());
        assertTrue(result.getMessage().contains("Disk full") || result.getMessage().contains("500"));
    }

    @Test
    public void testProjectCreationNullErrorStreamHandling() {
        SelfDevHttpUtils.HttpResponse resp = new SelfDevHttpUtils.HttpResponse(500, null, "text/plain", 100, null, false);
        assertFalse(resp.isHttpSuccess());
        assertFalse(SelfDevHttpUtils.isProjectCreationSuccessful(resp, "SelfDevProject"));
    }

    @Test
    public void testProjectCreationMalformedJson() throws Exception {
        server.createContext("/server/project/create", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) {
                try {
                    byte[] resp = "NOT_JSON_BODY".getBytes();
                    exchange.getResponseHeaders().set("Content-Type", "text/plain");
                    exchange.sendResponseHeaders(200, resp.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(resp);
                    }
                } catch (Exception ignored) {
                }
            }
        });

        CreateEvoProjectTask task = new CreateEvoProjectTask("CREATE_EVO_PROJECT");
        TaskResult result = task.execute(context);

        assertNotNull(result);
        assertEquals(TaskStatus.FAILED, result.getStatus());
    }

    @Test
    public void testProjectCreationConnectionFailure() {
        ResourceManager.getInstance().setServerPort(59999);
        ResourceManager.getInstance().setSupervisorPort(59999);

        CreateEvoProjectTask task = new CreateEvoProjectTask("CREATE_EVO_PROJECT");
        TaskResult result = task.execute(context);

        assertNotNull(result);
        assertEquals(TaskStatus.FAILED, result.getStatus());
        assertTrue(result.getMessage().contains("Failed to connect") || result.getMessage().contains("refused") || result.getMessage().contains("Failed to create"));
    }

    @Test
    public void testProjectCreationThreadInterrupted() {
        AtomicInteger callCount = new AtomicInteger(0);
        server.createContext("/server/project/create", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) {
                callCount.incrementAndGet();
                try {
                    byte[] resp = "{\"status\":\"ERROR\"}".getBytes();
                    exchange.sendResponseHeaders(500, resp.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(resp);
                    }
                } catch (Exception ignored) {
                }
            }
        });

        Thread.currentThread().interrupt();

        CreateEvoProjectTask task = new CreateEvoProjectTask("CREATE_EVO_PROJECT");
        TaskResult result = task.execute(context);

        assertTrue("Thread interrupt flag must be restored", Thread.interrupted());
        assertNotNull(result);
        assertEquals(TaskStatus.SKIPPED, result.getStatus());
        assertTrue(callCount.get() <= 1);
    }

    @Test
    public void testInferenceTaskSubmissionSuccess() throws Exception {
        server.createContext("/task", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) {
                try {
                    byte[] resp = "{\"status\":\"SUCCESS\",\"summary\":\"Inference task submitted\",\"sessionId\":\"SelfDevProject\",\"prompt\":\"hi\"}".getBytes();
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, resp.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(resp);
                    }
                } catch (Exception ignored) {
                }
            }
        });

        CreateInferenceTask task = new CreateInferenceTask("CREATE_INFERENCE_TASK");
        TaskResult result = task.execute(context);

        assertNotNull(result);
        assertEquals(TaskStatus.SUCCESS, result.getStatus());
        assertEquals("SelfDevProject", result.getDiagnostics().get("sessionId"));
        assertEquals("hi", result.getDiagnostics().get("prompt"));
        assertEquals("local", result.getDiagnostics().get("model"));
    }

    @Test
    public void testInferenceTaskSyncSuccess() throws Exception {
        server.createContext("/task", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) {
                try {
                    byte[] resp = "{\"summary\":\"Done\",\"type\":\"SUCCESS\",\"content\":\"Hello user\"}".getBytes();
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, resp.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(resp);
                    }
                } catch (Exception ignored) {
                }
            }
        });

        CreateInferenceTask task = new CreateInferenceTask("CREATE_INFERENCE_TASK");
        TaskResult result = task.execute(context);

        assertNotNull(result);
        assertEquals(TaskStatus.SUCCESS, result.getStatus());
    }

    @Test
    public void testInferenceTaskFailureResponse() throws Exception {
        server.createContext("/task", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) {
                try {
                    byte[] resp = "{\"status\":\"ERROR\",\"message\":\"Model unavailable\"}".getBytes();
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, resp.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(resp);
                    }
                } catch (Exception ignored) {
                }
            }
        });

        CreateInferenceTask task = new CreateInferenceTask("CREATE_INFERENCE_TASK");
        TaskResult result = task.execute(context);

        assertNotNull(result);
        assertEquals(TaskStatus.FAILED, result.getStatus());
    }

    @Test
    public void testInferenceTaskThreadInterrupted() {
        Thread.currentThread().interrupt();

        CreateInferenceTask task = new CreateInferenceTask("CREATE_INFERENCE_TASK");
        TaskResult result = task.execute(context);

        assertTrue("Thread interrupt flag must be restored", Thread.interrupted());
        assertNotNull(result);
        assertEquals(TaskStatus.SKIPPED, result.getStatus());
    }

    @Test
    public void testTaskGraphDependencies() {
        CreateEvoProjectTask createProject = new CreateEvoProjectTask("CREATE_EVO_PROJECT");
        createProject.addDependency("START_EVO");
        assertTrue(createProject.getDependencies().contains("START_EVO"));

        CreateInferenceTask createInference = new CreateInferenceTask("CREATE_INFERENCE_TASK");
        createInference.addDependency("CREATE_EVO_PROJECT");
        assertTrue(createInference.getDependencies().contains("CREATE_EVO_PROJECT"));

        AnalyticChatValidationTask analyticChat = new AnalyticChatValidationTask("ANALYTIC_CHAT_VALIDATION");
        analyticChat.addDependency("CREATE_INFERENCE_TASK");
        assertTrue(analyticChat.getDependencies().contains("CREATE_INFERENCE_TASK"));
    }

    @Test
    public void testAnalyticChatValidationSuccess() throws Exception {
        server.createContext("/server/conversation/SelfDevProject", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) {
                try {
                    byte[] resp = "[{\"sender\":\"User\",\"text\":\"hi\",\"agentType\":\"user\"},{\"sender\":\"Final Response\",\"text\":\"Hello! How can I help you?\",\"agentType\":\"final-response\"}]".getBytes();
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, resp.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(resp);
                    }
                } catch (Exception ignored) {
                }
            }
        });

        AnalyticChatValidationTask task = new AnalyticChatValidationTask("ANALYTIC_CHAT_VALIDATION");
        TaskResult result = task.execute(context);

        assertNotNull(result);
        assertEquals(TaskStatus.SUCCESS, result.getStatus());
        assertEquals("SelfDevProject", result.getDiagnostics().get("sessionId"));
        assertTrue(result.getDiagnostics().get("response").toString().contains("Hello!"));
    }

    @Test
    public void testAnalyticChatValidationThreadInterrupted() {
        Thread.currentThread().interrupt();

        AnalyticChatValidationTask task = new AnalyticChatValidationTask("ANALYTIC_CHAT_VALIDATION");
        TaskResult result = task.execute(context);

        assertTrue("Thread interrupt flag must be restored", Thread.interrupted());
        assertNotNull(result);
        assertEquals(TaskStatus.SKIPPED, result.getStatus());
    }

    @Test
    public void testAnalyticChatValidationConnectionFailure() {
        ResourceManager.getInstance().setServerPort(59999);
        ResourceManager.getInstance().setSupervisorPort(59999);

        AnalyticChatValidationTask task = new AnalyticChatValidationTask("ANALYTIC_CHAT_VALIDATION");
        TaskResult result = task.execute(context);

        assertNotNull(result);
        assertEquals(TaskStatus.FAILED, result.getStatus());
        assertTrue(result.getMessage().contains("Did not receive valid final response"));
    }
}
