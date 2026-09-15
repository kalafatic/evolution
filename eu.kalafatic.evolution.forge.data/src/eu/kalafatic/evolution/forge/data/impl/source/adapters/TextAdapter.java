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
 * Universal source adapter for plain text, Markdown, code, XML, HTML, and document files.
 */
public class TextAdapter implements DatasetSourceAdapter {

    @Override
    public boolean supports(DatasetItem item) {
        if (item == null || item.getPath() == null) return false;
        String path = item.getPath().trim().toLowerCase();
        if (path.endsWith(".txt") || path.endsWith(".md") || path.endsWith(".java") || path.endsWith(".xml") ||
            path.endsWith(".html") || path.endsWith(".htm") || path.endsWith(".properties") || path.endsWith(".json") == false) {
            File f = new File(item.getPath());
            return f.exists() && f.isFile();
        }
        return "FILE".equalsIgnoreCase(item.getType()) || "TEXT".equalsIgnoreCase(item.getType());
    }

    @Override
    public DatasetInspection inspect(DatasetItem item) {
        if (item == null || item.getPath() == null) {
            return new DatasetInspection(item, "TextAdapter", false, 0, "text", false, "Path is null");
        }
        File file = new File(item.getPath());
        boolean exists = file.exists() && file.isFile();
        long size = exists ? file.length() : 0;
        return new DatasetInspection(item, "TextAdapter", exists, size, "text", exists, exists ? "Text File Source" : "File does not exist");
    }

    @Override
    public List<NormalizedSample> convert(DatasetItem item, DatasetPreparationContext context) throws Exception {
        List<NormalizedSample> samples = new ArrayList<>();
        if (item == null || item.getPath() == null) return samples;

        File file = new File(item.getPath());
        if (!file.exists() || !file.isFile()) {
            context.log("[forge.dataset] [TextAdapter] File not found: " + item.getPath());
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
