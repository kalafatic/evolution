package eu.kalafatic.evolution.controller.teip.mcp;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.json.JSONArray;
import org.json.JSONObject;

import eu.kalafatic.utils.log.Log;

/**
 * Standard MCP JSON-RPC 2.0 client connecting EVO to TRYST's MCP server over HTTP.
 * Enforces fail-closed tool permission scoping, associating tool calls with
 * executionId, taskId, and correlationId.
 */
public class TrystMcpClient {

    private final String serverUrl;
    private final String authToken;
    private final HttpClient httpClient;
    private final Set<String> grantedToolScopes;

    public TrystMcpClient(String serverUrl) {
        this(serverUrl, null, Collections.emptyList());
    }

    public TrystMcpClient(String serverUrl, String authToken, List<String> grantedToolScopes) {
        this.serverUrl = serverUrl != null ? serverUrl.trim() : "http://localhost:3000/mcp";
        this.authToken = authToken;
        this.grantedToolScopes = new HashSet<>();
        if (grantedToolScopes != null) {
            for (String s : grantedToolScopes) {
                if (s != null) this.grantedToolScopes.add(s.trim());
            }
        }
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public Set<String> getGrantedToolScopes() {
        return Collections.unmodifiableSet(grantedToolScopes);
    }

    /**
     * Initializes the MCP protocol session.
     */
    public JSONObject initialize() throws Exception {
        JSONObject params = new JSONObject();
        params.put("protocolVersion", "2024-11-05");
        params.put("capabilities", new JSONObject());

        JSONObject clientInfo = new JSONObject();
        clientInfo.put("name", "EVO-TEIP-Agent");
        clientInfo.put("version", "1.0.0");
        params.put("clientInfo", clientInfo);

        return sendRpcRequest("initialize", params);
    }

    /**
     * Discovers available tools from TRYST MCP server.
     */
    public JSONArray listTools() throws Exception {
        JSONObject resp = sendRpcRequest("tools/list", new JSONObject());
        return resp.optJSONArray("tools") != null ? resp.optJSONArray("tools") : new JSONArray();
    }

    /**
     * Executes a tool on TRYST with scoped authorization and context association.
     */
    public JSONObject callTool(String toolName, JSONObject arguments, String executionId, String taskId, String correlationId) throws Exception {
        if (toolName == null || toolName.trim().isEmpty()) {
            throw new IllegalArgumentException("Tool name cannot be empty");
        }

        // Scope verification: tool must match an explicitly granted scope
        // E.g. scope "tryst:run_tests" or "run_tests" or wildcard "*"
        boolean authorized = grantedToolScopes.contains("*")
                || grantedToolScopes.contains(toolName)
                || grantedToolScopes.contains("tryst:" + toolName);

        if (!authorized) {
            String err = String.format("Tool '%s' is not within grantedToolScopes %s for execution %s",
                    toolName, grantedToolScopes, executionId);
            try {
                Log.log("[TEIP][MCP-SECURITY] " + err);
            } catch (Throwable ignored) {}
            throw new SecurityException(err);
        }

        JSONObject params = new JSONObject();
        params.put("name", toolName);
        params.put("arguments", arguments != null ? arguments : new JSONObject());

        // Context metadata
        JSONObject meta = new JSONObject();
        if (executionId != null) meta.put("executionId", executionId);
        if (taskId != null) meta.put("taskId", taskId);
        if (correlationId != null) meta.put("correlationId", correlationId);
        params.put("_meta", meta);

        return sendRpcRequest("tools/call", params);
    }

    private JSONObject sendRpcRequest(String method, JSONObject params) throws Exception {
        JSONObject req = new JSONObject();
        req.put("jsonrpc", "2.0");
        req.put("id", UUID.randomUUID().toString());
        req.put("method", method);
        req.put("params", params != null ? params : new JSONObject());

        String effectiveUrl = serverUrl;
        if (!effectiveUrl.startsWith("http://") && !effectiveUrl.startsWith("https://")) {
            effectiveUrl = "http://" + effectiveUrl;
        }

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(effectiveUrl))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(req.toString()));

        if (authToken != null && !authToken.trim().isEmpty()) {
            builder.header("Authorization", "Bearer " + authToken.trim());
        }

        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("TRYST MCP server HTTP error: " + response.statusCode() + " - " + response.body());
        }

        JSONObject rpcResponse = new JSONObject(response.body());
        if (rpcResponse.has("error")) {
            JSONObject errObj = rpcResponse.getJSONObject("error");
            throw new RuntimeException("MCP RPC error: " + errObj.optString("message", errObj.toString()));
        }

        return rpcResponse.optJSONObject("result") != null ? rpcResponse.optJSONObject("result") : new JSONObject();
    }
}
