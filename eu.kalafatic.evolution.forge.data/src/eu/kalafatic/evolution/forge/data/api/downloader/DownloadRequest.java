package eu.kalafatic.evolution.forge.data.api.downloader;

import java.util.HashMap;
import java.util.Map;

/**
 * Value object specifying a single remote transport download request.
 */
public class DownloadRequest {

    private final String url;
    private String method = "GET";
    private int connectTimeoutMs = 5000;
    private int readTimeoutMs = 10000;
    private String userAgent = "EVO-Forge-Client/2.6";
    private final Map<String, String> headers = new HashMap<>();

    public DownloadRequest(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    public String getMethod() {
        return method;
    }

    public DownloadRequest setMethod(String method) {
        this.method = method;
        return this;
    }

    public int getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public DownloadRequest setConnectTimeoutMs(int connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
        return this;
    }

    public int getReadTimeoutMs() {
        return readTimeoutMs;
    }

    public DownloadRequest setReadTimeoutMs(int readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
        return this;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public DownloadRequest setUserAgent(String userAgent) {
        this.userAgent = userAgent;
        return this;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public DownloadRequest addHeader(String key, String value) {
        this.headers.put(key, value);
        return this;
    }
}
