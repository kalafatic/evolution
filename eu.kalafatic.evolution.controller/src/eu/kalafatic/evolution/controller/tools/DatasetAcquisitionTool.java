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

        Map<String, Object> metadata = context != null ? context.getMetadata() : Map.of();

        String sourceType = params.optString("sourceType", (String) metadata.get("sourceType"));
        String repo = params.optString("repository", (String) metadata.get("repository"));
        if (repo == null || repo.trim().isEmpty()) {
            repo = params.optString("domain", (String) metadata.get("domain"));
        }
        String split = params.optString("split", (String) metadata.get("split"));

        long targetUsableBytes = 0;
        if (params.has("targetUsableBytes")) {
            targetUsableBytes = params.getLong("targetUsableBytes");
        } else if (params.has("targetMetric")) {
            targetUsableBytes = params.getLong("targetMetric");
        } else if (metadata.containsKey("targetUsableBytes")) {
            targetUsableBytes = ((Number) metadata.get("targetUsableBytes")).longValue();
        } else if (metadata.containsKey("targetMetric")) {
            targetUsableBytes = ((Number) metadata.get("targetMetric")).longValue();
        }

        long minUsableBytes = 0;
        if (params.has("minimumUsableBytes")) {
            minUsableBytes = params.getLong("minimumUsableBytes");
        } else if (metadata.containsKey("minimumUsableBytes")) {
            minUsableBytes = ((Number) metadata.get("minimumUsableBytes")).longValue();
        } else {
            minUsableBytes = targetUsableBytes;
        }

        TrainingDataPreferences.Builder builder = TrainingDataPreferences.builder()
                .minimumUsableBytes(minUsableBytes)
                .targetUsableBytes(targetUsableBytes);

        if (sourceType != null && !sourceType.trim().isEmpty()) {
            builder.addProvider(sourceType.trim());
        }

        if (repo != null && !repo.trim().isEmpty()) {
            builder.addDomain(repo.trim());
        }

        if (split != null && !split.trim().isEmpty()) {
            builder.contentType(split.trim());
        }

        TrainingDataAcquisitionRequest request = new TrainingDataAcquisitionRequest()
                .setPreferences(builder.build());

        TrainingDataAcquisitionResult result = acquisitionService.acquireDataset(request);

        JSONObject resObj = new JSONObject();
        resObj.put("status", result.getStatus().name());
        resObj.put("requestedMinimumUsableBytes", result.getRequestedMinimumUsableBytes());
        resObj.put("downloadedBytes", result.getDownloadedBytes());
        resObj.put("extractedBytes", result.getExtractedBytes());
        resObj.put("usableContentBytes", result.getAcceptedContentBytes());
        resObj.put("quantity", result.getAcceptedContentBytes());
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
            context.getMetadata().put("isSourceExhausted", result.isSourceExhausted());
            context.getMetadata().put("acquisitionResult", result);
        }

        return resObj.toString();
    }
}
