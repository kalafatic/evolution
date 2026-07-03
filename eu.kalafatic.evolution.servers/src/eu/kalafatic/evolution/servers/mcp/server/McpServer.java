package eu.kalafatic.evolution.servers.mcp.server;

import fi.iki.elonen.NanoHTTPD;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.kalafatic.evolution.servers.mcp.protocol.McpRequest;
import eu.kalafatic.evolution.servers.mcp.protocol.McpResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class McpServer extends NanoHTTPD {
    private static final Logger logger = LoggerFactory.getLogger(McpServer.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JsonRpcDispatcher dispatcher;

    public McpServer(int port, JsonRpcDispatcher dispatcher) {
        super(port);
        this.dispatcher = dispatcher;
    }

    @Override
    public Response serve(IHTTPSession session) {
        if (Method.POST.equals(session.getMethod())) {
            try {
                Map<String, String> files = new HashMap<>();
                session.parseBody(files);
                String postData = files.get("postData");

                logger.info("Received request: {}", postData);

                McpRequest request = objectMapper.readValue(postData, McpRequest.class);
                McpResponse response = dispatcher.dispatch(request);

                String jsonResponse = objectMapper.writeValueAsString(response);
                logger.info("Sending response: {}", jsonResponse);

                return newFixedLengthResponse(Response.Status.OK, "application/json", jsonResponse);
            } catch (Exception e) {
                logger.error("Error processing request", e);
                return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, e.getMessage());
            }
        }
        return newFixedLengthResponse(Response.Status.METHOD_NOT_ALLOWED, MIME_PLAINTEXT, "Only POST allowed");
    }

    public void startServer() throws IOException {
        start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
        logger.info("MCP Server started on port {}", getListeningPort());
    }

    public void stopServer() {
        stop();
        logger.info("MCP Server stopped.");
    }
}
