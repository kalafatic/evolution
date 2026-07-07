package eu.kalafatic.evolution.controller.orchestration.mcp;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Basic MCP Client (JSON-RPC 2.0).
 */
public class McpClient {

    private final String serverUrl;
    private final HttpClient httpClient;

    public McpClient(String serverUrl) {
        this.serverUrl = serverUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * Sends an initialization request to the MCP server.
     *
     * @return Server's initialization response
     * @throws Exception If an error occurs
     */
    public String initialize() throws Exception {
        JSONObject params = new JSONObject();
        params.put("protocolVersion", "2024-11-05");
        params.put("capabilities", new JSONObject());
        JSONObject clientInfo = new JSONObject();
        clientInfo.put("name", "EvoAgent");
        clientInfo.put("version", "1.0.0");
        params.put("clientInfo", clientInfo);

        return sendRpcRequest("initialize", params).toString();
    }

    /**
     * Lists available resources from the MCP server.
     *
     * @return List of resource objects
     * @throws Exception If an error occurs
     */
    public String listResources() throws Exception {
        JSONObject result = sendRpcRequest("resources/list", new JSONObject());
        return result.getJSONArray("resources").toString();
    }

    /**
     * Lists available tools from the MCP server.
     *
     * @return List of tool objects
     * @throws Exception If an error occurs
     */
    public String listTools() throws Exception {
        JSONObject result = sendRpcRequest("tools/list", new JSONObject());
        return result.getJSONArray("tools").toString();
    }

    /**
     * Calls a tool on the MCP server.
     *
     * @param name Tool name
     * @param arguments Tool arguments
     * @return Tool execution result
     * @throws Exception If an error occurs
     */
    public String callTool(String name, JSONObject arguments) throws Exception {
        JSONObject params = new JSONObject();
        params.put("name", name);
        params.put("arguments", arguments);
        JSONObject result = sendRpcRequest("tools/call", params);
        return result.toString();
    }

    /**
     * Lists available prompts from the MCP server.
     *
     * @return List of prompt objects
     * @throws Exception If an error occurs
     */
    public String listPrompts() throws Exception {
        JSONObject result = sendRpcRequest("prompts/list", new JSONObject());
        return result.getJSONArray("prompts").toString();
    }

    /**
     * Gets a prompt from the MCP server.
     *
     * @param name Prompt name
     * @param arguments Prompt arguments
     * @return Prompt content
     * @throws Exception If an error occurs
     */
    public String getPrompt(String name, JSONObject arguments) throws Exception {
        JSONObject params = new JSONObject();
        params.put("name", name);
        params.put("arguments", arguments);
        JSONObject result = sendRpcRequest("prompts/get", params);
        return result.toString();
    }

    /**
     * Reads a specific resource by URI.
     *
     * @param uri The resource URI
     * @return The resource's content
     * @throws Exception If an error occurs
     */
    public String readResource(String uri) throws Exception {
        JSONObject params = new JSONObject();
        params.put("uri", uri);
        JSONObject response = sendRpcRequest("resources/read", params);
        JSONArray contents = response.getJSONArray("contents");
        if (contents.length() > 0) {
            return contents.getJSONObject(0).optString("text", "");
        }
        return "";
    }

    /**
     * Sends a generic JSON-RPC request.
     *
     * @param method The method name
     * @param params The parameters
     * @return The result as a JSON string
     * @throws Exception If an error occurs
     */
    public String sendGenericRequest(String method, JSONObject params) throws Exception {
        return sendRpcRequest(method, params).toString();
    }

    private JSONObject sendRpcRequest(String method, JSONObject params) throws Exception {
        JSONObject request = new JSONObject();
        request.put("jsonrpc", "2.0");
        request.put("id", UUID.randomUUID().toString());
        request.put("method", method);
        request.put("params", params);

        String effectiveUrl = serverUrl;
        if (!effectiveUrl.startsWith("http://") && !effectiveUrl.startsWith("https://")) {
            effectiveUrl = "http://" + effectiveUrl;
        }

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(effectiveUrl))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(request.toString()))
                .build();

        HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        if (httpResponse.statusCode() != 200) {
            throw new Exception("MCP server error: " + httpResponse.statusCode() + " - " + httpResponse.body());
        }

        JSONObject rpcResponse = new JSONObject(httpResponse.body());
        if (rpcResponse.has("error")) {
            JSONObject error = rpcResponse.getJSONObject("error");
            throw new Exception("JSON-RPC error: " + error.getInt("code") + " - " + error.getString("message"));
        }

        return rpcResponse.getJSONObject("result");
    }

	public boolean testConnection() {
		try {
			initialize();
			return true;
		} catch (Exception e) {
			return false;
		}
	}
}
