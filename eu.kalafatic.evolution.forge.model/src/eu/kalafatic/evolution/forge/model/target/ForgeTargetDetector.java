package eu.kalafatic.evolution.forge.model.target;

import eu.kalafatic.evolution.forge.model.llm.EvoModelArtifact;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ForgeTargetDetector {

    public static ForgeTarget detect(String pathStr) {
        if (pathStr == null || pathStr.trim().isEmpty()) {
            return new ForgeTarget("", ForgeTargetType.TRAINING_DATA, false, "Target path is empty");
        }

        Path path = Paths.get(pathStr.trim());
        if (!Files.exists(path)) {
            return new ForgeTarget(pathStr, ForgeTargetType.TRAINING_DATA, false, "Path does not exist: " + pathStr);
        }

        String name = path.getFileName() != null ? path.getFileName().toString() : "";

        // 1. Check if path is a .evo model file or ends with .evo
        if (!Files.isDirectory(path) && (name.toLowerCase().endsWith(".evo") || isEvoFile(path))) {
            return inspectEvoModelFile(pathStr, path);
        }

        // 2. Check if directory is a recognized EVO Forge workspace
        if (Files.isDirectory(path)) {
            if (isEvoWorkspace(path)) {
                return inspectEvoWorkspace(pathStr, path);
            } else {
                // Normal training data directory
                ForgeTarget target = new ForgeTarget(pathStr, ForgeTargetType.TRAINING_DATA, true, "Valid training data directory");
                target.setDisplayName(name.isEmpty() ? pathStr : name);
                return target;
            }
        }

        // 3. Otherwise normal training data file (.txt, .pdf, .md, .json, .csv, etc.)
        ForgeTarget target = new ForgeTarget(pathStr, ForgeTargetType.TRAINING_DATA, true, "Valid training data file");
        target.setDisplayName(name);
        return target;
    }

    private static boolean isEvoFile(Path path) {
        if (path.toString().toLowerCase().endsWith(".evo")) {
            return true;
        }
        try {
            byte[] magic = new byte[8];
            try (java.io.InputStream is = Files.newInputStream(path)) {
                int read = is.read(magic);
                if (read >= 8) {
                    String s = new String(magic, java.nio.charset.StandardCharsets.UTF_8);
                    return s.startsWith("EVO_NAT2") || s.startsWith("PK\003\004");
                }
            }
        } catch (Exception ignored) {}
        return false;
    }

    private static boolean isEvoWorkspace(Path dir) {
        Path configPath = dir.resolve("config.json");
        Path modelPath = dir.resolve("model.json");
        Path weightsPath = dir.resolve("weights.bin");
        Path sessionPath = dir.resolve("session_info.json");
        Path reportPath = dir.resolve("training-report.json");
        Path modelfile = dir.resolve("Modelfile");
        Path exportsOllama = dir.resolve("exports/ollama");
        Path exportsModelfile = dir.resolve("exports/ollama/Modelfile");
        Path exportsGguf = dir.resolve("exports/ollama/evo.gguf");
        Path rootGguf = dir.resolve("evo.gguf");

        boolean hasConfig = Files.exists(configPath) || Files.exists(modelPath);
        boolean hasWeights = Files.exists(weightsPath);
        boolean hasModelfile = Files.exists(modelfile) || Files.exists(exportsModelfile);
        boolean hasGguf = Files.exists(rootGguf) || Files.exists(exportsGguf);
        boolean hasExports = Files.isDirectory(exportsOllama);
        boolean hasEvoInDir = false;

        try (java.util.stream.Stream<Path> stream = Files.list(dir)) {
            hasEvoInDir = stream.anyMatch(p -> p.getFileName().toString().toLowerCase().endsWith(".evo"));
        } catch (Exception ignored) {}

        return (hasConfig && (hasWeights || hasEvoInDir || hasModelfile || hasGguf))
                || Files.exists(sessionPath)
                || Files.exists(reportPath)
                || hasModelfile
                || (hasExports && hasGguf);
    }

    private static ForgeTarget inspectEvoModelFile(String pathStr, Path path) {
        try {
            EvoModelArtifact artifact = EvoModelArtifact.load(path);
            ForgeTarget target = new ForgeTarget(pathStr, ForgeTargetType.EVO_MODEL, true, "Valid EVO model");
            target.setArtifact(artifact);
            target.setModelName(artifact.getModelName());
            target.setParameterCount(artifact.getParameterCount());
            target.setVocabSize(artifact.getVocabSize());
            String archSummary = String.format("%d layers, %d heads, embed: %d, dff: %d, maxSeq: %d",
                    artifact.getLayers(), artifact.getHeads(), artifact.getEmbeddingSize(), artifact.getDff(), artifact.getMaxSeqLen());
            target.setArchitectureSummary(archSummary);
            target.setStatusMessage(String.format("Valid EVO model (%s params, Vocab: %d)",
                    formatParamCount(artifact.getParameterCount()), artifact.getVocabSize()));
            target.setDisplayName(path.getFileName().toString());
            return target;
        } catch (Exception e) {
            ForgeTarget target = new ForgeTarget(pathStr, ForgeTargetType.EVO_MODEL, false, "Invalid EVO model file: " + e.getMessage());
            target.setDisplayName(path.getFileName().toString());
            return target;
        }
    }

    private static ForgeTarget inspectEvoWorkspace(String pathStr, Path dir) {
        try {
            EvoModelArtifact artifact = null;
            // 1. Try loading directly via EvoModelArtifact.load(dir) (requires weights.bin)
            try {
                artifact = EvoModelArtifact.load(dir);
            } catch (Exception ex) {
                // Check if directory contains a standalone .evo artifact
                try (java.util.stream.Stream<Path> stream = Files.list(dir)) {
                    java.util.Optional<Path> evoFileOpt = stream
                            .filter(p -> p.getFileName().toString().toLowerCase().endsWith(".evo"))
                            .findFirst();
                    if (evoFileOpt.isPresent()) {
                        artifact = EvoModelArtifact.load(evoFileOpt.get());
                    }
                } catch (Exception ignored) {}
            }

            if (artifact != null) {
                ForgeTarget target = new ForgeTarget(pathStr, ForgeTargetType.EVO_WORKSPACE, true, "Valid EVO Forge workspace");
                target.setArtifact(artifact);
                target.setModelName(artifact.getModelName());
                target.setParameterCount(artifact.getParameterCount());
                target.setVocabSize(artifact.getVocabSize());
                String archSummary = String.format("%d layers, %d heads, embed: %d, dff: %d, maxSeq: %d",
                        artifact.getLayers(), artifact.getHeads(), artifact.getEmbeddingSize(), artifact.getDff(), artifact.getMaxSeqLen());
                target.setArchitectureSummary(archSummary);
                target.setStatusMessage(String.format("Valid Forge workspace (%s params, Vocab: %d)",
                        formatParamCount(artifact.getParameterCount()), artifact.getVocabSize()));
                target.setDisplayName(dir.getFileName().toString());
                return target;
            }

            // 2. Fallback: Parse exported workspace metadata (model.json, config.json, Modelfile, training-report.json)
            ForgeTarget target = inspectExportedEvoWorkspace(pathStr, dir);
            if (target != null) {
                return target;
            }

            ForgeTarget failedTarget = new ForgeTarget(pathStr, ForgeTargetType.EVO_WORKSPACE, false, "Invalid Forge workspace: No model metadata found");
            failedTarget.setDisplayName(dir.getFileName() != null ? dir.getFileName().toString() : pathStr);
            return failedTarget;
        } catch (Exception e) {
            ForgeTarget target = new ForgeTarget(pathStr, ForgeTargetType.EVO_WORKSPACE, false, "Invalid Forge workspace: " + e.getMessage());
            target.setDisplayName(dir.getFileName() != null ? dir.getFileName().toString() : pathStr);
            return target;
        }
    }

    private static ForgeTarget inspectExportedEvoWorkspace(String pathStr, Path dir) {
        Path manifestPath = dir.resolve("model.json");
        Path configPath = dir.resolve("config.json");
        Path reportPath = dir.resolve("training-report.json");
        Path modelfile = dir.resolve("Modelfile");
        Path exportsModelfile = dir.resolve("exports/ollama/Modelfile");
        Path exportsGguf = dir.resolve("exports/ollama/evo.gguf");
        Path rootGguf = dir.resolve("evo.gguf");

        String modelName = dir.getFileName() != null ? dir.getFileName().toString() : "evo_model";
        int vocabSize = 0, dModel = 0, numHeads = 0, numBlocks = 0, dff = 0, maxSeqLen = 0;
        long paramCount = 0;

        if (Files.exists(reportPath)) {
            try {
                String reportStr = Files.readString(reportPath);
                org.json.JSONObject obj = new org.json.JSONObject(reportStr);
                if (obj.has("modelName")) modelName = obj.optString("modelName", modelName);
                if (obj.has("parameterCount")) paramCount = obj.optLong("parameterCount", 0);
            } catch (Exception ignored) {}
        }

        if (Files.exists(manifestPath)) {
            try {
                String jsonStr = Files.readString(manifestPath);
                org.json.JSONObject obj = new org.json.JSONObject(jsonStr);
                if (obj.has("modelName")) modelName = obj.optString("modelName", modelName);
                if (obj.has("vocabSize")) vocabSize = obj.getInt("vocabSize");
                else if (obj.has("vocab_size")) vocabSize = obj.getInt("vocab_size");
                if (obj.has("dModel")) dModel = obj.getInt("dModel");
                else if (obj.has("d_model")) dModel = obj.getInt("d_model");
                if (obj.has("numHeads")) numHeads = obj.getInt("numHeads");
                else if (obj.has("num_heads")) numHeads = obj.getInt("num_heads");
                if (obj.has("numBlocks")) numBlocks = obj.getInt("numBlocks");
                else if (obj.has("num_blocks")) numBlocks = obj.getInt("num_blocks");
                if (obj.has("dff")) dff = obj.getInt("dff");
                if (obj.has("maxSeqLen")) maxSeqLen = obj.getInt("maxSeqLen");
                else if (obj.has("max_seq_len")) maxSeqLen = obj.getInt("max_seq_len");
            } catch (Exception ignored) {}
        }

        if (Files.exists(configPath)) {
            try {
                String jsonStr = Files.readString(configPath);
                org.json.JSONObject obj = new org.json.JSONObject(jsonStr);
                if (vocabSize <= 0) {
                    if (obj.has("vocabSize")) vocabSize = obj.getInt("vocabSize");
                    else if (obj.has("vocab_size")) vocabSize = obj.getInt("vocab_size");
                }
                if (dModel <= 0) {
                    if (obj.has("dModel")) dModel = obj.getInt("dModel");
                    else if (obj.has("d_model")) dModel = obj.getInt("d_model");
                }
                if (numHeads <= 0) {
                    if (obj.has("numHeads")) numHeads = obj.getInt("numHeads");
                    else if (obj.has("num_heads")) numHeads = obj.getInt("num_heads");
                }
                if (numBlocks <= 0) {
                    if (obj.has("numBlocks")) numBlocks = obj.getInt("numBlocks");
                    else if (obj.has("num_blocks")) numBlocks = obj.getInt("num_blocks");
                }
                if (dff <= 0) {
                    if (obj.has("dff")) dff = obj.getInt("dff");
                }
                if (maxSeqLen <= 0) {
                    if (obj.has("maxSeqLen")) maxSeqLen = obj.getInt("maxSeqLen");
                    else if (obj.has("max_seq_len")) maxSeqLen = obj.getInt("max_seq_len");
                }
            } catch (Exception ignored) {}
        }

        Path mFile = Files.exists(modelfile) ? modelfile : (Files.exists(exportsModelfile) ? exportsModelfile : null);
        if (mFile != null) {
            try {
                java.util.List<String> lines = Files.readAllLines(mFile);
                for (String line : lines) {
                    String trimmed = line.trim();
                    if (trimmed.startsWith("PARAMETER num_ctx") && maxSeqLen <= 0) {
                        try {
                            maxSeqLen = Integer.parseInt(trimmed.substring("PARAMETER num_ctx".length()).trim());
                        } catch (Exception ignored) {}
                    }
                }
            } catch (Exception ignored) {}
        }

        boolean hasExportArtifacts = Files.exists(rootGguf) || Files.exists(exportsGguf) || mFile != null || Files.exists(reportPath);

        if (numBlocks > 0 || dModel > 0 || hasExportArtifacts) {
            ForgeTarget target = new ForgeTarget(pathStr, ForgeTargetType.EVO_WORKSPACE, true, "Valid EVO Forge workspace");
            target.setModelName(modelName);
            target.setVocabSize(vocabSize);
            if (paramCount <= 0 && numBlocks > 0 && dModel > 0) {
                // Rough param count estimation: vocab*dModel + numBlocks*(4*dModel^2 + 2*dModel*dff)
                paramCount = (long) vocabSize * dModel + (long) numBlocks * (4L * dModel * dModel + 2L * dModel * (dff > 0 ? dff : 4 * dModel));
            }
            target.setParameterCount(paramCount);

            String archSummary = String.format("%d layers, %d heads, embed: %d, dff: %d, maxSeq: %d",
                    numBlocks, numHeads, dModel, dff, maxSeqLen);
            target.setArchitectureSummary(archSummary);
            target.setStatusMessage(String.format("Valid Forge workspace (%s params, Vocab: %d)",
                    formatParamCount(paramCount), vocabSize));
            target.setDisplayName(dir.getFileName() != null ? dir.getFileName().toString() : modelName);
            return target;
        }

        return null;
    }

    private static String formatParamCount(long params) {
        if (params >= 1_000_000_000) {
            return String.format("%.2fB", params / 1_000_000_000.0);
        } else if (params >= 1_000_000) {
            return String.format("%.2fM", params / 1_000_000.0);
        } else if (params >= 1_000) {
            return String.format("%.1fK", params / 1_000.0);
        } else {
            return String.valueOf(params);
        }
    }
}
