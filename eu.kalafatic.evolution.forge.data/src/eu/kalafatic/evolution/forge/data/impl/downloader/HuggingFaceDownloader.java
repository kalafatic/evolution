package eu.kalafatic.evolution.forge.data.impl.downloader;

import eu.kalafatic.evolution.forge.data.api.downloader.DataDownloader;
import eu.kalafatic.evolution.forge.data.api.downloader.DownloadRequest;
import eu.kalafatic.evolution.forge.data.api.downloader.DownloadResult;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Specialized Hugging Face download implementation owning exclusively HTTP transport,
 * API requests, connection timeouts, retries, response streaming, and byte metrics.
 */
public class HuggingFaceDownloader implements DataDownloader {

    @Override
    public DownloadResult download(DownloadRequest request) throws IOException {
        URL url = URI.create(request.getUrl()).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(request.getMethod());
        conn.setConnectTimeout(request.getConnectTimeoutMs());
        conn.setReadTimeout(request.getReadTimeoutMs());
        if (request.getUserAgent() != null) {
            conn.setRequestProperty("User-Agent", request.getUserAgent());
        }
        for (Map.Entry<String, String> header : request.getHeaders().entrySet()) {
            conn.setRequestProperty(header.getKey(), header.getValue());
        }

        int status = conn.getResponseCode();
        String message = conn.getResponseMessage();
        String contentType = conn.getContentType();

        InputStream stream = (status >= 200 && status < 300) ? conn.getInputStream() : conn.getErrorStream();
        if (stream == null) {
            throw new IOException("Failed connection to " + request.getUrl() + ". HTTP Status: " + status + " (" + message + ")");
        }

        StringBuilder sb = new StringBuilder();
        long totalBytes = 0;
        try (InputStream in = stream;
             BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        String body = sb.toString();
        totalBytes = body.getBytes(StandardCharsets.UTF_8).length;

        if (status < 200 || status >= 300) {
            throw new IOException("Hugging Face HTTP Error (" + status + "): " + body.trim());
        }

        return new DownloadResult(status, message, body, null, totalBytes, contentType);
    }
}
