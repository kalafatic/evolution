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
 * Concrete TrainingDataAcquisitionService implementation orchestrating target-driven pipeline stages,
 * preference satisfaction evaluation, and multi-source streaming with progressive Hugging Face search expansion.
 *
 * Invariant: Status READY is granted IF AND ONLY IF validatedUsableBytes >= minimumTargetBytes.
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

        TrainingDataPreferences prefs = request.getPreferences();
        if (prefs == null) {
            prefs = TrainingDataPreferences.builder().minimumUsableBytes(targetUsableBytes).build();
        }

        // If no explicit sources provided, perform initial smart discovery
        if (sources.isEmpty()) {
            List<DataSourceCandidate> discovered = discovery.discover(prefs);
            for (DataSourceCandidate cand : discovered) {
                if (cand.getSource() != null) {
                    sources.add(cand.getSource());
                }
            }
        }

        if (sources.isEmpty()) {
            throw new IllegalArgumentException("Acquisition request must contain at least one training data source or matching preferences.");
        }

        DataSizeAccounting accounting = new DefaultDataSizeAccounting();
        DatasetSourceStats stats = accounting.getSnapshot();
        stats.setRequestedUsableBytes(targetUsableBytes);

        List<NormalizedSample> acceptedSamples = new ArrayList<>();
        long accumulatedUsableBytes = 0;
        Set<String> processedSourceNames = new HashSet<>();

        int sourceIndex = 0;
        int searchRound = 1;
        int maxSearchRounds = 10;
        int consecutiveEmptyExpansions = 0;

        System.out.printf("[ACQUISITION LOG] Starting Acquisition Loop. Hard Target Usable Bytes: %d (%.2f MB)\n",
                targetUsableBytes, targetUsableBytes / (1024.0 * 1024.0));

        while ((targetUsableBytes <= 0 || accumulatedUsableBytes < targetUsableBytes)) {

            // Process all available sources in the sources list
            while (sourceIndex < sources.size() && (targetUsableBytes <= 0 || accumulatedUsableBytes < targetUsableBytes)) {
                DatasetSource source = sources.get(sourceIndex++);
                if (source == null || !processedSourceNames.add(source.getSourceName())) {
                    continue;
                }

                try {
                    source.initialize();
                } catch (Exception initEx) {
                    System.err.println("[ACQUISITION LOG] Failed to initialize source " + source.getSourceName() + ": " + initEx.getMessage());
                    continue;
                }

                long sourceStartUsable = accumulatedUsableBytes;
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
                } catch (Exception ex) {
                    System.err.println("[ACQUISITION LOG] Error reading source " + source.getSourceName() + ": " + ex.getMessage());
                }

                long sourceYield = accumulatedUsableBytes - sourceStartUsable;
                long remaining = targetUsableBytes > 0 ? Math.max(0, targetUsableBytes - accumulatedUsableBytes) : 0;
                System.out.printf("[ACQUISITION PROGRESS] Target: %.2f MB | Downloaded: %.2f MB | Usable: %.2f MB / %.2f MB | Remaining: %.2f MB | Source: %s (Yield: %.2f MB)\n",
                        targetUsableBytes / (1024.0 * 1024.0),
                        stats.getDownloadedBytes() / (1024.0 * 1024.0),
                        accumulatedUsableBytes / (1024.0 * 1024.0),
                        targetUsableBytes / (1024.0 * 1024.0),
                        remaining / (1024.0 * 1024.0),
                        source.getSourceName(),
                        sourceYield / (1024.0 * 1024.0));
            }

            // TARGET-DRIVEN SEARCH EXPANSION LOOP:
            // If accumulatedUsableBytes < targetUsableBytes and current sources list is exhausted,
            // trigger progressive discovery expansion to find additional datasets/files!
            if (targetUsableBytes > 0 && accumulatedUsableBytes < targetUsableBytes) {
                if (searchRound >= maxSearchRounds || consecutiveEmptyExpansions >= 3) {
                    System.out.println("[ACQUISITION LOG] Search strategy exhausted after " + searchRound + " rounds. No more candidates available.");
                    break;
                }

                System.out.printf("[ACQUISITION SEARCH EXPANSION] Search Round %d: Usable bytes: %d / %d (Shortfall: %d bytes). Triggering progressive query expansion...\n",
                        searchRound, accumulatedUsableBytes, targetUsableBytes, targetUsableBytes - accumulatedUsableBytes);

                List<DataSourceCandidate> expandedCandidates = discovery.discover(prefs);
                int newlyAddedCount = 0;
                for (DataSourceCandidate cand : expandedCandidates) {
                    if (cand.getSource() != null && !processedSourceNames.contains(cand.getSource().getSourceName())) {
                        sources.add(cand.getSource());
                        newlyAddedCount++;
                    }
                }

                if (newlyAddedCount == 0) {
                    consecutiveEmptyExpansions++;
                } else {
                    consecutiveEmptyExpansions = 0;
                }

                searchRound++;
            }
        }

        accounting.setTrainValidationRatio(valSplitRatio);

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
            failureReason = "INSUFFICIENT_SOURCE_DATA: Source universe exhausted before satisfying minimum usable bytes requirement. Requested: "
                    + targetUsableBytes + " bytes (" + (targetUsableBytes / (1024 * 1024)) + " MB), Acquired Usable: "
                    + accumulatedUsableBytes + " bytes (" + (accumulatedUsableBytes / (1024 * 1024)) + " MB), Shortfall: "
                    + shortfall + " bytes (" + (shortfall / (1024 * 1024)) + " MB), Coverage: " + String.format("%.2f", coveragePercent) + "%.";
            System.err.println("[ACQUISITION FAILED INVARIANT] " + failureReason);
        } else {
            System.out.printf("[ACQUISITION SUCCESS] Hard Target Satisfied! Requested: %d bytes, Acquired Usable: %d bytes (%.2f%% coverage).\n",
                    targetUsableBytes, accumulatedUsableBytes, coveragePercent);
        }

        List<String> sourcesUsedNames = new ArrayList<>(processedSourceNames);
        List<String> sourcesExhaustedNames = sourceExhausted ? new ArrayList<>(processedSourceNames) : List.of();
        List<String> hardFailures = (targetReached && eval.isAllHardRequirementsSatisfied()) ? List.of() : List.of("MinimumUsableBytesNotSatisfied");

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
                "EVO_TARGET_DRIVEN_MULTI_SOURCE_PIPELINE"
        );
    }
}
