package eu.kalafatic.utils.semantic;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Tool for attaching and retrieving AI metadata sidecars.
 * Includes Semantic Freshness Validation.
 */
public class AIContextTool {

    public static final String METADATA_SUFFIX = ".ai.json";

    public EvoMetadata loadMetadata(File artifact) {
        File sidecar = getSidecarFile(artifact);
        if (sidecar.exists()) {
            try {
                String content = new String(Files.readAllBytes(sidecar.toPath()));
                EvoMetadata meta = parseSimpleJson(content, artifact.getPath());

                // Freshness Validation
                if (sidecar.lastModified() < artifact.lastModified()) {
                    meta.setStale(true);
                }

                return meta;
            } catch (IOException e) {
                // Log error
            }
        }
        return null;
    }

    public void saveMetadata(File artifact, EvoMetadata metadata) {
        File sidecar = getSidecarFile(artifact);
        try {
            String json = serializeSimpleJson(metadata);
            Files.write(sidecar.toPath(), json.getBytes());
        } catch (IOException e) {
            // Log error
        }
    }

    public boolean isMetadataStale(File artifact) {
        File sidecar = getSidecarFile(artifact);
        if (!sidecar.exists()) return true;
        return sidecar.lastModified() < artifact.lastModified();
    }

    private File getSidecarFile(File artifact) {
        return new File(artifact.getParentFile(), artifact.getName() + METADATA_SUFFIX);
    }

    private EvoMetadata parseSimpleJson(String json, String path) {
        EvoMetadata meta = new EvoMetadata();
        meta.setPath(path);
        // Extremely basic extraction for demonstration
        if (json.contains("\"domain\":")) {
            meta.setDomain(extractValue(json, "domain"));
        }
        if (json.contains("\"purpose\":")) {
            meta.setPurpose(extractValue(json, "purpose"));
        }
        if (json.contains("\"role\":")) {
            meta.setRole(extractValue(json, "role"));
        }
        if (json.contains("\"stability\":")) {
            meta.setStability(extractValue(json, "stability"));
        }
        return meta;
    }

    private String extractValue(String json, String key) {
        try {
            int start = json.indexOf("\"" + key + "\":") + key.length() + 3;
            int end = json.indexOf("\"", start + 1);
            if (start > 0 && end > start) {
                return json.substring(start + 1, end);
            }
        } catch (Exception e) {}
        return "unknown";
    }

    private String serializeSimpleJson(EvoMetadata metadata) {
        return "{\n" +
               "  \"domain\": \"" + (metadata.getDomain() != null ? metadata.getDomain() : "") + "\",\n" +
               "  \"purpose\": \"" + (metadata.getPurpose() != null ? metadata.getPurpose() : "") + "\",\n" +
               "  \"role\": \"" + (metadata.getRole() != null ? metadata.getRole() : "") + "\",\n" +
               "  \"stability\": \"" + (metadata.getStability() != null ? metadata.getStability() : "") + "\"\n" +
               "}";
    }
}
