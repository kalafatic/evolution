package eu.kalafatic.evolution.servers.mcp.demo;

import eu.kalafatic.evolution.servers.mcp.resources.ResourceRegistry;
import java.io.IOException;

public class McpDocumentResourceProvider implements ResourceRegistry.ResourceProvider {
    private final DemoDocumentService service;

    public McpDocumentResourceProvider(DemoDocumentService service) {
        this.service = service;
    }

    @Override
    public String read(String uri) throws Exception {
        if (uri.startsWith("docs://")) {
            String path = uri.substring(7);
            DemoDocumentService.DocumentContent doc = service.getDocument(path);
            if (doc != null) {
                return doc.getContent();
            }
            throw new IOException("Document not found: " + path);
        }
        throw new IllegalArgumentException("Unsupported URI: " + uri);
    }
}
