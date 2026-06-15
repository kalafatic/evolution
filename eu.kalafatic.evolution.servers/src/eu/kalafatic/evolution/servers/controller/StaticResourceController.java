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

        String resourcePath = BASE_PATH + uri;
        InputStream is = getClass().getResourceAsStream(resourcePath);

        if (is == null) {
            return NanoHTTPD.newFixedLengthResponse(Response.Status.NOT_FOUND, NanoHTTPD.MIME_PLAINTEXT, "404 Not Found: " + uri);
        }

        String mimeType = getMimeType(uri);
        try {
            return NanoHTTPD.newFixedLengthResponse(Response.Status.OK, mimeType, is, is.available());
        } catch (IOException e) {
            return NanoHTTPD.newFixedLengthResponse(Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT, "Internal Error");
        }
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
