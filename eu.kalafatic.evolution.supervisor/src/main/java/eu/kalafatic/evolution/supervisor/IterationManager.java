package eu.kalafatic.evolution.supervisor;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class IterationManager {
    private final File baseDir;

    public IterationManager(File baseDir) {
        this.baseDir = baseDir;
    }

    public File prepareIteration(int iterNumber) throws IOException {
        String iterName = String.format("iter-%03d", iterNumber);
        File iterDir = new File(new File(baseDir, "iterations"), iterName);
        if (!iterDir.exists() && !iterDir.mkdirs()) {
            throw new IOException("Could not create iteration directory: " + iterDir.getAbsolutePath());
        }

        File workspaceDir = new File(baseDir, "workspace");
        if (!workspaceDir.exists()) {
            throw new IOException("Workspace directory missing: " + workspaceDir.getAbsolutePath());
        }

        // Create at least variant-A
        File variantA = new File(iterDir, "variant-A");
        copyDirectory(workspaceDir.toPath(), variantA.toPath());

        // Optionally variant-B
        File variantB = new File(iterDir, "variant-B");
        copyDirectory(workspaceDir.toPath(), variantB.toPath());

        return iterDir;
    }

    public void promoteVariant(File variantDir, File workspaceDir) throws IOException {
        System.out.println("[PROMOTE] Promoting " + variantDir.getName() + " to workspace");
        if (workspaceDir.exists()) {
            deleteDirectory(workspaceDir.toPath());
        }
        copyDirectory(variantDir.toPath(), workspaceDir.toPath());
    }

    private void copyDirectory(Path source, Path target) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path targetDir = target.resolve(source.relativize(dir));
                if (!Files.exists(targetDir)) {
                    Files.createDirectories(targetDir);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.copy(file, target.resolve(source.relativize(file)), StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void deleteDirectory(Path path) throws IOException {
        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
