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

    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_BACKOFF_MS = 300L;

    @Override
    public DownloadResult download(DownloadRequest request) throws IOException {
        int attempt = 0;
        IOException lastException = null;

        while (attempt < MAX_RETRIES) {
            attempt++;
            long startMs = System.currentTimeMillis();
            try {
                URL url = URI.create(request.getUrl()).toURL();
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setInstanceFollowRedirects(true);
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
                int redirects = 0;
                while ((status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM
                        || status == HttpURLConnection.HTTP_SEE_OTHER || status == 307 || status == 308)
                        && redirects < 5) {
                    redirects++;
                    String location = conn.getHeaderField("Location");
                    if (location == null) break;
                    url = URI.create(location).toURL();
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setInstanceFollowRedirects(true);
                    conn.setRequestMethod(request.getMethod());
                    conn.setConnectTimeout(request.getConnectTimeoutMs());
                    conn.setReadTimeout(request.getReadTimeoutMs());
                    if (request.getUserAgent() != null) {
                        conn.setRequestProperty("User-Agent", request.getUserAgent());
                    }
                    for (Map.Entry<String, String> header : request.getHeaders().entrySet()) {
                        conn.setRequestProperty(header.getKey(), header.getValue());
                    }
                    status = conn.getResponseCode();
                }

                String message = conn.getResponseMessage();
                String contentType = conn.getContentType();

                InputStream stream = (status >= 200 && status < 300) ? conn.getInputStream() : conn.getErrorStream();
                if (stream == null) {
                    throw new IOException("Failed connection to " + request.getUrl() + ". HTTP Status: " + status + " (" + message + ")");
                }

                java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                int read;
                try (InputStream in = stream) {
                    while ((read = in.read(buf)) != -1) {
                        baos.write(buf, 0, read);
                    }
                }
                byte[] rawBytes = baos.toByteArray();
                String body = new String(rawBytes, StandardCharsets.UTF_8);
                long totalBytes = rawBytes.length;

                // Check for transient server errors that warrant a retry (e.g. HTTP 500, 502, 503, 504, 429)
                if (status == 429 || (status >= 500 && status <= 504)) {
                    if (attempt < MAX_RETRIES) {
                        long backoff = INITIAL_BACKOFF_MS * (1L << (attempt - 1));
                        System.err.printf("[HF-DOWNLOADER] Transient HTTP %d response on attempt %d/%d for %s. Retrying in %d ms...\n",
                                status, attempt, MAX_RETRIES, request.getUrl(), backoff);
                        try {
                            Thread.sleep(backoff);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new IOException("Download interrupted during retry backoff", ie);
                        }
                        continue;
                    }
                }

                if (status < 200 || status >= 300) {
                    System.err.printf("[HF][DOWNLOAD] dataset_http_error status=%d message=%s bytes=%d url=%s\n",
                            status, message, totalBytes, request.getUrl());
                    return new DownloadResult(status, message, body, rawBytes, null, totalBytes, contentType);
                }

                return new DownloadResult(status, message, body, rawBytes, null, totalBytes, contentType);

            } catch (IOException ioe) {
                lastException = ioe;
                if (attempt < MAX_RETRIES && isTransientError(ioe)) {
                    long backoff = INITIAL_BACKOFF_MS * (1L << (attempt - 1));
                    System.err.printf("[HF-DOWNLOADER] Transient connection failure (%s) on attempt %d/%d for %s. Retrying in %d ms...\n",
                            ioe.getMessage(), attempt, MAX_RETRIES, request.getUrl(), backoff);
                    try {
                        Thread.sleep(backoff);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new IOException("Download interrupted during retry backoff", ie);
                    }
                } else {
                    throw ioe;
                }
            }
        }

        throw lastException != null ? lastException : new IOException("Download failed after " + MAX_RETRIES + " attempts for " + request.getUrl());
    }

    private boolean isTransientError(IOException ioe) {
        String msg = ioe.getMessage() != null ? ioe.getMessage() : "";
        if (msg.contains("HTTP 401") || msg.contains("HTTP 403") || msg.contains("HTTP 404")) {
            return false;
        }
        return true;
    }
}
