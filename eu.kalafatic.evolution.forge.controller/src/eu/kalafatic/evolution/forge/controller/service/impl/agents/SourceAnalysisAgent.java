package eu.kalafatic.evolution.forge.controller.service.impl.agents;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import eu.kalafatic.evolution.forge.data.api.NormalizedSample;
import eu.kalafatic.evolution.forge.data.api.artifact.EvoDatasetArtifact;

public class SourceAnalysisAgent {
    private final Set<String> processedHashes = new HashSet<>();

    public List<KnowledgeUnit> analyze(List<Path> filePaths, Path rootPath) {
        List<KnowledgeUnit> results = new ArrayList<>();
        for (Path file : filePaths) {
            try {
                if (!Files.exists(file) || Files.isDirectory(file)) {
                    continue;
                }
                String fileType = identifyFileType(file);
                String content;
                if ("EVODATA".equals(fileType)) {
                    content = readEvodataContent(file);
                } else {
                    content = Files.readString(file, StandardCharsets.UTF_8);
                }

                if (content == null || content.trim().isEmpty()) {
                    continue;
                }

                String hash = computeSha256(content);
                // Detect duplicate content
                if (processedHashes.contains(hash)) {
                    System.out.println("[SourceAnalysisAgent] Skipping duplicate file content: " + file.getFileName());
                    continue;
                }
                processedHashes.add(hash);

                String relativePath;
                try {
                    relativePath = (rootPath != null && file.startsWith(rootPath))
                            ? rootPath.relativize(file).toString().replace("\\", "/")
                            : file.getFileName().toString();
                } catch (Exception e) {
                    relativePath = file.getFileName().toString();
                }

                KnowledgeUnit unit = new KnowledgeUnit(relativePath, fileType, content, hash);
                unit.getMetadata().put("fileName", file.getFileName().toString());
                unit.getMetadata().put("size", Files.size(file));
                unit.getMetadata().put("lastModified", Files.getLastModifiedTime(file).toMillis());

                results.add(unit);
            } catch (IOException e) {
                System.err.println("[SourceAnalysisAgent] Error reading file: " + file + " - " + e.getMessage());
            } catch (Exception e) {
                System.err.println("[SourceAnalysisAgent] Error processing file: " + file + " - " + e.getMessage());
            }
        }
        return results;
    }

    private String readEvodataContent(Path file) {
        try {
            EvoDatasetArtifact artifact = EvoDatasetArtifact.load(file.toFile());
            StringBuilder sb = new StringBuilder();
            if (artifact.getTrainSamples() != null) {
                for (NormalizedSample sample : artifact.getTrainSamples()) {
                    String fullText = sample.toFullText();
                    if (fullText != null && !fullText.trim().isEmpty()) {
                        sb.append(fullText).append("\n\n");
                    }
                }
            }
            if (artifact.getValSamples() != null) {
                for (NormalizedSample sample : artifact.getValSamples()) {
                    String fullText = sample.toFullText();
                    if (fullText != null && !fullText.trim().isEmpty()) {
                        sb.append(fullText).append("\n\n");
                    }
                }
            }
            return sb.toString();
        } catch (Exception e) {
            System.err.println("[SourceAnalysisAgent] Error loading .evodata artifact from " + file + ": " + e.getMessage());
            return "";
        }
    }

    public String identifyFileType(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        if (name.endsWith(".evodata")) {
            return "EVODATA";
        } else if (name.endsWith(".md")) {
            return "MARKDOWN";
        } else if (name.endsWith(".java")) {
            return "JAVA";
        } else if (name.endsWith(".xml")) {
            if (name.equals("pom.xml")) {
                return "POM";
            }
            return "XML";
        } else if (name.endsWith(".json")) {
            return "JSON";
        } else if (name.endsWith(".properties")) {
            return "PROPERTIES";
        } else if (name.equals("manifest.mf")) {
            return "OSGI_MANIFEST";
        } else if (name.endsWith(".html") || name.endsWith(".htm")) {
            return "WEB";
        }
        return "UNKNOWN";
    }

    private String computeSha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            return String.valueOf(text.hashCode());
        }
    }
}
