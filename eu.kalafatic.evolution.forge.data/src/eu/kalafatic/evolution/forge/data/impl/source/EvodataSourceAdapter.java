package eu.kalafatic.evolution.forge.data.impl.source;

import eu.kalafatic.evolution.forge.data.api.NormalizedSample;
import eu.kalafatic.evolution.forge.data.api.artifact.EvoDatasetArtifact;
import eu.kalafatic.evolution.forge.data.api.discovery.DataSourceCandidate;
import eu.kalafatic.evolution.forge.data.api.service.TrainingDataAcquisitionRequest;
import eu.kalafatic.evolution.forge.data.api.source.DatasetSource;
import eu.kalafatic.evolution.forge.data.api.source.DatasetSourceConfig;
import eu.kalafatic.evolution.forge.data.api.source.DatasetSourceStats;
import eu.kalafatic.evolution.forge.data.api.source.TrainingDataSourceAdapter;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Adapter and DatasetSource implementation for reading existing .evodata binary dataset archives.
 */
public class EvodataSourceAdapter implements TrainingDataSourceAdapter, DatasetSource {

    private DatasetSourceConfig config;
    private final DatasetSourceStats stats = new DatasetSourceStats();
    private final List<NormalizedSample> samples = new ArrayList<>();
    private int currentIndex = 0;

    public EvodataSourceAdapter() {
        this(new DatasetSourceConfig("EVODATA", ""));
    }

    public EvodataSourceAdapter(DatasetSourceConfig config) {
        this.config = config != null ? config : new DatasetSourceConfig("EVODATA", "");
    }

    @Override
    public boolean supports(DataSourceCandidate candidate) {
        return candidate != null && ("EVODATA".equalsIgnoreCase(candidate.getProvider()) || candidate.getDatasetId().endsWith(".evodata") || candidate.getUri().endsWith(".evodata"));
    }

    @Override
    public DatasetSource createSource(DataSourceCandidate candidate, TrainingDataAcquisitionRequest request) throws Exception {
        if (candidate.getSource() != null) {
            return candidate.getSource();
        }
        DatasetSourceConfig cfg = new DatasetSourceConfig("EVODATA", candidate.getUri().replace("file:", ""));
        EvodataSourceAdapter source = new EvodataSourceAdapter(cfg);
        source.initialize();
        return source;
    }

    @Override
    public String getSourceName() {
        return "EVODATA Archive: " + config.getRepository();
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
        File file = new File(config.getRepository());
        if (file.exists() && file.isFile()) {
            EvoDatasetArtifact artifact = EvoDatasetArtifact.load(file);
            if (artifact != null && artifact.getSamples() != null) {
                for (NormalizedSample sample : artifact.getSamples()) {
                    samples.add(sample);
                    long rawLen = sample.toFullText().getBytes(StandardCharsets.UTF_8).length;
                    stats.addRawContentBytes(rawLen);
                    stats.addBytesRead(rawLen);
                    stats.incrementSamplesRead();
                }
            }
        }
    }

    @Override
    public boolean hasNext() {
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
