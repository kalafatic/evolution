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
    private final java.util.ArrayDeque<NormalizedSample> pendingRecords = new java.util.ArrayDeque<>();

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

        // Serve records already produced by hybrid recordization of the previous file.
        if (serveNextPendingRecord()) {
            return;
        }

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
                boolean isJsonl = file.getName().toLowerCase().endsWith(".jsonl");

                if (isJsonl) {
                    line = currentReader.readLine();
                    if (line != null) {
                        String trimmed = line.trim();
                        if (!trimmed.isEmpty()) {
                            NormalizedSample sample = parseJsonlLine(trimmed, file.getAbsolutePath());
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
                    // Hybrid recordization for txt/md/java files:
                    // - lines too short to ever pass quality filtering (<= 3 chars) become
                    //   standalone records so the pipeline can REJECT and account them;
                    // - all remaining lines are merged per file (whole-file sample
                    //   semantics preserved for meaningful content).
                    List<String> mergedLines = new ArrayList<>();
                    while ((line = currentReader.readLine()) != null) {
                        String trimmed = line.trim();
                        if (!trimmed.isEmpty() && trimmed.length() <= 3) {
                            if (!mergedLines.isEmpty()) {
                                pendingRecords.add(buildMergedRecord(mergedLines, file));
                                mergedLines.clear();
                            }
                            pendingRecords.add(NormalizedSample.createTextSample(trimmed, file.getAbsolutePath() + "#short-record"));
                        } else {
                            mergedLines.add(line);
                        }
                    }
                    currentReader.close();
                    currentReader = null;
                    currentFileIdx++;

                    if (!mergedLines.isEmpty()) {
                        pendingRecords.add(buildMergedRecord(mergedLines, file));
                    }

                    // Serve the first produced record; remaining records are served
                    // on subsequent advanceToNextSample() calls.
                    if (serveNextPendingRecord()) {
                        return;
                    }
                }
            } catch (Exception e) {
                currentFileIdx++;
                currentReader = null;
            }
        }
    }

    private boolean serveNextPendingRecord() {
        NormalizedSample record = pendingRecords.pollFirst();
        if (record == null) {
            return false;
        }
        nextBufferedSample = record;
        stats.incrementSamplesRead();
        stats.addBytesRead(record.toFullText().getBytes(StandardCharsets.UTF_8).length);
        return true;
    }

    private NormalizedSample buildMergedRecord(List<String> lines, File file) {
        StringBuilder sb = new StringBuilder();
        for (String l : lines) {
            sb.append(l).append("\n");
        }
        String content = sb.toString().trim();
        NormalizedSample sample = NormalizedSample.createTextSample(content, file.getAbsolutePath());
        if (file.getName().endsWith(".java")) {
            sample.setType(TrainingSampleType.CODE);
        }
        return sample;
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

    private NormalizedSample parseJsonlLine(String line, String sourcePath) {
        if (line.startsWith("{") && line.endsWith("}")) {
            try {
                org.json.JSONObject obj = new org.json.JSONObject(line);
                String typeStr = obj.optString("type", null);
                if ("INSTRUCTION".equalsIgnoreCase(typeStr)) {
                    String inst = obj.optString("instruction", obj.optString("prompt", ""));
                    String resp = obj.optString("response", obj.optString("completion", ""));
                    return NormalizedSample.createInstructionSample(inst, resp, sourcePath);
                } else {
                    String text = obj.optString("text", obj.optString("content", null));
                    if (text != null) {
                        return NormalizedSample.createTextSample(text, sourcePath);
                    }
                }
            } catch (Exception ignored) {}
        }
        return NormalizedSample.createTextSample(line, sourcePath);
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
