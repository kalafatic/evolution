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
            if (session.getMethod() == NanoHTTPD.Method.POST && "/mcp".equals(uri)) {
                return handleMcp(session);
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
