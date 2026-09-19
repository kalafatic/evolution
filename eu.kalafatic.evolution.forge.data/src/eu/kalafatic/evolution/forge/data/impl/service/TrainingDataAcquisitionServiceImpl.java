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

        List<NormalizedSample> acceptedSamples = new ArrayList<>();
        long accumulatedUsableBytes = 0;
        Set<String> attemptedSourceNames = new HashSet<>();
        Set<String> successfullyReadSourceNames = new HashSet<>();
        Set<String> exhaustedSourceNames = new HashSet<>();
        Set<String> failedSourceNames = new HashSet<>();

        long cleanerRejections = 0;
        long filterRejections = 0;
        long duplicateRejections = 0;

        int sourceIndex = 0;
        int searchRound = 1;
        int maxSearchRounds = 10;
        int consecutiveEmptyExpansions = 0;

        String runId = request.getRunId();
        double targetUsableMB = targetUsableBytes / (1024.0 * 1024.0);

        System.out.printf("[HF-ACQ][run=%s][START]\nrequestedUsableBytes=%d\nrequestedUsableMB=%.2f\nsourceCount=%d\n",
                runId, targetUsableBytes, targetUsableMB, sources.size());

        for (DatasetSource src : sources) {
            if (src != null && src.getConfig() != null) {
                if (src.getConfig().getRunId() == null) {
                    src.getConfig().setRunId(runId);
                }
                var cfg = src.getConfig();
                System.out.printf("[HF-ACQ][run=%s][SOURCES]\nsource=%s\ntype=%s\nrepository=%s\nconfig=%s\nsplit=%s\nrevision=%s\norigin=%s\n",
                        runId, src.getSourceName(), cfg.getSourceType(), cfg.getRepository(),
                        cfg.getConfiguration() != null ? cfg.getConfiguration() : "default",
                        cfg.getSplit() != null ? cfg.getSplit() : "train",
                        cfg.getRevision() != null ? cfg.getRevision() : "main",
                        cfg.getOrigin() != null ? cfg.getOrigin() : "REQUEST");
            }
        }

        while ((targetUsableBytes <= 0 || accumulatedUsableBytes < targetUsableBytes)) {

            // Process all available sources in the sources list
            while (sourceIndex < sources.size() && (targetUsableBytes <= 0 || accumulatedUsableBytes < targetUsableBytes)) {
                DatasetSource source = sources.get(sourceIndex++);
                if (source == null || !attemptedSourceNames.add(source.getSourceName())) {
                    continue;
                }

                var cfg = source.getConfig();
                String srcRepo = cfg != null ? cfg.getRepository() : source.getSourceName();
                String srcCfg = cfg != null && cfg.getConfiguration() != null ? cfg.getConfiguration() : "default";
                String srcSplit = cfg != null && cfg.getSplit() != null ? cfg.getSplit() : "train";
                String srcRev = cfg != null && cfg.getRevision() != null ? cfg.getRevision() : "main";
                long targetRemainingBytes = targetUsableBytes > 0 ? Math.max(0, targetUsableBytes - accumulatedUsableBytes) : 0;

                System.out.printf("[HF-ACQ][run=%s][SOURCE-START]\nsource=%s\nrepository=%s\nconfig=%s\nsplit=%s\nrevision=%s\ntargetRemainingBytes=%d\n",
                        runId, source.getSourceName(), srcRepo, srcCfg, srcSplit, srcRev, targetRemainingBytes);

                var res = source.preflight();
                if (!res.isAccessible() || !res.isAvailable()) {
                    String reason = res.getFailureReason() != null ? res.getFailureReason() : "Inaccessible";
                    failedSourceNames.add(source.getSourceName() + " (Preflight: " + reason + ")");
                    System.out.printf("[HF-ACQ][run=%s][INITIALIZE]\nsource=%s\nstatus=FAILED\nreason=%s\n",
                            runId, source.getSourceName(), reason);
                    continue;
                }

                try {
                    source.initialize();
                    System.out.printf("[HF-ACQ][run=%s][INITIALIZE]\nsource=%s\nstatus=SUCCESS\n",
                            runId, source.getSourceName());
                } catch (Exception initEx) {
                    failedSourceNames.add(source.getSourceName() + " (Init error: " + initEx.getMessage() + ")");
                    System.out.printf("[HF-ACQ][run=%s][INITIALIZE]\nsource=%s\nstatus=FAILED\nreason=%s\n",
                            runId, source.getSourceName(), initEx.getMessage());
                    continue;
                }

                long sourceStartUsable = accumulatedUsableBytes;
                boolean sourceReadError = false;
                try (source) {
                    while ((targetUsableBytes <= 0 || accumulatedUsableBytes < targetUsableBytes) && source.hasNext()) {
                        NormalizedSample s = source.next();
                        byte[] rawBytes = s.toFullText().getBytes(StandardCharsets.UTF_8);
                        long rawLen = rawBytes.length;

                        accounting.recordRaw(rawLen);

                        NormalizedSample clean = normalizer.normalize(s);
                        if (clean == null) {
                            cleanerRejections++;
                            accounting.recordRejected(rawLen);
                            continue;
                        }

                        if (!filter.accept(clean)) {
                            filterRejections++;
                            accounting.recordRejected(rawLen);
                            continue;
                        }

                        if (deduplicator.isDuplicate(clean)) {
                            duplicateRejections++;
                            accounting.recordDuplicate(rawLen);
                            continue;
                        }

                        deduplicator.register(clean);
                        byte[] cleanBytes = clean.toFullText().getBytes(StandardCharsets.UTF_8);
                        long cleanLen = cleanBytes.length;

                        if (cleanLen == 0) {
                            cleanerRejections++;
                            accounting.recordRejected(rawLen);
                            continue;
                        }

                        acceptedSamples.add(clean);
                        accumulatedUsableBytes += cleanLen;

                        long tokens = clean.getTokenCount() > 0 ? clean.getTokenCount() : cleanLen / 4;
                        accounting.recordAccepted(cleanLen, tokens);

                        if (targetUsableBytes > 0 && accumulatedUsableBytes >= targetUsableBytes) {
                            break;
                        }
                    }
                } catch (Exception ex) {
                    sourceReadError = true;
                    failedSourceNames.add(source.getSourceName() + " (Read error: " + ex.getMessage() + ")");
                    System.err.println("[ACQUISITION LOG] Error reading source " + source.getSourceName() + ": " + ex.getMessage());
                }

                successfullyReadSourceNames.add(source.getSourceName());
                if (!sourceReadError) {
                    exhaustedSourceNames.add(source.getSourceName());
                }

                long sourceYield = accumulatedUsableBytes - sourceStartUsable;
                long remaining = targetUsableBytes > 0 ? Math.max(0, targetUsableBytes - accumulatedUsableBytes) : 0;
                System.out.printf("[HF-ACQ][run=%s][SOURCE-END]\nsource=%s\nyieldBytes=%d\nyieldMB=%.2f\ntotalUsableBytes=%d\ntotalUsableMB=%.2f\ntargetRemainingBytes=%d\ntargetRemainingMB=%.2f\n",
                        runId, source.getSourceName(), sourceYield, sourceYield / (1024.0 * 1024.0),
                        accumulatedUsableBytes, accumulatedUsableBytes / (1024.0 * 1024.0),
                        remaining, remaining / (1024.0 * 1024.0));
            }

            // TARGET-DRIVEN SEARCH EXPANSION LOOP:
            // If accumulatedUsableBytes < targetUsableBytes and current sources list is exhausted,
            // trigger progressive discovery expansion ONLY IF explicit sources were NOT requested by the caller!
            boolean hasExplicitSources = request.getSources() != null && !request.getSources().isEmpty();
            if (hasExplicitSources) {
                System.out.println("[ACQUISITION LOG] Explicit sources were provided in request. Disabling multi-domain search expansion to prevent mixing unrelated datasets.");
                break;
            }

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
                    if (cand.getSource() != null && !attemptedSourceNames.contains(cand.getSource().getSourceName())) {
                        if (cand.getSource().getConfig() != null) {
                            cand.getSource().getConfig().setRunId(runId);
                        }
                        sources.add(cand.getSource());
                        newlyAddedCount++;
                    }
                }

                if (newlyAddedCount == 0) {
                    System.out.println("[ACQUISITION LOG] Search expansion yielded 0 new candidate sources. Short-circuiting expansion loop.");
                    break;
                } else {
                    consecutiveEmptyExpansions = 0;
                }

                searchRound++;
            }
        }

        // CRITICAL PIPELINE INVARIANT CHECK
        if (!acceptedSamples.isEmpty() && accumulatedUsableBytes == 0) {
            throw new IllegalStateException("CRITICAL PIPELINE INVARIANT VIOLATION: acceptedSamples count="
                    + acceptedSamples.size() + " but accumulatedUsableBytes is 0! Data conversion/accounting broken.");
        }

        accounting.setTrainValidationRatio(valSplitRatio);
        DatasetSourceStats stats = accounting.getSnapshot();
        stats.setRequestedUsableBytes(targetUsableBytes);

        TrainingDataPreferenceEvaluation eval = preferenceEvaluator.evaluate(prefs, stats);

        boolean targetReached = targetUsableBytes <= 0 || accumulatedUsableBytes >= targetUsableBytes;
        boolean sourceExhausted = !targetReached;
        long shortfall = targetReached ? 0 : (targetUsableBytes - accumulatedUsableBytes);
        double coveragePercent = targetUsableBytes > 0 ? ((accumulatedUsableBytes * 100.0) / targetUsableBytes) : 100.0;

        TrainingDataAcquisitionResult.Status status = (targetReached && eval.isAllHardRequirementsSatisfied())
                ? TrainingDataAcquisitionResult.Status.READY
                : TrainingDataAcquisitionResult.Status.INSUFFICIENT_SOURCE_DATA;

        List<String> sourcesUsedNames = new ArrayList<>(successfullyReadSourceNames);
        List<String> sourcesExhaustedNamesList = sourceExhausted ? new ArrayList<>(exhaustedSourceNames) : List.of();
        List<String> errorMessages = new ArrayList<>(failedSourceNames);

        String failureReason = null;
        if (!targetReached) {
            failureReason = "INSUFFICIENT_SOURCE_DATA: Source universe exhausted before satisfying minimum usable bytes requirement. Requested: "
                    + targetUsableBytes + " bytes (" + (targetUsableBytes / (1024 * 1024)) + " MB), Acquired Usable: "
                    + accumulatedUsableBytes + " bytes (" + (accumulatedUsableBytes / (1024 * 1024)) + " MB), Max Download: "
                    + prefs.getMaximumDownloadBytes() + " bytes (" + (prefs.getMaximumDownloadBytes() / (1024 * 1024)) + " MB), Shortfall: "
                    + shortfall + " bytes (" + (shortfall / (1024 * 1024)) + " MB), Coverage: " + String.format("%.2f", coveragePercent) + "%, Sources used: " + sourcesUsedNames;
            if (!failedSourceNames.isEmpty()) {
                failureReason += ", Failed sources: " + failedSourceNames;
            }
            failureReason += ". Rejections breakdown: Cleaner=" + cleanerRejections + ", QualityFilter=" + filterRejections + ", Duplicates=" + duplicateRejections + ".";
            System.err.println("[ACQ-TRACE] TARGET NOT REACHED! " + failureReason);
        } else {
            System.out.printf("[ACQ-TRACE] TARGET REACHED! Requested: %d bytes, Acquired Usable: %d bytes (%.2f%% coverage).\n",
                    targetUsableBytes, accumulatedUsableBytes, coveragePercent);
        }

        List<String> hardFailures = (targetReached && eval.isAllHardRequirementsSatisfied()) ? List.of() : List.of("MinimumUsableBytesNotSatisfied");

        System.out.printf("[HF-ACQ][run=%s][FINAL-RESULT]\nstatus=%s\ntargetReached=%b\nrequestedUsableBytes=%d\naccumulatedUsableBytes=%d\nshortfallBytes=%d\ncoveragePercent=%.2f%%\nacceptedSamples=%d\ncleanerRejections=%d\nfilterRejections=%d\nduplicateRejections=%d\nsourcesUsed=%s\n",
                runId, status, targetReached, targetUsableBytes, accumulatedUsableBytes, shortfall, coveragePercent,
                acceptedSamples.size(), cleanerRejections, filterRejections, duplicateRejections, sourcesUsedNames);

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
                sourcesExhaustedNamesList,
                hardFailures,
                List.of(),
                errorMessages,
                "EVO_TARGET_DRIVEN_MULTI_SOURCE_PIPELINE"
        );
    }
}
