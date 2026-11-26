package eu.kalafatic.evolution.view.ecosystem;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import org.json.JSONObject;

/**
 * HTTP gateway to UPM (the authoritative ecosystem control center).
 *
 * <p>All ecosystem data and operations go through this single client — no
 * parallel product registry, monitoring or lifecycle implementation exists in
 * the view bundle. UPM's address is resolved by probing the configured base
 * URL and the standard fallback port range; nothing is hardcoded per product.</p>
 */
public final class EcosystemClient {

    public static final String SYS_UPM_URL = "evo.ecosystem.upm.url";
    public static final String DEFAULT_HOST = "127.0.0.1";
    public static final int DEFAULT_PORT = 29080;
    public static final int FALLBACK_RANGE = 100;
    public static final int CONNECT_TIMEOUT_MS = 1500;
    public static final int READ_TIMEOUT_MS = 5000;

    private volatile String baseUrl;
    private volatile String lastError = "";

    public EcosystemClient() {
        this(System.getProperty(SYS_UPM_URL, ""));
    }

    public EcosystemClient(String configuredUrl) {
        this.baseUrl = (configuredUrl == null || configuredUrl.isBlank())
                ? null : normalize(configuredUrl);
    }

    /** The active UPM base URL (null until discovery succeeded). */
    public String getBaseUrl() {
        return baseUrl;
    }

    public String getLastError() {
        return lastError;
    }

    /**
     * Resolves the live UPM endpoint: the configured URL first, then the
     * default host with the fallback port range. Discovery is verified with
     * a real HTTP probe — a port is only accepted when it answers as UPM.
     *
     * @return true when a live UPM was found
     */
    public synchronized boolean discover() {
        if (baseUrl != null && probeInfo(baseUrl) != null) {
            return true;
        }
        // Probe the configured URL even when it fails the info check once —
        // try the range only when it is not reachable at all.
        String host = DEFAULT_HOST;
        int port = DEFAULT_PORT;
        if (baseUrl != null) {
            try {
                URL u = new URL(baseUrl);
                host = u.getHost();
                port = u.getPort() > 0 ? u.getPort() : DEFAULT_PORT;
            } catch (Exception ignored) {
                // keep defaults
            }
        }
        for (int p = port; p <= port + FALLBACK_RANGE; p++) {
            String candidate = "http://" + host + ":" + p;
            JSONObject info = probeInfo(candidate);
            if (info != null) {
                baseUrl = candidate;
                lastError = "";
                return true;
            }
        }
        lastError = "No UPM instance found on " + host + ":" + port + "+" + FALLBACK_RANGE;
        return false;
    }

    /** GETs a path relative to the base URL; returns null on any failure. */
    public JSONObject get(String path) {
        if (baseUrl == null) return null;
        try {
            HttpURLConnection c = open(baseUrl + path, "GET");
            c.setRequestProperty("Accept", "application/json");
            int code = c.getResponseCode();
            if (code != 200) {
                lastError = "UPM " + path + " -> HTTP " + code;
                return null;
            }
            String body = readBody(c);
            if (body == null || body.isBlank()) {
                return new JSONObject();
            }
            return new JSONObject(body);
        } catch (Exception e) {
            lastError = "UPM " + path + " failed: " + e.getMessage();
            return null;
        }
    }

    /** POSTs a JSON body; returns the response JSON or null. */
    public JSONObject post(String path, JSONObject body) {
        if (baseUrl == null) return null;
        try {
            HttpURLConnection c = open(baseUrl + path, "POST");
            c.setRequestProperty("Content-Type", "application/json");
            c.setDoOutput(true);
            byte[] payload = body.toString().getBytes(StandardCharsets.UTF_8);
            try (OutputStream os = c.getOutputStream()) {
                os.write(payload);
            }
            int code = c.getResponseCode();
            String resp = readBody(c);
            if (code >= 400) {
                lastError = "UPM " + path + " -> HTTP " + code
                        + (resp != null && !resp.isBlank() ? ": " + truncate(resp, 200) : "");
                return null;
            }
            if (resp == null || resp.isBlank()) {
                return new JSONObject();
            }
            try {
                return new JSONObject(resp);
            } catch (Exception e) {
                JSONObject wrapped = new JSONObject();
                wrapped.put("raw", resp);
                return wrapped;
            }
        } catch (Exception e) {
            lastError = "UPM " + path + " failed: " + e.getMessage();
            return null;
        }
    }

    // ------------------------------------------------------------------
    // Convenience operations (all through UPM's ops/profile API)
    // ------------------------------------------------------------------

    /** Start / stop / restart a product through UPM. */
    public JSONObject productOp(String projectId, String type, String mode) {
        JSONObject body = new JSONObject();
        body.put("projectId", projectId);
        body.put("type", type);          // run | stop | restart
        if (mode != null && !mode.isBlank()) {
            body.put("mode", mode);
        }
        return post("/api/ops", body);
    }

    /** Launch a complete UPM profile (dependency-aware startup). */
    public JSONObject profileLaunch(String profileId) {
        return post("/api/profiles/" + profileId + "/launch", new JSONObject());
    }

    public JSONObject profileStop(String profileId) {
        return post("/api/profiles/" + profileId + "/stop", new JSONObject());
    }

    // ------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------

    private static String normalize(String url) {
        String u = url.trim();
        if (!u.startsWith("http://") && !u.startsWith("https://")) {
            u = "http://" + u;
        }
        while (u.endsWith("/")) {
            u = u.substring(0, u.length() - 1);
        }
        return u;
    }

    /** Fast reachability probe: /api/health must answer as UPM. */
    private static JSONObject probeInfo(String base) {
        try {
            HttpURLConnection c = (HttpURLConnection) new URL(base + "/api/health").openConnection();
            c.setConnectTimeout(1200);
            c.setReadTimeout(2500);
            c.setRequestMethod("GET");
            if (c.getResponseCode() != 200) return null;
            String body = readBody(c);
            if (body == null || body.isBlank()) return null;
            JSONObject info = new JSONObject(body);
            String name = info.optString("name", "");
            String app = info.optString("app", "");
            if (!name.toLowerCase().contains("universal project manager")
                    && !name.toLowerCase().contains("upm")
                    && !app.toLowerCase().contains("universal-project-manager")) {
                return null;
            }
            return info;
        } catch (Exception e) {
            return null;
        }
    }

    private static HttpURLConnection open(String url, String method) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(CONNECT_TIMEOUT_MS);
        c.setReadTimeout(READ_TIMEOUT_MS);
        c.setRequestMethod(method);
        c.setUseCaches(false);
        return c;
    }

    private static String readBody(HttpURLConnection c) throws IOException {
        try (InputStream is = c.getResponseCode() >= 400 ? c.getErrorStream() : c.getInputStream()) {
            if (is == null) return "";
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }

    /** True when the given TCP port is listening (auxiliary, observational). */
    public static boolean portListening(String host, int port) {
        try (Socket s = new Socket()) {
            s.connect(new java.net.InetSocketAddress(host, port), 800);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
