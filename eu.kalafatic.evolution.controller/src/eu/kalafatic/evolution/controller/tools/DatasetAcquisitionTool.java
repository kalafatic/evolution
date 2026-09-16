package eu.kalafatic.evolution.controller.tools;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.cognitive.loop.CognitiveFailureType;
import eu.kalafatic.evolution.forge.data.api.preference.TrainingDataPreferences;
import eu.kalafatic.evolution.forge.data.api.service.TrainingDataAcquisitionRequest;
import eu.kalafatic.evolution.forge.data.api.service.TrainingDataAcquisitionResult;
import eu.kalafatic.evolution.forge.data.api.service.TrainingDataAcquisitionService;
import eu.kalafatic.evolution.forge.data.api.source.DatasetSourceConfig;
import eu.kalafatic.evolution.forge.data.impl.service.TrainingDataAcquisitionServiceImpl;
import eu.kalafatic.evolution.forge.data.impl.source.HuggingFaceDatasetSource;
import eu.kalafatic.evolution.forge.data.impl.source.LocalDatasetSource;

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

        String sourceType = params.optString("sourceType", null);
        if (sourceType == null || sourceType.trim().isEmpty()) {
            sourceType = (String) metadata.get("sourceType");
        }
        if (sourceType == null || sourceType.trim().isEmpty()) {
            sourceType = "HUGGING_FACE";
        }

        String repo = params.optString("repository", null);
        if (repo == null || repo.trim().isEmpty()) {
            repo = params.optString("domain", null);
        }
        if (repo == null || repo.trim().isEmpty()) {
            repo = (String) metadata.get("repository");
        }
        if (repo == null || repo.trim().isEmpty()) {
            repo = (String) metadata.get("domain");
        }
        if (repo == null || repo.trim().isEmpty()) {
            repo = "Salesforce/wikitext";
        }

        String split = params.optString("split", null);
        if (split == null || split.trim().isEmpty()) {
            split = (String) metadata.get("split");
        }
        if (split == null || split.trim().isEmpty()) {
            split = "train";
        }

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

        if ("HUGGING_FACE".equalsIgnoreCase(sourceType)) {
            DatasetSourceConfig cfg = new DatasetSourceConfig("HUGGING_FACE", repo);
            cfg.setSplit(split);
            request.addSource(new HuggingFaceDatasetSource(cfg));
        } else if ("LOCAL".equalsIgnoreCase(sourceType)) {
            DatasetSourceConfig cfg = new DatasetSourceConfig("LOCAL", repo);
            request.addSource(new LocalDatasetSource(cfg));
        }

        TrainingDataAcquisitionResult result;
        try {
            result = acquisitionService.acquireDataset(request);
        } catch (Exception ex) {
            String exMsg = ex.getMessage() != null ? ex.getMessage() : ex.toString();
            CognitiveFailureType failureType;
            if (exMsg.contains("401") || exMsg.contains("403") || exMsg.contains("AUTH")) {
                failureType = CognitiveFailureType.AUTHENTICATION_FAILURE;
            } else if (exMsg.contains("404") || exMsg.contains("Not found") || exMsg.contains("Config not found")) {
                failureType = CognitiveFailureType.INVALID_CONFIGURATION;
            } else if (exMsg.contains("Connect") || exMsg.contains("Timeout") || exMsg.contains("Network") || exMsg.contains("CONNECTION_ERROR")) {
                failureType = CognitiveFailureType.NETWORK_FAILURE;
            } else {
                failureType = CognitiveFailureType.CAPABILITY_FAILURE;
            }

            JSONObject errObj = new JSONObject();
            errObj.put("status", "FAILED");
            errObj.put("requestedMinimumUsableBytes", targetUsableBytes);
            errObj.put("usableContentBytes", 0);
            errObj.put("quantity", 0);
            errObj.put("remainingBytes", targetUsableBytes);
            errObj.put("sourceType", sourceType);
            errObj.put("repository", repo);
            errObj.put("split", split);
            errObj.put("isSourceExhausted", false);
            errObj.put("failureType", failureType.name());
            errObj.put("failureReason", exMsg);
            errObj.put("recommendedNextActions", new JSONArray(List.of("TRY_OTHER_DATASET", "TRY_OTHER_SOURCE")));

            if (context != null) {
                context.getMetadata().put("usableContentBytes", 0L);
                context.getMetadata().put("quantity", 0L);
                context.getMetadata().put("remainingBytes", targetUsableBytes);
                context.getMetadata().put("sourceType", sourceType);
                context.getMetadata().put("repository", repo);
                context.getMetadata().put("split", split);
                context.getMetadata().put("failureType", failureType.name());
                context.getMetadata().put("isSourceExhausted", false);
            }
            return errObj.toString();
        }

        long usableBytes = result.getAcceptedContentBytes();
        long remaining = Math.max(0, targetUsableBytes - usableBytes);
        boolean targetReached = result.isTargetReached() || (targetUsableBytes > 0 && usableBytes >= targetUsableBytes);

        String failureReason = result.getFailureReason() != null ? result.getFailureReason() : "";

        CognitiveFailureType failureType;
        if (targetReached) {
            failureType = CognitiveFailureType.TARGET_REACHED;
        } else if (failureReason.contains("HTTP 401") || failureReason.contains("HTTP 403") || failureReason.contains("AUTHENTICATION")) {
            failureType = CognitiveFailureType.AUTHENTICATION_FAILURE;
        } else if (failureReason.contains("HTTP 404") || failureReason.contains("Not found") || failureReason.contains("Config not found") || failureReason.contains("Split not found")) {
            failureType = CognitiveFailureType.INVALID_CONFIGURATION;
        } else if (failureReason.contains("CONNECTION_ERROR") || failureReason.contains("TIMEOUT") || failureReason.contains("Network")) {
            failureType = CognitiveFailureType.NETWORK_FAILURE;
        } else if (result.getDownloadedBytes() == 0 && usableBytes == 0) {
            failureType = CognitiveFailureType.SOURCE_EMPTY;
        } else if (result.isSourceExhausted() || usableBytes == 0) {
            failureType = CognitiveFailureType.SOURCE_EXHAUSTED;
        } else {
            failureType = CognitiveFailureType.CAPABILITY_FAILURE;
        }

        List<String> nextActions = targetReached ? List.of() : List.of("TRY_OTHER_SPLIT", "TRY_OTHER_DATASET", "TRY_OTHER_SOURCE");

        JSONObject resObj = new JSONObject();
        resObj.put("status", result.getStatus().name());
        resObj.put("requestedMinimumUsableBytes", result.getRequestedMinimumUsableBytes());
        resObj.put("downloadedBytes", result.getDownloadedBytes());
        resObj.put("extractedBytes", result.getExtractedBytes());
        resObj.put("usableContentBytes", usableBytes);
        resObj.put("quantity", usableBytes);
        resObj.put("remainingBytes", remaining);
        resObj.put("rejectedBytes", result.getRejectedBytes());
        resObj.put("duplicateBytes", result.getDuplicateBytes());
        resObj.put("trainingBytes", result.getTrainingBytes());
        resObj.put("validationBytes", result.getValidationBytes());
        resObj.put("sourcesUsed", result.getSourcesUsed());
        resObj.put("sourceType", sourceType);
        resObj.put("repository", repo);
        resObj.put("split", split);
        resObj.put("isSourceExhausted", result.isSourceExhausted());
        resObj.put("failureType", failureType.name());
        resObj.put("failureReason", failureReason);
        resObj.put("recommendedNextActions", new JSONArray(nextActions));

        if (context != null) {
            context.getMetadata().put("usableContentBytes", usableBytes);
            context.getMetadata().put("quantity", usableBytes);
            context.getMetadata().put("remainingBytes", remaining);
            context.getMetadata().put("sourceType", sourceType);
            context.getMetadata().put("repository", repo);
            context.getMetadata().put("split", split);
            context.getMetadata().put("acquisitionStatus", result.getStatus().name());
            context.getMetadata().put("isSourceExhausted", result.isSourceExhausted());
            context.getMetadata().put("failureType", failureType.name());
            context.getMetadata().put("recommendedNextActions", nextActions);
            context.getMetadata().put("acquisitionResult", result);
        }

        return resObj.toString();
    }
}
