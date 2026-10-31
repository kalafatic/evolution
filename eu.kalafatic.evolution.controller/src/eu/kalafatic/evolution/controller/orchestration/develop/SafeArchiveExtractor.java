package eu.kalafatic.evolution.controller.orchestration.develop;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Utility for safe extraction of project ZIP archives with Zip Slip protection,
 * file size caps, and extraction count limits.
 */
public class SafeArchiveExtractor {

    public static final long MAX_EXTRACTED_BYTES = 250L * 1024 * 1024; // 250 MB
    public static final int MAX_ENTRIES = 10000;
    public static final long MAX_SINGLE_FILE_BYTES = 50L * 1024 * 1024; // 50 MB

    public static int extractArchive(byte[] archiveBytes, File destinationDir) throws IOException {
        if (archiveBytes == null || archiveBytes.length == 0) {
            throw new IllegalArgumentException("Archive payload cannot be null or empty");
        }
        try (InputStream in = new ByteArrayInputStream(archiveBytes)) {
            return extractArchive(in, destinationDir);
        }
    }

    public static int extractArchive(InputStream archiveStream, File destinationDir) throws IOException {
        if (destinationDir == null) {
            throw new IllegalArgumentException("Destination directory cannot be null");
        }
        if (!destinationDir.exists()) {
            destinationDir.mkdirs();
        }

        String canonicalDestDirPath = destinationDir.getCanonicalPath();
        int entriesCount = 0;
        long totalBytesExtracted = 0;

        try (ZipInputStream zis = new ZipInputStream(archiveStream)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                entriesCount++;
                if (entriesCount > MAX_ENTRIES) {
                    throw new SecurityException("Archive exceeds maximum allowed entries limit (" + MAX_ENTRIES + ")");
                }

                String entryName = entry.getName();
                File targetFile = new File(destinationDir, entryName);
                String canonicalTargetPath = targetFile.getCanonicalPath();

                // Zip Slip protection
                if (!canonicalTargetPath.startsWith(canonicalDestDirPath + File.separator) &&
                    !canonicalTargetPath.equals(canonicalDestDirPath)) {
                    throw new SecurityException("Zip Slip vulnerability detected: entry '" + entryName + "' attempts to escape destination directory");
                }

                if (entry.isDirectory()) {
                    if (!targetFile.exists()) {
                        targetFile.mkdirs();
                    }
                } else {
                    File parent = targetFile.getParentFile();
                    if (parent != null && !parent.exists()) {
                        parent.mkdirs();
                    }

                    long entryBytes = 0;
                    byte[] buffer = new byte[8192];
                    try (FileOutputStream fos = new FileOutputStream(targetFile)) {
                        int read;
                        while ((read = zis.read(buffer)) != -1) {
                            entryBytes += read;
                            totalBytesExtracted += read;

                            if (entryBytes > MAX_SINGLE_FILE_BYTES) {
                                throw new SecurityException("Archive entry '" + entryName + "' exceeds single file limit (" + MAX_SINGLE_FILE_BYTES + " bytes)");
                            }
                            if (totalBytesExtracted > MAX_EXTRACTED_BYTES) {
                                throw new SecurityException("Archive exceeds cumulative extraction size limit (" + MAX_EXTRACTED_BYTES + " bytes)");
                            }

                            fos.write(buffer, 0, read);
                        }
                    }
                }
                zis.closeEntry();
            }
        }

        return entriesCount;
    }
}
