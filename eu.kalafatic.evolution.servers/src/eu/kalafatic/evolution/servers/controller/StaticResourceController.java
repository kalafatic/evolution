package eu.kalafatic.evolution.servers.controller;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Response;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class StaticResourceController {
    private static final String BASE_PATH = "/eu/kalafatic/evolution/servers/web";

    public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        if ("/".equals(uri)) {
            uri = "/login.html";
        }

        if (uri.endsWith("/evolution.css") || uri.endsWith("/evolution.js")) {
            String fileName = uri.substring(uri.lastIndexOf('/') + 1);
            InputStream is = getClass().getResourceAsStream("/eu/kalafatic/evolution/controller/orchestration/" + fileName);
            if (is == null) {
                // Fallback to absolute if needed
                is = getClass().getResourceAsStream("/" + fileName);
            }
            if (is != null) {
                try {
                    byte[] data = readAllBytes(is);
                    return NanoHTTPD.newFixedLengthResponse(Response.Status.OK, getMimeType(uri), new java.io.ByteArrayInputStream(data), data.length);
                } catch (IOException e) {}
            }
        }

        String resourcePath = BASE_PATH + uri;
        InputStream is = getClass().getResourceAsStream(resourcePath);

        if (is == null) {
            return NanoHTTPD.newFixedLengthResponse(Response.Status.NOT_FOUND, NanoHTTPD.MIME_PLAINTEXT, "404 Not Found: " + uri);
        }

        String mimeType = getMimeType(uri);
        try {
            byte[] data = readAllBytes(is);
            return NanoHTTPD.newFixedLengthResponse(Response.Status.OK, mimeType, new java.io.ByteArrayInputStream(data), data.length);
        } catch (IOException e) {
            return NanoHTTPD.newFixedLengthResponse(Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT, "Internal Error: " + e.getMessage());
        }
    }

    private byte[] readAllBytes(InputStream is) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[16384];
        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        return buffer.toByteArray();
    }

    private String getMimeType(String uri) {
        if (uri.endsWith(".html")) return "text/html";
        if (uri.endsWith(".css")) return "text/css";
        if (uri.endsWith(".js")) return "application/javascript";
        if (uri.endsWith(".png")) return "image/png";
        if (uri.endsWith(".jpg") || uri.endsWith(".jpeg")) return "image/jpeg";
        if (uri.endsWith(".svg")) return "image/svg+xml";
        return "application/octet-stream";
    }
}
