package eu.kalafatic.evolution.supervisor.bootstrap;

public class CopyResult {
    private final boolean success;
    private final String message;
    private int filesCopied;
    private long totalBytes;
    private long durationMs;

    public CopyResult(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public int getFilesCopied() {
        return filesCopied;
    }

    public void setFilesCopied(int filesCopied) {
        this.filesCopied = filesCopied;
    }

    public long getTotalBytes() {
        return totalBytes;
    }

    public void setTotalBytes(long totalBytes) {
        this.totalBytes = totalBytes;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }

    @Override
    public String toString() {
        return String.format("CopyResult{success=%s, message='%s', filesCopied=%d, totalBytes=%d, durationMs=%d}",
                success, message, filesCopied, totalBytes, durationMs);
    }
}
