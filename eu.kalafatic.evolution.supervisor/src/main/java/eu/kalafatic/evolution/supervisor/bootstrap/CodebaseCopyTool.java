package eu.kalafatic.evolution.supervisor.bootstrap;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;

public class CodebaseCopyTool {

    public CopyResult copy(CopyConfiguration config) {
        long startTime = System.currentTimeMillis();
        File source = config.getSourcePath();
        File target = config.getTargetPath();

        if (!source.exists()) {
            return new CopyResult(false, "Source path does not exist: " + source.getAbsolutePath());
        }

        if (target.exists() && !config.isOverwrite()) {
            return new CopyResult(false, "Target path already exists and overwrite is disabled: " + target.getAbsolutePath());
        }

        final int[] filesCopied = {0};
        final long[] totalBytes = {0};

        try {
            if (target.exists() && config.isOverwrite()) {
                deleteRecursively(target);
            }
            target.mkdirs();

            Files.walkFileTree(source.toPath(), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    if (isExcluded(dir.toFile(), config)) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    Path targetDir = target.toPath().resolve(source.toPath().relativize(dir));
                    if (!Files.exists(targetDir)) {
                        Files.createDirectories(targetDir);
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (isExcluded(file.toFile(), config)) {
                        return FileVisitResult.CONTINUE;
                    }
                    Path targetFile = target.toPath().resolve(source.toPath().relativize(file));
                    Files.copy(file, targetFile, StandardCopyOption.REPLACE_EXISTING);
                    filesCopied[0]++;
                    totalBytes[0] += attrs.size();
                    return FileVisitResult.CONTINUE;
                }
            });

            CopyResult result = new CopyResult(true, "Codebase copied successfully");
            result.setFilesCopied(filesCopied[0]);
            result.setTotalBytes(totalBytes[0]);
            result.setDurationMs(System.currentTimeMillis() - startTime);
            return result;

        } catch (IOException e) {
            return new CopyResult(false, "Failed to copy codebase: " + e.getMessage());
        }
    }

    private boolean isExcluded(File file, CopyConfiguration config) {
        String fileName = file.getName();
        for (String exclusion : config.getExclusions()) {
            if (fileName.equals(exclusion) || file.getAbsolutePath().contains(File.separator + exclusion + File.separator)) {
                return true;
            }
        }
        return false;
    }

    private void deleteRecursively(File file) throws IOException {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) {
                    deleteRecursively(f);
                }
            }
        }
        if (!file.delete()) {
            throw new IOException("Failed to delete: " + file.getAbsolutePath());
        }
    }
}
