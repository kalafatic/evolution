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
        if (json.contains("\"mediatedRelevanceScore\":")) {
            meta.setMediatedRelevanceScore(extractDoubleValue(json, "mediatedRelevanceScore"));
        }
        if (json.contains("\"importanceScore\":")) {
            meta.setImportanceScore(extractDoubleValue(json, "importanceScore"));
        }
        if (json.contains("\"summary\":")) {
            meta.setSummary(extractValue(json, "summary"));
        }
        if (json.contains("\"evolutionaryNotes\":")) {
            meta.setEvolutionaryNotes(extractValue(json, "evolutionaryNotes"));
        }
        if (json.contains("\"contextSelectionHints\":")) {
            meta.setContextSelectionHints(extractListValue(json, "contextSelectionHints"));
        }
        if (json.contains("\"dependencyLinks\":")) {
            meta.setDependencyLinks(extractListValue(json, "dependencyLinks"));
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

    private double extractDoubleValue(String json, String key) {
        try {
            String val = extractRawValue(json, key);
            if (val != null) return Double.parseDouble(val.trim());
        } catch (Exception e) {}
        return 0.0;
    }

    private List<String> extractListValue(String json, String key) {
        List<String> list = new ArrayList<>();
        try {
            int start = json.indexOf("\"" + key + "\":") + key.length() + 3;
            int listStart = json.indexOf("[", start);
            int listEnd = json.indexOf("]", listStart);
            if (listStart >= 0 && listEnd > listStart) {
                String rawList = json.substring(listStart + 1, listEnd);
                String[] items = rawList.split(",");
                for (String item : items) {
                    item = item.trim();
                    if (item.startsWith("\"") && item.endsWith("\"")) {
                        list.add(item.substring(1, item.length() - 1));
                    } else if (!item.isEmpty()) {
                        list.add(item);
                    }
                }
            }
        } catch (Exception e) {}
        return list;
    }

    private String extractRawValue(String json, String key) {
        try {
            int start = json.indexOf("\"" + key + "\":") + key.length() + 3;
            int endComma = json.indexOf(",", start);
            int endBrace = json.indexOf("}", start);
            int end = (endComma != -1 && endBrace != -1) ? Math.min(endComma, endBrace) : (endComma != -1 ? endComma : endBrace);
            if (start > 0 && end > start) {
                return json.substring(start, end).trim();
            }
        } catch (Exception e) {}
        return null;
    }

    private String serializeSimpleJson(EvoMetadata metadata) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"domain\": \"").append(metadata.getDomain() != null ? metadata.getDomain() : "").append("\",\n");
        sb.append("  \"purpose\": \"").append(metadata.getPurpose() != null ? metadata.getPurpose() : "").append("\",\n");
        sb.append("  \"role\": \"").append(metadata.getRole() != null ? metadata.getRole() : "").append("\",\n");
        sb.append("  \"stability\": \"").append(metadata.getStability() != null ? metadata.getStability() : "").append("\",\n");
        sb.append("  \"mediatedRelevanceScore\": ").append(metadata.getMediatedRelevanceScore()).append(",\n");
        sb.append("  \"importanceScore\": ").append(metadata.getImportanceScore()).append(",\n");
        sb.append("  \"summary\": \"").append(metadata.getSummary() != null ? metadata.getSummary().replace("\"", "\\\"") : "").append("\",\n");
        sb.append("  \"evolutionaryNotes\": \"").append(metadata.getEvolutionaryNotes() != null ? metadata.getEvolutionaryNotes().replace("\"", "\\\"") : "").append("\",\n");
        sb.append("  \"contextSelectionHints\": ").append(serializeList(metadata.getContextSelectionHints())).append(",\n");
        sb.append("  \"dependencyLinks\": ").append(serializeList(metadata.getDependencyLinks())).append("\n");
        sb.append("}");
        return sb.toString();
    }

    private String serializeList(List<String> list) {
        if (list == null || list.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            sb.append("\"").append(list.get(i)).append("\"");
            if (i < list.size() - 1) sb.append(", ");
        }
        sb.append("]");
        return sb.toString();
    }
}
