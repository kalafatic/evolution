package eu.kalafatic.evolution.forge.data.impl.source;

import eu.kalafatic.evolution.forge.data.api.NormalizedSample;
import eu.kalafatic.evolution.forge.data.api.TrainingSampleType;
import eu.kalafatic.evolution.forge.data.api.collector.CollectionContext;
import eu.kalafatic.evolution.forge.data.api.collector.CollectionResult;
import eu.kalafatic.evolution.forge.data.api.collector.TrainingDataItem;
import eu.kalafatic.evolution.forge.data.api.source.DatasetSource;
import eu.kalafatic.evolution.forge.data.api.source.DatasetSourceConfig;
import eu.kalafatic.evolution.forge.data.api.source.DatasetSourceStats;
import eu.kalafatic.evolution.forge.data.impl.collector.EvoCodebaseDataCollector;
import eu.kalafatic.evolution.forge.data.impl.collector.EvoCodebaseDataCollector.CodebaseMode;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * First-class dataset source representing the complete Git-aware EVO Codebase.
 */
public class EvoCodebaseDatasetSource implements DatasetSource {

    private final DatasetSourceConfig config;
    private final DatasetSourceStats stats = new DatasetSourceStats();
    private final EvoCodebaseDataCollector collector;
    private final List<NormalizedSample> samples = new ArrayList<>();
    private int currentIndex = 0;

    public EvoCodebaseDatasetSource(DatasetSourceConfig config) {
        this(config, CodebaseMode.MIXED_AUTO);
    }

    public EvoCodebaseDatasetSource(DatasetSourceConfig config, CodebaseMode mode) {
        this.config = config != null ? config : new DatasetSourceConfig("EVO_CODEBASE", ".");
        this.collector = new EvoCodebaseDataCollector(mode);
    }

    @Override
    public String getSourceName() {
        return "EVO Codebase: " + config.getRepository();
    }

    @Override
    public DatasetSourceConfig getConfig() {
        return config;
    }

    @Override
    public DatasetSourceStats getStats() {
        return stats;
    }

    @Override
    public void initialize() throws Exception {
        samples.clear();
        currentIndex = 0;

        File root = new File(config.getRepository());
        if (!root.exists()) {
            root = new File(System.getProperty("user.dir"));
        }

        CollectionContext context = new CollectionContext();
        context.setProjectDirectory(root);
        CollectionResult result = collector.collect(context);

        if (result.getItems() != null) {
            for (TrainingDataItem item : result.getItems()) {
                if (item == null || item.getContent() == null || item.getContent().trim().isEmpty()) {
                    continue;
                }

                NormalizedSample sample = new NormalizedSample();
                String fileType = (String) item.getMetadata().getOrDefault("fileType", "TEXT");

                if ("JAVA_SOURCE".equalsIgnoreCase(fileType) || "JAVA_TEST".equalsIgnoreCase(fileType) || "SHELL".equalsIgnoreCase(fileType)) {
                    sample.setType(TrainingSampleType.CODE);
                } else if ("INSTRUCTION".equalsIgnoreCase(fileType)) {
                    sample.setType(TrainingSampleType.INSTRUCTION);
                } else if ("DOCUMENTATION".equalsIgnoreCase(fileType) || "MARKDOWN".equalsIgnoreCase(fileType) || "PDF".equalsIgnoreCase(fileType)) {
                    sample.setType(TrainingSampleType.QA);
                } else {
                    sample.setType(TrainingSampleType.TEXT);
                }

                sample.setText(item.getContent());
                sample.setSource(item.getSource());

                Double quality = (Double) item.getMetadata().get("qualityScore");
                if (quality != null) {
                    sample.setQualityScore(quality);
                }

                String language = (String) item.getMetadata().get("language");
                if (language != null) {
                    sample.setLanguage(language);
                }

                // Transfer all provenance metadata
                sample.getMetadata().putAll(item.getMetadata());
                sample.recalculateCountsAndHash();

                samples.add(sample);

                long rawLen = item.getContent().getBytes(StandardCharsets.UTF_8).length;
                stats.addRawContentBytes(rawLen);
                stats.addBytesRead(rawLen);
                stats.incrementSamplesRead();
            }
        }
    }

    @Override
    public boolean hasNext() {
        if (config.getMaxSamples() > 0 && currentIndex >= config.getMaxSamples()) {
            return false;
        }
        if (config.getMaxBytes() > 0 && stats.getAcceptedBytes() >= config.getMaxBytes()) {
            return false;
        }
        return currentIndex < samples.size();
    }

    @Override
    public NormalizedSample next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        NormalizedSample current = samples.get(currentIndex++);
        stats.incrementAccepted();
        stats.addAcceptedBytes(current.toFullText().getBytes(StandardCharsets.UTF_8).length);
        return current;
    }

    @Override
    public void close() {
        samples.clear();
        currentIndex = 0;
    }
}
