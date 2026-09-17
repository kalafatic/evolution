package eu.kalafatic.evolution.forge.data.impl.source.adapters;

import eu.kalafatic.evolution.forge.data.api.NormalizedSample;
import eu.kalafatic.evolution.forge.data.api.source.DatasetInspection;
import eu.kalafatic.evolution.forge.data.api.source.DatasetItem;
import eu.kalafatic.evolution.forge.data.api.source.DatasetPreparationContext;
import eu.kalafatic.evolution.forge.data.api.source.DatasetSourceAdapter;
import eu.kalafatic.evolution.forge.data.api.source.SourceAdapterRegistry;
import eu.kalafatic.evolution.forge.data.impl.source.DatasetMetadataFilter;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Universal source adapter for directory / folder targets.
 * Intelligently discovers data files, excludes metadata/infrastructure files (README, .git, etc.),
 * calculates dataset sizes, and delegates conversion to SourceAdapterRegistry.
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
        if (!folder.exists() || !folder.isDirectory()) {
            return new DatasetInspection(item, "FolderAdapter", false, 0, "folder", false, "Folder does not exist");
        }

        List<Path> allFiles = scanFolderFiles(folder);
        long totalDataBytes = 0;
        int dataFilesCount = 0;
        int metadataFilesCount = 0;

        for (Path path : allFiles) {
            if (DatasetMetadataFilter.isMetadataFile(path)) {
                metadataFilesCount++;
                continue;
            }
            File file = path.toFile();
            DatasetItem fileItem = new DatasetItem(true, file.getAbsolutePath(), "FILE");
            DatasetSourceAdapter adapter = registry != null ? registry.findAdapter(fileItem) : null;
            if (adapter != null && !(adapter instanceof FolderAdapter)) {
                dataFilesCount++;
                totalDataBytes += file.length();
            } else {
                metadataFilesCount++;
            }
        }

        boolean supported = dataFilesCount > 0;
        String details = supported ?
                String.format("Folder Dataset Source (%d data files, %d metadata files)", dataFilesCount, metadataFilesCount) :
                String.format("Folder contains no supported data files (%d data files, %d metadata files)", dataFilesCount, metadataFilesCount);

        return new DatasetInspection(item, "FolderAdapter", true, totalDataBytes, "folder", supported, details);
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

        List<Path> allFiles = scanFolderFiles(folder);
        List<Path> dataFiles = new ArrayList<>();
        int metadataCount = 0;

        for (Path path : allFiles) {
            if (DatasetMetadataFilter.isMetadataFile(path)) {
                metadataCount++;
                continue;
            }
            File file = path.toFile();
            DatasetItem fileItem = new DatasetItem(true, file.getAbsolutePath(), "FILE");
            DatasetSourceAdapter adapter = registry != null ? registry.findAdapter(fileItem) : null;
            if (adapter != null && !(adapter instanceof FolderAdapter)) {
                dataFiles.add(path);
            } else {
                metadataCount++;
            }
        }

        context.log(String.format("[FORGE-DISCOVERY] source=%s detectedStructure=DATASET_CONTAINER files=%d dataFiles=%d metadataFiles=%d",
                folder.getAbsolutePath(), allFiles.size(), dataFiles.size(), metadataCount));

        for (Path filePath : dataFiles) {
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

        context.log(String.format("[FORGE-PROCESS] source=%s recordsRead=%d recordsAccepted=%d recordsRejected=0 duplicates=0",
                folder.getAbsolutePath(), samples.size(), samples.size()));
        return samples;
    }

    private List<Path> scanFolderFiles(File folder) {
        List<Path> discoveredFiles = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(folder.toPath())) {
            discoveredFiles = walk.filter(Files::isRegularFile)
                    .sorted()
                    .collect(Collectors.toList());
        } catch (Exception ex) {
            // Log or ignore
        }
        return discoveredFiles;
    }
}
