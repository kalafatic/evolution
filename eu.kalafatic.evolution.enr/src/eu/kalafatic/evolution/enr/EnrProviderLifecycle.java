package eu.kalafatic.evolution.enr;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import eu.kalafatic.evolution.controller.orchestration.llm.LocalModelDescriptor;
import eu.kalafatic.evolution.controller.orchestration.llm.ProviderLifecycle;
import eu.kalafatic.evolution.enr.EnrInferenceEngine.EnrModel;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

/**
 * ENR availability/startup lifecycle. Availability is a live HTTP ping of the ENR
 * inference API ({@code /health} on the authoritative 28780 port); startup reuses the
 * existing {@link EnrHttpServer} (NanoHTTPD) lifecycle; health is verified by repeated
 * pings; model discovery reuses the in-process {@link EnrRuntime} catalog.
 *
 * <p>The single {@link EnrHttpServer} instance is shared with {@link EnrActivator} so the
 * AI Commander "Start ENR" action and the bundle activator never launch a second server
 * on the same port.</p>
 */
public class EnrProviderLifecycle implements ProviderLifecycle {

    public static final String KEY = "enr";

    private static volatile EnrHttpServer server;
    private static volatile EnrWebServer webServer;

    @Override
    public String providerKey() {
        return KEY;
    }

    @Override
    public String displayName() {
        return "ENR";
    }

    /** Starts (or reuses) the shared ENR HTTP server. Idempotent and synchronized. */
    public static synchronized EnrHttpServer startServer() throws IOException {
        if (server != null && server.isAlive()) {
            return server;
        }
        EnrHttpServer instance = new EnrHttpServer(EnrRuntime.instance());
        instance.start(60_000, false);
        server = instance;
        return server;
    }

    /** Stops the shared server (bundle shutdown path). */
    public static synchronized void stopServer() {
        if (server != null) {
            server.stop();
            server = null;
        }
    }

    /**
     * Starts (or reuses) the independent ENR Web listener on the authoritative 28880 port.
     * The Web UI's {@code api_base_url} points at the given API host/port, so the two
     * listeners never share or intercept each other's surface. Idempotent: if a live Web
     * server exists it is returned unchanged and no duplicate listener is started.
     */
    public static synchronized EnrWebServer startWebServer(String apiHost, int apiPort)
            throws IOException {
        if (webServer != null && webServer.isAlive()) {
            return webServer;
        }
        EnrWebServer instance = EnrWebServer.start(EnrWebServer.resolveWebPort(), apiHost, apiPort);
        webServer = instance;
        return webServer;
    }

    /** Stops the independent Web listener (does not stop the API server). */
    public static synchronized void stopWebServer() {
        if (webServer != null) {
            webServer.stop();
            webServer = null;
        }
    }

    /** @return the live Web listener, or null when the Web listener is not running. */
    public static synchronized EnrWebServer webServer() {
        return (webServer != null && webServer.isAlive()) ? webServer : null;
    }

    /** @return true when the ENR Web listener is alive on its authoritative port. */
    public static boolean isWebRunning() {
        return webServer() != null;
    }

    @Override
    public boolean isAvailable(Orchestrator orchestrator) {
        return pingHealth();
    }

    @Override
    public CompletableFuture<Void> start(Orchestrator orchestrator, Consumer<String> progress) {
        return CompletableFuture.runAsync(() -> {
            if (progress != null) {
                progress.accept("Starting ENR inference service...");
            }
            try {
                EnrHttpServer api = startServer();
                // ENR Web must come up whenever the runtime is started through EVO; a
                // pre-existing API is reused (never duplicated) and only the Web is ensured.
                startWebServer("127.0.0.1", api.getListeningPort());
            } catch (IOException e) {
                throw new IllegalStateException("ENR startup failed: " + e.getMessage(), e);
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> waitHealthy(Orchestrator orchestrator, long timeoutMillis) {
        return CompletableFuture.supplyAsync(() -> {
            long deadline = System.currentTimeMillis() + Math.max(0, timeoutMillis);
            while (System.currentTimeMillis() < deadline) {
                if (pingHealth()) {
                    return true;
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
            return pingHealth();
        });
    }

    @Override
    public List<LocalModelDescriptor> discoverModels(Orchestrator orchestrator) {
        List<EnrModel> enrModels = EnrRuntime.instance().listModels();
        List<LocalModelDescriptor> models = new ArrayList<>(enrModels.size());
        for (EnrModel model : enrModels) {
            models.add(new LocalModelDescriptor(model.id(), model.name(), model.provider(),
                    model.engine(), model.available()));
        }
        return models;
    }

    private static boolean pingHealth() {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(2))
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://127.0.0.1:" + EnrHttpServer.resolvePort() + "/health"))
                    .timeout(Duration.ofSeconds(3))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200 || response.body() == null) {
                return false;
            }
            String body = response.body();
            // Inference-ready means the runtime reports UP (and ready), not merely that
            // the HTTP server responded. This distinguishes "API up" from "inference ready".
            return body.contains("\"ready\":true") && body.contains("\"status\":\"UP\"");
        } catch (Throwable t) {
            return false;
        }
    }
}
