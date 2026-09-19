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

    public DownloadResult(int statusCode, String statusMessage, String contentText, InputStream inputStream, long downloadedBytes, String contentType) {
        this.statusCode = statusCode;
        this.statusMessage = statusMessage;
        this.contentText = contentText;
        this.inputStream = inputStream;
        this.downloadedBytes = downloadedBytes;
        this.contentType = contentType;
    }

    public boolean isSuccess() {
        return statusCode >= 200 && statusCode < 300;
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

    @Override
    public void close() {
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (Exception ignored) {}
        }
    }
}
