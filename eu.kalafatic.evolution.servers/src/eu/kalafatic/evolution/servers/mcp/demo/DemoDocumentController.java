package eu.kalafatic.evolution.servers.mcp.demo;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Response;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

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

    public Response handle(IHTTPSession session) {
        String uri = session.getUri();
        try {
            if (session.getMethod() == NanoHTTPD.Method.GET && "/".equals(uri)) {
                return serveMcpHtml();
            }
            if (session.getMethod() == NanoHTTPD.Method.POST && "/mcp".equals(uri)) {
                return handleMcp(session);
            }
            if (session.getMethod() == NanoHTTPD.Method.GET && "/ui/tools".equals(uri)) {
                return handleUiTools();
            }
            if (session.getMethod() == NanoHTTPD.Method.POST && "/ui/execute".equals(uri)) {
                return handleUiExecute(session);
            }
            if (session.getMethod() == NanoHTTPD.Method.GET && "/ui/info".equals(uri)) {
                return handleUiInfo();
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

    private Response handleUiTools() throws IOException {
        if (mcpDispatcher == null) {
            return errorResponse(Response.Status.INTERNAL_ERROR, "MCP Dispatcher not initialized");
        }
        return jsonResponse(mcpDispatcher.getToolRegistry().listTools());
    }

    private Response handleUiExecute(IHTTPSession session) throws Exception {
        if (mcpDispatcher == null) {
            return errorResponse(Response.Status.INTERNAL_ERROR, "MCP Dispatcher not initialized");
        }
        Map<String, String> files = new java.util.HashMap<>();
        session.parseBody(files);
        String postData = files.get("postData");

        com.fasterxml.jackson.databind.JsonNode node = objectMapper.readTree(postData);
        String toolName = node.get("tool").asText();
        com.fasterxml.jackson.databind.JsonNode args = node.get("arguments");

        eu.kalafatic.evolution.servers.mcp.protocol.McpRequest request = new eu.kalafatic.evolution.servers.mcp.protocol.McpRequest();
        request.setMethod("tools/call");
        request.setId(System.currentTimeMillis());

        com.fasterxml.jackson.databind.node.ObjectNode params = objectMapper.createObjectNode();
        params.put("name", toolName);
        params.set("arguments", args);
        request.setParams(params);

        eu.kalafatic.evolution.servers.mcp.protocol.McpResponse response = mcpDispatcher.dispatch(request);
        return jsonResponse(response.getResult());
    }

    private Response handleUiInfo() throws IOException {
        Map<String, Object> info = new java.util.HashMap<>();
        info.put("version", "1.0");
        info.put("status", "Running");
        info.put("uptime", "N/A"); // Could be calculated
        return jsonResponse(info);
    }

    private Response handleMcp(IHTTPSession session) throws Exception {
        if (mcpDispatcher == null) {
            return errorResponse(Response.Status.INTERNAL_ERROR, "MCP Dispatcher not initialized");
        }
        Map<String, String> files = new java.util.HashMap<>();
        session.parseBody(files);
        String postData = files.get("postData");

        eu.kalafatic.evolution.servers.mcp.protocol.McpRequest request = objectMapper.readValue(postData, eu.kalafatic.evolution.servers.mcp.protocol.McpRequest.class);
        eu.kalafatic.evolution.servers.mcp.protocol.McpResponse response = mcpDispatcher.dispatch(request);

        return jsonResponse(response);
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
