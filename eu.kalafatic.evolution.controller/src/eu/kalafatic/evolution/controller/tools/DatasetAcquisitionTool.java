package eu.kalafatic.evolution.controller.tools;

import java.io.File;
import java.util.Map;

import org.json.JSONObject;

import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.forge.data.api.preference.TrainingDataPreferences;
import eu.kalafatic.evolution.forge.data.api.service.TrainingDataAcquisitionRequest;
import eu.kalafatic.evolution.forge.data.api.service.TrainingDataAcquisitionResult;
import eu.kalafatic.evolution.forge.data.api.service.TrainingDataAcquisitionService;
import eu.kalafatic.evolution.forge.data.impl.service.TrainingDataAcquisitionServiceImpl;

/**
 * System tool capability for dataset acquisition and preparation.
 * Interacts with TrainingDataAcquisitionService to download, extract,
 * clean, deduplicate, and package training datasets into .evodata artifacts.
 */
public class DatasetAcquisitionTool implements ITool {

    public static final String NAME = "dataset_acquisition";

    private final TrainingDataAcquisitionService acquisitionService;

    public DatasetAcquisitionTool() {
        this(new TrainingDataAcquisitionServiceImpl());
    }

    public DatasetAcquisitionTool(TrainingDataAcquisitionService acquisitionService) {
        this.acquisitionService = acquisitionService;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String execute(String command, File workingDir, TaskContext context) throws Exception {
        JSONObject params = new JSONObject();
        if (command != null && !command.trim().isEmpty() && command.trim().startsWith("{")) {
            try {
                params = new JSONObject(command);
            } catch (Exception ignored) {}
        }

        // Context metadata override or JSON parameters
        Map<String, Object> metadata = context != null ? context.getMetadata() : Map.of();

        String sourceType = params.optString("sourceType", (String) metadata.getOrDefault("sourceType", "HUGGING_FACE"));
        String repo = params.optString("repository", (String) metadata.getOrDefault("repository", "wikitext"));
        String split = params.optString("split", (String) metadata.getOrDefault("split", "train"));
        long targetUsableBytes = params.optLong("targetUsableBytes",
                ((Number) metadata.getOrDefault("targetUsableBytes", 52_428_800L)).longValue());

        TrainingDataPreferences.Builder builder = TrainingDataPreferences.builder()
                .minimumUsableBytes(targetUsableBytes / 2)
                .targetUsableBytes(targetUsableBytes);

        if ("HUGGING_FACE".equalsIgnoreCase(sourceType) || repo.contains("/")) {
            builder.addDomain(repo);
        }

        TrainingDataAcquisitionRequest request = new TrainingDataAcquisitionRequest()
                .setPreferences(builder.build());

        TrainingDataAcquisitionResult result = acquisitionService.acquireDataset(request);

        JSONObject resObj = new JSONObject();
        resObj.put("status", result.getStatus().name());
        resObj.put("downloadedBytes", result.getDownloadedBytes());
        resObj.put("extractedBytes", result.getExtractedBytes());
        resObj.put("usableContentBytes", result.getAcceptedContentBytes());
        resObj.put("rejectedBytes", result.getRejectedBytes());
        resObj.put("duplicateBytes", result.getDuplicateBytes());
        resObj.put("trainingBytes", result.getTrainingBytes());
        resObj.put("validationBytes", result.getValidationBytes());
        resObj.put("sourcesUsed", result.getSourcesUsed());
        resObj.put("isSourceExhausted", result.isSourceExhausted());

        if (context != null) {
            context.getMetadata().put("usableContentBytes", result.getAcceptedContentBytes());
            context.getMetadata().put("quantity", result.getAcceptedContentBytes());
            context.getMetadata().put("acquisitionStatus", result.getStatus().name());
            context.getMetadata().put("acquisitionResult", result);
        }

        return resObj.toString();
    }
}
