package eu.kalafatic.evolution.forge.data.impl.collector;

import eu.kalafatic.evolution.forge.data.api.collector.CollectionContext;
import eu.kalafatic.evolution.forge.data.api.collector.CollectionResult;
import eu.kalafatic.evolution.forge.data.api.collector.CollectorType;
import eu.kalafatic.evolution.forge.data.api.collector.SourceType;
import eu.kalafatic.evolution.forge.data.api.collector.TrainingDataCollector;
import eu.kalafatic.evolution.forge.data.api.collector.TrainingDataItem;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Collects training knowledge from user-selected files or directories on disk.
 */
public class DiskDataCollector extends TrainingDataCollector {

    @Override
    public CollectorType getType() {
        return CollectorType.DISK;
    }

    @Override
    public String getName() {
        return "Disk Data Collector";
    }

    @Override
    public CollectionResult collect(CollectionContext context) {
        long startTime = System.currentTimeMillis();
        List<File> diskPaths = context.getDiskPaths();

        if (diskPaths == null || diskPaths.isEmpty()) {
            return CollectionResult.success(getType(), new ArrayList<>(), System.currentTimeMillis() - startTime);
        }

        List<TrainingDataItem> items = new ArrayList<>();
        List<String> exclusions = context.getExclusions();

        for (File pathFile : diskPaths) {
            if (pathFile == null || !pathFile.exists()) {
                continue;
            }

            if (pathFile.isFile()) {
                processFile(pathFile.toPath(), pathFile.toPath().getParent(), context, exclusions, items);
            } else if (pathFile.isDirectory()) {
                try (Stream<Path> walk = Files.walk(pathFile.toPath())) {
                    List<Path> files = walk
                            .filter(Files::isRegularFile)
                            .sorted()
                            .collect(Collectors.toList());

                    for (Path file : files) {
                        processFile(file, pathFile.toPath(), context, exclusions, items);
                    }
                } catch (Exception e) {
                    // Continue with other paths
                }
            }
        }

        return CollectionResult.success(getType(), items, System.currentTimeMillis() - startTime);
    }

    private void processFile(Path filePath, Path rootPath, CollectionContext context, List<String> exclusions, List<TrainingDataItem> items) {
        try {
            if (!isNotExcluded(filePath, rootPath, exclusions)) {
                return;
            }

            long size = Files.size(filePath);
            if (context.getMaxFileSize() > 0 && size > context.getMaxFileSize()) {
                return;
            }

            String content = Files.readString(filePath);
            String fileName = filePath.getFileName().toString();
            String fullPath = filePath.toAbsolutePath().toString().replace('\\', '/');

            TrainingDataItem item = new TrainingDataItem();
            item.setSourceType(SourceType.DISK);
            item.setSource(fullPath);
            item.setTitle(fileName);
            item.setContent(content);

            item.addMetadata("fileSize", size);
            item.addMetadata("extension", getExtension(fileName));
            item.addMetadata("sourcePath", fullPath);

            items.add(item);
        } catch (Exception e) {
            // Non-text binary or unreadable files skipped gracefully
        }
    }

    private boolean isNotExcluded(Path path, Path rootPath, List<String> exclusions) {
        String str = path.toAbsolutePath().toString().replace('\\', '/');
        if (exclusions != null) {
            for (String exclusion : exclusions) {
                if (str.contains(exclusion)) {
                    return false;
                }
            }
        }
        return true;
    }

    private String getExtension(String fileName) {
        int idx = fileName.lastIndexOf('.');
        if (idx > 0 && idx < fileName.length() - 1) {
            return fileName.substring(idx + 1).toLowerCase();
        }
        return "unknown";
    }
}
