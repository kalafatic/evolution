package eu.kalafatic.utils.semantic;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Tool for attaching and retrieving AI metadata sidecars.
 */
public class AIContextTool {

    public static final String METADATA_SUFFIX = ".ai.json";

    public EvoMetadata loadMetadata(File artifact) {
        File sidecar = getSidecarFile(artifact);
        if (sidecar.exists()) {
            try {
                String content = new String(Files.readAllBytes(sidecar.toPath()));
                // Simple parsing for now (could use Jackson later if available in utils)
                return parseSimpleJson(content, artifact.getPath());
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
        return meta;
    }

    private String extractValue(String json, String key) {
        int start = json.indexOf("\"" + key + "\":") + key.length() + 3;
        int end = json.indexOf("\"", start + 1);
        if (start > 0 && end > start) {
            return json.substring(start + 1, end);
        }
        return "unknown";
    }

    private String serializeSimpleJson(EvoMetadata metadata) {
        return "{\n" +
               "  \"domain\": \"" + metadata.getDomain() + "\",\n" +
               "  \"purpose\": \"" + metadata.getPurpose() + "\",\n" +
               "  \"role\": \"" + metadata.getRole() + "\",\n" +
               "  \"stability\": \"" + metadata.getStability() + "\"\n" +
               "}";
    }
}
