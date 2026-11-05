package eu.kalafatic.evolution.forge.data.impl.source.adapters;

import eu.kalafatic.evolution.forge.data.api.NormalizedSample;
import eu.kalafatic.evolution.forge.data.api.source.DatasetInspection;
import eu.kalafatic.evolution.forge.data.api.source.DatasetItem;
import eu.kalafatic.evolution.forge.data.api.source.DatasetPreparationContext;
import eu.kalafatic.evolution.forge.data.api.source.DatasetSourceAdapter;
import eu.kalafatic.evolution.forge.data.impl.source.DatasetMetadataFilter;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Universal source adapter for plain text, Markdown, code, XML, HTML, and document files.
 * Ignores metadata and documentation files (README, LICENSE, NOTICE, etc.).
 */
public class TextAdapter implements DatasetSourceAdapter {

    @Override
    public boolean supports(DatasetItem item) {
        if (item == null || item.getPath() == null) return false;
        String type = item.getType() != null ? item.getType().trim().toUpperCase() : "";
        String path = item.getPath().trim().toLowerCase();

        if (path.endsWith(".json") || path.endsWith(".jsonl") || path.endsWith(".parquet") ||
            path.endsWith(".csv") || path.endsWith(".evodata")) {
            return false;
        }

        File f = new File(item.getPath());
        if (f.exists() && f.isDirectory() && !"FILE".equals(type)) {
            return false;
        }
        if (f.exists() && DatasetMetadataFilter.isMetadataFile(f)) {
            return false;
        }

        if ("FILE".equals(type) || "TEXT".equals(type) || "DOCUMENT".equals(type)) {
            return true;
        }
        return path.endsWith(".txt") || path.endsWith(".md") || path.endsWith(".java") ||
               path.endsWith(".py") || path.endsWith(".cpp") || path.endsWith(".xml") ||
               path.endsWith(".html") || path.endsWith(".adoc") || path.endsWith(".rst");
    }

    @Override
    public DatasetInspection inspect(DatasetItem item) {
        if (item == null || item.getPath() == null) {
            return new DatasetInspection(item, "TextAdapter", false, 0, "text", false, "Path is null");
        }
        File file = new File(item.getPath());
        boolean exists = file.exists() && file.isFile();
        boolean isMetadata = exists && DatasetMetadataFilter.isMetadataFile(file);
        boolean supported = exists && !isMetadata;
        long size = supported ? file.length() : 0;
        String details = isMetadata ? "Ignored metadata file" : (exists ? "Text File Source" : "File does not exist");

        return new DatasetInspection(item, "TextAdapter", exists, size, "text", supported, details);
    }

    @Override
    public List<NormalizedSample> convert(DatasetItem item, DatasetPreparationContext context) throws Exception {
        List<NormalizedSample> samples = new ArrayList<>();
        if (item == null || item.getPath() == null) return samples;

        File file = new File(item.getPath());
        if (!file.exists() || !file.isFile() || DatasetMetadataFilter.isMetadataFile(file)) {
            context.log("[forge.dataset] [TextAdapter] Ignored or missing file: " + item.getPath());
            return samples;
        }

        context.log("[forge.dataset] Preparing source Text: " + file.getName());
        String text = Files.readString(file.toPath(), StandardCharsets.UTF_8).trim();
        if (!text.isEmpty()) {
            samples.add(NormalizedSample.createTextSample(text, file.getName()));
        }

        context.log("[forge.dataset] Text Parsed 1 sample (" + text.length() + " chars)");
        return samples;
    }
}
