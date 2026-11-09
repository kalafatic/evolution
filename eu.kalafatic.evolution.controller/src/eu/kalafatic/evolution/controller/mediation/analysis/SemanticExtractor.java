package eu.kalafatic.evolution.controller.mediation.analysis;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.kalafatic.evolution.controller.mediation.model.FileDescriptor;
import eu.kalafatic.evolution.controller.mediation.model.SemanticNode;
import eu.kalafatic.evolution.controller.mediation.model.TargetDescriptor;
import eu.kalafatic.evolution.controller.mediation.model.TargetSnapshot;
import eu.kalafatic.utils.semantic.AIContextTool;
import eu.kalafatic.utils.semantic.EvoMetadata;

/**
 * Extracts semantic information from source files to build the Target Reality Model.
 */
public class SemanticExtractor {

    private final AIContextTool contextTool = new AIContextTool();

    public void extract(TargetSnapshot snapshot, File root) {
        for (String path : snapshot.getNodes().keySet()) {
            File file = new File(root, path);
            if (file.exists() && !file.isDirectory()) {
                SemanticNode node = snapshot.getNodes().get(path);
                if (node != null) {
                    analyzeNode(node, root, snapshot);
                }
            }
        }
    }
    
    public void extract(TargetDescriptor target, File root) {
        // Compatibility for TargetDescriptor-based tests
        for (FileDescriptor file : target.getFiles()) {
            analyzeFileDescriptor(file, root);
        }
    }
    
    private void analyzeFileDescriptor(FileDescriptor file, File root) {
        File actualFile = new File(root, file.getPath());
        if (!actualFile.exists() || actualFile.length() > 500000) return;
        try {
            List<String> lines = Files.readAllLines(actualFile.toPath());
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.contains("@")) {
                    if (!file.getTags().contains("Annotated")) file.getTags().add("Annotated");
                }
                if (trimmed.contains("public static void main") || trimmed.contains("setup()") || trimmed.contains("loop()")) {
                    if (!file.getTags().contains("Executory")) file.getTags().add("Executory");
                }
            }
        } catch (IOException e) {}
    }

    public void extractToSnapshot(TargetSnapshot snapshot, List<String> paths) {
        File root = snapshot != null && snapshot.getRootPath() != null && !snapshot.getRootPath().isEmpty()
                ? new File(snapshot.getRootPath())
                : new File(".");
        extractToSnapshot(snapshot, paths, root);
    }

    public void extractToSnapshot(TargetSnapshot snapshot, List<String> paths, File root) {
        File targetRoot = (root != null) ? root :
                (snapshot != null && snapshot.getRootPath() != null && !snapshot.getRootPath().isEmpty()
                        ? new File(snapshot.getRootPath())
                        : new File("."));
        for (String path : paths) {
            SemanticNode node = snapshot.getNodes().get(path);
            if (node != null) {
                analyzeNode(node, targetRoot, snapshot);
            }
        }
    }

    public void validateAndRepairMetadata(File root) {
        scanAndRepairRecursive(root, root);
    }

    private void scanAndRepairRecursive(File current, File root) {
        if (current.isDirectory()) {
            if (current.getName().equals(".git") || current.getName().equals("target")) return;
            File[] children = current.listFiles();
            if (children != null) {
                for (File child : children) scanAndRepairRecursive(child, root);
            }
        } else {
            String name = current.getName();
            if (name.endsWith(".java") || name.endsWith(".ts") || name.endsWith(".py") || name.endsWith(".cpp")) {
                repairFileMetadata(current, root);
            }
        }
    }

    private void repairFileMetadata(File file, File root) {
        EvoMetadata meta = contextTool.loadMetadata(file);
        boolean newlyCreated = false;
        if (meta == null) {
            meta = new EvoMetadata();
            String fullPath = file.getAbsolutePath();
            String rootPath = root.getAbsolutePath();
            if (fullPath.startsWith(rootPath)) {
                meta.setPath(fullPath.substring(rootPath.length()).replaceFirst("^[/\\\\]", ""));
            } else {
                meta.setPath(file.getName());
            }
            newlyCreated = true;
        }

        boolean changed = newlyCreated;

        // Auto-infer missing mandatory fields
        if (meta.getDomain() == null || meta.getDomain().isEmpty() || "unknown".equals(meta.getDomain())) {
            meta.setDomain(inferDomainFromPath(meta.getPath()));
            changed = true;
        }

        if (meta.getArchitecturalLayer() == null || meta.getArchitecturalLayer().isEmpty()) {
            meta.setArchitecturalLayer(inferLayerFromPath(meta.getPath()));
            changed = true;
        }

        if (meta.getSystemCriticality() == null || meta.getSystemCriticality().isEmpty()) {
            meta.setSystemCriticality(inferCriticality(meta.getPath()));
            changed = true;
        }

        // Heuristic concept discovery
        if (meta.getConcepts().isEmpty()) {
            List<String> concepts = discoverConcepts(file);
            if (!concepts.isEmpty()) {
                meta.setConcepts(concepts);
                changed = true;
            }
        }

        if (changed) {
            contextTool.saveMetadata(file, meta);
        }
    }

    private void analyzeNode(SemanticNode node, File root, TargetSnapshot snapshot) {
        File actualFile = new File(root, node.getPath());
        if (!actualFile.exists()) return;

        // Specialized handling for native EVO models, data, and config files
        String name = actualFile.getName().toLowerCase();
        if (name.endsWith(".evo")) {
            analyzeEvoModelNode(node, actualFile, snapshot);
            return;
        } else if (name.endsWith(".evodata") || name.endsWith(".parquet")) {
            analyzeDatasetNode(node, actualFile, snapshot);
            return;
        } else if (name.equals("training-report.json") || name.equals("model.json") || name.equals("config.json")) {
            analyzeModelJsonNode(node, actualFile, snapshot);
            return;
        } else if (name.equals("modelfile") || name.endsWith(".modelfile")) {
            analyzeModelfileNode(node, actualFile, snapshot);
            return;
        }

        if (actualFile.length() > 500000) return;

        try {
            List<String> lines = Files.readAllLines(actualFile.toPath());
            analyzeContentForNode(node, lines, snapshot);

            StringBuilder summary = new StringBuilder();
            int count = 0;
            for (String line : lines) {
                if (!line.trim().isEmpty()) {
                    summary.append(line.trim()).append(" ");
                    if (++count >= 3) break;
                }
            }
            node.setSummary(summary.toString().trim());

            Map<String, String> annotationMeta = extractAnnotationMetadataFromLines(lines);
            EvoMetadata sidecarMeta = contextTool.loadMetadata(actualFile);
            String packageDomain = inferDomainFromPath(node.getPath());
            String markdownContext = loadPackageMarkdownContext(actualFile.getParentFile());

            resolveMetadataForNode(node, annotationMeta, sidecarMeta, markdownContext, packageDomain);

        } catch (IOException e) {}
    }

    private void analyzeEvoModelNode(SemanticNode node, File file, TargetSnapshot snapshot) {
        node.setType("model");
        node.getAttributes().put("domain", "ai_model");
        node.getAttributes().put("role", "native_llm_artifact");
        addTagIfNotExists(node.getTags(), "EVO_MODEL");
        addTagIfNotExists(node.getTags(), "AI_MODEL");

        try {
            eu.kalafatic.evolution.forge.model.llm.EvoModelArtifact artifact =
                    eu.kalafatic.evolution.forge.model.llm.EvoModelArtifact.load(file.toPath());
            node.getAttributes().put("model_name", artifact.getModelName());
            node.getAttributes().put("parameters", String.valueOf(artifact.getParameterCount()));
            node.getAttributes().put("vocab_size", String.valueOf(artifact.getVocabSize()));
            node.getAttributes().put("layers", String.valueOf(artifact.getLayers()));
            node.getAttributes().put("heads", String.valueOf(artifact.getHeads()));
            node.getAttributes().put("embed_dim", String.valueOf(artifact.getEmbeddingSize()));
            node.getAttributes().put("dff", String.valueOf(artifact.getDff()));
            node.getAttributes().put("max_seq_len", String.valueOf(artifact.getMaxSeqLen()));

            String summary = String.format("Native EVO LLM Model '%s': %d layers, %d heads, embed: %d, dff: %d, maxSeq: %d, vocab: %d",
                    artifact.getModelName(), artifact.getLayers(), artifact.getHeads(),
                    artifact.getEmbeddingSize(), artifact.getDff(), artifact.getMaxSeqLen(), artifact.getVocabSize());
            node.setSummary(summary);
            node.getStructures().add("model:" + artifact.getModelName());
        } catch (Exception ex) {
            node.setSummary("Native EVO Model Container (" + file.length() + " bytes)");
        }
    }

    private void analyzeDatasetNode(SemanticNode node, File file, TargetSnapshot snapshot) {
        node.setType("dataset");
        node.getAttributes().put("domain", "training_data");
        node.getAttributes().put("role", "dataset_artifact");
        addTagIfNotExists(node.getTags(), "DATASET");
        node.setSummary("Training dataset archive: " + file.getName() + " (" + (file.length() / 1024) + " KB)");
    }

    private void analyzeModelJsonNode(SemanticNode node, File file, TargetSnapshot snapshot) {
        node.setType("data");
        node.getAttributes().put("domain", "ai_model");
        node.getAttributes().put("role", "model_metadata");
        addTagIfNotExists(node.getTags(), "MODEL_METADATA");

        try {
            String content = Files.readString(file.toPath());
            org.json.JSONObject obj = new org.json.JSONObject(content);
            StringBuilder sb = new StringBuilder();
            sb.append("EVO Model Metadata: ");
            if (obj.has("modelName")) {
                sb.append("Model: ").append(obj.optString("modelName")).append(" | ");
                node.getAttributes().put("model_name", obj.optString("modelName"));
            }
            if (obj.has("parameterCount")) {
                sb.append("Params: ").append(obj.optLong("parameterCount")).append(" | ");
                node.getAttributes().put("parameters", String.valueOf(obj.optLong("parameterCount")));
            }
            if (obj.has("finalLoss")) {
                sb.append("Loss: ").append(String.format(java.util.Locale.US, "%.4f", obj.optDouble("finalLoss"))).append(" | ");
                node.getAttributes().put("final_loss", String.valueOf(obj.optDouble("finalLoss")));
            }
            if (obj.has("vocabSize")) node.getAttributes().put("vocab_size", String.valueOf(obj.optInt("vocabSize")));
            if (obj.has("numBlocks")) node.getAttributes().put("layers", String.valueOf(obj.optInt("numBlocks")));
            if (obj.has("dModel")) node.getAttributes().put("embed_dim", String.valueOf(obj.optInt("dModel")));

            node.setSummary(sb.toString().replaceAll(" \\| $", ""));
        } catch (Exception e) {
            node.setSummary("Model metadata file: " + file.getName());
        }
    }

    private void analyzeModelfileNode(SemanticNode node, File file, TargetSnapshot snapshot) {
        node.setType("model");
        node.getAttributes().put("domain", "ai_model");
        node.getAttributes().put("role", "ollama_modelfile");
        addTagIfNotExists(node.getTags(), "MODELFILE");
        node.setSummary("Ollama Modelfile runtime definition");
    }

    private void resolveMetadataForNode(SemanticNode node, Map<String, String> ann, EvoMetadata side, String md, String pkg) {
        String domain = ann.getOrDefault("domain",
                        (side != null && side.getDomain() != null && !side.getDomain().isEmpty()) ? side.getDomain() :
                        (md != null ? md : pkg));

        String role = ann.getOrDefault("role", (side != null && side.getRole() != null) ? side.getRole() : "unknown");

        if (domain != null) node.getAttributes().put("domain", domain);
        if (role != null) node.getAttributes().put("role", role);

        if (side != null && side.isStale()) {
            node.getTags().add("warning:stale_metadata");
        }
    }

    private String inferDomainFromPath(String path) {
        if (path.contains("/mediation/")) return "mediation";
        if (path.contains("/trajectory/")) return "trajectory";
        if (path.contains("/supervision/")) return "supervision";
        if (path.contains("/execution/")) return "execution";
        if (path.contains("/orchestration/")) return "orchestration";
        if (path.contains("/genome/")) return "genome";
        return "core";
    }

    private String inferLayerFromPath(String path) {
        if (path.contains("/model/")) return "model";
        if (path.contains("/controller/")) return "controller";
        if (path.contains("/view/") || path.contains("/ui/")) return "view";
        if (path.contains("/utils/")) return "utils";
        return "unknown";
    }

    private String inferCriticality(String path) {
        if (path.contains("IterationManager") || path.contains("Orchestrator") || path.contains("DarwinEngine")) return "HIGH";
        return "MEDIUM";
    }

    private List<String> discoverConcepts(File file) {
        List<String> concepts = new ArrayList<>();
        try {
            String content = new String(Files.readAllLines(file.toPath()).stream().limit(100).collect(java.util.stream.Collectors.joining("\n")));
            if (content.contains("Agent")) concepts.add("Agent");
            if (content.contains("Darwin")) concepts.add("Evolution");
            if (content.contains("Genome")) concepts.add("Genome");
            if (content.contains("Mediated")) concepts.add("Mediation");
        } catch (IOException e) {}
        return concepts;
    }

    private String loadPackageMarkdownContext(File directory) {
        File contextFile = new File(directory, "PACKAGE_CONTEXT.md");
        if (contextFile.exists()) {
            try {
                List<String> lines = Files.readAllLines(contextFile.toPath());
                for (String line : lines) {
                    if (line.contains("Domain: ")) {
                         return line.substring(line.indexOf("Domain:") + 7).trim().toLowerCase();
                    }
                }
            } catch (IOException e) {}
        }
        return null;
    }

    private Map<String, String> extractAnnotationMetadataFromLines(List<String> lines) {
        Map<String, String> meta = new HashMap<>();
        boolean inAnnotation = false;
        StringBuilder annotationBlock = new StringBuilder();

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.contains("@EvolutionComponent")) {
                inAnnotation = true;
            }

            if (inAnnotation) {
                annotationBlock.append(line);
                if (trimmed.endsWith(")")) {
                    inAnnotation = false;
                    parseAnnotationBlock(annotationBlock.toString(), meta);
                    break;
                }
            }
        }
        return meta;
    }

    private void parseAnnotationBlock(String block, Map<String, String> meta) {
        if (block.contains("domain")) meta.put("domain", extractAnnotationValue(block, "domain"));
        if (block.contains("role")) meta.put("role", extractAnnotationValue(block, "role"));
    }

    private void analyzeContentForNode(SemanticNode node, List<String> lines, TargetSnapshot snapshot) {
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.contains("@")) {
                addTagIfNotExists(node.getTags(), "Annotated");
            }
            if (trimmed.startsWith("public class ") || trimmed.startsWith("class ")) {
                node.getStructures().add("struct:" + trimmed);
            }
            if (trimmed.startsWith("import ") || trimmed.startsWith("#include ")) {
                String dep = extractDependency(trimmed);
                if (dep != null) {
                    node.getDependencies().add(dep);
                }
            }
        }
    }

    private String extractDependency(String line) {
        if (line.startsWith("import ")) return line.substring(7).replace(";", "").trim();
        if (line.startsWith("#include ")) return line.substring(9).replace("\"", "").replace("<", "").replace(">", "").trim();
        return null;
    }

    private void addTagIfNotExists(List<String> tags, String tag) {
        if (!tags.contains(tag)) tags.add(tag);
    }

    private String extractAnnotationValue(String block, String key) {
        try {
            int start = block.indexOf(key + " = \"") + key.length() + 4;
            if (start < key.length() + 4) {
                 start = block.indexOf(key + "=\"") + key.length() + 2;
            }
            int end = block.indexOf("\"", start);
            if (start > 0 && end > start) {
                return block.substring(start, end);
            }
        } catch (Exception e) {}
        return "unknown";
    }
}
