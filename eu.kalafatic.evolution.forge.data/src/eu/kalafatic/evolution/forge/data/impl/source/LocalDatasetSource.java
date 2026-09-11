package eu.kalafatic.evolution.forge.data.impl.source;

import eu.kalafatic.evolution.forge.data.api.NormalizedSample;
import eu.kalafatic.evolution.forge.data.api.TrainingSampleType;
import eu.kalafatic.evolution.forge.data.api.source.DatasetSource;
import eu.kalafatic.evolution.forge.data.api.source.DatasetSourceConfig;
import eu.kalafatic.evolution.forge.data.api.source.DatasetSourceStats;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Local source implementation reading text, markdown, json, or directory tree.
 */
public class LocalDatasetSource implements DatasetSource {

    private final DatasetSourceConfig config;
    private final DatasetSourceStats stats = new DatasetSourceStats();
    private List<File> filesToProcess = new ArrayList<>();
    private int currentFileIdx = 0;
    private BufferedReader currentReader = null;
    private NormalizedSample nextBufferedSample = null;

    public LocalDatasetSource(String path) {
        this(new DatasetSourceConfig("LOCAL", path != null ? path : "."));
    }

    public LocalDatasetSource(DatasetSourceConfig config) {
        this.config = config != null ? config : new DatasetSourceConfig("LOCAL", ".");
    }

    @Override
    public String getSourceName() {
        return "Local: " + config.getRepository();
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
        File root = new File(config.getRepository());
        filesToProcess.clear();
        if (root.exists()) {
            if (root.isFile()) {
                filesToProcess.add(root);
            } else if (root.isDirectory()) {
                scanDirectory(root);
            }
        }
        currentFileIdx = 0;
        advanceToNextSample();
    }

    private void scanDirectory(File dir) {
        File[] children = dir.listFiles();
        if (children == null) return;
        for (File child : children) {
            if (child.isDirectory()) {
                if (!child.getName().startsWith(".") && !"target".equals(child.getName()) && !"build".equals(child.getName())) {
                    scanDirectory(child);
                }
            } else if (child.isFile()) {
                String name = child.getName().toLowerCase();
                if (name.endsWith(".txt") || name.endsWith(".md") || name.endsWith(".json") || name.endsWith(".jsonl") || name.endsWith(".java")) {
                    filesToProcess.add(child);
                }
            }
        }
    }

    private void advanceToNextSample() {
        nextBufferedSample = null;

        // Check hard bounds
        if (config.getMaxSamples() > 0 && stats.getTotalSamplesRead() >= config.getMaxSamples()) {
            return;
        }
        if (config.getMaxBytes() > 0 && stats.getAcceptedBytes() >= config.getMaxBytes()) {
            return;
        }

        while (currentFileIdx < filesToProcess.size()) {
            File file = filesToProcess.get(currentFileIdx);
            try {
                if (currentReader == null) {
                    currentReader = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8));
                }

                String line;
                StringBuilder textBuf = new StringBuilder();
                boolean isJsonl = file.getName().toLowerCase().endsWith(".jsonl");

                if (isJsonl) {
                    line = currentReader.readLine();
                    if (line != null) {
                        if (!line.trim().isEmpty()) {
                            NormalizedSample sample = NormalizedSample.createTextSample(line.trim(), file.getAbsolutePath());
                            stats.incrementSamplesRead();
                            stats.addBytesRead(line.getBytes(StandardCharsets.UTF_8).length);
                            nextBufferedSample = sample;
                            return;
                        }
                    } else {
                        currentReader.close();
                        currentReader = null;
                        currentFileIdx++;
                    }
                } else {
                    // Read complete file for txt/md/java
                    while ((line = currentReader.readLine()) != null) {
                        textBuf.append(line).append("\n");
                    }
                    currentReader.close();
                    currentReader = null;
                    currentFileIdx++;

                    String content = textBuf.toString().trim();
                    if (!content.isEmpty()) {
                        NormalizedSample sample = NormalizedSample.createTextSample(content, file.getAbsolutePath());
                        if (file.getName().endsWith(".java")) {
                            sample.setType(TrainingSampleType.CODE);
                        }
                        stats.incrementSamplesRead();
                        stats.addBytesRead(content.getBytes(StandardCharsets.UTF_8).length);
                        nextBufferedSample = sample;
                        return;
                    }
                }
            } catch (Exception e) {
                currentFileIdx++;
                currentReader = null;
            }
        }
    }

    @Override
    public boolean hasNext() {
        return nextBufferedSample != null;
    }

    @Override
    public NormalizedSample next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        NormalizedSample current = nextBufferedSample;
        advanceToNextSample();
        return current;
    }

    @Override
    public void close() {
        if (currentReader != null) {
            try {
                currentReader.close();
            } catch (Exception ignored) {}
            currentReader = null;
        }
    }
}
