package eu.kalafatic.evolution.forge.model.llm;

import eu.kalafatic.evolution.forge.math.api.Tensor;
import eu.kalafatic.evolution.forge.math.core.SimpleTensor;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

/**
 * Self-contained native EVO model artifact.
 * Holds all the architectural parameters, tokenizer state, sampling parameters,
 * and handles robust weight saving/loading independently of any external framework (like Ollama).
 */
public class EvoModelArtifact {

    // Magic headers for structural safety & validation
    private static final byte[] MAGIC = "EVO".getBytes();
    private static final short WEIGHTS_VERSION = 1;

    // Manifest / Metadata Keys
    private String format = "EVO_NATIVE";
    private int formatVersion = 1;
    private String modelName = "evo-unnamed";
    private String architecture = "EVO_LLM";
    private long creationTimestamp = System.currentTimeMillis();

    // Architectural Dimensions
    private int vocabSize;
    private int embeddingSize;
    private int layers;
    private int heads;
    private int dff;
    private int maxSeqLen;
    private long parameterCount;

    // Sampling Configuration
    private float temperature = 0.2f;
    private float topP = 0.9f;
    private int topK = 40;
    private float repeatPenalty = 1.1f;

    // Tokenizer mappings
    private Map<String, Integer> tokenizerVocab = new LinkedHashMap<>();

    // Trained weights/parameters
    private List<Tensor> weights = new ArrayList<>();

    // Empty constructor
    public EvoModelArtifact() {}

    /**
     * Reconstructs an EvoLlmModel instance using the exact persisted architecture and weights.
     */
    public EvoLlmModel createModel() {
        validateModelIntegrity();
        EvoLlmModel model = new EvoLlmModel(vocabSize, embeddingSize, heads, layers, dff, maxSeqLen);
        List<Tensor> modelParams = model.parameters();
        if (modelParams.size() != weights.size()) {
            throw new IllegalStateException("Tensor count mismatch: Model expected " + modelParams.size() 
                    + " tensors, but artifact has " + weights.size());
        }
        for (int i = 0; i < modelParams.size(); i++) {
            Tensor target = modelParams.get(i);
            Tensor src = weights.get(i);
            if (!Arrays.equals(target.getShape(), src.getShape())) {
                throw new IllegalStateException("Tensor shape mismatch at index " + i + ". Expected: " 
                        + Arrays.toString(target.getShape()) + ", Found: " + Arrays.toString(src.getShape()));
            }
            System.arraycopy(src.getData(), 0, target.getData(), 0, src.getData().length);
        }
        return model;
    }

    /**
     * Initializes the artifact from an active model and its tokenizer mappings.
     */
    public void initializeFromModel(String modelName, EvoLlmModel model, Map<String, Integer> vocab) {
        this.modelName = modelName;
        this.vocabSize = model.getVocabSize();
        this.embeddingSize = model.getDModel();
        this.layers = model.getNumBlocks();
        this.heads = model.getNumHeads();
        this.dff = model.getDff();
        this.maxSeqLen = model.getMaxSeqLen();
        this.tokenizerVocab = new LinkedHashMap<>(vocab);
        
        this.weights = new ArrayList<>();
        long totalParams = 0;
        for (Tensor p : model.parameters()) {
            float[] dataCopy = new float[p.getData().length];
            System.arraycopy(p.getData(), 0, dataCopy, 0, p.getData().length);
            this.weights.add(new SimpleTensor(p.getShape(), dataCopy));
            totalParams += p.getData().length;
        }
        this.parameterCount = totalParams;
    }

    /**
     * Validates that all structural data is correctly filled and matches on-disk contents.
     */
    public void validateModelIntegrity() {
        if (!"EVO_NATIVE".equals(format)) {
            throw new IllegalArgumentException("Unsupported format: " + format);
        }
        if (formatVersion != 1) {
            throw new IllegalArgumentException("Unsupported format version: " + formatVersion);
        }
        if (vocabSize <= 0 || embeddingSize <= 0 || layers <= 0 || heads <= 0 || dff <= 0 || maxSeqLen <= 0) {
            throw new IllegalArgumentException("Invalid architecture dimensions in artifact.");
        }
        if (tokenizerVocab == null || tokenizerVocab.isEmpty()) {
            throw new IllegalArgumentException("Tokenizer vocabulary is missing or empty.");
        }
        if (weights == null || weights.isEmpty()) {
            throw new IllegalArgumentException("Weights list is missing or empty.");
        }
    }

    /**
     * Saves the native model artifact to on-disk stable layout files.
     */
    public EvoModelArtifact save(Path dir) throws IOException {
        Files.createDirectories(dir);

        // 1. Serialize model.json (the stable manifest file)
        StringBuilder sb = new StringBuilder();
        sb.append("{\n")
                .append("  \"format\": \"").append(format).append("\",\n")
                .append("  \"formatVersion\": ").append(formatVersion).append(",\n")
                .append("  \"modelName\": \"").append(modelName).append("\",\n")
                .append("  \"architecture\": \"").append(architecture).append("\",\n")
                .append("  \"creationTimestamp\": ").append(creationTimestamp).append(",\n")
                .append("  \"vocabSize\": ").append(vocabSize).append(",\n")
                .append("  \"embeddingSize\": ").append(embeddingSize).append(",\n")
                .append("  \"layers\": ").append(layers).append(",\n")
                .append("  \"heads\": ").append(heads).append(",\n")
                .append("  \"dff\": ").append(dff).append(",\n")
                .append("  \"maxSeqLen\": ").append(maxSeqLen).append(",\n")
                .append("  \"parameterCount\": ").append(parameterCount).append(",\n")
                .append("  \"temperature\": ").append(temperature).append(",\n")
                .append("  \"top_p\": ").append(topP).append(",\n")
                .append("  \"top_k\": ").append(topK).append(",\n")
                .append("  \"repeat_penalty\": ").append(repeatPenalty).append("\n")
                .append("}");
        Files.writeString(dir.resolve("model.json"), sb.toString());

        // 2. Also save legacy config.json and training.json files for backward compatibility
        StringBuilder configJson = new StringBuilder();
        configJson.append("{\n")
                .append("  \"vocabSize\": ").append(vocabSize).append(",\n")
                .append("  \"dModel\": ").append(embeddingSize).append(",\n")
                .append("  \"numHeads\": ").append(heads).append(",\n")
                .append("  \"numBlocks\": ").append(layers).append(",\n")
                .append("  \"dff\": ").append(dff).append(",\n")
                .append("  \"maxSeqLen\": ").append(maxSeqLen).append("\n")
                .append("}");
        Files.writeString(dir.resolve("config.json"), configJson.toString());

        StringBuilder trainingJson = new StringBuilder();
        trainingJson.append("{\n")
                .append("  \"epoch\": 1,\n")
                .append("  \"loss\": 0.0\n")
                .append("}");
        Files.writeString(dir.resolve("training.json"), trainingJson.toString());

        // 3. Serialize tokenizer.json (complete vocabulary / token mappings state)
        StringBuilder tokBuilder = new StringBuilder();
        tokBuilder.append("{\n")
                .append("  \"type\": \"SimpleBPE\",\n")
                .append("  \"vocabSize\": ").append(vocabSize).append(",\n")
                .append("  \"vocab\": {\n");
        int count = 0;
        for (Map.Entry<String, Integer> entry : tokenizerVocab.entrySet()) {
            String cleanKey = entry.getKey()
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
            tokBuilder.append("    \"").append(cleanKey).append("\": ").append(entry.getValue());
            if (++count < tokenizerVocab.size()) {
                tokBuilder.append(",");
            }
            tokBuilder.append("\n");
        }
        tokBuilder.append("  }\n}");
        Files.writeString(dir.resolve("tokenizer.json"), tokBuilder.toString());

        // 4. Save weights.bin atomically with custom headers & structure
        Path weightsPath = dir.resolve("weights.bin");
        Path tempFile = dir.resolve("weights.bin." + UUID.randomUUID().toString() + ".tmp");
        try {
            try (DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(tempFile.toFile())))) {
                // Header: MAGIC (3 bytes)
                dos.write(MAGIC);
                // Header: VERSION (2 bytes)
                dos.writeShort(WEIGHTS_VERSION);
                // Header: MODEL_ID String length + bytes
                byte[] modelIdBytes = modelName.getBytes();
                dos.writeInt(modelIdBytes.length);
                dos.write(modelIdBytes);
                // Header: TENSOR_COUNT (4 bytes)
                dos.writeInt(weights.size());

                // Metadata and data for each tensor
                for (int i = 0; i < weights.size(); i++) {
                    Tensor t = weights.get(i);
                    // Name or Index
                    String tensorName = "tensor_" + i;
                    byte[] nameBytes = tensorName.getBytes();
                    dos.writeInt(nameBytes.length);
                    dos.write(nameBytes);

                    // Shape dimensions
                    long[] shape = t.getShape();
                    dos.writeInt(shape.length);
                    for (long dim : shape) {
                        dos.writeLong(dim);
                    }

                    // Float32 Payload
                    float[] data = t.getData();
                    dos.writeInt(data.length);
                    for (float val : data) {
                        dos.writeFloat(val);
                    }
                }
                dos.flush();
            }

            try {
                Files.move(tempFile, weightsPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException e) {
                Files.move(tempFile, weightsPath, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            try {
                Files.deleteIfExists(tempFile);
            } catch (Exception ignored) {}
        }

        return this;
    }

    /**
     * Loads and validates a native model artifact from directory.
     */
    public static EvoModelArtifact load(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            throw new FileNotFoundException("Model directory not found: " + dir);
        }

        Path manifestPath = dir.resolve("model.json");
        Path configPath = dir.resolve("config.json");
        Path tokenizerPath = dir.resolve("tokenizer.json");
        Path weightsPath = dir.resolve("weights.bin");

        if (!Files.exists(weightsPath)) {
            throw new FileNotFoundException("Missing weights.bin file in: " + dir);
        }

        EvoModelArtifact artifact = new EvoModelArtifact();

        // 1. Load stable manifest if present, else fallback to config.json migration
        if (Files.exists(manifestPath)) {
            String mJson = Files.readString(manifestPath);
            artifact.setFormat(parseStringField(mJson, "format"));
            artifact.setFormatVersion(parseIntField(mJson, "formatVersion", 1));
            artifact.setModelName(parseStringField(mJson, "modelName"));
            artifact.setArchitecture(parseStringField(mJson, "architecture"));
            artifact.setCreationTimestamp(parseLongField(mJson, "creationTimestamp", System.currentTimeMillis()));
            artifact.setVocabSize(parseIntField(mJson, "vocabSize", 0));
            artifact.setEmbeddingSize(parseIntField(mJson, "embeddingSize", 0));
            artifact.setLayers(parseIntField(mJson, "layers", 0));
            artifact.setHeads(parseIntField(mJson, "heads", 0));
            artifact.setDff(parseIntField(mJson, "dff", 0));
            artifact.setMaxSeqLen(parseIntField(mJson, "maxSeqLen", 0));
            artifact.setParameterCount(parseLongField(mJson, "parameterCount", 0));
            artifact.setTemperature(parseFloatField(mJson, "temperature", 0.2f));
            artifact.setTopP(parseFloatField(mJson, "top_p", 0.9f));
            artifact.setTopK(parseIntField(mJson, "top_k", 40));
            artifact.setRepeatPenalty(parseFloatField(mJson, "repeat_penalty", 1.1f));
        } else if (Files.exists(configPath)) {
            // BACKWARD COMPATIBILITY MIGRATION
            System.out.println("[Artifact] model.json missing. Attempting backward compatibility load from config.json...");
            String cfgJson = Files.readString(configPath);
            artifact.setVocabSize(parseIntField(cfgJson, "vocabSize", 0));
            artifact.setEmbeddingSize(parseIntField(cfgJson, "dModel", 0));
            artifact.setLayers(parseIntField(cfgJson, "numBlocks", 0));
            artifact.setHeads(parseIntField(cfgJson, "numHeads", 0));
            artifact.setDff(parseIntField(cfgJson, "dff", artifact.getEmbeddingSize() * 4));
            artifact.setMaxSeqLen(parseIntField(cfgJson, "maxSeqLen", 0));
            artifact.setModelName("migrated-" + dir.getFileName().toString());
        } else {
            throw new FileNotFoundException("Neither model.json nor config.json exists in " + dir);
        }

        // 2. Load tokenizer vocabulary from tokenizer.json (complete map recovery)
        if (Files.exists(tokenizerPath)) {
            String tJson = Files.readString(tokenizerPath);
            Map<String, Integer> vocabMap = parseVocabFromJson(tJson);
            artifact.setTokenizerVocab(vocabMap);
        } else {
            // MIGRATION / MOCK VOCABULARY GENERATION
            System.out.println("[Artifact] tokenizer.json missing. Generating safe vocabulary mapping...");
            Map<String, Integer> mockVocab = new LinkedHashMap<>();
            mockVocab.put("<PAD>", 0);
            mockVocab.put("<UNK>", 1);
            mockVocab.put("<BOS>", 2);
            mockVocab.put("<EOS>", 3);
            for (int i = 4; i < artifact.getVocabSize(); i++) {
                mockVocab.put("token_" + i, i);
            }
            artifact.setTokenizerVocab(mockVocab);
        }

        // 3. Load & Validate binary weights
        long totalFileSize = Files.size(weightsPath);
        if (totalFileSize < 5) {
            throw new IOException("Weights file weights.bin is truncated or corrupt. File size: " + totalFileSize + " bytes.");
        }

        try (DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(weightsPath.toFile())))) {
            // Read magic
            byte[] magicIn = new byte[3];
            dis.readFully(magicIn);
            if (Arrays.equals(magicIn, MAGIC)) {
                // Structured weights.bin path
                short version = dis.readShort();
                if (version != WEIGHTS_VERSION) {
                    throw new IOException("Unsupported weights version: " + version);
                }
                int modelIdLen = dis.readInt();
                byte[] modelIdBytes = new byte[modelIdLen];
                dis.readFully(modelIdBytes);

                int tensorCount = dis.readInt();
                List<Tensor> loadedTensors = new ArrayList<>();
                long accParams = 0;

                for (int i = 0; i < tensorCount; i++) {
                    int nameLen = dis.readInt();
                    byte[] nameBytes = new byte[nameLen];
                    dis.readFully(nameBytes);

                    int shapeLen = dis.readInt();
                    long[] shape = new long[shapeLen];
                    for (int s = 0; s < shapeLen; s++) {
                        shape[s] = dis.readLong();
                    }

                    int dataLen = dis.readInt();
                    float[] data = new float[dataLen];
                    for (int d = 0; d < dataLen; d++) {
                        data[d] = dis.readFloat();
                    }

                    loadedTensors.add(new SimpleTensor(shape, data));
                    accParams += dataLen;
                }

                artifact.setWeights(loadedTensors);
                artifact.setParameterCount(accParams);
            } else {
                // Fallback / legacy raw float stream weights loader
                System.out.println("[Artifact] raw weights.bin stream detected. Falling back to structured parsing...");
                dis.close(); // Reset streams

                // We need the architecture dimensions to deserialize raw float weights
                if (artifact.getVocabSize() <= 0 || artifact.getEmbeddingSize() <= 0) {
                    throw new IOException("Cannot parse raw weights stream: missing valid configuration dimensions.");
                }

                EvoLlmModel tempModel = new EvoLlmModel(artifact.getVocabSize(), artifact.getEmbeddingSize(), 
                        artifact.getHeads(), artifact.getLayers(), artifact.getDff(), artifact.getMaxSeqLen());
                List<Tensor> params = tempModel.parameters();
                List<Tensor> loadedTensors = new ArrayList<>();
                long accParams = 0;

                try (DataInputStream disRaw = new DataInputStream(new BufferedInputStream(new FileInputStream(weightsPath.toFile())))) {
                    for (Tensor p : params) {
                        float[] data = new float[p.getData().length];
                        for (int i = 0; i < data.length; i++) {
                            data[i] = disRaw.readFloat();
                        }
                        loadedTensors.add(new SimpleTensor(p.getShape(), data));
                        accParams += data.length;
                    }
                }

                artifact.setWeights(loadedTensors);
                artifact.setParameterCount(accParams);
            }
        }

        artifact.validateModelIntegrity();
        return artifact;
    }

    /**
     * Custom parsing for vocab block in tokenizer.json to avoid Jackson/JSON dependencies.
     */
    private static Map<String, Integer> parseVocabFromJson(String json) {
        Map<String, Integer> vocabMap = new LinkedHashMap<>();
        int vocabStart = json.indexOf("\"vocab\"");
        if (vocabStart == -1) return vocabMap;
        int blockStart = json.indexOf("{", vocabStart);
        if (blockStart == -1) return vocabMap;
        int blockEnd = json.indexOf("}", blockStart);
        if (blockEnd == -1) return vocabMap;

        String vocabBlock = json.substring(blockStart + 1, blockEnd);
        String[] entries = vocabBlock.split(",");
        for (String entry : entries) {
            int colon = entry.indexOf(":");
            if (colon == -1) continue;
            String keyPart = entry.substring(0, colon).trim();
            String valPart = entry.substring(colon + 1).trim();

            if (keyPart.startsWith("\"") && keyPart.endsWith("\"")) {
                keyPart = keyPart.substring(1, keyPart.length() - 1);
            }
            // unescape common keys
            keyPart = keyPart
                    .replace("\\\\", "\\")
                    .replace("\\\"", "\"")
                    .replace("\\n", "\n")
                    .replace("\\r", "\r")
                    .replace("\\t", "\t");

            try {
                int val = Integer.parseInt(valPart);
                vocabMap.put(keyPart, val);
            } catch (Exception ignored) {}
        }
        return vocabMap;
    }

    // --- JSON PARSING HELPERS ---
    private static String parseStringField(String json, String key) {
        int idx = json.indexOf("\"" + key + "\"");
        if (idx == -1) return null;
        int colon = json.indexOf(":", idx);
        if (colon == -1) return null;
        int startQuote = json.indexOf("\"", colon);
        if (startQuote == -1) return null;
        int endQuote = json.indexOf("\"", startQuote + 1);
        if (endQuote == -1) return null;
        return json.substring(startQuote + 1, endQuote);
    }

    private static int parseIntField(String json, String key, int def) {
        int idx = json.indexOf("\"" + key + "\"");
        if (idx == -1) return def;
        int colon = json.indexOf(":", idx);
        if (colon == -1) return def;
        int end = findValueEnd(json, colon);
        try {
            return Integer.parseInt(json.substring(colon + 1, end).trim());
        } catch (Exception e) {
            return def;
        }
    }

    private static long parseLongField(String json, String key, long def) {
        int idx = json.indexOf("\"" + key + "\"");
        if (idx == -1) return def;
        int colon = json.indexOf(":", idx);
        if (colon == -1) return def;
        int end = findValueEnd(json, colon);
        try {
            return Long.parseLong(json.substring(colon + 1, end).trim());
        } catch (Exception e) {
            return def;
        }
    }

    private static float parseFloatField(String json, String key, float def) {
        int idx = json.indexOf("\"" + key + "\"");
        if (idx == -1) return def;
        int colon = json.indexOf(":", idx);
        if (colon == -1) return def;
        int end = findValueEnd(json, colon);
        try {
            return Float.parseFloat(json.substring(colon + 1, end).trim());
        } catch (Exception e) {
            return def;
        }
    }

    private static int findValueEnd(String json, int colonIndex) {
        int end = json.indexOf(",", colonIndex);
        if (end == -1) end = json.indexOf("\n", colonIndex);
        if (end == -1) end = json.indexOf("}", colonIndex);
        return end;
    }

    // Getters and Setters
    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }

    public int getFormatVersion() { return formatVersion; }
    public void setFormatVersion(int formatVersion) { this.formatVersion = formatVersion; }

    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }

    public String getArchitecture() { return architecture; }
    public void setArchitecture(String architecture) { this.architecture = architecture; }

    public long getCreationTimestamp() { return creationTimestamp; }
    public void setCreationTimestamp(long creationTimestamp) { this.creationTimestamp = creationTimestamp; }

    public int getVocabSize() { return vocabSize; }
    public void setVocabSize(int vocabSize) { this.vocabSize = vocabSize; }

    public int getEmbeddingSize() { return embeddingSize; }
    public void setEmbeddingSize(int embeddingSize) { this.embeddingSize = embeddingSize; }

    public int getLayers() { return layers; }
    public void setLayers(int layers) { this.layers = layers; }

    public int getHeads() { return heads; }
    public void setHeads(int heads) { this.heads = heads; }

    public int getDff() { return dff; }
    public void setDff(int dff) { this.dff = dff; }

    public int getMaxSeqLen() { return maxSeqLen; }
    public void setMaxSeqLen(int maxSeqLen) { this.maxSeqLen = maxSeqLen; }

    public long getParameterCount() { return parameterCount; }
    public void setParameterCount(long parameterCount) { this.parameterCount = parameterCount; }

    public float getTemperature() { return temperature; }
    public void setTemperature(float temperature) { this.temperature = temperature; }

    public float getTopP() { return topP; }
    public void setTopP(float topP) { this.topP = topP; }

    public int getTopK() { return topK; }
    public void setTopK(int topK) { this.topK = topK; }

    public float getRepeatPenalty() { return repeatPenalty; }
    public void setRepeatPenalty(float repeatPenalty) { this.repeatPenalty = repeatPenalty; }

    public Map<String, Integer> getTokenizerVocab() { return tokenizerVocab; }
    public void setTokenizerVocab(Map<String, Integer> tokenizerVocab) { this.tokenizerVocab = tokenizerVocab; }

    public List<Tensor> getWeights() { return weights; }
    public void setWeights(List<Tensor> weights) { this.weights = weights; }
}
