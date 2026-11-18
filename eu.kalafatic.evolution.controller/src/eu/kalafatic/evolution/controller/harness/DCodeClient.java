package eu.kalafatic.evolution.controller.harness;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Minimal DCode REST client (Phase 6, FHC 1.0 wire mapping).
 *
 * <p>EVO delegates coding tasks to the native DCode harness through DCode's stable
 * {@code /api/v1} surface: workspace + repository registration, session creation,
 * execution submission, state polling, message collection, change collection and
 * cancellation. DCode owns harness execution state; EVO owns Developer Mode, target
 * resolution and orchestration. DCode never commits in delegated mode.</p>
 */
public class DCodeClient {

    private final String endpoint;
    private final String authToken;
    private final Duration requestTimeout;
    private final HttpClient httpClient;

    public DCodeClient(String endpoint, String authToken, Duration requestTimeout) {
        this.endpoint = endpoint == null || endpoint.isBlank()
                ? DCodeCodingHarnessAdapter.DEFAULT_ENDPOINT
                : (endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length() - 1)
                        : endpoint);
        this.authToken = authToken;
        this.requestTimeout = requestTimeout == null ? Duration.ofSeconds(30) : requestTimeout;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10)).build();
    }

    public JSONObject health() throws Exception {
        HttpResponse<String> response = send("GET", "/health", null);
        if (response.statusCode() != 200) {
            throw new IllegalStateException("DCode health check failed: HTTP "
                    + response.statusCode() + " " + response.body());
        }
        return new JSONObject(response.body());
    }

    public JSONObject createWorkspace(String name, String rootPath) throws Exception {
        JSONObject body = new JSONObject();
        body.put("name", name);
        body.put("rootPath", rootPath);
        HttpResponse<String> response = send("POST", "/api/v1/workspaces",
                body.toString());
        if (response.statusCode() != 200) {
            throw new IllegalStateException("DCode workspace creation failed: HTTP "
                    + response.statusCode() + " " + response.body());
        }
        return new JSONObject(response.body());
    }

    public JSONObject addRepository(String workspaceId, String name, String localPath)
            throws Exception {
        JSONObject body = new JSONObject();
        body.put("name", name);
        body.put("localPath", localPath);
        body.put("accessPolicy", "READ_WRITE");
        HttpResponse<String> response = send("POST", "/api/v1/workspaces/"
                + workspaceId + "/repositories", body.toString());
        if (response.statusCode() != 200) {
            throw new IllegalStateException("DCode repository registration failed: HTTP "
                    + response.statusCode() + " " + response.body());
        }
        return new JSONObject(response.body());
    }

    public JSONObject createSession(String title, String workspaceId, String providerId,
            String modelId) throws Exception {
        JSONObject body = new JSONObject();
        body.put("title", title);
        body.put("workspaceId", workspaceId);
        if (providerId != null && !providerId.isBlank()) {
            body.put("providerId", providerId);
        }
        if (modelId != null && !modelId.isBlank()) {
            body.put("modelId", modelId);
        }
        HttpResponse<String> response = send("POST", "/api/v1/sessions",
                body.toString());
        if (response.statusCode() != 200) {
            throw new IllegalStateException("DCode session creation failed: HTTP "
                    + response.statusCode() + " " + response.body());
        }
        return new JSONObject(response.body());
    }

    public JSONObject startSession(String sessionId, String task) throws Exception {
        JSONObject body = new JSONObject();
        body.put("task", task);
        HttpResponse<String> response = send("POST", "/api/v1/sessions/"
                + sessionId + "/start", body.toString());
        if (response.statusCode() != 200) {
            throw new IllegalStateException("DCode execution submission failed: HTTP "
                    + response.statusCode() + " " + response.body());
        }
        return new JSONObject(response.body());
    }

    public JSONObject session(String sessionId) throws Exception {
        HttpResponse<String> response = send("GET", "/api/v1/sessions/"
                + sessionId, null);
        if (response.statusCode() != 200) {
            throw new IllegalStateException("DCode session lookup failed: HTTP "
                    + response.statusCode() + " " + response.body());
        }
        return new JSONObject(response.body());
    }

    public List<JSONObject> messages(String sessionId) throws Exception {
        HttpResponse<String> response = send("GET", "/api/v1/sessions/"
                + sessionId + "/messages", null);
        if (response.statusCode() != 200) {
            throw new IllegalStateException("DCode message lookup failed: HTTP "
                    + response.statusCode() + " " + response.body());
        }
        JSONArray array = new JSONArray(response.body());
        List<JSONObject> messages = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            messages.add(array.getJSONObject(i));
        }
        return messages;
    }

    public JSONObject changes(String sessionId) throws Exception {
        HttpResponse<String> response = send("GET", "/api/v1/sessions/"
                + sessionId + "/changes", null);
        if (response.statusCode() != 200) {
            throw new IllegalStateException("DCode change lookup failed: HTTP "
                    + response.statusCode() + " " + response.body());
        }
        return new JSONObject(response.body());
    }

    public void cancel(String sessionId) throws Exception {
        send("POST", "/api/v1/sessions/" + sessionId + "/cancel", null);
    }

    private HttpResponse<String> send(String method, String path, String body)
            throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(endpoint + path))
                .timeout(requestTimeout)
                .header("Content-Type", "application/json");
        if (authToken != null && !authToken.isBlank()) {
            builder.header("Authorization", "Bearer " + authToken);
        }
        if (body == null) {
            builder.method(method, HttpRequest.BodyPublishers.noBody());
        } else {
            builder.method(method, HttpRequest.BodyPublishers.ofString(body));
        }
        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }
}
