package eu.kalafatic.evolution.controller.harness;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Minimal TEIP 1.1 client for the DStudio Harness Platform (Phase 4).
 *
 * <p>EVO connects to DStudio as its external coding-harness provider using the canonical
 * TEIP 1.1 contract ({@code /teip/v1/*}). DStudio owns the harness adapters and execution
 * lifecycle; EVO retains ownership of Developer Mode, target resolution and orchestration.</p>
 */
public class DStudioTeipClient {

    private final String endpoint;
    private final String authToken;
    private final Duration requestTimeout;
    private final HttpClient httpClient;

    public DStudioTeipClient(String endpoint, String authToken, Duration requestTimeout) {
        this.endpoint = endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length() - 1)
                : endpoint;
        this.authToken = authToken;
        this.requestTimeout = requestTimeout == null ? Duration.ofSeconds(30) : requestTimeout;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10)).build();
    }

    public JSONObject health() throws Exception {
        HttpResponse<String> response = send("GET", "/teip/v1/health", null, null);
        if (response.statusCode() != 200) {
            throw new IllegalStateException("DStudio health check failed: HTTP "
                    + response.statusCode() + " " + response.body());
        }
        return new JSONObject(response.body());
    }

    public JSONObject createExecution(JSONObject request, String idempotencyKey)
            throws Exception {
        HttpResponse<String> response = send("POST", "/teip/v1/executions",
                request.toString(), idempotencyKey);
        if (response.statusCode() != 202 && response.statusCode() != 200) {
            throw new IllegalStateException("DStudio rejected execution: HTTP "
                    + response.statusCode() + " " + response.body());
        }
        return new JSONObject(response.body());
    }

    public JSONObject status(String executionId) throws Exception {
        HttpResponse<String> response = send("GET",
                "/teip/v1/executions/" + executionId, null, null);
        if (response.statusCode() != 200) {
            throw new IllegalStateException("DStudio status failed: HTTP "
                    + response.statusCode() + " " + response.body());
        }
        return new JSONObject(response.body());
    }

    public List<JSONObject> events(String executionId, long afterSequence)
            throws Exception {
        HttpResponse<String> response = send("GET", "/teip/v1/executions/"
                + executionId + "/events?afterSequence=" + afterSequence + "&limit=100",
                null, null);
        if (response.statusCode() != 200) {
            throw new IllegalStateException("DStudio events failed: HTTP "
                    + response.statusCode() + " " + response.body());
        }
        JSONObject body = new JSONObject(response.body());
        List<JSONObject> events = new ArrayList<>();
        JSONArray array = body.optJSONArray("events");
        if (array != null) {
            for (int i = 0; i < array.length(); i++) {
                events.add(array.getJSONObject(i));
            }
        }
        return events;
    }

    public JSONObject result(String executionId) throws Exception {
        HttpResponse<String> response = send("GET", "/teip/v1/executions/"
                + executionId + "/result", null, null);
        if (response.statusCode() == 409) {
            return null;
        }
        if (response.statusCode() != 200) {
            throw new IllegalStateException("DStudio result failed: HTTP "
                    + response.statusCode() + " " + response.body());
        }
        return new JSONObject(response.body());
    }

    public void cancel(String executionId) throws Exception {
        send("POST", "/teip/v1/executions/" + executionId + "/cancel", null, null);
    }

    private HttpResponse<String> send(String method, String path, String body,
            String idempotencyKey) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(endpoint + path))
                .timeout(requestTimeout)
                .header("Content-Type", "application/json");
        if (authToken != null && !authToken.isBlank()) {
            builder.header("Authorization", "Bearer " + authToken);
        }
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            builder.header("Idempotency-Key", idempotencyKey);
        }
        if (body == null) {
            builder.method(method, HttpRequest.BodyPublishers.noBody());
        } else {
            builder.method(method, HttpRequest.BodyPublishers.ofString(body));
        }
        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    public String getEndpoint() {
        return endpoint;
    }
}
