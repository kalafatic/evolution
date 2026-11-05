package eu.kalafatic.evolution.forge.data.impl.source.adapters;

import eu.kalafatic.evolution.forge.data.api.NormalizedSample;
import eu.kalafatic.evolution.forge.data.api.source.DatasetInspection;
import eu.kalafatic.evolution.forge.data.api.source.DatasetItem;
import eu.kalafatic.evolution.forge.data.api.source.DatasetPreparationContext;
import eu.kalafatic.evolution.forge.data.api.source.DatasetSourceAdapter;
import eu.kalafatic.evolution.forge.data.api.source.DatasetSourceConfig;
import eu.kalafatic.evolution.forge.data.api.source.ResolvedSource;
import eu.kalafatic.evolution.forge.data.impl.source.HuggingFaceDatasetSource;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Universal source adapter for Hugging Face dataset repositories.
 */
public class HuggingFaceAdapter implements DatasetSourceAdapter {

    @Override
    public boolean supports(DatasetItem item) {
        if (item == null || item.getPath() == null) return false;
        String type = item.getType() != null ? item.getType().trim().toUpperCase() : "";
        String path = item.getPath().trim();

        if ("HUGGING_FACE".equals(type) || "HUGGINGFACE".equals(type) || "HF".equals(type)) {
            return true;
        }

        // Do not match explicit local file/folder types
        if ("FILE".equals(type) || "FOLDER".equals(type)) {
            return false;
        }

        // If path looks like an absolute local filesystem path or has a typical file extension, do not match as HF repo
        if (path.startsWith("/") || path.startsWith("\\") || path.matches("^[A-Za-z]:.*")) {
            return false;
        }
        String lower = path.toLowerCase();
        if (lower.endsWith(".txt") || lower.endsWith(".md") || lower.endsWith(".json") ||
            lower.endsWith(".jsonl") || lower.endsWith(".parquet") || lower.endsWith(".csv") ||
            lower.endsWith(".evodata") || lower.endsWith(".java") || lower.endsWith(".py")) {
            return false;
        }

        // If not a local file on disk, check if path resembles HF repo ID (e.g. "wikitext", "databricks/dolly-15k")
        File f = new File(path);
        if (!f.exists() && (path.equalsIgnoreCase("wikitext") || path.equalsIgnoreCase("gsm8k") ||
            path.equalsIgnoreCase("fineweb") || path.contains("huggingface") ||
            (path.contains("/") && !path.contains(":") && !path.startsWith(".")))) {
            return true;
        }

        return false;
    }

    @Override
    public DatasetInspection inspect(DatasetItem item) {
        if (item == null || item.getPath() == null) {
            return new DatasetInspection(item, "HuggingFaceAdapter", false, 0, "huggingface", false, "Path is null");
        }
        String path = item.getPath().trim();
        return new DatasetInspection(item, "HuggingFaceAdapter", true, 50 * 1024 * 1024, "huggingface", true, "HuggingFace Repository: " + path);
    }

    @Override
    public List<NormalizedSample> convert(DatasetItem item, DatasetPreparationContext context) throws Exception {
        List<NormalizedSample> samples = new ArrayList<>();
        if (item == null || item.getPath() == null) return samples;

        String repoId = item.getPath().trim();
        context.log("[forge.dataset] Preparing HuggingFace dataset source: " + repoId);

        try {
            DatasetSourceConfig config = new DatasetSourceConfig("HUGGING_FACE", repoId);
            if (context.getTargetUsableBytes() > 0) {
                config.setMaxBytes(context.getTargetUsableBytes());
            }
            HuggingFaceDatasetSource hfSource = new HuggingFaceDatasetSource(config);

            ResolvedSource resolved = hfSource.preflight();
            if (resolved != null && resolved.isAccessible()) {
                hfSource.initialize();
                while (hfSource.hasNext()) {
                    if (context.isCancelled()) break;
                    NormalizedSample sample = hfSource.next();
                    if (sample != null) {
                        samples.add(sample);
                    }
                }
            } else {
                context.log("[forge.dataset] [HuggingFaceAdapter] Preflight check failed for: " + repoId);
            }
        } catch (Exception ex) {
            context.log("[forge.dataset] [HuggingFaceAdapter] Acquisition failed for " + repoId + ": " + ex.getMessage());
            throw ex;
        }

        context.log("[forge.dataset] HuggingFace Parsed " + samples.size() + " samples from " + repoId);
        return samples;
    }
}
