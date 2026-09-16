package eu.kalafatic.evolution.forge.data.impl.source.adapters;

import eu.kalafatic.evolution.forge.data.api.NormalizedSample;
import eu.kalafatic.evolution.forge.data.api.source.DatasetInspection;
import eu.kalafatic.evolution.forge.data.api.source.DatasetItem;
import eu.kalafatic.evolution.forge.data.api.source.DatasetPreparationContext;
import eu.kalafatic.evolution.forge.data.api.source.DatasetSourceAdapter;
import eu.kalafatic.evolution.forge.data.api.source.SourceAdapterRegistry;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Universal source adapter for directory / folder targets.
 * Recursively inspects child files and delegates conversion to SourceAdapterRegistry.
 */
public class FolderAdapter implements DatasetSourceAdapter {

    private final SourceAdapterRegistry registry;

    public FolderAdapter(SourceAdapterRegistry registry) {
        this.registry = registry;
    }

    @Override
    public boolean supports(DatasetItem item) {
        if (item == null || item.getPath() == null) return false;
        File file = new File(item.getPath());
        return file.exists() && file.isDirectory() || "FOLDER".equalsIgnoreCase(item.getType());
    }

    @Override
    public DatasetInspection inspect(DatasetItem item) {
        if (item == null || item.getPath() == null) {
            return new DatasetInspection(item, "FolderAdapter", false, 0, "folder", false, "Path is null");
        }
        File folder = new File(item.getPath());
        boolean exists = folder.exists() && folder.isDirectory();
        return new DatasetInspection(item, "FolderAdapter", exists, 0, "folder", exists, exists ? "Directory Folder Source" : "Folder does not exist");
    }

    @Override
    public List<NormalizedSample> convert(DatasetItem item, DatasetPreparationContext context) throws Exception {
        List<NormalizedSample> samples = new ArrayList<>();
        if (item == null || item.getPath() == null) return samples;

        File folder = new File(item.getPath());
        if (!folder.exists() || !folder.isDirectory()) {
            context.log("[forge.dataset] [FolderAdapter] Folder not found: " + item.getPath());
            return samples;
        }

        context.log("[forge.dataset] Preparing source Folder: " + folder.getAbsolutePath());

        List<Path> discoveredFiles;
        try (Stream<Path> walk = Files.walk(folder.toPath())) {
            discoveredFiles = walk.filter(Files::isRegularFile)
                    .filter(p -> !p.toString().contains("/.git/") && !p.toString().contains("\\.git\\") &&
                                 !p.toString().contains("/target/") && !p.toString().contains("\\target\\") &&
                                 !p.toString().contains("/node_modules/") && !p.toString().contains("\\node_modules\\") &&
                                 !p.toString().contains("/bin/") && !p.toString().contains("\\bin\\"))
                    .sorted()
                    .collect(Collectors.toList());
        }

        context.log("[forge.dataset] Folder scan found " + discoveredFiles.size() + " files in " + folder.getName());

        for (Path filePath : discoveredFiles) {
            if (context.isCancelled()) break;
            File file = filePath.toFile();
            DatasetItem fileItem = new DatasetItem(true, file.getAbsolutePath(), "FILE");

            DatasetSourceAdapter adapter = registry != null ? registry.findAdapter(fileItem) : null;
            if (adapter != null && !(adapter instanceof FolderAdapter)) {
                List<NormalizedSample> converted = adapter.convert(fileItem, context);
                if (converted != null && !converted.isEmpty()) {
                    samples.addAll(converted);
                }
            }
        }

        context.log("[forge.dataset] Folder total collected samples: " + samples.size());
        return samples;
    }
}
