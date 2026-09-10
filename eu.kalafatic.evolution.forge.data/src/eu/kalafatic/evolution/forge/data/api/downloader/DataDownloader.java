package eu.kalafatic.evolution.forge.data.api.downloader;

import java.io.IOException;

/**
 * Contract for transport-level remote dataset downloading and stream acquisition.
 * Handles HTTP/network transport, headers, pagination offsets, byte tracking, retries, and errors.
 * Does not own record parsing, normalization, filtering, model training, or dataset composition.
 */
public interface DataDownloader {

    /**
     * Executes a download request for remote dataset content or API metadata.
     *
     * @param request the download request configuration
     * @return result containing response content, status code, headers, and downloaded byte metrics
     * @throws IOException if network or transport errors occur
     */
    DownloadResult download(DownloadRequest request) throws IOException;
}
