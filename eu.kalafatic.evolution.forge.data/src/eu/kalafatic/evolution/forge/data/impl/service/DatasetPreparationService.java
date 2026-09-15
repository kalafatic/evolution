package eu.kalafatic.evolution.forge.data.impl.service;

import eu.kalafatic.evolution.forge.data.api.NormalizedMessage;
import eu.kalafatic.evolution.forge.data.api.NormalizedSample;
import eu.kalafatic.evolution.forge.data.api.TrainingSampleType;
import eu.kalafatic.evolution.forge.data.api.artifact.EvoDatasetArtifact;
import eu.kalafatic.evolution.forge.data.api.source.DatasetInspection;
import eu.kalafatic.evolution.forge.data.api.source.DatasetItem;
import eu.kalafatic.evolution.forge.data.api.source.DatasetPreparationContext;
import eu.kalafatic.evolution.forge.data.api.source.DatasetSourceAdapter;
import eu.kalafatic.evolution.forge.data.api.source.DatasetSourceConfig;
import eu.kalafatic.evolution.forge.data.api.source.DatasetSourceStats;
import eu.kalafatic.evolution.forge.data.api.source.SourceAdapterRegistry;
import eu.kalafatic.evolution.forge.data.api.service.DatasetPreparationResult;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Authoritative dataset preparation service converting user-selected dataset items into a unified EVO native .evodata dataset artifact.
 */
public class DatasetPreparationService {

    private final SourceAdapterRegistry registry;

    public DatasetPreparationService() {
        this(SourceAdapterRegistry.createDefaultRegistry());
    }

    public DatasetPreparationService(SourceAdapterRegistry registry) {
        this.registry = registry != null ? registry : SourceAdapterRegistry.createDefaultRegistry();
    }

    public DatasetPreparationResult prepareDatasets(List<DatasetItem> datasetItems, DatasetPreparationContext context, File outputDir) throws Exception {
        DatasetPreparationResult result = new DatasetPreparationResult();
        if (context == null) {
            context = new DatasetPreparationContext();
        }

        if (outputDir == null) {
            outputDir = new File(System.getProperty("user.home"), "workspace/datasets");
        }
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        // 1. Filter to checked items while strictly preserving user-defined order
        List<DatasetItem> checkedItems = new ArrayList<>();
        if (datasetItems != null) {
            for (DatasetItem item : datasetItems) {
                if (item != null && item.isChecked() && item.getPath() != null && !item.getPath().trim().isEmpty()) {
                    checkedItems.add(item);
                }
            }
        }

        context.log("[forge.dataset]");
        context.log("[forge.dataset] Selected dataset items: " + checkedItems.size());

        if (checkedItems.isEmpty()) {
            context.log("[forge.dataset] Warning: No checked dataset sources selected.");
            result.setStatus(DatasetPreparationResult.Status.INSUFFICIENT_DATA);
            return result;
        }

        // 2. Compute deterministic cache key for source identity & configuration reuse
        String cacheKey = computeCacheKey(checkedItems, context.getTargetUsableBytes());
        File cacheFile = new File(outputDir, "prepared_" + cacheKey + ".evodata");

        if (cacheFile.exists() && cacheFile.length() > 0) {
            try {
                EvoDatasetArtifact cachedArtifact = EvoDatasetArtifact.load(cacheFile);
                if (cachedArtifact != null && cachedArtifact.getStatus() == EvoDatasetArtifact.Status.READY && !cachedArtifact.getTrainSamples().isEmpty()) {
                    context.log("[forge.dataset] Reusing cached prepared dataset artifact: " + cacheFile.getName());
                    result.setArtifact(cachedArtifact);
                    result.setSamples(cachedArtifact.getSamples());
                    result.setStatus(DatasetPreparationResult.Status.SUCCESS);
                    result.setOutputPath(cacheFile.getAbsolutePath());

                    populateResultMetrics(result, cachedArtifact.getSamples(), checkedItems.size());
                    logFinalSummary(context, result, cacheFile.getAbsolutePath());
                    return result;
                }
            } catch (Exception ex) {
                context.log("[forge.dataset] Cache artifact loading failed, rebuilding: " + ex.getMessage());
            }
        }

        // 3. Process each checked source using adapter registry in exact order
        List<NormalizedSample> allRawSamples = new ArrayList<>();
        int sourceIndex = 1;
        int totalN = checkedItems.size();

        for (DatasetItem item : checkedItems) {
            if (context.isCancelled()) {
                context.log("[forge.dataset] Dataset preparation cancelled.");
                result.setStatus(DatasetPreparationResult.Status.CANCELLED);
                return result;
            }

            DatasetSourceAdapter adapter = registry.findAdapter(item);
            String adapterName = adapter != null ? adapter.getClass().getSimpleName() : "NONE";

            context.log("[forge.dataset]");
            context.log("[forge.dataset] Preparing source " + sourceIndex + "/" + totalN);
            context.log("Path: " + item.getPath());
            context.log("Type: " + item.getType());
            context.log("Adapter: " + adapterName);

            if (adapter == null) {
                context.log("Status: SKIPPED (No matching adapter found)");
                sourceIndex++;
                continue;
            }

            DatasetInspection inspection = adapter.inspect(item);
            if (!inspection.isSupported() || !inspection.isExists()) {
                context.log("Status: FAILED (" + inspection.getDetails() + ")");
                sourceIndex++;
                continue;
            }

            try {
                List<NormalizedSample> converted = adapter.convert(item, context);
                context.log("Status: SUCCESS (" + (converted != null ? converted.size() : 0) + " samples)");
                if (converted != null) {
                    allRawSamples.addAll(converted);
                }
            } catch (Exception ex) {
                context.log("Status: FAILED (" + ex.getMessage() + ")");
            }
            sourceIndex++;
        }

        // 4. Deduplicate and enforce target size
        Set<String> seenHashes = new HashSet<>();
        List<NormalizedSample> deduplicatedSamples = new ArrayList<>();
        long currentUsableBytes = 0;
        long targetBytes = context.getTargetUsableBytes();
        long duplicateBytes = 0;

        for (NormalizedSample sample : allRawSamples) {
            if (context.isCancelled()) break;
            String h = sample.getHash();
            if (h != null && !h.isEmpty() && seenHashes.contains(h)) {
                duplicateBytes += sample.toFullText().getBytes(StandardCharsets.UTF_8).length;
                continue;
            }
            if (h != null && !h.isEmpty()) {
                seenHashes.add(h);
            }

            byte[] sampleBytes = sample.toFullText().getBytes(StandardCharsets.UTF_8);
            if (targetBytes > 0 && currentUsableBytes >= targetBytes) {
                break; // Target size reached
            }

            deduplicatedSamples.add(sample);
            currentUsableBytes += sampleBytes.length;
        }

        if (deduplicatedSamples.isEmpty()) {
            context.log("[forge.dataset] Error: No usable normalized samples generated from checked sources.");
            result.setStatus(DatasetPreparationResult.Status.FAILED);
            return result;
        }

        // 5. Build and save single EvoDatasetArtifact
        DatasetSourceConfig sourceConfig = new DatasetSourceConfig("MULTI_SOURCE", "checked_sources_" + checkedItems.size());
        DatasetSourceStats stats = context.getStats();
        if (stats == null) stats = new DatasetSourceStats();
        stats.setRequestedUsableBytes(targetBytes);
        stats.setAcceptedBytes(currentUsableBytes);
        stats.setDuplicateBytes(duplicateBytes);

        EvoDatasetArtifact artifact = new EvoDatasetArtifact(cacheFile);
        artifact.save(deduplicatedSamples, sourceConfig, stats, 0.02);

        result.setArtifact(artifact);
        result.setSamples(artifact.getSamples());
        result.setOutputPath(cacheFile.getAbsolutePath());
        result.setStatus(artifact.getStatus() == EvoDatasetArtifact.Status.INSUFFICIENT_SOURCE_DATA ?
                          DatasetPreparationResult.Status.INSUFFICIENT_DATA : DatasetPreparationResult.Status.SUCCESS);

        populateResultMetrics(result, deduplicatedSamples, checkedItems.size());
        result.setDuplicateBytes(duplicateBytes);

        logFinalSummary(context, result, cacheFile.getAbsolutePath());
        return result;
    }

    private void populateResultMetrics(DatasetPreparationResult result, List<NormalizedSample> samples, int sourcesCount) {
        result.setSourcesProcessed(sourcesCount);
        result.setSamples(samples);

        int convCount = 0;
        int msgCount = 0;
        long totalBytes = 0;
        long tokens = 0;

        for (NormalizedSample s : samples) {
            byte[] b = s.toFullText().getBytes(StandardCharsets.UTF_8);
            totalBytes += b.length;
            tokens += s.getTokenCount() > 0 ? s.getTokenCount() : (b.length / 4);

            if (s.getType() == TrainingSampleType.CONVERSATION) {
                convCount++;
                if (s.getConversationMessages() != null) {
                    msgCount += s.getConversationMessages().size();
                }
            } else if (s.getMessages() != null && !s.getMessages().isEmpty()) {
                convCount++;
                msgCount += s.getMessages().size();
            }
        }

        result.setAcceptedBytes(totalBytes);
        result.setEstimatedTokens(tokens);
        result.setConversationsCount(convCount);
        result.setMessagesCount(msgCount);
        if (result.getArtifact() != null) {
            result.setTrainSamplesCount(result.getArtifact().getTrainSamples().size());
            result.setValSamplesCount(result.getArtifact().getValSamples().size());
        } else {
            result.setTrainSamplesCount(samples.size());
            result.setValSamplesCount(0);
        }
    }

    private void logFinalSummary(DatasetPreparationContext context, DatasetPreparationResult result, String outputPath) {
        context.log("[forge.dataset]");
        context.log("[forge.dataset]");
        context.log("Native preparation complete");
        context.log("");
        context.log("Sources: " + result.getSourcesProcessed());
        context.log("Samples: " + result.getSamples().size());
        context.log("Conversations: " + result.getConversationsCount());
        context.log("Messages: " + result.getMessagesCount());
        context.log("Accepted bytes: " + result.getAcceptedBytes());
        context.log("Rejected bytes: " + result.getRejectedBytes());
        context.log("Duplicate bytes: " + result.getDuplicateBytes());
        context.log("Estimated tokens: " + result.getEstimatedTokens());
        context.log("Train samples: " + result.getTrainSamplesCount());
        context.log("Validation samples: " + result.getValSamplesCount());
        context.log("Output: " + outputPath);
        context.log("Status: " + (result.getStatus() == DatasetPreparationResult.Status.SUCCESS ? "READY" : result.getStatus().name()));
    }

    private String computeCacheKey(List<DatasetItem> items, long targetBytes) {
        StringBuilder sb = new StringBuilder();
        sb.append(targetBytes).append(";");
        for (DatasetItem item : items) {
            sb.append(item.getPath()).append(":");
            File f = new File(item.getPath());
            if (f.exists()) {
                sb.append(f.lastModified()).append(":").append(f.length());
            }
            sb.append(";");
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(sb.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                String h = Integer.toHexString(0xff & b);
                if (h.length() == 1) hex.append('0');
                hex.append(h);
            }
            return hex.substring(0, 16);
        } catch (Exception ex) {
            return String.valueOf(sb.toString().hashCode());
        }
    }
}
