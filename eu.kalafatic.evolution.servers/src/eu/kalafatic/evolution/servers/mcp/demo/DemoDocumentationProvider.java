package eu.kalafatic.evolution.servers.mcp.demo;

import fi.iki.elonen.NanoHTTPD;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

public class DemoDocumentationProvider extends NanoHTTPD {
    private final DemoConfiguration config;
    private final DemoDocumentController controller;
    private final DemoDocumentService service;

    public DemoDocumentationProvider(DemoConfiguration config) {
        super(config.getHost(), config.getPort());
        this.config = config;
        this.service = new DemoDocumentService(config.getDocsFolder());
        this.controller = new DemoDocumentController(service);
        initializeDocs();
    }

    private void initializeDocs() {
        File docsDir = new File(config.getDocsFolder());
        if (!docsDir.exists()) {
            docsDir.mkdirs();
        }

        createSampleFile("README.md", "# Demo Documentation\n\nThis is a demo MCP documentation server.\n\nIt exposes Markdown documents that can later be indexed by the Evolution LLM.");
        createSampleFile("getting-started.md", "# Getting Started\n\nTo use this server, simply query the endpoints provided.");
        createSampleFile("architecture.md", "# Architecture\n\nThis demo server follows a clean separation of concerns.");

        File useCasesDir = new File(docsDir, "use-cases");
        if (!useCasesDir.exists()) {
            useCasesDir.mkdirs();
        }
        createSampleFile("use-cases/hello-world.md", "# Hello World Use Case\n\nThis is a simple hello world example.");
    }

    private void createSampleFile(String relativePath, String content) {
        File file = new File(config.getDocsFolder(), relativePath);
        if (!file.exists()) {
            try {
                Files.writeString(file.toPath(), content, StandardCharsets.UTF_8);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public Response serve(IHTTPSession session) {
        return controller.handle(session);
    }

    public void startServer() throws IOException {
        start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
        List<DemoDocumentScanner.DocumentInfo> docs = service.listDocuments();
        System.out.println("-----------------------------------------");
        System.out.println("Demo Documentation MCP");
        System.out.println("-----------------------------------------");
        System.out.println("Host : " + config.getHost());
        System.out.println("Port : " + config.getPort());
        System.out.println("Docs : " + config.getDocsFolder() + "/");
        System.out.println("");
        System.out.println("Markdown files : " + docs.size());
        System.out.println("");
        System.out.println("Server started.");
        System.out.println("-----------------------------------------");
    }

    public void stopServer() {
        stop();
    }
}
