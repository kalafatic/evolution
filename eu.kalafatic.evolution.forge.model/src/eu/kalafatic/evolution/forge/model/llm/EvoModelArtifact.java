package eu.kalafatic.evolution.forge.model.llm;

import eu.kalafatic.evolution.forge.math.api.Tensor;
import eu.kalafatic.evolution.forge.math.core.SimpleTensor;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * EvoModelArtifact - Complete portable model artifact with full vocabulary preservation
 *
 * This class stores:
 * 1. Model architecture configuration
 * 2. All model weights (tensors)
 * 3. Full tokenizer vocabulary (bidirectional mapping)
 * 4. Inference parameters (temperature, topP, etc.)
 */
public class EvoModelArtifact {

    // ============ CORE FIELDS ============
    private String modelName;
    private String modelVersion = "1.0.0";
    private long createdAt;

    // Architecture
    private int vocabSize;
    private int dModel;
    private int numHeads;
    private int numBlocks;
    private int dff;
    private int maxSeqLen;

    // Vocabulary (BIDIRECTIONAL - CRITICAL FOR EXPORT!)
    private Map<Integer, String> idToToken;  // Token ID -> Token string
    private Map<String, Integer> tokenToId;   // Token string -> Token ID

    // Special tokens
    private int bosTokenId = 1;
    private int eosTokenId = 2;
    private int unkTokenId = 0;
    private int padTokenId = 0;

    // Model weights (flattened)
    private List<float[]> weightData;
    private List<long[]> weightShapes;
    private List<String> weightNames;

    // Inference parameters
    private float temperature = 0.7f;
    private float topP = 0.9f;
    private int topK = 40;
    private float repeatPenalty = 1.1f;
    private float frequencyPenalty = 0.0f;
    private float presencePenalty = 0.0f;

    // Metadata
    private Map<String, String> metadata = new HashMap<>();

    // ============ CONSTRUCTORS ============
    public EvoModelArtifact() {
        this.createdAt = System.currentTimeMillis();
        this.idToToken = new LinkedHashMap<>();
        this.tokenToId = new LinkedHashMap<>();
        this.weightData = new ArrayList<>();
        this.weightShapes = new ArrayList<>();
        this.weightNames = new ArrayList<>();
    }

    /**
     * Converts this artifact into a canonical ModelSnapshot.
     */
    public ModelSnapshot toSnapshot() {
        EvoLlmModel tempModel = createModel();
        return tempModel.createSnapshot();
    }

    // ============ INITIALIZATION ============
    public void initializeFromModel(String name, EvoLlmModel model, Map<String, Integer> tokenizerVocab) {
        this.modelName = name;
        this.vocabSize = model.getVocabSize();
        this.dModel = model.getDModel();
        this.numHeads = model.getNumHeads();
        this.numBlocks = model.getNumBlocks();
        this.dff = model.getDff();
        this.maxSeqLen = model.getMaxSeqLen();

        // Initialize vocabulary (bidirectional)
        this.tokenToId = new LinkedHashMap<>(tokenizerVocab);
        this.idToToken = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : tokenizerVocab.entrySet()) {
            this.idToToken.put(entry.getValue(), entry.getKey());
        }

        // Ensure special tokens exist
        ensureSpecialTokens();

        // Extract weights
        this.weightData.clear();
        this.weightShapes.clear();
        this.weightNames.clear();

        for (Tensor t : model.parameters()) {
            float[] data = t.getData().clone();
            long[] shape = t.getShape().clone();
            weightData.add(data);
            weightShapes.add(shape);
            weightNames.add(generateWeightName(weightData.size() - 1));
        }

        this.metadata.put("created_at", String.valueOf(createdAt));
        this.metadata.put("model_type", "evo_llm");
    }

    private void ensureSpecialTokens() {
        if (tokenToId.containsKey("<s>")) {
            bosTokenId = tokenToId.get("<s>");
        }
        if (tokenToId.containsKey("</s>")) {
            eosTokenId = tokenToId.get("</s>");
        }
        if (tokenToId.containsKey("<unk>")) {
            unkTokenId = tokenToId.get("<unk>");
        }
        if (tokenToId.containsKey("<pad>")) {
            padTokenId = tokenToId.get("<pad>");
        }
    }

    private String generateWeightName(int index) {
        if (index == 0) return "token_embd.weight";

        int paramsPerBlock = 9;
        int offset = 1;

        for (int block = 0; block < numBlocks; block++) {
            int base = offset + block * paramsPerBlock;
            if (index == base) return "blk." + block + ".attn_norm.weight";
            if (index == base + 1) return "blk." + block + ".attn_q.weight";
            if (index == base + 2) return "blk." + block + ".attn_k.weight";
            if (index == base + 3) return "blk." + block + ".attn_v.weight";
            if (index == base + 4) return "blk." + block + ".attn_output.weight";
            if (index == base + 5) return "blk." + block + ".ffn_norm.weight";
            if (index == base + 6) return "blk." + block + ".ffn_gate.weight";
            if (index == base + 7) return "blk." + block + ".ffn_up.weight";
            if (index == base + 8) return "blk." + block + ".ffn_down.weight";
        }

        int outputNormIdx = offset + numBlocks * paramsPerBlock;
        if (index == outputNormIdx) return "output_norm.weight";
        if (index == outputNormIdx + 1) return "output.weight";

        return "weight_" + index;
    }

    // ============ SAVE / LOAD ============
    public void save(Path path) throws IOException {
        if (path == null) {
            throw new IllegalArgumentException("Save path cannot be null");
        }

        if (Files.isDirectory(path) || (!path.toString().endsWith(".evo") && !path.getFileName().toString().contains("."))) {
            saveToDirectory(path);
            return;
        }

        saveToFile(path);
    }

    public void saveToDirectory(Path dir) throws IOException {
        Files.createDirectories(dir);

        // 1. model.json
        JSONObject modelJson = new JSONObject();
        modelJson.put("modelName", modelName != null ? modelName : "evo_model");
        modelJson.put("vocabSize", vocabSize);
        modelJson.put("dModel", dModel);
        modelJson.put("numHeads", numHeads);
        modelJson.put("numBlocks", numBlocks);
        modelJson.put("dff", dff);
        modelJson.put("maxSeqLen", maxSeqLen);
        Files.writeString(dir.resolve("model.json"), modelJson.toString(2));

        // 2. config.json
        JSONObject configJson = new JSONObject();
        configJson.put("model_name", modelName != null ? modelName : "evo_model");
        configJson.put("vocab_size", vocabSize);
        configJson.put("vocabSize", vocabSize);
        configJson.put("d_model", dModel);
        configJson.put("dModel", dModel);
        configJson.put("num_heads", numHeads);
        configJson.put("numHeads", numHeads);
        configJson.put("num_blocks", numBlocks);
        configJson.put("numBlocks", numBlocks);
        configJson.put("dff", dff);
        configJson.put("max_seq_len", maxSeqLen);
        configJson.put("maxSeqLen", maxSeqLen);
        configJson.put("temperature", temperature);
        configJson.put("top_p", topP);
        configJson.put("topK", topK);
        configJson.put("top_k", topK);
        configJson.put("repeat_penalty", repeatPenalty);
        Files.writeString(dir.resolve("config.json"), configJson.toString(2));

        // 3. tokenizer.json
        JSONObject tokenizerJson = new JSONObject();
        tokenizerJson.put("vocab_size", vocabSize);
        tokenizerJson.put("bos_token", "<s>");
        tokenizerJson.put("eos_token", "</s>");
        tokenizerJson.put("unk_token", "<unk>");
        JSONObject vocabObj = new JSONObject();
        if (tokenToId != null) {
            for (Map.Entry<String, Integer> entry : tokenToId.entrySet()) {
                vocabObj.put(entry.getKey(), entry.getValue());
            }
        }
        tokenizerJson.put("vocab", vocabObj);
        if (idToToken != null) {
            JSONArray tokensArr = new JSONArray();
            for (Map.Entry<Integer, String> entry : idToToken.entrySet()) {
                tokensArr.put(entry.getValue());
            }
            tokenizerJson.put("tokens", tokensArr);
        }
        Files.writeString(dir.resolve("tokenizer.json"), tokenizerJson.toString(2));

        // 4. weights.bin
        Path weightsPath = dir.resolve("weights.bin");
        try (DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(weightsPath.toFile())))) {
            EvoLlmModel tempModel = createModel();
            for (Tensor p : tempModel.parameters()) {
                for (float val : p.getData()) {
                    dos.writeFloat(val);
                }
            }
        }

        // 5. Also save binary .evo file into directory
        String evoName = (modelName != null && !modelName.isEmpty()) ? modelName + ".evo" : "model.evo";
        saveToFile(dir.resolve(evoName));
    }

    public void saveToFile(Path path) throws IOException {
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        Path tempFile = path.getParent().resolve(path.getFileName().toString() + ".tmp");
        try (FileOutputStream fos = new FileOutputStream(tempFile.toFile());
             GZIPOutputStream gzos = new GZIPOutputStream(fos);
             DataOutputStream dos = new DataOutputStream(gzos)) {

            dos.writeUTF("EVO_ARTIFACT_V1");
            dos.writeUTF(modelName != null ? modelName : "evo_model");
            dos.writeUTF(modelVersion);
            dos.writeLong(createdAt);

            dos.writeInt(vocabSize);
            dos.writeInt(dModel);
            dos.writeInt(numHeads);
            dos.writeInt(numBlocks);
            dos.writeInt(dff);
            dos.writeInt(maxSeqLen);

            dos.writeFloat(temperature);
            dos.writeFloat(topP);
            dos.writeInt(topK);
            dos.writeFloat(repeatPenalty);
            dos.writeFloat(frequencyPenalty);
            dos.writeFloat(presencePenalty);

            dos.writeInt(bosTokenId);
            dos.writeInt(eosTokenId);
            dos.writeInt(unkTokenId);
            dos.writeInt(padTokenId);

            dos.writeInt(idToToken.size());
            for (Map.Entry<Integer, String> entry : idToToken.entrySet()) {
                dos.writeInt(entry.getKey());
                dos.writeUTF(entry.getValue());
            }

            dos.writeInt(metadata.size());
            for (Map.Entry<String, String> entry : metadata.entrySet()) {
                dos.writeUTF(entry.getKey());
                dos.writeUTF(entry.getValue());
            }

            dos.writeInt(weightData.size());
            for (int i = 0; i < weightData.size(); i++) {
                float[] data = weightData.get(i);
                long[] shape = weightShapes.get(i);
                String name = weightNames.get(i);

                dos.writeUTF(name);
                dos.writeInt(shape.length);
                for (long dim : shape) {
                    dos.writeLong(dim);
                }
                dos.writeInt(data.length);
                for (float val : data) {
                    dos.writeFloat(val);
                }
            }
        }

        Files.move(tempFile, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);

        Path vocabJsonPath = path.getParent().resolve(path.getFileName().toString().replace(".evo", "_vocab.json"));
        saveVocabularyJson(vocabJsonPath);
    }

    private static EvoModelArtifact loadFromDirectory(Path dir) throws IOException {
        Path manifestPath = dir.resolve("model.json");
        Path configPath = dir.resolve("config.json");
        Path weightsPath = dir.resolve("weights.bin");
        Path tokenizerPath = dir.resolve("tokenizer.json");

        if (!Files.exists(configPath) && !Files.exists(manifestPath)) {
            throw new FileNotFoundException("Neither model.json nor config.json found in directory: " + dir);
        }
        if (!Files.exists(weightsPath)) {
            throw new FileNotFoundException("weights.bin missing in directory: " + dir);
        }

        EvoModelArtifact artifact = new EvoModelArtifact();
        artifact.modelName = dir.getFileName() != null ? dir.getFileName().toString() : "evo_model";

        if (Files.exists(manifestPath)) {
            try {
                String jsonStr = Files.readString(manifestPath);
                JSONObject obj = new JSONObject(jsonStr);
                if (obj.has("modelName")) artifact.modelName = obj.optString("modelName", artifact.modelName);
                if (obj.has("vocabSize")) artifact.vocabSize = obj.getInt("vocabSize");
                else if (obj.has("vocab_size")) artifact.vocabSize = obj.getInt("vocab_size");

                if (obj.has("dModel")) artifact.dModel = obj.getInt("dModel");
                else if (obj.has("d_model")) artifact.dModel = obj.getInt("d_model");
                else if (obj.has("hidden_size")) artifact.dModel = obj.getInt("hidden_size");

                if (obj.has("numHeads")) artifact.numHeads = obj.getInt("numHeads");
                else if (obj.has("num_heads")) artifact.numHeads = obj.getInt("num_heads");

                if (obj.has("numBlocks")) artifact.numBlocks = obj.getInt("numBlocks");
                else if (obj.has("num_blocks")) artifact.numBlocks = obj.getInt("num_blocks");
                else if (obj.has("num_hidden_layers")) artifact.numBlocks = obj.getInt("num_hidden_layers");

                if (obj.has("dff")) artifact.dff = obj.getInt("dff");
                else if (obj.has("intermediate_size")) artifact.dff = obj.getInt("intermediate_size");

                if (obj.has("maxSeqLen")) artifact.maxSeqLen = obj.getInt("maxSeqLen");
                else if (obj.has("max_seq_len")) artifact.maxSeqLen = obj.getInt("max_seq_len");
            } catch (Exception ignored) {}
        }

        if (Files.exists(configPath)) {
            try {
                String jsonStr = Files.readString(configPath);
                JSONObject obj = new JSONObject(jsonStr);
                if (artifact.vocabSize <= 0) {
                    if (obj.has("vocabSize")) artifact.vocabSize = obj.getInt("vocabSize");
                    else if (obj.has("vocab_size")) artifact.vocabSize = obj.getInt("vocab_size");
                }
                if (artifact.dModel <= 0) {
                    if (obj.has("dModel")) artifact.dModel = obj.getInt("dModel");
                    else if (obj.has("d_model")) artifact.dModel = obj.getInt("d_model");
                    else if (obj.has("hidden_size")) artifact.dModel = obj.getInt("hidden_size");
                }
                if (artifact.numHeads <= 0) {
                    if (obj.has("numHeads")) artifact.numHeads = obj.getInt("numHeads");
                    else if (obj.has("num_heads")) artifact.numHeads = obj.getInt("num_heads");
                }
                if (artifact.numBlocks <= 0) {
                    if (obj.has("numBlocks")) artifact.numBlocks = obj.getInt("numBlocks");
                    else if (obj.has("num_blocks")) artifact.numBlocks = obj.getInt("num_blocks");
                    else if (obj.has("num_hidden_layers")) artifact.numBlocks = obj.getInt("num_hidden_layers");
                }
                if (artifact.dff <= 0) {
                    if (obj.has("dff")) artifact.dff = obj.getInt("dff");
                    else if (obj.has("intermediate_size")) artifact.dff = obj.getInt("intermediate_size");
                }
                if (artifact.maxSeqLen <= 0) {
                    if (obj.has("maxSeqLen")) artifact.maxSeqLen = obj.getInt("maxSeqLen");
                    else if (obj.has("max_seq_len")) artifact.maxSeqLen = obj.getInt("max_seq_len");
                }
                if (obj.has("temperature")) artifact.temperature = (float) obj.getDouble("temperature");
                if (obj.has("topP")) artifact.topP = (float) obj.getDouble("topP");
                else if (obj.has("top_p")) artifact.topP = (float) obj.getDouble("top_p");
                if (obj.has("topK")) artifact.topK = obj.getInt("topK");
                else if (obj.has("top_k")) artifact.topK = obj.getInt("top_k");
                if (obj.has("repeatPenalty")) artifact.repeatPenalty = (float) obj.getDouble("repeatPenalty");
                else if (obj.has("repeat_penalty")) artifact.repeatPenalty = (float) obj.getDouble("repeat_penalty");
            } catch (Exception ignored) {}
        }

        if (Files.exists(tokenizerPath)) {
            try {
                String jsonStr = Files.readString(tokenizerPath);
                Map<String, Integer> vocab = parseVocabFromJson(jsonStr);
                if (!vocab.isEmpty()) {
                    artifact.tokenToId = new LinkedHashMap<>(vocab);
                    artifact.idToToken = new LinkedHashMap<>();
                    for (Map.Entry<String, Integer> entry : vocab.entrySet()) {
                        artifact.idToToken.put(entry.getValue(), entry.getKey());
                    }
                }
            } catch (Exception ignored) {}
        }
        artifact.ensureSpecialTokens();

        if (artifact.vocabSize <= 0 && !artifact.idToToken.isEmpty()) {
            artifact.vocabSize = artifact.idToToken.size();
        }

        EvoLlmModel tempModel = new EvoLlmModel(artifact.vocabSize, artifact.dModel, artifact.numHeads, artifact.numBlocks, artifact.dff, artifact.maxSeqLen);
        try (DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(weightsPath.toFile())))) {
            List<Tensor> params = tempModel.parameters();
            for (Tensor p : params) {
                float[] data = p.getData();
                for (int i = 0; i < data.length; i++) {
                    data[i] = dis.readFloat();
                }
            }
        } catch (EOFException e) {
            throw new IOException("weights.bin is truncated or corrupt", e);
        }

        artifact.weightData.clear();
        artifact.weightShapes.clear();
        artifact.weightNames.clear();

        for (Tensor t : tempModel.parameters()) {
            float[] data = t.getData().clone();
            long[] shape = t.getShape().clone();
            artifact.weightData.add(data);
            artifact.weightShapes.add(shape);
            artifact.weightNames.add(artifact.generateWeightName(artifact.weightData.size() - 1));
        }

        return artifact;
    }

    private static Map<String, Integer> parseVocabFromJson(String jsonStr) {
        Map<String, Integer> vocab = new LinkedHashMap<>();
        if (jsonStr == null || jsonStr.trim().isEmpty()) {
            return vocab;
        }

        try {
            JSONObject obj = new JSONObject(jsonStr);
            if (obj.has("vocab")) {
                JSONObject vObj = obj.getJSONObject("vocab");
                for (Iterator<String> it = vObj.keys(); it.hasNext(); ) {
                    String key = it.next();
                    vocab.put(key, vObj.getInt(key));
                }
                if (!vocab.isEmpty()) return vocab;
            }
            if (obj.has("tokens")) {
                JSONArray tArr = obj.getJSONArray("tokens");
                for (int i = 0; i < tArr.length(); i++) {
                    vocab.put(tArr.getString(i), i);
                }
                if (!vocab.isEmpty()) return vocab;
            }
        } catch (Exception ignored) {}

        int vocabIdx = jsonStr.indexOf("\"vocab\"");
        if (vocabIdx == -1) return vocab;
        int braceStart = jsonStr.indexOf("{", vocabIdx);
        if (braceStart == -1) return vocab;

        int i = braceStart + 1;
        int len = jsonStr.length();
        while (i < len) {
            char c = jsonStr.charAt(i);
            if (c == '}') break;
            if (c == '"') {
                int tokenStart = i + 1;
                StringBuilder sb = new StringBuilder();
                int j = tokenStart;
                boolean escaped = false;
                while (j < len) {
                    char ch = jsonStr.charAt(j);
                    if (escaped) {
                        sb.append(ch);
                        escaped = false;
                    } else if (ch == '\\') {
                        escaped = true;
                    } else if (ch == '"') {
                        break;
                    } else {
                        sb.append(ch);
                    }
                    j++;
                }
                String token = sb.toString();
                int colon = jsonStr.indexOf(":", j);
                if (colon != -1) {
                    int k = colon + 1;
                    while (k < len && Character.isWhitespace(jsonStr.charAt(k))) k++;
                    int numStart = k;
                    while (k < len && (Character.isDigit(jsonStr.charAt(k)) || jsonStr.charAt(k) == '-')) k++;
                    if (k > numStart) {
                        try {
                            int id = Integer.parseInt(jsonStr.substring(numStart, k));
                            vocab.put(token, id);
                        } catch (NumberFormatException ignored) {}
                    }
                    i = k;
                    continue;
                }
            }
            i++;
        }
        return vocab;
    }

    private void saveVocabularyJson(Path path) throws IOException {
        Map<String, Object> vocabData = new LinkedHashMap<>();
        vocabData.put("vocab_size", vocabSize);
        vocabData.put("bos_token", "<s>");
        vocabData.put("eos_token", "</s>");
        vocabData.put("unk_token", "<unk>");
        vocabData.put("vocab", tokenToId);

        JSONObject json = new JSONObject(vocabData);
        Files.writeString(path, json.toString(2));
    }

    public static EvoModelArtifact load(Path path) throws IOException {
        if (path == null || !Files.exists(path)) {
            if (path != null && !path.toString().endsWith(".evo") && Files.exists(Paths.get(path.toString() + ".evo"))) {
                path = Paths.get(path.toString() + ".evo");
            } else {
                throw new FileNotFoundException("Model artifact path does not exist: " + path);
            }
        }

        if (Files.isDirectory(path)) {
            return loadFromDirectory(path);
        }

        EvoModelArtifact artifact = new EvoModelArtifact();

        try (FileInputStream fis = new FileInputStream(path.toFile());
             GZIPInputStream gzis = new GZIPInputStream(fis);
             DataInputStream dis = new DataInputStream(gzis)) {

            String magic = dis.readUTF();
            if (!"EVO_ARTIFACT_V1".equals(magic)) {
                throw new IOException("Invalid artifact format: " + magic);
            }

            artifact.modelName = dis.readUTF();
            artifact.modelVersion = dis.readUTF();
            artifact.createdAt = dis.readLong();

            artifact.vocabSize = dis.readInt();
            artifact.dModel = dis.readInt();
            artifact.numHeads = dis.readInt();
            artifact.numBlocks = dis.readInt();
            artifact.dff = dis.readInt();
            artifact.maxSeqLen = dis.readInt();

            artifact.temperature = dis.readFloat();
            artifact.topP = dis.readFloat();
            artifact.topK = dis.readInt();
            artifact.repeatPenalty = dis.readFloat();
            artifact.frequencyPenalty = dis.readFloat();
            artifact.presencePenalty = dis.readFloat();

            artifact.bosTokenId = dis.readInt();
            artifact.eosTokenId = dis.readInt();
            artifact.unkTokenId = dis.readInt();
            artifact.padTokenId = dis.readInt();

            int vocabSize = dis.readInt();
            artifact.idToToken = new LinkedHashMap<>();
            artifact.tokenToId = new LinkedHashMap<>();

            for (int i = 0; i < vocabSize; i++) {
                int id = dis.readInt();
                String token = dis.readUTF();
                artifact.idToToken.put(id, token);
                artifact.tokenToId.put(token, id);
            }

            int metaSize = dis.readInt();
            artifact.metadata = new HashMap<>();
            for (int i = 0; i < metaSize; i++) {
                String key = dis.readUTF();
                String value = dis.readUTF();
                artifact.metadata.put(key, value);
            }

            int weightCount = dis.readInt();
            artifact.weightData = new ArrayList<>(weightCount);
            artifact.weightShapes = new ArrayList<>(weightCount);
            artifact.weightNames = new ArrayList<>(weightCount);

            for (int i = 0; i < weightCount; i++) {
                String name = dis.readUTF();
                int shapeLen = dis.readInt();
                long[] shape = new long[shapeLen];
                for (int d = 0; d < shapeLen; d++) {
                    shape[d] = dis.readLong();
                }
                int dataLen = dis.readInt();
                float[] data = new float[dataLen];
                for (int j = 0; j < dataLen; j++) {
                    data[j] = dis.readFloat();
                }
                artifact.weightData.add(data);
                artifact.weightShapes.add(shape);
                artifact.weightNames.add(name);
            }
        }

        return artifact;
    }

    // ============ MODEL RECREATION ============
    public EvoLlmModel createModel() {
        EvoLlmModel model = new EvoLlmModel(vocabSize, dModel, numHeads, numBlocks, dff, maxSeqLen);

        List<Tensor> modelParams = model.parameters();
        if (modelParams.size() == weightData.size()) {
            for (int i = 0; i < modelParams.size(); i++) {
                Tensor t = modelParams.get(i);
                float[] data = weightData.get(i);
                if (data.length == t.getSize()) {
                    System.arraycopy(data, 0, t.getData(), 0, data.length);
                } else {
                    System.out.println("[Artifact] Warning: Weight " + i + " size mismatch: " +
                        data.length + " vs " + t.getSize());
                    int copyLen = (int) Math.min(data.length, t.getSize());
                    System.arraycopy(data, 0, t.getData(), 0, copyLen);
                }
            }
        }

        if (idToToken != null && !idToToken.isEmpty()) {
            model.getIdToToken().putAll(idToToken);
        }

        return model;
    }

    // ============ GETTERS / SETTERS ============
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }

    public int getVocabSize() { return vocabSize; }
    public int getDModel() { return dModel; }
    public int getNumHeads() { return numHeads; }
    public int getNumBlocks() { return numBlocks; }
    public int getDff() { return dff; }
    public int getMaxSeqLen() { return maxSeqLen; }

    public EvoLlmArchitecture getArchitectureConfig() {
        return new EvoLlmArchitecture(vocabSize, dModel, numHeads, numBlocks, dff, maxSeqLen);
    }
    public int getEmbeddingSize() { return dModel; }
    public int getLayers() { return numBlocks; }
    public int getHeads() { return numHeads; }

    public long getParameterCount() {
        long total = 0;
        if (weightData != null) {
            for (float[] w : weightData) {
                if (w != null) total += w.length;
            }
        }
        return total;
    }

    public List<Tensor> getWeights() {
        List<Tensor> tensors = new ArrayList<>();
        if (weightData != null && weightShapes != null) {
            for (int i = 0; i < weightData.size(); i++) {
                float[] data = weightData.get(i);
                long[] shape = i < weightShapes.size() ? weightShapes.get(i) : new long[] { data.length };
                SimpleTensor tensor = new SimpleTensor(shape);
                System.arraycopy(data, 0, tensor.getData(), 0, Math.min(data.length, tensor.getData().length));
                tensors.add(tensor);
            }
        }
        return tensors;
    }

    public Map<Integer, String> getIdToToken() { return idToToken; }
    public Map<String, Integer> getTokenizerVocab() { return tokenToId; }

    public int getBosTokenId() { return bosTokenId; }
    public int getEosTokenId() { return eosTokenId; }
    public int getUnkTokenId() { return unkTokenId; }
    public int getPadTokenId() { return padTokenId; }

    public float getTemperature() { return temperature; }
    public void setTemperature(float temperature) { this.temperature = temperature; }

    public float getTopP() { return topP; }
    public void setTopP(float topP) { this.topP = topP; }

    public int getTopK() { return topK; }
    public void setTopK(int topK) { this.topK = topK; }

    public float getRepeatPenalty() { return repeatPenalty; }
    public void setRepeatPenalty(float repeatPenalty) { this.repeatPenalty = repeatPenalty; }

    public float getFrequencyPenalty() { return frequencyPenalty; }
    public void setFrequencyPenalty(float frequencyPenalty) { this.frequencyPenalty = frequencyPenalty; }

    public float getPresencePenalty() { return presencePenalty; }
    public void setPresencePenalty(float presencePenalty) { this.presencePenalty = presencePenalty; }

    public Map<String, String> getMetadata() { return metadata; }

    public List<float[]> getWeightData() { return weightData; }
    public List<long[]> getWeightShapes() { return weightShapes; }
    public List<String> getWeightNames() { return weightNames; }
}
