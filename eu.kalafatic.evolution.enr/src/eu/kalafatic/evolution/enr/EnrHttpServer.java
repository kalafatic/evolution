package eu.kalafatic.evolution.enr;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.Writer;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.json.JSONArray;
import org.json.JSONObject;

import eu.kalafatic.evolution.controller.resource.PortRegistry;
import eu.kalafatic.evolution.enr.EnrInferenceEngine.EnrCapability;
import eu.kalafatic.evolution.enr.EnrInferenceEngine.EnrChatMessage;
import eu.kalafatic.evolution.enr.EnrInferenceEngine.EnrChatRequest;
import eu.kalafatic.evolution.enr.EnrInferenceEngine.EnrChatResponse;
import eu.kalafatic.evolution.enr.EnrInferenceEngine.EnrHealth;
import eu.kalafatic.evolution.enr.EnrInferenceEngine.EnrModel;
import eu.kalafatic.evolution.enr.EnrInferenceEngine.EnrProviderInfo;
import eu.kalafatic.evolution.enr.EnrInferenceEngine.EnrRuntimeStatus;
import fi.iki.elonen.NanoHTTPD;

/**
 * ENR 1.0 HTTP surface (Phase 6): model discovery, model lifecycle, blocking and streaming
 * chat, runtime status, provider and capability discovery. This is the wire contract that
 * TRYST, DStudio and other family modules consume when they cannot bind ENR in-process.
 *
 * <p>Dev-mode default: loopback bind, bearer token required only when configured via the
 * system property {@code evo.enr.auth.token} (family dev-mode convention, TEIP 1.1 §6).</p>
 */
public class EnrHttpServer extends NanoHTTPD {

    public static final String API_PREFIX = "/enr/v1";
    public static final String SYS_ENR_PORT = "evo.enr.port";
    public static final String SYS_ENR_AUTH_TOKEN = "evo.enr.auth.token";
    public static final String SYS_ENR_MAX_CONCURRENT = "evo.enr.maxConcurrent";

    private final EnrInferenceEngine engine;

    // Admission-control limiter shared by chat and chat/stream: bounds concurrent
    // CPU-bound inference work; over-limit requests are rejected honestly (429).
    private final java.util.concurrent.Semaphore requestLimiter =
            new java.util.concurrent.Semaphore(resolveMaxConcurrent(), true);

    public EnrHttpServer(EnrInferenceEngine engine) {
        super(resolvePort());
        this.engine = engine == null ? EnrRuntime.instance() : engine;
    }

    public EnrHttpServer(int port, EnrInferenceEngine engine) {
        super(port);
        this.engine = engine == null ? EnrRuntime.instance() : engine;
    }

    static int resolveMaxConcurrent() {
        String property = System.getProperty(SYS_ENR_MAX_CONCURRENT);
        if (property != null && !property.isBlank()) {
            try {
                return Math.max(1, Integer.parseInt(property.trim()));
            } catch (NumberFormatException ignored) {
                // fall through to default
            }
        }
        return Math.max(1, Runtime.getRuntime().availableProcessors() / 2);
    }

    /**
     * Resolves the ENR bind port: {@code evo.enr.port} system property, else the family
     * convention {@link PortRegistry#EVO_ENR} (28780). If the default is occupied the first
     * free port in the fallback range +1..+100 is used.
     */
    public static int resolvePort() {
        String property = System.getProperty(SYS_ENR_PORT);
        if (property != null && !property.isBlank()) {
            try {
                return Integer.parseInt(property.trim());
            } catch (NumberFormatException ignored) {
                // fall through to convention
            }
        }
        int convention = PortRegistry.EVO_ENR.getDefaultPort();
        for (int offset = 0; offset < 100; offset++) {
            int candidate = convention + offset;
            try (java.net.ServerSocket probe = new java.net.ServerSocket()) {
                probe.setReuseAddress(true);
                probe.bind(new java.net.InetSocketAddress(InetAddress.getLoopbackAddress(),
                        candidate));
                return candidate;
            } catch (IOException ignored) {
                // occupied: try next
            }
        }
        return convention;
    }

    public boolean isAuthorized(String authorizationHeader) {
        String token = System.getProperty(SYS_ENR_AUTH_TOKEN);
        if (token == null || token.isBlank()) {
            return true;
        }
        return authorizationHeader != null
                && authorizationHeader.trim().equals("Bearer " + token.trim());
    }

    @Override
    public Response serve(IHTTPSession session) {
        try {
            if (!isAuthorized(session.getHeaders().get("authorization"))) {
                return json(Response.Status.UNAUTHORIZED, error("AUTHENTICATION_FAILED",
                        "Invalid or missing Bearer authorization token", false));
            }
            String uri = session.getUri();
            Method method = session.getMethod();
            if ("/health".equals(uri) || (API_PREFIX + "/health").equals(uri)) {
                return json(Response.Status.OK, health());
            }
            if ("/status".equals(uri) || (API_PREFIX + "/status").equals(uri)) {
                return json(Response.Status.OK, health());
            }
            if (uri.startsWith(API_PREFIX + "/models")) {
                return models(method, uri.substring((API_PREFIX + "/models").length()),
                        session);
            }
            if ((API_PREFIX + "/chat").equals(uri)) {
                return chat(method, session);
            }
            if ((API_PREFIX + "/chat/stream").equals(uri)) {
                return chatStream(session);
            }
            if ((API_PREFIX + "/runtime").equals(uri)) {
                return json(Response.Status.OK, runtime());
            }
            if ((API_PREFIX + "/providers").equals(uri)) {
                return json(Response.Status.OK, providers());
            }
            if ((API_PREFIX + "/capabilities").equals(uri)) {
                return json(Response.Status.OK, capabilities());
            }
            return json(Response.Status.NOT_FOUND, error("NOT_FOUND",
                    "Unknown ENR route: " + uri, false));
        } catch (IllegalArgumentException e) {
            return json(Response.Status.BAD_REQUEST, error("VALIDATION_FAILED",
                    e.getMessage(), false));
        } catch (UnsupportedOperationException e) {
            return json(Response.Status.CONFLICT, error("CONFLICT", e.getMessage(), false));
        } catch (Exception e) {
            return json(Response.Status.INTERNAL_ERROR, error("INTERNAL_ERROR",
                    e.getClass().getSimpleName() + ": " + e.getMessage(), false));
        }
    }

    // ------------------------------------------------------------------
    // Route handlers
    // ------------------------------------------------------------------

    private Response models(Method method, String rest, IHTTPSession session) {
        String trimmed = rest.startsWith("/") ? rest.substring(1) : rest;
        if (trimmed.isEmpty() && method == Method.GET) {
            JSONArray array = new JSONArray();
            for (EnrModel model : engine.listModels()) {
                array.put(modelNode(model));
            }
            return json(Response.Status.OK, array);
        }
        if ("pull".equals(trimmed) && method == Method.POST) {
            JSONObject body = body(session);
            String model = body.optString("model", "");
            if (model.isBlank()) {
                throw new IllegalArgumentException("Field 'model' is required");
            }
            boolean ok = engine.pullModel(model, null);
            JSONObject node = new JSONObject();
            node.put("model", model);
            node.put("success", ok);
            return json(ok ? Response.Status.OK : Response.Status.INTERNAL_ERROR, node);
        }
        String[] parts = trimmed.split("/");
        String modelId = parts[0];
        if (parts.length == 1 && method == Method.DELETE) {
            boolean result = engine.deleteModel(modelId);
            JSONObject node = new JSONObject();
            node.put("model", modelId);
            node.put("success", result);
            return json(result ? Response.Status.OK : Response.Status.NOT_FOUND, node);
        }
        if (parts.length == 1 && method == Method.GET) {
            Optional<EnrModel> model = engine.findModel(modelId);
            if (model.isEmpty()) {
                return json(Response.Status.NOT_FOUND, error("NOT_FOUND",
                        "Unknown model: " + modelId, false));
            }
            return json(Response.Status.OK, modelNode(model.get()));
        }
        if (parts.length == 2) {
            boolean result = switch (parts[1]) {
                case "load" -> {
                    requireMethod(method, Method.POST);
                    yield engine.loadModel(modelId);
                }
                case "unload" -> {
                    requireMethod(method, Method.POST);
                    yield engine.unloadModel(modelId);
                }
                default -> throw new IllegalArgumentException("Unknown model operation: "
                        + parts[1]);
            };
            JSONObject node = new JSONObject();
            node.put("model", modelId);
            node.put("success", result);
            return json(result ? Response.Status.OK : Response.Status.NOT_FOUND, node);
        }
        throw new IllegalArgumentException("Unknown models route: " + rest);
    }

    private Response chat(Method method, IHTTPSession session) {
        requireMethod(method, Method.POST);
        if (!requestLimiter.tryAcquire()) {
            return json(Response.Status.TOO_MANY_REQUESTS, error("RESOURCE_LIMIT",
                    "Concurrent inference limit reached", false));
        }
        try {
            EnrChatRequest request = chatRequest(body(session));
            EnrChatResponse response = engine.chat(request);
            JSONObject node = new JSONObject();
            node.put("content", response.content());
            node.put("finishReason", response.finishReason());
            node.put("engine", response.engine());
            node.put("model", response.model());
            node.put("durationMs", response.durationMs());
            node.put("promptTokens", response.promptTokens());
            node.put("completionTokens", response.completionTokens());
            return json(Response.Status.OK, node);
        } finally {
            requestLimiter.release();
        }
    }

    private Response chatStream(IHTTPSession session) {
        // Admission control: bound concurrent inference work so unbounded client
        // requests can never exhaust CPU/RAM. Rejects honestly with 429 when the
        // limit is reached.
        if (!requestLimiter.tryAcquire()) {
            return json(Response.Status.TOO_MANY_REQUESTS, error("RESOURCE_LIMIT",
                    "Concurrent inference limit reached (evo.enr.maxConcurrent=" + requestLimiter.availablePermits()
                            + " permits remain)", false));
        }

        EnrChatRequest request = chatRequest(body(session));
        PipedOutputStream pipedOut = new PipedOutputStream();
        InputStream pipedIn;
        try {
            pipedIn = new PipedInputStream(pipedOut, 64 * 1024);
        } catch (IOException e) {
            requestLimiter.release();
            return json(Response.Status.INTERNAL_ERROR, error("INTERNAL_ERROR",
                    "Cannot open SSE pipe: " + e.getMessage(), false));
        }
        java.util.concurrent.atomic.AtomicBoolean clientGone = new java.util.concurrent.atomic.AtomicBoolean(false);
        Thread worker = new Thread(() -> {
            try (Writer writer = new OutputStreamWriter(pipedOut, StandardCharsets.UTF_8)) {
                engine.streamChat(request, chunk -> {
                    try {
                        JSONObject node = new JSONObject();
                        node.put("chunk", chunk.text());
                        node.put("engine", chunk.engine());
                        node.put("done", chunk.done());
                        writer.write("data: " + node + "\n\n");
                        writer.flush();
                    } catch (IOException e) {
                        // Client disconnected: cancel the generation cooperatively.
                        clientGone.set(true);
                        throw new RuntimeException("SSE stream closed", e);
                    }
                }, clientGone::get);
                if (!clientGone.get()) {
                    writer.write("event: done\ndata: {}\n\n");
                    writer.flush();
                }
            } catch (Throwable t) {
                if (!clientGone.get()) {
                    try {
                        Writer writer = new OutputStreamWriter(pipedOut,
                                StandardCharsets.UTF_8);
                        writer.write("event: error\ndata: " + new JSONObject().put("message",
                                String.valueOf(t.getMessage())) + "\n\n");
                        writer.flush();
                    } catch (IOException ignored) {
                    }
                }
            } finally {
                try {
                    pipedOut.close();
                } catch (IOException ignored) {
                }
                requestLimiter.release();
            }
        }, "enr-sse");
        worker.setDaemon(true);
        worker.start();
        Response response = newChunkedResponse(Response.Status.OK,
                "text/event-stream; charset=utf-8", pipedIn);
        response.addHeader("Cache-Control", "no-cache");
        response.addHeader("Connection", "close");
        return response;
    }

    private JSONObject runtime() {
        EnrRuntimeStatus status = engine.runtimeStatus();
        JSONObject node = new JSONObject();
        node.put("ready", status.ready());
        node.put("availableProcessors", status.availableProcessors());
        node.put("systemLoadAverage", status.systemLoadAverage());
        node.put("maxHeapMb", status.maxHeapMb());
        node.put("usedHeapMb", status.usedHeapMb());
        JSONObject gpu = new JSONObject();
        gpu.put("detected", status.gpu().detected());
        gpu.put("available", status.gpu().available());
        gpu.put("deviceName", status.gpu().deviceName());
        gpu.put("totalVramMb", status.gpu().totalVramMb());
        gpu.put("usedVramMb", status.gpu().usedVramMb());
        gpu.put("freeVramMb", status.gpu().freeVramMb());
        gpu.put("driverVersion", status.gpu().driverVersion());
        gpu.put("message", status.gpu().message());
        node.put("gpu", gpu);
        node.put("activeEngines", new JSONArray(status.activeEngines()));
        return node;
    }

    private JSONArray providers() {
        JSONArray array = new JSONArray();
        for (EnrProviderInfo provider : engine.listProviders()) {
            JSONObject node = new JSONObject();
            node.put("id", provider.id());
            node.put("name", provider.name());
            node.put("type", provider.type());
            node.put("baseUrl", provider.baseUrl());
            node.put("local", provider.local());
            node.put("available", provider.available());
            array.put(node);
        }
        return array;
    }

    private JSONArray capabilities() {
        JSONArray array = new JSONArray();
        for (EnrCapability capability : engine.capabilities()) {
            JSONObject node = new JSONObject();
            node.put("id", capability.id());
            node.put("name", capability.name());
            node.put("description", capability.description());
            node.put("features", new JSONArray(capability.features()));
            array.put(node);
        }
        return array;
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private EnrChatRequest chatRequest(JSONObject body) {
        String model = body.optString("model", "");
        if (model.isBlank()) {
            throw new IllegalArgumentException("Field 'model' is required");
        }
        List<EnrChatMessage> messages = new ArrayList<>();
        JSONArray array = body.optJSONArray("messages");
        if (array != null) {
            for (int i = 0; i < array.length(); i++) {
                JSONObject message = array.optJSONObject(i);
                if (message == null) {
                    continue;
                }
                messages.add(new EnrChatMessage(message.optString("role", "user"),
                        message.optString("content", "")));
            }
        }
        if (messages.isEmpty()) {
            String prompt = body.optString("prompt", "");
            messages.add(EnrChatMessage.user(prompt.isBlank() ? "Respond." : prompt));
        }
        return new EnrChatRequest(model, messages, body.optDouble("temperature", 0.7),
                body.optInt("maxTokens", 512));
    }

    private JSONObject body(IHTTPSession session) {
        try {
            Map<String, String> files = new java.util.HashMap<>();
            session.parseBody(files);
            String postData = files.get("postData");
            return postData == null || postData.isBlank() ? new JSONObject()
                    : new JSONObject(postData);
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    private static JSONObject health() {
        EnrHealth health = EnrRuntime.instance().health();
        JSONObject node = new JSONObject();
        node.put("application", "ENR");
        node.put("version", EnrInferenceEngine.ENGINE_VERSION);
        node.put("status", health.status());
        node.put("ready", health.ready());
        node.put("timestamp", Instant.now().toString());
        return node;
    }

    private static JSONObject modelNode(EnrModel model) {
        JSONObject node = new JSONObject();
        node.put("id", model.id());
        node.put("name", model.name());
        node.put("provider", model.provider());
        node.put("engine", model.engine());
        node.put("sizeBytes", model.sizeBytes());
        node.put("available", model.available());
        node.put("contextLength", model.contextLength());
        node.put("contextLengthKnown", model.contextLengthKnown());
        return node;
    }

    private static JSONObject error(String code, String message, boolean retryable) {
        JSONObject node = new JSONObject();
        node.put("code", code);
        node.put("message", message == null ? "" : message);
        node.put("retryable", retryable);
        node.put("timestamp", Instant.now().toString());
        return node;
    }

    private static Response json(Response.IStatus status, JSONObject body) {
        return newFixedLengthResponse(status, "application/json; charset=utf-8",
                body.toString());
    }

    private static Response json(Response.IStatus status, JSONArray body) {
        return newFixedLengthResponse(status, "application/json; charset=utf-8",
                body.toString());
    }

    private static void requireMethod(Method actual, Method expected) {
        if (actual != expected) {
            throw new IllegalArgumentException("Use " + expected);
        }
    }
}
