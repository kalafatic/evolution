package eu.kalafatic.evolution.controller.mediation.scanner;

import java.io.File;
import java.util.EnumMap;
import java.util.Map;
import eu.kalafatic.evolution.controller.mediation.model.TargetSnapshot.TargetType;

/**
 * Multi-source deterministic & heuristic target classification detector.
 * Eliminates path substring dependencies (e.g. "evolution") and accurately
 * categorizes targets into SOFTWARE, DATASET, KNOWLEDGE_BASE, AI_MODEL, ARCHIVE,
 * CONFIGURATION, HETEROGENEOUS, UNKNOWN, or SELF.
 */
public class TargetClassificationDetector {

    public static class ClassificationResult {
        private final TargetType category;
        private final double confidence;
        private final String summary;

        public ClassificationResult(TargetType category, double confidence, String summary) {
            this.category = category;
            this.confidence = confidence;
            this.summary = summary;
        }

        public TargetType getCategory() { return category; }
        public double getConfidence() { return confidence; }
        public String getSummary() { return summary; }
    }

    public static ClassificationResult detect(File root) {
        if (root == null || !root.exists()) {
            return new ClassificationResult(TargetType.UNKNOWN, 0.0, "Target path is missing or inaccessible.");
        }

        // 1. Check if root is SELF (EVO repository / bundles)
        if (isEvoSelfRepository(root)) {
            return new ClassificationResult(TargetType.SELF, 1.0, "EVO Platform Self-Development Repository");
        }

        Map<TargetType, Double> scores = new EnumMap<>(TargetType.class);
        for (TargetType type : TargetType.values()) {
            scores.put(type, 0.0);
        }

        int fileCount = 0;
        int dirCount = 0;

        File[] files = root.listFiles();
        if (files != null) {
            for (File child : files) {
                if (child.getName().startsWith(".")) continue;
                if (child.isDirectory()) {
                    dirCount++;
                    String dName = child.getName().toLowerCase();
                    if (dName.equals("src") || dName.equals("lib") || dName.equals("app") || dName.equals("modules") || dName.equals("packages")) {
                        scores.put(TargetType.PROJECT, scores.get(TargetType.PROJECT) + 3.0);
                    } else if (dName.contains("doc") || dName.contains("wiki") || dName.contains("guide")) {
                        scores.put(TargetType.KNOWLEDGE_BASE, scores.get(TargetType.KNOWLEDGE_BASE) + 2.0);
                    } else if (dName.contains("data") || dName.contains("dataset") || dName.contains("corpus")) {
                        scores.put(TargetType.DATASET, scores.get(TargetType.DATASET) + 2.0);
                    } else if (dName.contains("model") || dName.contains("weights") || dName.contains("checkpoint")) {
                        scores.put(TargetType.AI_MODEL, scores.get(TargetType.AI_MODEL) + 2.0);
                    }
                } else {
                    fileCount++;
                    String fName = child.getName().toLowerCase();

                    // Software Project Manifests
                    if (fName.equals("pom.xml") || fName.equals("package.json") || fName.equals("build.gradle") ||
                        fName.equals("requirements.txt") || fName.equals("cargo.toml") || fName.equals("go.mod") ||
                        fName.equals("cmakelists.txt") || fName.equals("makefile") || fName.endsWith(".csproj") || fName.endsWith(".sln")) {
                        scores.put(TargetType.PROJECT, scores.get(TargetType.PROJECT) + 5.0);
                    }

                    // AI Model Descriptors
                    if (fName.equals("config.json") || fName.equals("tokenizer.json") || fName.equals("model_index.json") ||
                        fName.endsWith(".gguf") || fName.endsWith(".safetensors") || fName.endsWith(".bin") ||
                        fName.endsWith(".onnx") || fName.endsWith(".pb") || fName.endsWith(".pt")) {
                        scores.put(TargetType.AI_MODEL, scores.get(TargetType.AI_MODEL) + 4.0);
                    }

                    // Dataset Files
                    if (fName.endsWith(".parquet") || fName.endsWith(".jsonl") || fName.endsWith(".evodata") ||
                        fName.equals("dataset_info.json") || fName.equals("manifest.json") || fName.endsWith(".tsv") || fName.endsWith(".arrow")) {
                        scores.put(TargetType.DATASET, scores.get(TargetType.DATASET) + 4.0);
                    }

                    // Documentation Files
                    if (fName.equals("readme.md") || fName.endsWith(".adoc") || fName.endsWith(".rst") || fName.endsWith(".pdf") || fName.endsWith(".docx")) {
                        scores.put(TargetType.KNOWLEDGE_BASE, scores.get(TargetType.KNOWLEDGE_BASE) + 2.0);
                    }

                    // Config Files
                    if (fName.endsWith(".yaml") || fName.endsWith(".yml") || fName.endsWith(".toml") || fName.endsWith(".ini") || fName.endsWith(".env")) {
                        scores.put(TargetType.CONFIGURATION, scores.get(TargetType.CONFIGURATION) + 1.0);
                    }

                    // Archives
                    if (fName.endsWith(".zip") || fName.endsWith(".tar.gz") || fName.endsWith(".7z") || fName.endsWith(".rar")) {
                        scores.put(TargetType.ARCHIVE, scores.get(TargetType.ARCHIVE) + 2.0);
                    }
                }
            }
        }

        // Determine dominant score
        TargetType bestType = TargetType.UNKNOWN;
        double maxScore = 0.0;
        int activeCategoriesCount = 0;

        for (Map.Entry<TargetType, Double> entry : scores.entrySet()) {
            if (entry.getValue() > 0) {
                activeCategoriesCount++;
            }
            if (entry.getValue() > maxScore) {
                maxScore = entry.getValue();
                bestType = entry.getKey();
            }
        }

        if (maxScore == 0.0) {
            if (fileCount == 0 && dirCount == 0) {
                return new ClassificationResult(TargetType.UNKNOWN, 0.0, "Empty Target Directory");
            }
            return new ClassificationResult(TargetType.UNKNOWN, 0.3, "Heterogeneous or unclassified artifact collection.");
        }

        if (activeCategoriesCount >= 3 && maxScore < 8.0) {
            return new ClassificationResult(TargetType.HETEROGENEOUS, 0.75, "Heterogeneous Target Collection (Multiple distinct artifact types discovered).");
        }

        double confidence = Math.min(0.99, 0.5 + (maxScore / 10.0));

        String summary = switch (bestType) {
            case PROJECT -> "Software Source Repository";
            case DATASET -> "Data / Training Corpus Collection";
            case KNOWLEDGE_BASE -> "Documentation / Knowledge Base";
            case AI_MODEL -> "AI Model Artifact / Configuration";
            case ARCHIVE -> "Compressed Archive Collection";
            case CONFIGURATION -> "Environment Configuration Set";
            case HETEROGENEOUS -> "Heterogeneous Artifact Collection";
            case SELF -> "EVO Platform Self Repository";
            default -> "Uncertain Target Entity";
        };

        return new ClassificationResult(bestType, confidence, summary);
    }

    private static boolean isEvoSelfRepository(File root) {
        if (root == null) return false;
        File pom = new File(root, "pom.xml");
        if (pom.exists() && pom.isFile()) {
            try {
                String content = java.nio.file.Files.readString(pom.toPath());
                if (content.contains("eu.kalafatic.evolution.aggregator") || content.contains("eu.kalafatic.evolution")) {
                    return true;
                }
            } catch (Exception ignored) {}
        }
        File viewModule = new File(root, "eu.kalafatic.evolution.view");
        File controllerModule = new File(root, "eu.kalafatic.evolution.controller");
        return viewModule.exists() && controllerModule.exists();
    }
}
