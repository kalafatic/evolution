package eu.kalafatic.evolution.view.util;

import java.io.*;
import java.nio.file.*;
import java.util.zip.*;

/**
 * Utility for ZIP operations used by the Mediated Editor.
 */
public class ZipUtil {

    public static void unpack(File zipFile, File destDir) throws IOException {
        if (!destDir.exists()) destDir.mkdirs();
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry = zis.getNextEntry();
            while (entry != null) {
                File newFile = newFile(destDir, entry);
                if (entry.isDirectory()) {
                    if (!newFile.isDirectory() && !newFile.mkdirs()) {
                        throw new IOException("Failed to create directory " + newFile);
                    }
                } else {
                    File parent = newFile.getParentFile();
                    if (!parent.isDirectory() && !parent.mkdirs()) {
                        throw new IOException("Failed to create directory " + parent);
                    }
                    try (FileOutputStream fos = new FileOutputStream(newFile)) {
                        byte[] buffer = new byte[4096];
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }
                zis.closeEntry();
                entry = zis.getNextEntry();
            }
        }
    }

    private static File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
        File destFile = new File(destinationDir, zipEntry.getName());
        String destDirPath = destinationDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();
        if (!destFilePath.startsWith(destDirPath + File.separator) && !destFilePath.equals(destDirPath)) {
            throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
        }
        return destFile;
    }

    public static void pack(File sourceDir, File zipFile) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
            packDirectory(sourceDir, sourceDir, zos);
        }
    }

    private static void packDirectory(File root, File source, ZipOutputStream zos) throws IOException {
        File[] files = source.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (file.isDirectory()) {
                packDirectory(root, file, zos);
            } else {
                String name = root.toPath().relativize(file.toPath()).toString().replace("\\", "/");
                ZipEntry zipEntry = new ZipEntry(name);
                zos.putNextEntry(zipEntry);
                Files.copy(file.toPath(), zos);
                zos.closeEntry();
            }
        }
    }
}
