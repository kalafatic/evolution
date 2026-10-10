package eu.kalafatic.utils.semantic;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Tool for attaching and retrieving AI metadata sidecars.
 * Includes Semantic Freshness Validation and Functionality Intelligence support.
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

    public FunctionalityMetadata loadFunctionalityMetadata(File artifact) {
        File sidecar = getSidecarFile(artifact);
        if (sidecar.exists()) {
            try {
                String content = new String(Files.readAllBytes(sidecar.toPath()));
                FunctionalityMetadata meta = parseFunctionalityJson(content, artifact.getPath());

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
            String json = metadata instanceof FunctionalityMetadata ?
                    serializeFunctionalityJson((FunctionalityMetadata) metadata) :
                    serializeSimpleJson(metadata);
            Files.write(sidecar.toPath(), json.getBytes());
        } catch (IOException e) {
            // Log error
        }
    }

    public void saveFunctionalityMetadata(File artifact, FunctionalityMetadata metadata) {
        File sidecar = getSidecarFile(artifact);
        try {
            String json = serializeFunctionalityJson(metadata);
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
        populateBaseFields(json, path, meta);
        return meta;
    }

    private FunctionalityMetadata parseFunctionalityJson(String json, String path) {
        FunctionalityMetadata meta = new FunctionalityMetadata();
        populateBaseFields(json, path, meta);

        meta.setFunctionalityId(extractValue(json, "functionalityId"));
        meta.setFunctionalityName(extractValue(json, "functionalityName"));
        meta.setPrimaryClass(extractValue(json, "primaryClass"));
        meta.setFqcn(extractValue(json, "fqcn"));
        meta.setModuleName(extractValue(json, "moduleName"));
        meta.setSourcePath(extractValue(json, "sourcePath"));
        meta.setImplementationStatus(extractValue(json, "implementationStatus"));

        meta.setImportanceScore0To100(extractIntValue(json, "importanceScore0To100", -1));
        meta.setUsageEstimate0To100(extractIntValue(json, "usageEstimate0To100", -1));
        meta.setComplexityScore0To100(extractIntValue(json, "complexityScore0To100", -1));
        meta.setCentralityScore0To100(extractIntValue(json, "centralityScore0To100", -1));
        meta.setMaturityScore0To100(extractIntValue(json, "maturityScore0To100", -1));
        meta.setRiskScore0To100(extractIntValue(json, "riskScore0To100", -1));

        meta.setEvaluationRationale(extractValue(json, "evaluationRationale"));
        meta.setEvaluationMethod(extractValue(json, "evaluationMethod"));
        meta.setConfidenceLevel(extractDoubleValue(json, "confidenceLevel"));
        meta.setEvaluationTimestamp(extractValue(json, "evaluationTimestamp"));
        meta.setSourceRevision(extractValue(json, "sourceRevision"));
        meta.setRelatedFunctionalities(extractListValue(json, "relatedFunctionalities"));

        return meta;
    }

    private void populateBaseFields(String json, String path, EvoMetadata meta) {
        meta.setPath(path);
        meta.setDomain(extractValue(json, "domain"));
        meta.setPurpose(extractValue(json, "purpose"));
        meta.setRole(extractValue(json, "role"));
        meta.setStability(extractValue(json, "stability"));
        meta.setMediatedRelevanceScore(extractDoubleValue(json, "mediatedRelevanceScore"));
        meta.setImportanceScore(extractDoubleValue(json, "importanceScore"));
        meta.setSummary(extractValue(json, "summary"));
        meta.setEvolutionaryNotes(extractValue(json, "evolutionaryNotes"));
        meta.setContextSelectionHints(extractListValue(json, "contextSelectionHints"));
        meta.setDependencyLinks(extractListValue(json, "dependencyLinks"));

        // Enhanced Genome Fields
        meta.setArchitecturalLayer(extractValue(json, "architecturalLayer"));
        meta.setSystemCriticality(extractValue(json, "systemCriticality"));
        meta.setMutationRisk(extractValue(json, "mutationRisk"));
        meta.setEvolutionPriority(extractValue(json, "evolutionPriority"));
        meta.setParticipatesInCoreLoop(extractBooleanValue(json, "participatesInCoreLoop"));
        meta.setCoreLoopRole(extractValue(json, "coreLoopRole"));
        meta.setConcepts(extractListValue(json, "concepts"));
        meta.setCapabilities(extractListValue(json, "capabilities"));
    }

    private String extractValue(String json, String key) {
        String val = extractRawValue(json, key);
        return val != null ? val : "";
    }

    private int extractIntValue(String json, String key, int defaultVal) {
        try {
            String val = extractRawValue(json, key);
            if (val != null) return Integer.parseInt(val.trim());
        } catch (Exception e) {}
        return defaultVal;
    }

    private double extractDoubleValue(String json, String key) {
        try {
            String val = extractRawValue(json, key);
            if (val != null) return Double.parseDouble(val.trim());
        } catch (Exception e) {}
        return 0.0;
    }

    private boolean extractBooleanValue(String json, String key) {
        try {
            String val = extractRawValue(json, key);
            if (val != null) return Boolean.parseBoolean(val.trim());
        } catch (Exception e) {}
        return false;
    }

    private List<String> extractListValue(String json, String key) {
        List<String> list = new ArrayList<>();
        try {
            int keyIndex = json.indexOf("\"" + key + "\":");
            if (keyIndex == -1) return list;

            int start = keyIndex + key.length() + 3;
            int listStart = json.indexOf("[", start);
            int listEnd = json.indexOf("]", listStart);
            if (listStart >= 0 && listEnd > listStart) {
                String rawList = json.substring(listStart + 1, listEnd);
                if (rawList.trim().isEmpty()) return list;
                String[] items = rawList.split(",");
                for (String item : items) {
                    item = item.trim();
                    if (item.startsWith("\"") && item.endsWith("\"")) {
                        list.add(item.substring(1, item.length() - 1).replace("\\\"", "\""));
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
            int keyIndex = json.indexOf("\"" + key + "\":");
            if (keyIndex == -1) return null;

            int start = keyIndex + key.length() + 3;
            String remaining = json.substring(start).trim();

            if (remaining.startsWith("\"")) {
                int end = -1;
                for (int i = 1; i < remaining.length(); i++) {
                    if (remaining.charAt(i) == '\"' && remaining.charAt(i-1) != '\\') {
                        end = i;
                        break;
                    }
                }
                if (end != -1) return remaining.substring(1, end).replace("\\\"", "\"");
            } else {
                int endComma = remaining.indexOf(",");
                int endBrace = remaining.indexOf("}");
                int end = (endComma != -1 && endBrace != -1) ? Math.min(endComma, endBrace) : (endComma != -1 ? endComma : endBrace);
                if (end != -1) return remaining.substring(0, end).trim();
                return remaining.trim();
            }
        } catch (Exception e) {}
        return null;
    }

    private String serializeSimpleJson(EvoMetadata metadata) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"domain\": \"").append(esc(metadata.getDomain())).append("\",\n");
        sb.append("  \"purpose\": \"").append(esc(metadata.getPurpose())).append("\",\n");
        sb.append("  \"role\": \"").append(esc(metadata.getRole())).append("\",\n");
        sb.append("  \"stability\": \"").append(esc(metadata.getStability())).append("\",\n");
        sb.append("  \"mediatedRelevanceScore\": ").append(metadata.getMediatedRelevanceScore()).append(",\n");
        sb.append("  \"importanceScore\": ").append(metadata.getImportanceScore()).append(",\n");
        sb.append("  \"architecturalLayer\": \"").append(esc(metadata.getArchitecturalLayer())).append("\",\n");
        sb.append("  \"systemCriticality\": \"").append(esc(metadata.getSystemCriticality())).append("\",\n");
        sb.append("  \"mutationRisk\": \"").append(esc(metadata.getMutationRisk())).append("\",\n");
        sb.append("  \"evolutionPriority\": \"").append(esc(metadata.getEvolutionPriority())).append("\",\n");
        sb.append("  \"participatesInCoreLoop\": ").append(metadata.isParticipatesInCoreLoop()).append(",\n");
        sb.append("  \"coreLoopRole\": \"").append(esc(metadata.getCoreLoopRole())).append("\",\n");
        sb.append("  \"concepts\": ").append(serializeList(metadata.getConcepts())).append(",\n");
        sb.append("  \"capabilities\": ").append(serializeList(metadata.getCapabilities())).append(",\n");
        sb.append("  \"summary\": \"").append(esc(metadata.getSummary())).append("\",\n");
        sb.append("  \"evolutionaryNotes\": \"").append(esc(metadata.getEvolutionaryNotes())).append("\",\n");
        sb.append("  \"contextSelectionHints\": ").append(serializeList(metadata.getContextSelectionHints())).append(",\n");
        sb.append("  \"dependencyLinks\": ").append(serializeList(metadata.getDependencyLinks())).append("\n");
        sb.append("}");
        return sb.toString();
    }

    private String serializeFunctionalityJson(FunctionalityMetadata metadata) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"domain\": \"").append(esc(metadata.getDomain())).append("\",\n");
        sb.append("  \"purpose\": \"").append(esc(metadata.getPurpose())).append("\",\n");
        sb.append("  \"role\": \"").append(esc(metadata.getRole())).append("\",\n");
        sb.append("  \"stability\": \"").append(esc(metadata.getStability())).append("\",\n");
        sb.append("  \"mediatedRelevanceScore\": ").append(metadata.getMediatedRelevanceScore()).append(",\n");
        sb.append("  \"importanceScore\": ").append(metadata.getImportanceScore()).append(",\n");

        sb.append("  \"functionalityId\": \"").append(esc(metadata.getFunctionalityId())).append("\",\n");
        sb.append("  \"functionalityName\": \"").append(esc(metadata.getFunctionalityName())).append("\",\n");
        sb.append("  \"primaryClass\": \"").append(esc(metadata.getPrimaryClass())).append("\",\n");
        sb.append("  \"fqcn\": \"").append(esc(metadata.getFqcn())).append("\",\n");
        sb.append("  \"moduleName\": \"").append(esc(metadata.getModuleName())).append("\",\n");
        sb.append("  \"sourcePath\": \"").append(esc(metadata.getSourcePath())).append("\",\n");
        sb.append("  \"implementationStatus\": \"").append(esc(metadata.getImplementationStatus())).append("\",\n");

        sb.append("  \"importanceScore0To100\": ").append(metadata.getImportanceScore0To100()).append(",\n");
        sb.append("  \"usageEstimate0To100\": ").append(metadata.getUsageEstimate0To100()).append(",\n");
        sb.append("  \"complexityScore0To100\": ").append(metadata.getComplexityScore0To100()).append(",\n");
        sb.append("  \"centralityScore0To100\": ").append(metadata.getCentralityScore0To100()).append(",\n");
        sb.append("  \"maturityScore0To100\": ").append(metadata.getMaturityScore0To100()).append(",\n");
        sb.append("  \"riskScore0To100\": ").append(metadata.getRiskScore0To100()).append(",\n");

        sb.append("  \"evaluationRationale\": \"").append(esc(metadata.getEvaluationRationale())).append("\",\n");
        sb.append("  \"evaluationMethod\": \"").append(esc(metadata.getEvaluationMethod())).append("\",\n");
        sb.append("  \"confidenceLevel\": ").append(metadata.getConfidenceLevel()).append(",\n");
        sb.append("  \"evaluationTimestamp\": \"").append(esc(metadata.getEvaluationTimestamp())).append("\",\n");
        sb.append("  \"sourceRevision\": \"").append(esc(metadata.getSourceRevision())).append("\",\n");
        sb.append("  \"relatedFunctionalities\": ").append(serializeList(metadata.getRelatedFunctionalities())).append(",\n");

        sb.append("  \"summary\": \"").append(esc(metadata.getSummary())).append("\",\n");
        sb.append("  \"evolutionaryNotes\": \"").append(esc(metadata.getEvolutionaryNotes())).append("\",\n");
        sb.append("  \"contextSelectionHints\": ").append(serializeList(metadata.getContextSelectionHints())).append(",\n");
        sb.append("  \"dependencyLinks\": ").append(serializeList(metadata.getDependencyLinks())).append("\n");
        sb.append("}");
        return sb.toString();
    }

    private String esc(String val) {
        if (val == null) return "";
        return val.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String serializeList(List<String> list) {
        if (list == null || list.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            sb.append("\"").append(esc(list.get(i))).append("\"");
            if (i < list.size() - 1) sb.append(", ");
        }
        sb.append("]");
        return sb.toString();
    }
}
