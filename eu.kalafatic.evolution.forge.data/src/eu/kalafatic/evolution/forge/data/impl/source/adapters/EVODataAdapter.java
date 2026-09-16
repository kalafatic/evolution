package eu.kalafatic.evolution.forge.data.impl.source.adapters;

import eu.kalafatic.evolution.forge.data.api.NormalizedSample;
import eu.kalafatic.evolution.forge.data.api.artifact.EvoDatasetArtifact;
import eu.kalafatic.evolution.forge.data.api.source.DatasetInspection;
import eu.kalafatic.evolution.forge.data.api.source.DatasetItem;
import eu.kalafatic.evolution.forge.data.api.source.DatasetPreparationContext;
import eu.kalafatic.evolution.forge.data.api.source.DatasetSourceAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Universal source adapter for existing .evodata native training dataset archives.
 */
public class EVODataAdapter implements DatasetSourceAdapter {

    @Override
    public boolean supports(DatasetItem item) {
        if (item == null || item.getPath() == null) return false;
        String path = item.getPath().trim().toLowerCase();
        return path.endsWith(".evodata") || "EVO_DATA".equalsIgnoreCase(item.getType());
    }

    @Override
    public DatasetInspection inspect(DatasetItem item) {
        if (item == null || item.getPath() == null) {
            return new DatasetInspection(item, "EVODataAdapter", false, 0, "evodata", false, "Item or path is null");
        }
        File file = new File(item.getPath());
        boolean exists = file.exists() && file.isFile();
        long size = exists ? file.length() : 0;
        return new DatasetInspection(item, "EVODataAdapter", exists, size, "evodata", exists, exists ? "Valid .evodata file" : "File does not exist");
    }

    @Override
    public List<NormalizedSample> convert(DatasetItem item, DatasetPreparationContext context) throws Exception {
        List<NormalizedSample> samples = new ArrayList<>();
        if (item == null || item.getPath() == null) return samples;

        File file = new File(item.getPath());
        if (!file.exists() || !file.isFile()) {
            context.log("[forge.dataset] [EVODataAdapter] Warning: File not found " + item.getPath());
            return samples;
        }

        context.log("[forge.dataset] Loading existing .evodata archive: " + file.getName());
        EvoDatasetArtifact artifact = EvoDatasetArtifact.load(file);
        if (artifact != null && artifact.getSamples() != null) {
            samples.addAll(artifact.getSamples());
            context.log("[forge.dataset] Reused " + samples.size() + " native samples from " + file.getName());
        }
        return samples;
    }
}
