package eu.kalafatic.evolution.forge.data.impl.downloader;

import eu.kalafatic.evolution.forge.data.api.downloader.DataDownloader;
import eu.kalafatic.evolution.forge.data.api.downloader.DownloadRequest;
import eu.kalafatic.evolution.forge.data.api.downloader.DownloadResult;
import eu.kalafatic.evolution.forge.data.api.downloader.TransportClassification;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Specialized Hugging Face download implementation owning exclusively HTTP transport,
 * API requests, connection timeouts, bounded retries, response streaming, and byte metrics.
 */
public class HuggingFaceDownloader implements DataDownloader {

    @Override
    public DownloadResult download(DownloadRequest request) throws IOException {
        int maxAttempts = Math.max(1, request.getMaxRetries() + 1);
        long backoffBase = Math.max(10, request.getBackoffBaseMs());

        DownloadResult lastResult = null;
        IOException lastException = null;

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            if (attempt > 0) {
                long sleepMs = backoffBase * (1L << Math.min(attempt - 1, 4));
                try {
                    Thread.sleep(sleepMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Download interrupted during retry backoff", ie);
                }
            }

            long startMs = System.currentTimeMillis();
            HttpURLConnection conn = null;
            try {
                URL url = URI.create(request.getUrl()).toURL();
                conn = (HttpURLConnection) url.openConnection();
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
                String body = "";
                long totalBytes = 0;
                if (stream != null) {
                    StringBuilder sb = new StringBuilder();
                    try (InputStream in = stream;
                         BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            sb.append(line).append("\n");
                        }
                    }
                    body = sb.toString();
                    totalBytes = body.getBytes(StandardCharsets.UTF_8).length;
                }

                long durationMs = System.currentTimeMillis() - startMs;
                TransportClassification classification = DownloadResult.classify(status, body);

                lastResult = new DownloadResult(
                        status,
                        message != null ? message : "",
                        body,
                        null,
                        totalBytes,
                        contentType,
                        durationMs,
                        attempt,
                        classification
                );

                if (classification.isSuccess() || classification.isPermanentFailure()) {
                    return lastResult;
                }

                if (!classification.isTransient()) {
                    return lastResult;
                }

            } catch (SocketTimeoutException ste) {
                long durationMs = System.currentTimeMillis() - startMs;
                lastException = ste;
                lastResult = new DownloadResult(
                        408,
                        "Request Timeout: " + ste.getMessage(),
                        "",
                        null,
                        0L,
                        null,
                        durationMs,
                        attempt,
                        TransportClassification.TIMEOUT
                );
            } catch (IOException ioe) {
                long durationMs = System.currentTimeMillis() - startMs;
                lastException = ioe;
                lastResult = new DownloadResult(
                        -1,
                        "Connection Error: " + ioe.getMessage(),
                        "",
                        null,
                        0L,
                        null,
                        durationMs,
                        attempt,
                        TransportClassification.CONNECTION_ERROR
                );
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }

        if (lastResult != null) {
            return lastResult;
        }
        throw (lastException != null) ? lastException : new IOException("Download failed for " + request.getUrl());
    }
}
