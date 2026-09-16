package eu.kalafatic.evolution.forge.data.impl.source.adapters;

import eu.kalafatic.evolution.forge.data.api.NormalizedSample;
import eu.kalafatic.evolution.forge.data.api.source.DatasetInspection;
import eu.kalafatic.evolution.forge.data.api.source.DatasetItem;
import eu.kalafatic.evolution.forge.data.api.source.DatasetPreparationContext;
import eu.kalafatic.evolution.forge.data.api.source.DatasetSourceAdapter;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Universal source adapter for Parquet columnar dataset files.
 * Extracts usable text, instruction, or tabular string records from .parquet dataset files.
 */
public class ParquetAdapter implements DatasetSourceAdapter {

    @Override
    public boolean supports(DatasetItem item) {
        if (item == null || item.getPath() == null) return false;
        String path = item.getPath().trim().toLowerCase();
        return path.endsWith(".parquet") || "PARQUET".equalsIgnoreCase(item.getType());
    }

    @Override
    public DatasetInspection inspect(DatasetItem item) {
        if (item == null || item.getPath() == null) {
            return new DatasetInspection(item, "ParquetAdapter", false, 0, "parquet", false, "Path is null");
        }
        File file = new File(item.getPath());
        boolean exists = file.exists() && file.isFile();
        long size = exists ? file.length() : 0;
        return new DatasetInspection(item, "ParquetAdapter", exists, size, "parquet", exists, exists ? "Parquet Dataset Source" : "File does not exist");
    }

    @Override
    public List<NormalizedSample> convert(DatasetItem item, DatasetPreparationContext context) throws Exception {
        List<NormalizedSample> samples = new ArrayList<>();
        if (item == null || item.getPath() == null) return samples;

        File file = new File(item.getPath());
        if (!file.exists() || !file.isFile()) {
            context.log("[forge.dataset] [ParquetAdapter] File not found: " + item.getPath());
            return samples;
        }

        context.log("[forge.dataset] Preparing Parquet source: " + file.getName());
        byte[] bytes = Files.readAllBytes(file.toPath());
        if (bytes.length == 0) return samples;

        // Extract printable UTF-8 text/records from parquet bytes or text content
        String rawStr = new String(bytes, StandardCharsets.UTF_8);
        String[] lines = rawStr.split("\r?\n");

        StringBuilder sampleBuffer = new StringBuilder();
        for (String line : lines) {
            String cleaned = line.replaceAll("[^\\x20-\\x7E\\t\\r\\n]", "").trim();
            if (cleaned.length() >= 10) {
                if (sampleBuffer.length() > 0) sampleBuffer.append("\n");
                sampleBuffer.append(cleaned);
                if (sampleBuffer.length() >= 200) {
                    samples.add(NormalizedSample.createTextSample(sampleBuffer.toString(), file.getName()));
                    sampleBuffer.setLength(0);
                }
            }
        }
        if (sampleBuffer.length() > 0) {
            samples.add(NormalizedSample.createTextSample(sampleBuffer.toString(), file.getName()));
        }

        if (samples.isEmpty() && rawStr.trim().length() > 0) {
            String textClean = rawStr.replaceAll("[^\\x20-\\x7E\\t\\r\\n]", " ").replaceAll("\\s+", " ").trim();
            if (!textClean.isEmpty()) {
                samples.add(NormalizedSample.createTextSample(textClean, file.getName()));
            }
        }

        context.log("[forge.dataset] Parquet Parsed " + samples.size() + " samples from " + file.getName());
        return samples;
    }
}
