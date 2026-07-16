package eu.kalafatic.evolution.forge.model.llm;

import eu.kalafatic.evolution.forge.math.api.Tensor;
import eu.kalafatic.evolution.forge.math.core.SimpleTensor;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class EvoModelSerializer {

    public static void save(EvoLlmModel model, Path dir, float finalLoss, int epoch) throws IOException {
        Files.createDirectories(dir);

        // 1. config.json
        StringBuilder configJson = new StringBuilder();
        configJson.append("{\n")
                .append("  \"vocabSize\": ").append(model.getVocabSize()).append(",\n")
                .append("  \"dModel\": ").append(model.getDModel()).append(",\n")
                .append("  \"numHeads\": ").append(model.getNumHeads()).append(",\n")
                .append("  \"numBlocks\": ").append(model.getNumBlocks()).append(",\n")
                .append("  \"dff\": ").append(model.getDff()).append(",\n")
                .append("  \"maxSeqLen\": ").append(model.getMaxSeqLen()).append("\n")
                .append("}");
        Files.writeString(dir.resolve("config.json"), configJson.toString());

        // 2. training.json
        StringBuilder trainingJson = new StringBuilder();
        trainingJson.append("{\n")
                .append("  \"epoch\": ").append(epoch).append(",\n")
                .append("  \"loss\": ").append(finalLoss).append("\n")
                .append("}");
        Files.writeString(dir.resolve("training.json"), trainingJson.toString());

        // 3. weights.bin
        try (DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(dir.resolve("weights.bin").toFile())))) {
            List<Tensor> params = model.parameters();
            for (Tensor p : params) {
                float[] data = p.getData();
                for (float val : data) {
                    dos.writeFloat(val);
                }
            }
        }
    }

    public static EvoLlmModel load(Path dir) throws IOException {
        // 1. Load config.json
        String configStr = Files.readString(dir.resolve("config.json"));
        int vocabSize = parseVal(configStr, "vocabSize");
        int dModel = parseVal(configStr, "dModel");
        int numHeads = parseVal(configStr, "numHeads");
        int numBlocks = parseVal(configStr, "numBlocks");
        int dff = parseVal(configStr, "dff");
        int maxSeqLen = parseVal(configStr, "maxSeqLen");

        EvoLlmModel model = new EvoLlmModel(vocabSize, dModel, numHeads, numBlocks, dff, maxSeqLen);

        // 2. Load weights.bin
        try (DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(dir.resolve("weights.bin").toFile())))) {
            List<Tensor> params = model.parameters();
            for (Tensor p : params) {
                float[] data = p.getData();
                for (int i = 0; i < data.length; i++) {
                    data[i] = dis.readFloat();
                }
            }
        }
        return model;
    }

    private static int parseVal(String json, String key) {
        int idx = json.indexOf("\"" + key + "\"");
        if (idx == -1) return 0;
        int colon = json.indexOf(":", idx);
        if (colon == -1) return 0;
        int end = json.indexOf(",", colon);
        if (end == -1) {
            end = json.indexOf("\n", colon);
        }
        if (end == -1) {
            end = json.indexOf("}", colon);
        }
        String valStr = json.substring(colon + 1, end).trim();
        return Integer.parseInt(valStr);
    }
}
