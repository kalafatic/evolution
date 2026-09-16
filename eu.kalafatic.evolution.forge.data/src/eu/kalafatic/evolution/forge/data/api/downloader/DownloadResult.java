package eu.kalafatic.evolution.forge.data.api.downloader;

import java.io.InputStream;

/**
 * Value object representing the result of a download request.
 */
public class DownloadResult implements java.io.Closeable {

    private final int statusCode;
    private final String statusMessage;
    private final String contentText;
    private final InputStream inputStream;
    private final long downloadedBytes;
    private final String contentType;
    private final long durationMs;
    private final int retryCount;
    private final TransportClassification transportClassification;

    public DownloadResult(int statusCode, String statusMessage, String contentText, InputStream inputStream, long downloadedBytes, String contentType) {
        this(statusCode, statusMessage, contentText, inputStream, downloadedBytes, contentType, 0L, 0, classify(statusCode, contentText));
    }

    public DownloadResult(int statusCode, String statusMessage, String contentText, InputStream inputStream, long downloadedBytes, String contentType, long durationMs, int retryCount, TransportClassification transportClassification) {
        this.statusCode = statusCode;
        this.statusMessage = statusMessage;
        this.contentText = contentText;
        this.inputStream = inputStream;
        this.downloadedBytes = downloadedBytes;
        this.contentType = contentType;
        this.durationMs = durationMs;
        this.retryCount = retryCount;
        this.transportClassification = transportClassification != null ? transportClassification : classify(statusCode, contentText);
    }

    public static TransportClassification classify(int statusCode, String contentText) {
        if (statusCode >= 200 && statusCode < 300) {
            if (contentText != null && contentText.trim().isEmpty()) {
                return TransportClassification.EMPTY_RESPONSE;
            }
            return TransportClassification.HTTP_SUCCESS;
        }
        if (statusCode == 401) return TransportClassification.HTTP_401;
        if (statusCode == 403) return TransportClassification.HTTP_403;
        if (statusCode == 404) return TransportClassification.HTTP_404;
        if (statusCode == 429) return TransportClassification.HTTP_429;
        if (statusCode >= 500 && statusCode < 600) return TransportClassification.HTTP_5XX;
        if (statusCode == 408) return TransportClassification.TIMEOUT;
        if (statusCode == -1) return TransportClassification.CONNECTION_ERROR;
        return TransportClassification.HTTP_OTHER;
    }

    public boolean isSuccess() {
        return transportClassification == TransportClassification.HTTP_SUCCESS || (statusCode >= 200 && statusCode < 300 && transportClassification != TransportClassification.EMPTY_RESPONSE);
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public String getContentText() {
        return contentText;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public long getDownloadedBytes() {
        return downloadedBytes;
    }

    public String getContentType() {
        return contentType;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public TransportClassification getTransportClassification() {
        return transportClassification;
    }

    @Override
    public void close() {
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (Exception ignored) {}
        }
    }
}
