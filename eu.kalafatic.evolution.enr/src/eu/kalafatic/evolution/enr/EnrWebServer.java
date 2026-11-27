package eu.kalafatic.evolution.enr;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;

import org.json.JSONObject;

import eu.kalafatic.evolution.controller.resource.PortAvailabilityChecker;
import eu.kalafatic.evolution.controller.resource.PortRegistry;
import fi.iki.elonen.NanoHTTPD;

/**
 * ENR Web Control Center (Phase 6, independent listener).
 *
 * <p>Serves the static single-page ENR status UI from classpath resources and exposes
 * two web-specific endpoints: {@code /health} (independent Web-listener readiness) and
 * {@code /api/wcc/info} (UI bootstrap metadata, including the authoritative ENR API base
 * URL). The UI communicates with the ENR API over that {@code api_base_url} — never over
 * the Web listener — so the two listeners stay cleanly separated: the Web server cannot
 * replace, intercept or accidentally bind the API listener.</p>
 *
 * <p>The listener binds the authoritative family Web port
 * {@link PortRegistry#EVO_ENR_WEB} (28880). There is no fallback port: if the port is
 * owned by another process the conflict is reported with the owning process and the start
 * fails clearly, matching the EVO collision policy.</p>
 *
 * <p>ENR Web is model- and provider-independent: it starts and serves its UI regardless of
 * Ollama availability, installed models or inference initialization. The UI simply reports
 * whatever the ENR API currently reports.</p>
 */
public class EnrWebServer extends NanoHTTPD {

    public static final String SYS_ENR_WEB_PORT = "evo.enr.web.port";
    public static final String SYS_ENR_WEB_ENABLED = "evo.enr.web.enabled";
    public static final String WEB_VERSION = "1.0";

    /** Resolves the ENR Web bind port: {@code evo.enr.web.port} property, else 28880. */
    public static int resolveWebPort() {
        String property = System.getProperty(SYS_ENR_WEB_PORT);
        if (property != null && !property.isBlank()) {
            try {
                return Integer.parseInt(property.trim());
            } catch (NumberFormatException ignored) {
                // fall through to convention
            }
        }
        return PortRegistry.EVO_ENR_WEB.getDefaultPort();
    }

    /**
     * Pre-flight conflict check for the authoritative Web port. Returns null when the port
     * is free, otherwise a human-readable report identifying the owning process. This never
     * terminates another process and never moves ENR to another port.
     */
    public static String conflictOnPort(int webPort) {
        PortAvailabilityChecker.ConflictReport report =
                PortAvailabilityChecker.inspectPort(webPort, "ENR Web Control Center");
        return report.isInUse() ? report.toString() : null;
    }

    private final String apiBaseUrl;

    /**
     * @param port the Web listener port (authoritative 28880)
     * @param apiHost the ENR API bind host (used to build {@code api_base_url})
     * @param apiPort the ENR API listening port (used to build {@code api_base_url})
     */
    public EnrWebServer(int port, String apiHost, int apiPort) {
        super(port);
        String host = "0.0.0.0".equals(apiHost) || apiHost == null ? "127.0.0.1" : apiHost;
        this.apiBaseUrl = "http://" + host + ":" + apiPort;
    }

    public String apiBaseUrl() {
        return apiBaseUrl;
    }

    @Override
    public Response serve(IHTTPSession session) {
        try {
            String uri = session.getUri();
            Method method = session.getMethod();

            if ("/health".equals(uri)) {
                JSONObject node = new JSONObject();
                node.put("application", "ENR Web");
                node.put("version", WEB_VERSION);
                node.put("status", "UP");
                node.put("api_base_url", apiBaseUrl);
                node.put("timestamp", Instant.now().toString());
                return json(Response.Status.OK, node);
            }

            if ("/api/wcc/info".equals(uri)) {
                JSONObject node = new JSONObject();
                node.put("name", "ENR");
                node.put("version", WEB_VERSION);
                node.put("api_base_url", apiBaseUrl);
                node.put("web_base_url", "http://127.0.0.1:" + getListeningPort());
                node.put("auth_required", isAuthRequired());
                return json(Response.Status.OK, node);
            }

            if (method != Method.GET) {
                return json(Response.Status.METHOD_NOT_ALLOWED,
                        error("METHOD_NOT_ALLOWED", "Only GET is supported"));
            }

            String resource = switch (uri) {
                case "/", "/index.html" -> "web/index.html";
                case "/app.js" -> "web/app.js";
                case "/style.css" -> "web/style.css";
                default -> null;
            };
            if (resource == null) {
                return json(Response.Status.NOT_FOUND, error("NOT_FOUND", "No such page: " + uri));
            }
            InputStream in = EnrWebServer.class.getResourceAsStream(resource);
            if (in == null) {
                return json(Response.Status.NOT_FOUND,
                        error("NOT_FOUND", "Missing web resource: " + resource));
            }
            byte[] bytes;
            try (in) {
                bytes = in.readAllBytes();
            }
            String mime = resource.endsWith(".html") ? "text/html"
                    : resource.endsWith(".css") ? "text/css"
                    : resource.endsWith(".js") ? "application/javascript"
                    : "application/octet-stream";
            Response response = newFixedLengthResponse(Response.Status.OK,
                    mime + "; charset=utf-8", new java.io.ByteArrayInputStream(bytes),
                    bytes.length);
            response.addHeader("Cache-Control", "no-cache");
            return response;
        } catch (Exception e) {
            return json(Response.Status.INTERNAL_ERROR,
                    error("INTERNAL_ERROR", e.getClass().getSimpleName() + ": " + e.getMessage()));
        }
    }

    private static boolean isAuthRequired() {
        String token = System.getProperty(EnrHttpServer.SYS_ENR_AUTH_TOKEN);
        return token != null && !token.isBlank();
    }

    private static JSONObject error(String code, String message) {
        JSONObject node = new JSONObject();
        node.put("code", code);
        node.put("message", message == null ? "" : message);
        node.put("timestamp", Instant.now().toString());
        return node;
    }

    private static Response json(Response.IStatus status, JSONObject body) {
        return newFixedLengthResponse(status, "application/json; charset=utf-8",
                body.toString());
    }

    /**
     * Convenience starter: performs the conflict check and starts the listener. The caller
     * retains the returned instance for stop/restart.
     */
    public static EnrWebServer start(int webPort, String apiHost, int apiPort) throws IOException {
        String conflict = conflictOnPort(webPort);
        if (conflict != null) {
            throw new IOException(conflict);
        }
        EnrWebServer server = new EnrWebServer(webPort, apiHost, apiPort);
        server.start(60_000, false);
        return server;
    }
}
