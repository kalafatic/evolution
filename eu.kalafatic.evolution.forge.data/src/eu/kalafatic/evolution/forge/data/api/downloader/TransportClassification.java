package eu.kalafatic.evolution.forge.data.api.downloader;

/**
 * Structured transport classification for remote download responses and network errors.
 */
public enum TransportClassification {
    HTTP_SUCCESS,
    CONNECTION_ERROR,
    TIMEOUT,
    HTTP_401,
    HTTP_403,
    HTTP_404,
    HTTP_429,
    HTTP_5XX,
    HTTP_OTHER,
    EMPTY_RESPONSE;

    public boolean isSuccess() {
        return this == HTTP_SUCCESS;
    }

    public boolean isTransient() {
        return this == TIMEOUT || this == CONNECTION_ERROR || this == HTTP_429 || this == HTTP_5XX;
    }

    public boolean isPermanentFailure() {
        return this == HTTP_401 || this == HTTP_403 || this == HTTP_404;
    }
}
