package eu.kalafatic.evolution.selfdev.genome.mediation;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import eu.kalafatic.evolution.selfdev.genome.core.MediatedPackageArtifact;

public class MediatedPackageProcessor {

    public MediatedPackageArtifact process(File zipFile) {

        Map<String, String> files = new HashMap<>();

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {

            ZipEntry entry;

            while ((entry = zis.getNextEntry()) != null) {

                if (!entry.isDirectory()) {

                    String content = new String(zis.readAllBytes(), StandardCharsets.UTF_8);
                    files.put(entry.getName(), content);
                }
            }

        } catch (IOException e) {
            throw new RuntimeException("Mediated package processing failed", e);
        }

        MediatedPackageArtifact artifact = new MediatedPackageArtifact();

        artifact.setId(UUID.randomUUID().toString());
        artifact.setFiles(files);
        artifact.setTopic(files.getOrDefault("METADATA.yaml", "unknown"));

        return artifact;
    }
}
