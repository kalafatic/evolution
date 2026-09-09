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

        boolean hasConfig = Files.exists(configPath) || Files.exists(modelPath);
        boolean hasWeights = Files.exists(weightsPath);
        boolean hasEvoInDir = false;

        try (java.util.stream.Stream<Path> stream = Files.list(dir)) {
            hasEvoInDir = stream.anyMatch(p -> p.getFileName().toString().toLowerCase().endsWith(".evo"));
        } catch (Exception ignored) {}

        return (hasConfig && (hasWeights || hasEvoInDir)) || Files.exists(sessionPath) || Files.exists(reportPath);
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
            EvoModelArtifact artifact = EvoModelArtifact.load(dir);
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
        } catch (Exception e) {
            ForgeTarget target = new ForgeTarget(pathStr, ForgeTargetType.EVO_WORKSPACE, false, "Invalid Forge workspace: " + e.getMessage());
            target.setDisplayName(dir.getFileName().toString());
            return target;
        }
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
