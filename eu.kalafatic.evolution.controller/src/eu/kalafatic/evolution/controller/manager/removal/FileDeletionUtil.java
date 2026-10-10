package eu.kalafatic.evolution.controller.manager.removal;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Reliable, observable file and directory deletion utility.
 */
public class FileDeletionUtil {

    public static record DeletionStepResult(Path path, boolean existed, boolean deleted, String message, Exception exception) {}

    public static record DirectoryDeletionResult(Path rootPath, boolean success, List<DeletionStepResult> stepResults) {}

    /**
     * Safely deletes a single file or empty directory with verification.
     *
     * @param path Target path to delete.
     * @return DeletionStepResult capturing status and details.
     */
    public static DeletionStepResult deleteFile(Path path) {
        if (path == null) {
            return new DeletionStepResult(null, false, false, "Path is null", null);
        }

        Path normalized = path.toAbsolutePath().normalize();

        if (!Files.exists(normalized, LinkOption.NOFOLLOW_LINKS)) {
            return new DeletionStepResult(normalized, false, true, "File was already absent", null);
        }

        try {
            // Attempt to clear read-only attribute if needed
            try {
                if (!Files.isWritable(normalized)) {
                    normalized.toFile().setWritable(true);
                }
            } catch (Exception ignored) {}

            Files.delete(normalized);

            // Verify non-existence
            if (Files.exists(normalized, LinkOption.NOFOLLOW_LINKS)) {
                return new DeletionStepResult(normalized, true, false, "Filesystem reports file still exists after delete() call", null);
            }

            return new DeletionStepResult(normalized, true, true, "Successfully deleted", null);
        } catch (NoSuchFileException e) {
            return new DeletionStepResult(normalized, false, true, "File was already absent during delete", null);
        } catch (IOException e) {
            return new DeletionStepResult(normalized, true, false, "Failed to delete file: " + e.getMessage(), e);
        } catch (SecurityException e) {
            return new DeletionStepResult(normalized, true, false, "Security exception/access denied: " + e.getMessage(), e);
        }
    }

    /**
     * Recursively deletes a directory and all child paths, tracking each step result.
     *
     * @param dirPath Target directory path.
     * @return DirectoryDeletionResult summarizing overall success and per-file step results.
     */
    public static DirectoryDeletionResult deleteDirectoryRecursive(Path dirPath) {
        List<DeletionStepResult> steps = new ArrayList<>();
        if (dirPath == null) {
            steps.add(new DeletionStepResult(null, false, false, "Directory path is null", null));
            return new DirectoryDeletionResult(null, false, steps);
        }

        Path normalized = dirPath.toAbsolutePath().normalize();

        if (!Files.exists(normalized, LinkOption.NOFOLLOW_LINKS)) {
            steps.add(new DeletionStepResult(normalized, false, true, "Directory was already absent", null));
            return new DirectoryDeletionResult(normalized, true, steps);
        }

        if (!Files.isDirectory(normalized, LinkOption.NOFOLLOW_LINKS)) {
            DeletionStepResult step = deleteFile(normalized);
            steps.add(step);
            return new DirectoryDeletionResult(normalized, step.deleted(), steps);
        }

        boolean allChildrenSucceeded = deleteChildren(normalized, steps);

        // Delete the top directory itself if all children were deleted
        if (allChildrenSucceeded) {
            DeletionStepResult topStep = deleteFile(normalized);
            steps.add(topStep);
            return new DirectoryDeletionResult(normalized, topStep.deleted(), steps);
        } else {
            steps.add(new DeletionStepResult(normalized, true, false, "Aborted top directory deletion because child deletion failed", null));
            return new DirectoryDeletionResult(normalized, false, steps);
        }
    }

    private static boolean deleteChildren(Path dirPath, List<DeletionStepResult> steps) {
        boolean success = true;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dirPath)) {
            for (Path child : stream) {
                if (Thread.currentThread().isInterrupted()) {
                    steps.add(new DeletionStepResult(child, true, false, "Thread interrupted during directory traversal", null));
                    return false;
                }
                if (Files.isDirectory(child, LinkOption.NOFOLLOW_LINKS)) {
                    boolean childDirSuccess = deleteChildren(child, steps);
                    if (childDirSuccess) {
                        DeletionStepResult dirStep = deleteFile(child);
                        steps.add(dirStep);
                        if (!dirStep.deleted()) {
                            success = false;
                        }
                    } else {
                        success = false;
                    }
                } else {
                    DeletionStepResult fileStep = deleteFile(child);
                    steps.add(fileStep);
                    if (!fileStep.deleted()) {
                        success = false;
                    }
                }
            }
        } catch (IOException e) {
            steps.add(new DeletionStepResult(dirPath, true, false, "Failed to list directory contents: " + e.getMessage(), e));
            return false;
        }
        return success;
    }
}
