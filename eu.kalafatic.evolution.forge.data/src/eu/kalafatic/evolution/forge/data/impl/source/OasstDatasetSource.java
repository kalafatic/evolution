package eu.kalafatic.evolution.forge.data.impl.source;

import eu.kalafatic.evolution.forge.data.api.NormalizedSample;
import eu.kalafatic.evolution.forge.data.api.source.DatasetSource;
import eu.kalafatic.evolution.forge.data.api.source.DatasetSourceConfig;
import eu.kalafatic.evolution.forge.data.api.source.DatasetSourceStats;
import eu.kalafatic.evolution.forge.data.api.source.ResolvedSource;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.zip.GZIPInputStream;

/**
 * Dataset source for reading OASST1 message tree files (.jsonl or .jsonl.gz).
 */
public class OasstDatasetSource implements DatasetSource {

    private final DatasetSourceConfig config;
    private final File oasstFile;
    private final DatasetSourceStats stats = new DatasetSourceStats();
    private final List<NormalizedSample> extractedSamples = new ArrayList<>();
    private int currentIndex = 0;
    private boolean initialized = false;

    public OasstDatasetSource(File oasstFile) {
        this.oasstFile = oasstFile;
        this.config = new DatasetSourceConfig("OASST1", oasstFile != null ? oasstFile.getName() : "oasst1");
    }

    public OasstDatasetSource(DatasetSourceConfig config, File oasstFile) {
        this.config = config != null ? config : new DatasetSourceConfig("OASST1", "oasst1");
        this.oasstFile = oasstFile;
    }

    @Override
    public String getSourceName() {
        return "OASST1: " + (oasstFile != null ? oasstFile.getName() : "oasst1");
    }

    @Override
    public DatasetSourceConfig getConfig() {
        return config;
    }

    @Override
    public ResolvedSource preflight() {
        boolean exists = oasstFile != null && oasstFile.exists();
        long size = exists ? oasstFile.length() : 0L;
        return new ResolvedSource("OASST1", oasstFile != null ? oasstFile.getPath() : "oasst1", "train", "train", "default", "main", oasstFile != null ? oasstFile.getAbsolutePath() : "", "jsonl", size, exists, exists, exists ? null : "OASST1 dataset file does not exist");
    }

    @Override
    public DatasetSourceStats getStats() {
        return stats;
    }

    @Override
    public void initialize() throws Exception {
        if (initialized) return;
        initialized = true;

        if (oasstFile == null || !oasstFile.exists()) {
            return;
        }

        List<JSONObject> rawFlatMessages = new ArrayList<>();
        List<JSONObject> rawTrees = new ArrayList<>();

        InputStream fis = new FileInputStream(oasstFile);
        if (oasstFile.getName().endsWith(".gz")) {
            fis = new GZIPInputStream(fis);
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(fis, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) continue;

                try {
                    JSONObject obj = new JSONObject(trimmed);
                    if (obj.has("replies") || obj.has("prompt")) {
                        rawTrees.add(obj);
                    } else if (obj.has("role") || obj.has("text") || obj.has("message_id")) {
                        rawFlatMessages.add(obj);
                    }
                } catch (Exception ignored) {}
            }
        }

        if (!rawTrees.isEmpty()) {
            for (JSONObject tree : rawTrees) {
                List<NormalizedSample> convs = OasstConverter.convertTreeObject(tree, getSourceName());
                extractedSamples.addAll(convs);
            }
        } else if (!rawFlatMessages.isEmpty()) {
            List<NormalizedSample> convs = OasstConverter.convertFlatMessages(rawFlatMessages, getSourceName());
            extractedSamples.addAll(convs);
        }
    }

    @Override
    public boolean hasNext() {
        if (!initialized) {
            try {
                initialize();
            } catch (Exception e) {
                return false;
            }
        }
        return currentIndex < extractedSamples.size();
    }

    @Override
    public NormalizedSample next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        NormalizedSample sample = extractedSamples.get(currentIndex++);
        byte[] bytes = sample.toFullText().getBytes(StandardCharsets.UTF_8);
        long sampleBytes = bytes.length;
        long sampleTokens = sample.getTokenCount() > 0 ? sample.getTokenCount() : (sampleBytes / 4);

        stats.incrementSamplesRead();
        stats.addBytesRead(sampleBytes);
        stats.addAcceptedBytes(sampleBytes);
        stats.addEstimatedTokens(sampleTokens);

        return sample;
    }

    @Override
    public void close() {
        extractedSamples.clear();
    }
}
