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

    public DemoDocumentController(DemoDocumentService service) {
        this.service = service;
    }

    public Response handle(IHTTPSession session) {
        String uri = session.getUri();
        try {
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
