package eu.kalafatic.evolution.controller.teip.mcp;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.json.JSONArray;
import org.json.JSONObject;

import eu.kalafatic.utils.log.Log;

/**
 * Standard MCP JSON-RPC 2.0 client connecting EVO to TRYST's MCP server over HTTP
 * (TEIP 1.1 §8). Sends the REQUIRED Authorization: Bearer and X-Execution-Id headers,
 * enforces fail-closed tool permission scoping before transmission, and performs the
 * mandatory MCP initialize handshake (protocol version 2024-11-05).
 */
public class TrystMcpClient {

    /**
     * Canonical tool → required scope mapping (TEIP 1.1 §8.5).
     */
    private static final Map<String, String> TOOL_REQUIRED_SCOPE = Map.of(
            "get_project_metadata", "read:task",
            "list_workspace_files", "read:workspace",
            "read_workspace_file", "read:workspace",
            "search_workspace_code", "read:workspace",
            "get_task_context", "read:task",
            "propose_patch", "write:workspace",
            "run_build_and_test", "execute:build"
    );

    private final String serverUrl;
    private final String authToken;
    private final HttpClient httpClient;
    private final Set<String> grantedToolScopes;

    public TrystMcpClient(String serverUrl) {
        this(serverUrl, null, Collections.emptyList());
    }

    public TrystMcpClient(String serverUrl, String authToken, List<String> grantedToolScopes) {
        // TRYST MCP tool plane lives under /api/mcp on the TRYST HTTP port (38080).
        this.serverUrl = serverUrl != null && !serverUrl.trim().isEmpty()
                ? serverUrl.trim()
                : "http://localhost:38080/tryst/api/mcp";
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
     * Initializes the MCP protocol session (protocol version 2024-11-05).
     */
    public JSONObject initialize(String executionId, String correlationId) throws Exception {
        JSONObject params = new JSONObject();
        params.put("protocolVersion", "2024-11-05");
        params.put("capabilities", new JSONObject());

        JSONObject clientInfo = new JSONObject();
        clientInfo.put("name", "EVO-TEIP-Agent");
        clientInfo.put("version", "1.1");
        params.put("clientInfo", clientInfo);

        return sendRpcRequest("initialize", params, executionId, correlationId);
    }

    /**
     * Convenience overload without execution context (UI diagnostics).
     */
    public JSONObject initialize() throws Exception {
        return initialize(null, null);
    }

    /**
     * Discovers available tools from TRYST MCP server.
     */
    public JSONArray listTools(String executionId, String correlationId) throws Exception {
        JSONObject resp = sendRpcRequest("tools/list", new JSONObject(), executionId, correlationId);
        return resp.optJSONArray("tools") != null ? resp.optJSONArray("tools") : new JSONArray();
    }

    /**
     * Convenience overload without execution context (UI diagnostics).
     */
    public JSONArray listTools() throws Exception {
        return listTools(null, null);
    }

    /**
     * Executes a tool on TRYST with scoped authorization and context association.
     * Fail-closed: out-of-scope calls are refused BEFORE transmission.
     */
    public JSONObject callTool(String toolName, JSONObject arguments, String executionId, String taskId, String correlationId) throws Exception {
        if (toolName == null || toolName.trim().isEmpty()) {
            throw new IllegalArgumentException("Tool name cannot be empty");
        }

        // Scope verification per TEIP 1.1 §8.4/§8.5: the tool's required scope must be
        // granted. Legacy name-based grants ("tryst:<name>", "*") remain accepted.
        String requiredScope = TOOL_REQUIRED_SCOPE.get(toolName);
        boolean authorized = grantedToolScopes.contains("*")
                || grantedToolScopes.contains(toolName)
                || grantedToolScopes.contains("tryst:" + toolName)
                || (requiredScope != null && grantedToolScopes.contains(requiredScope));

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

        return sendRpcRequest("tools/call", params, executionId, correlationId);
    }

    private JSONObject sendRpcRequest(String method, JSONObject params, String executionId, String correlationId) throws Exception {
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
        // TEIP 1.1 §8.2: X-Execution-Id is REQUIRED, X-Correlation-Id optional
        if (executionId != null && !executionId.isBlank()) {
            builder.header("X-Execution-Id", executionId.trim());
        }
        if (correlationId != null && !correlationId.isBlank()) {
            builder.header("X-Correlation-Id", correlationId.trim());
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
