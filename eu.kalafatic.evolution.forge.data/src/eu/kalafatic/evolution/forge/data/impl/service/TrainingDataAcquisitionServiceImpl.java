package eu.kalafatic.evolution.forge.data.impl.service;

import eu.kalafatic.evolution.forge.data.api.NormalizedSample;
import eu.kalafatic.evolution.forge.data.api.processor.DataDeduplicator;
import eu.kalafatic.evolution.forge.data.api.processor.DataFilter;
import eu.kalafatic.evolution.forge.data.api.processor.DataNormalizer;
import eu.kalafatic.evolution.forge.data.api.processor.DataSizeAccounting;
import eu.kalafatic.evolution.forge.data.api.service.TrainingDataAcquisitionRequest;
import eu.kalafatic.evolution.forge.data.api.service.TrainingDataAcquisitionResult;
import eu.kalafatic.evolution.forge.data.api.service.TrainingDataAcquisitionService;
import eu.kalafatic.evolution.forge.data.api.source.DatasetSource;
import eu.kalafatic.evolution.forge.data.api.source.DatasetSourceStats;
import eu.kalafatic.evolution.forge.data.impl.pipeline.DataCleaner;
import eu.kalafatic.evolution.forge.data.impl.pipeline.DatasetDeduplicator;
import eu.kalafatic.evolution.forge.data.impl.pipeline.TrainingSampleQualityScorer;
import eu.kalafatic.evolution.forge.data.impl.processor.DefaultDataSizeAccounting;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Concrete TrainingDataAcquisitionService implementation orchestrating pipeline stages.
 */
public class TrainingDataAcquisitionServiceImpl implements TrainingDataAcquisitionService {

    private final DataNormalizer normalizer;
    private final DataFilter filter;
    private final DataDeduplicator deduplicator;

    public TrainingDataAcquisitionServiceImpl() {
        this(new DataCleaner(), new TrainingSampleQualityScorer(0.5), new DatasetDeduplicator(true));
    }

    public TrainingDataAcquisitionServiceImpl(DataNormalizer normalizer, DataFilter filter, DataDeduplicator deduplicator) {
        this.normalizer = normalizer != null ? normalizer : new DataCleaner();
        this.filter = filter != null ? filter : new TrainingSampleQualityScorer(0.5);
        this.deduplicator = deduplicator != null ? deduplicator : new DatasetDeduplicator(true);
    }

    @Override
    public TrainingDataAcquisitionResult acquireDataset(TrainingDataAcquisitionRequest request) throws Exception {
        if (request == null || request.getSources().isEmpty()) {
            throw new IllegalArgumentException("Acquisition request must contain at least one training data source.");
        }

        long targetUsableBytes = request.getMinimumUsableBytes();
        double valSplitRatio = request.getValidationSplitRatio();

        DataSizeAccounting accounting = new DefaultDataSizeAccounting();
        DatasetSourceStats stats = accounting.getSnapshot();
        stats.setRequestedUsableBytes(targetUsableBytes);

        List<NormalizedSample> acceptedSamples = new ArrayList<>();
        long accumulatedUsableBytes = 0;

        for (DatasetSource source : request.getSources()) {
            if (targetUsableBytes > 0 && accumulatedUsableBytes >= targetUsableBytes) {
                break;
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
        }

        accounting.setTrainValidationRatio(valSplitRatio);

        boolean targetReached = targetUsableBytes <= 0 || accumulatedUsableBytes >= targetUsableBytes;
        boolean sourceExhausted = !targetReached;
        long shortfall = targetReached ? 0 : (targetUsableBytes - accumulatedUsableBytes);
        double coveragePercent = targetUsableBytes > 0 ? ((accumulatedUsableBytes * 100.0) / targetUsableBytes) : 100.0;

        TrainingDataAcquisitionResult.Status status = targetReached
                ? TrainingDataAcquisitionResult.Status.READY
                : TrainingDataAcquisitionResult.Status.INSUFFICIENT_SOURCE_DATA;

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
                null
        );
    }
}
