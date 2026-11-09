package eu.kalafatic.evolution.servers.mcp.demo;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Response;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import eu.kalafatic.evolution.servers.mcp.model.McpExecutionContext;

public class DemoDocumentController {
    private final DemoDocumentService service;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private eu.kalafatic.evolution.servers.mcp.server.JsonRpcDispatcher mcpDispatcher;

    public DemoDocumentController(DemoDocumentService service) {
        this.service = service;
    }

    public void setMcpDispatcher(eu.kalafatic.evolution.servers.mcp.server.JsonRpcDispatcher dispatcher) {
        this.mcpDispatcher = dispatcher;
    }

    public eu.kalafatic.evolution.servers.mcp.server.JsonRpcDispatcher getMcpDispatcher() {
        return this.mcpDispatcher;
    }

    public Response handle(IHTTPSession session) {
        String uri = session.getUri();
        try {
            if (session.getMethod() == NanoHTTPD.Method.GET && ("/".equals(uri) || "/mcp".equals(uri) || "/mcp/".equals(uri) || "/mcp.html".equals(uri))) {
                return serveMcpHtml();
            }
            if (session.getMethod() == NanoHTTPD.Method.POST && ("/mcp".equals(uri) || "/mcp/".equals(uri))) {
                return handleMcp(session);
            }
            if (session.getMethod() == NanoHTTPD.Method.GET && "/ui/tools".equals(uri)) {
                return handleUiTools(session);
            }
            if (session.getMethod() == NanoHTTPD.Method.POST && "/ui/execute".equals(uri)) {
                return handleUiExecute(session);
            }
            if (session.getMethod() == NanoHTTPD.Method.GET && "/ui/info".equals(uri)) {
                return handleUiInfo(session);
            }
            if ("/health".equals(uri)) {
                return jsonResponse(Map.of("status", "UP"));
            } else if ("/documents".equals(uri)) {
                return jsonResponse(service.listDocuments());
            } else if ("/document".equals(uri)) {
                String path = session.getParms().get("path");
                if (path == null) {
                    return errorResponse(Response.Status.BAD_REQUEST, "Missing path parameter");
                }
                DemoDocumentService.DocumentContent doc = service.getDocument(path);
                if (doc == null) {
                    return errorResponse(Response.Status.NOT_FOUND, "Document not found: " + path);
                }
                return jsonResponse(doc);
            } else if ("/search".equals(uri)) {
                String query = session.getParms().get("q");
                if (query == null) {
                    return errorResponse(Response.Status.BAD_REQUEST, "Missing q parameter");
                }
                return jsonResponse(service.search(query));
            }
        } catch (Exception e) {
            return errorResponse(Response.Status.INTERNAL_ERROR, e.getMessage());
        }
        return NanoHTTPD.newFixedLengthResponse(Response.Status.NOT_FOUND, NanoHTTPD.MIME_PLAINTEXT, "Not Found");
    }

    private Response serveMcpHtml() throws IOException {
        java.io.InputStream is = getClass().getResourceAsStream("mcp.html");
        if (is == null) {
            return NanoHTTPD.newFixedLengthResponse(Response.Status.NOT_FOUND, NanoHTTPD.MIME_PLAINTEXT, "mcp.html not found");
        }
        byte[] bytes = is.readAllBytes();
        return NanoHTTPD.newFixedLengthResponse(Response.Status.OK, "text/html", new java.io.ByteArrayInputStream(bytes), bytes.length);
    }

    private Response handleUiTools(IHTTPSession session) throws IOException {
        if (mcpDispatcher == null) {
            return errorResponse(Response.Status.INTERNAL_ERROR, "MCP Dispatcher not initialized");
        }
        McpExecutionContext context = resolveExecutionContext(session);
        eu.kalafatic.evolution.servers.mcp.protocol.McpRequest req = new eu.kalafatic.evolution.servers.mcp.protocol.McpRequest();
        req.setMethod("tools/list");
        req.setId(System.currentTimeMillis());
        eu.kalafatic.evolution.servers.mcp.protocol.McpResponse resp = mcpDispatcher.dispatch(req, context);
        if (resp.getError() != null) {
            return errorResponse(Response.Status.FORBIDDEN, resp.getError().getMessage());
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) resp.getResult();
        return jsonResponse(map.get("tools"));
    }

    private Response handleUiExecute(IHTTPSession session) throws Exception {
        if (mcpDispatcher == null) {
            return errorResponse(Response.Status.INTERNAL_ERROR, "MCP Dispatcher not initialized");
        }
        Map<String, String> files = new java.util.HashMap<>();
        session.parseBody(files);
        String postData = files.get("postData");

        com.fasterxml.jackson.databind.JsonNode node = objectMapper.readTree(postData);
        if (node == null || !node.has("tool")) {
            return errorResponse(Response.Status.BAD_REQUEST, "Missing 'tool' property");
        }
        String toolName = node.get("tool").asText();
        com.fasterxml.jackson.databind.JsonNode args = node.get("arguments");

        McpExecutionContext context = resolveExecutionContext(session);

        eu.kalafatic.evolution.servers.mcp.protocol.McpRequest request = new eu.kalafatic.evolution.servers.mcp.protocol.McpRequest();
        request.setMethod("tools/call");
        request.setId(System.currentTimeMillis());

        com.fasterxml.jackson.databind.node.ObjectNode params = objectMapper.createObjectNode();
        params.put("name", toolName);
        params.set("arguments", args);
        request.setParams(params);

        eu.kalafatic.evolution.servers.mcp.protocol.McpResponse response = mcpDispatcher.dispatch(request, context);
        if (response.getError() != null) {
            Map<String, Object> err = new HashMap<>();
            err.put("isError", true);
            err.put("code", response.getError().getCode());
            err.put("message", response.getError().getMessage());
            return jsonResponse(err);
        }
        return jsonResponse(response.getResult());
    }

    private Response handleUiInfo(IHTTPSession session) throws IOException {
        McpExecutionContext context = resolveExecutionContext(session);
        Map<String, Object> info = new java.util.HashMap<>();
        info.put("version", "2024-11-05");
        info.put("serverName", "EvoMcpServer");
        info.put("status", "Running");
        info.put("sessionId", context.getSessionId());
        info.put("principal", context.getPrincipal());
        info.put("toolCount", mcpDispatcher != null ? mcpDispatcher.getToolRegistry().listTools().size() : 0);
        return jsonResponse(info);
    }

    private Response handleMcp(IHTTPSession session) throws Exception {
        if (mcpDispatcher == null) {
            return errorResponse(Response.Status.INTERNAL_ERROR, "MCP Dispatcher not initialized");
        }
        Map<String, String> files = new java.util.HashMap<>();
        session.parseBody(files);
        String postData = files.get("postData");

        McpExecutionContext context = resolveExecutionContext(session);
        eu.kalafatic.evolution.servers.mcp.protocol.McpRequest request = objectMapper.readValue(postData, eu.kalafatic.evolution.servers.mcp.protocol.McpRequest.class);
        eu.kalafatic.evolution.servers.mcp.protocol.McpResponse response = mcpDispatcher.dispatch(request, context);

        return jsonResponse(response);
    }

    private McpExecutionContext resolveExecutionContext(IHTTPSession session) {
        Map<String, String> headers = session.getHeaders();
        String user = headers.get("x-evo-user");
        if (user == null || user.trim().isEmpty()) {
            user = headers.get("X-Evo-User");
        }
        if (user == null || user.trim().isEmpty()) {
            user = "admin"; // Default principal in line with AdminApiController
        }

        String sid = session.getParms() != null ? session.getParms().get("sessionId") : null;
        if (sid == null || sid.trim().isEmpty()) {
            String auth = headers.get("authorization");
            if (auth == null) auth = headers.get("Authorization");
            if (auth != null && auth.startsWith("Bearer ")) {
                sid = auth.substring(7).trim();
            }
        }
        if (sid == null || sid.trim().isEmpty()) {
            sid = "mcp-session-" + System.currentTimeMillis();
        }

        return new McpExecutionContext(sid, user);
    }

    private Response jsonResponse(Object data) throws IOException {
        String json = objectMapper.writeValueAsString(data);
        return NanoHTTPD.newFixedLengthResponse(Response.Status.OK, "application/json", json);
    }

    private Response errorResponse(Response.Status status, String message) {
        try {
            String json = objectMapper.writeValueAsString(Collections.singletonMap("error", message));
            return NanoHTTPD.newFixedLengthResponse(status, "application/json", json);
        } catch (IOException e) {
            return NanoHTTPD.newFixedLengthResponse(status, NanoHTTPD.MIME_PLAINTEXT, message);
        }
    }
}
