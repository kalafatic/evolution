package eu.kalafatic.forge.controller.api;

public interface ExporterController {
    String exportModel(String sessionId, String modelId, String snapshotId) throws Exception;
}
