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
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

/**
 * Specialized Hugging Face download implementation owning exclusively HTTP transport,
 * API requests, connection timeouts, retries, redirect resolution, compressed streaming, and byte metrics.
 */
public class HuggingFaceDownloader implements DataDownloader {

    private static final int MAX_REDIRECTS = 5;
    private static final int MAX_RETRIES = 3;

    @Override
    public DownloadResult download(DownloadRequest request) throws IOException {
        IOException lastEx = null;
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                return executeDownloadWithRedirects(request, request.getUrl(), 0);
            } catch (IOException e) {
                lastEx = e;
                if (attempt < MAX_RETRIES) {
                    try {
                        Thread.sleep(200L * attempt);
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        throw lastEx != null ? lastEx : new IOException("Download failed after " + MAX_RETRIES + " retries for " + request.getUrl());
    }

    private DownloadResult executeDownloadWithRedirects(DownloadRequest request, String currentUrl, int redirectCount) throws IOException {
        if (redirectCount > MAX_REDIRECTS) {
            throw new IOException("Too many HTTP redirects (exceeded " + MAX_REDIRECTS + ") attempting to fetch " + request.getUrl());
        }

        URL url = URI.create(currentUrl).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setInstanceFollowRedirects(true);
        conn.setRequestMethod(request.getMethod());
        conn.setConnectTimeout(request.getConnectTimeoutMs());
        conn.setReadTimeout(request.getReadTimeoutMs());
        conn.setRequestProperty("Accept-Encoding", "gzip, deflate");

        if (request.getUserAgent() != null) {
            conn.setRequestProperty("User-Agent", request.getUserAgent());
        }
        for (Map.Entry<String, String> header : request.getHeaders().entrySet()) {
            conn.setRequestProperty(header.getKey(), header.getValue());
        }

        int status = conn.getResponseCode();
        String message = conn.getResponseMessage();
        String contentType = conn.getContentType();

        if (status == HttpURLConnection.HTTP_MOVED_PERM || status == HttpURLConnection.HTTP_MOVED_TEMP
                || status == HttpURLConnection.HTTP_SEE_OTHER || status == 307 || status == 308) {
            String redirectUrl = conn.getHeaderField("Location");
            if (redirectUrl != null && !redirectUrl.isEmpty()) {
                conn.disconnect();
                return executeDownloadWithRedirects(request, redirectUrl, redirectCount + 1);
            }
        }

        InputStream rawStream = (status >= 200 && status < 300) ? conn.getInputStream() : conn.getErrorStream();
        if (rawStream == null) {
            throw new IOException("Failed connection to " + currentUrl + ". HTTP Status: " + status + " (" + message + ")");
        }

        String encoding = conn.getContentEncoding();
        InputStream stream = rawStream;
        if ("gzip".equalsIgnoreCase(encoding)) {
            stream = new GZIPInputStream(rawStream);
        } else if ("deflate".equalsIgnoreCase(encoding)) {
            stream = new InflaterInputStream(rawStream);
        }

        StringBuilder sb = new StringBuilder();
        long totalBytes = 0;
        try (InputStream in = stream;
             BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        } finally {
            conn.disconnect();
        }

        String body = sb.toString();
        totalBytes = body.getBytes(StandardCharsets.UTF_8).length;

        if (status < 200 || status >= 300) {
            throw new IOException("Hugging Face HTTP Error (" + status + "): " + body.trim());
        }

        return new DownloadResult(status, message, body, null, totalBytes, contentType);
    }
}
