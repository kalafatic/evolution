package eu.kalafatic.evolution.forge.data.impl.service;

import eu.kalafatic.evolution.forge.data.api.NormalizedSample;
import eu.kalafatic.evolution.forge.data.api.discovery.DataSourceCandidate;
import eu.kalafatic.evolution.forge.data.api.discovery.TrainingDataSourceDiscovery;
import eu.kalafatic.evolution.forge.data.api.evaluation.TrainingDataPreferenceEvaluation;
import eu.kalafatic.evolution.forge.data.api.evaluation.TrainingDataPreferenceEvaluator;
import eu.kalafatic.evolution.forge.data.api.preference.TrainingDataPreferences;
import eu.kalafatic.evolution.forge.data.api.processor.DataDeduplicator;
import eu.kalafatic.evolution.forge.data.api.processor.DataFilter;
import eu.kalafatic.evolution.forge.data.api.processor.DataNormalizer;
import eu.kalafatic.evolution.forge.data.api.processor.DataSizeAccounting;
import eu.kalafatic.evolution.forge.data.api.service.TrainingDataAcquisitionRequest;
import eu.kalafatic.evolution.forge.data.api.service.TrainingDataAcquisitionResult;
import eu.kalafatic.evolution.forge.data.api.service.TrainingDataAcquisitionService;
import eu.kalafatic.evolution.forge.data.api.source.DatasetSource;
import eu.kalafatic.evolution.forge.data.api.source.DatasetSourceStats;
import eu.kalafatic.evolution.forge.data.impl.discovery.HuggingFaceSourceDiscovery;
import eu.kalafatic.evolution.forge.data.impl.evaluation.DefaultPreferenceEvaluator;
import eu.kalafatic.evolution.forge.data.impl.pipeline.DataCleaner;
import eu.kalafatic.evolution.forge.data.impl.pipeline.DatasetDeduplicator;
import eu.kalafatic.evolution.forge.data.impl.pipeline.TrainingSampleQualityScorer;
import eu.kalafatic.evolution.forge.data.impl.processor.DefaultDataSizeAccounting;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Concrete TrainingDataAcquisitionService implementation orchestrating pipeline stages,
 * preference satisfaction evaluation, and adaptive multi-source streaming.
 * Features smart automatic discovery expansion when initial sources exhaust early before reaching target usable content bytes.
 */
public class TrainingDataAcquisitionServiceImpl implements TrainingDataAcquisitionService {

    private final DataNormalizer normalizer;
    private final DataFilter filter;
    private final DataDeduplicator deduplicator;
    private final TrainingDataPreferenceEvaluator preferenceEvaluator;
    private final TrainingDataSourceDiscovery discovery;

    public TrainingDataAcquisitionServiceImpl() {
        this(new DataCleaner(), new TrainingSampleQualityScorer(0.5), new DatasetDeduplicator(true), new DefaultPreferenceEvaluator(), new HuggingFaceSourceDiscovery());
    }

    public TrainingDataAcquisitionServiceImpl(DataNormalizer normalizer, DataFilter filter, DataDeduplicator deduplicator) {
        this(normalizer, filter, deduplicator, new DefaultPreferenceEvaluator(), new HuggingFaceSourceDiscovery());
    }

    public TrainingDataAcquisitionServiceImpl(DataNormalizer normalizer, DataFilter filter, DataDeduplicator deduplicator, TrainingDataPreferenceEvaluator preferenceEvaluator) {
        this(normalizer, filter, deduplicator, preferenceEvaluator, new HuggingFaceSourceDiscovery());
    }

    public TrainingDataAcquisitionServiceImpl(DataNormalizer normalizer, DataFilter filter, DataDeduplicator deduplicator, TrainingDataPreferenceEvaluator preferenceEvaluator, TrainingDataSourceDiscovery discovery) {
        this.normalizer = normalizer != null ? normalizer : new DataCleaner();
        this.filter = filter != null ? filter : new TrainingSampleQualityScorer(0.5);
        this.deduplicator = deduplicator != null ? deduplicator : new DatasetDeduplicator(true);
        this.preferenceEvaluator = preferenceEvaluator != null ? preferenceEvaluator : new DefaultPreferenceEvaluator();
        this.discovery = discovery != null ? discovery : new HuggingFaceSourceDiscovery();
    }

    @Override
    public TrainingDataAcquisitionResult acquireDataset(TrainingDataAcquisitionRequest request) throws Exception {
        if (request == null) {
            throw new IllegalArgumentException("Acquisition request cannot be null.");
        }

        List<DatasetSource> sources = new ArrayList<>(request.getSources());
        if (request.getPlan() != null) {
            request.getPlan().getPrimaryCandidates().forEach(c -> {
                if (c.getSource() != null && !sources.contains(c.getSource())) {
                    sources.add(c.getSource());
                }
            });
            request.getPlan().getFallbackCandidates().forEach(c -> {
                if (c.getSource() != null && !sources.contains(c.getSource())) {
                    sources.add(c.getSource());
                }
            });
        }

        long targetUsableBytes = request.getMinimumUsableBytes();
        double valSplitRatio = request.getValidationSplitRatio();

        // If no sources provided explicitly, use smart discovery to find sources
        if (sources.isEmpty()) {
            TrainingDataPreferences prefs = request.getPreferences();
            if (prefs == null) {
                prefs = TrainingDataPreferences.builder().minimumUsableBytes(targetUsableBytes).build();
            }
            List<DataSourceCandidate> discovered = discovery.discover(prefs);
            for (DataSourceCandidate cand : discovered) {
                if (cand.getSource() != null) {
                    sources.add(cand.getSource());
                }
            }
        }

        if (sources.isEmpty()) {
            throw new IllegalArgumentException("Acquisition request must contain at least one training data source.");
        }

        DataSizeAccounting accounting = new DefaultDataSizeAccounting();
        DatasetSourceStats stats = accounting.getSnapshot();
        stats.setRequestedUsableBytes(targetUsableBytes);

        List<NormalizedSample> acceptedSamples = new ArrayList<>();
        long accumulatedUsableBytes = 0;
        Set<String> processedSourceNames = new HashSet<>();

        int sourceIndex = 0;
        while ((targetUsableBytes <= 0 || accumulatedUsableBytes < targetUsableBytes) && sourceIndex < sources.size()) {
            DatasetSource source = sources.get(sourceIndex++);
            if (source == null || !processedSourceNames.add(source.getSourceName())) {
                continue;
            }

            try {
                source.initialize();
            } catch (Exception initEx) {
                System.err.println("[ACQUISITION SERVICE] Failed to initialize source " + source.getSourceName() + ": " + initEx.getMessage());
                continue;
            }

            try (source) {
                while ((targetUsableBytes <= 0 || accumulatedUsableBytes < targetUsableBytes) && source.hasNext()) {
                    NormalizedSample s = source.next();
                    byte[] rawBytes = s.toFullText().getBytes(StandardCharsets.UTF_8);
                    long rawLen = rawBytes.length;

                    accounting.recordRaw(rawLen);

                    NormalizedSample clean = normalizer.normalize(s);
                    if (clean == null || !filter.accept(clean)) {
                        accounting.recordRejected(rawLen);
                        continue;
                    }

                    if (deduplicator.isDuplicate(clean)) {
                        accounting.recordDuplicate(rawLen);
                        continue;
                    }

                    deduplicator.register(clean);
                    byte[] cleanBytes = clean.toFullText().getBytes(StandardCharsets.UTF_8);
                    long cleanLen = cleanBytes.length;

                    acceptedSamples.add(clean);
                    accumulatedUsableBytes += cleanLen;

                    long tokens = clean.getTokenCount() > 0 ? clean.getTokenCount() : cleanLen / 4;
                    accounting.recordAccepted(cleanLen, tokens);

                    if (targetUsableBytes > 0 && accumulatedUsableBytes >= targetUsableBytes) {
                        break;
                    }
                }
            }

            // SMART ADAPTIVE FALLBACK EXPANSION:
            // If current sources exhaust before minimum usable bytes are satisfied, dynamically discover similar datasets!
            if (targetUsableBytes > 0 && accumulatedUsableBytes < targetUsableBytes && sourceIndex >= sources.size()) {
                System.out.println("[ACQUISITION SERVICE] Initial sources yielded " + accumulatedUsableBytes + " bytes / " + targetUsableBytes + " bytes. Triggering smart discovery fallback expansion...");
                TrainingDataPreferences prefs = request.getPreferences();
                if (prefs == null) {
                    prefs = TrainingDataPreferences.builder().minimumUsableBytes(targetUsableBytes - accumulatedUsableBytes).build();
                }
                List<DataSourceCandidate> expandedCandidates = discovery.discover(prefs);
                for (DataSourceCandidate cand : expandedCandidates) {
                    if (cand.getSource() != null && !processedSourceNames.contains(cand.getSource().getSourceName())) {
                        sources.add(cand.getSource());
                    }
                }
            }
        }

        accounting.setTrainValidationRatio(valSplitRatio);

        TrainingDataPreferences prefs = request.getPreferences();
        TrainingDataPreferenceEvaluation eval = preferenceEvaluator.evaluate(prefs, stats);

        boolean targetReached = targetUsableBytes <= 0 || accumulatedUsableBytes >= targetUsableBytes;
        boolean sourceExhausted = !targetReached;
        long shortfall = targetReached ? 0 : (targetUsableBytes - accumulatedUsableBytes);
        double coveragePercent = targetUsableBytes > 0 ? ((accumulatedUsableBytes * 100.0) / targetUsableBytes) : 100.0;

        TrainingDataAcquisitionResult.Status status = (targetReached && eval.isAllHardRequirementsSatisfied())
                ? TrainingDataAcquisitionResult.Status.READY
                : TrainingDataAcquisitionResult.Status.INSUFFICIENT_SOURCE_DATA;

        String failureReason = null;
        if (!targetReached) {
            failureReason = "INSUFFICIENT_SOURCE_DATA: Source universe exhausted before satisfying minimum usable bytes. Requested: "
                    + targetUsableBytes + " bytes, Acquired: " + accumulatedUsableBytes + " bytes, Shortfall: " + shortfall + " bytes.";
        }

        List<String> sourcesUsedNames = new ArrayList<>(processedSourceNames);
        List<String> sourcesExhaustedNames = sourceExhausted ? new ArrayList<>(processedSourceNames) : List.of();
        List<String> hardFailures = eval.isAllHardRequirementsSatisfied() ? List.of() : List.of("MinimumUsableBytesNotSatisfied");

        return new TrainingDataAcquisitionResult(
                acceptedSamples,
                stats,
                targetReached,
                sourceExhausted,
                coveragePercent,
                targetUsableBytes,
                accumulatedUsableBytes,
                shortfall,
                status,
                failureReason,
                eval,
                stats.getDownloadedBytes(),
                stats.getExtractedBytes(),
                stats.getRawContentBytes(),
                accumulatedUsableBytes,
                stats.getRejectedBytes(),
                stats.getDuplicateBytes(),
                stats.getTrainingBytes(),
                stats.getValidationBytes(),
                stats.getEstimatedTokens(),
                sourcesUsedNames,
                sourcesExhaustedNames,
                hardFailures,
                List.of(),
                failureReason != null ? List.of(failureReason) : List.of(),
                "EVO_MULTI_PROVIDER_PIPELINE"
        );
    }
}
