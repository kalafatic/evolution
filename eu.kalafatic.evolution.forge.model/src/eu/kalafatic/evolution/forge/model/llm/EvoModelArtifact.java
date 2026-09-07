package eu.kalafatic.evolution.forge.model.llm;

import eu.kalafatic.evolution.forge.math.api.Tensor;
import eu.kalafatic.evolution.forge.math.core.SimpleTensor;
import eu.kalafatic.evolution.forge.model.protocol.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * EvoModelArtifact - Portable model artifact container wrapping EvoModelReader and EvoModelWriter.
 */
public class EvoModelArtifact {

    // ============ CORE FIELDS ============
    private String modelName;
    private String modelVersion = "2.0.0";
    private long createdAt;
    private String modelContentHash;

    // Architecture Descriptor
    private EvoArchitectureDescriptor architectureDescriptor;

    // Tokenizer Descriptor
    private EvoTokenizerDescriptor tokenizerDescriptor;

    // Tensor Manifest & Data
    private List<EvoTensorDescriptor> manifest = new ArrayList<>();
    private List<float[]> weightData = new ArrayList<>();
    private List<long[]> weightShapes = new ArrayList<>();
    private List<String> weightNames = new ArrayList<>();
    private List<EvoDtype> weightDtypes = new ArrayList<>();

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
        this.architectureDescriptor = new EvoArchitectureDescriptor();
        this.tokenizerDescriptor = new EvoTokenizerDescriptor();
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
        this.architectureDescriptor = new EvoArchitectureDescriptor(
                model.getVocabSize(),
                model.getDModel(),
                model.getNumHeads(),
                model.getNumBlocks(),
                model.getDff(),
                model.getMaxSeqLen()
        );

        this.tokenizerDescriptor = new EvoTokenizerDescriptor(
                model.getVocabSize(),
                tokenizerVocab
        );

        // Extract weights
        this.weightData.clear();
        this.weightShapes.clear();
        this.weightNames.clear();
        this.weightDtypes.clear();

        List<Tensor> params = model.parameters();
        for (int i = 0; i < params.size(); i++) {
            Tensor t = params.get(i);
            float[] data = t.getData().clone();
            long[] shape = t.getShape().clone();
            weightData.add(data);
            weightShapes.add(shape);
            weightNames.add(generateWeightName(i));
            weightDtypes.add(EvoDtype.F32);
        }

        this.metadata.put("created_at", String.valueOf(createdAt));
        this.metadata.put("model_type", "evo_llm");

        recalculateManifestAndHash();
    }

    public void recalculateManifestAndHash() {
        manifest.clear();
        for (int i = 0; i < weightData.size(); i++) {
            String name = weightNames.get(i);
            long[] shape = weightShapes.get(i);
            float[] data = weightData.get(i);
            EvoDtype dtype = weightDtypes.size() > i ? weightDtypes.get(i) : EvoDtype.F32;
            String checksum = EvoModelContentHash.calculateFloatArrayChecksum(data);

            EvoTensorDescriptor desc = new EvoTensorDescriptor(
                    i, name, dtype, shape, 0, 0, data.length * 4, data.length * 4, 1, checksum
            );
            manifest.add(desc);
        }

        this.modelContentHash = EvoModelContentHash.calculateContentHash(
                architectureDescriptor, tokenizerDescriptor, manifest, weightData
        );
    }

    private String generateWeightName(int index) {
        if (index == 0) return "token_embd.weight";

        int paramsPerBlock = 9;
        int offset = 1;

        for (int block = 0; block < architectureDescriptor.getNumBlocks(); block++) {
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

        int outputNormIdx = offset + architectureDescriptor.getNumBlocks() * paramsPerBlock;
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
        modelJson.put("vocabSize", getVocabSize());
        modelJson.put("dModel", getDModel());
        modelJson.put("numHeads", getNumHeads());
        modelJson.put("numBlocks", getNumBlocks());
        modelJson.put("dff", getDff());
        modelJson.put("maxSeqLen", getMaxSeqLen());
        Files.writeString(dir.resolve("model.json"), modelJson.toString(2));

        // 2. config.json
        JSONObject configJson = new JSONObject();
        configJson.put("model_name", modelName != null ? modelName : "evo_model");
        configJson.put("vocab_size", getVocabSize());
        configJson.put("vocabSize", getVocabSize());
        configJson.put("d_model", getDModel());
        configJson.put("dModel", getDModel());
        configJson.put("num_heads", getNumHeads());
        configJson.put("numHeads", getNumHeads());
        configJson.put("num_blocks", getNumBlocks());
        configJson.put("numBlocks", getNumBlocks());
        configJson.put("dff", getDff());
        configJson.put("max_seq_len", getMaxSeqLen());
        configJson.put("maxSeqLen", getMaxSeqLen());
        configJson.put("temperature", temperature);
        configJson.put("top_p", topP);
        configJson.put("topK", topK);
        configJson.put("top_k", topK);
        configJson.put("repeat_penalty", repeatPenalty);
        Files.writeString(dir.resolve("config.json"), configJson.toString(2));

        // 3. tokenizer.json
        JSONObject tokenizerJson = new JSONObject();
        tokenizerJson.put("vocab_size", getVocabSize());
        tokenizerJson.put("bos_token", "<s>");
        tokenizerJson.put("eos_token", "</s>");
        tokenizerJson.put("unk_token", "<unk>");
        JSONObject vocabObj = new JSONObject();
        if (tokenizerDescriptor.getTokenToId() != null) {
            for (Map.Entry<String, Integer> entry : tokenizerDescriptor.getTokenToId().entrySet()) {
                vocabObj.put(entry.getKey(), entry.getValue());
            }
        }
        tokenizerJson.put("vocab", vocabObj);
        if (tokenizerDescriptor.getIdToToken() != null) {
            JSONArray tokensArr = new JSONArray();
            for (Map.Entry<Integer, String> entry : tokenizerDescriptor.getIdToToken().entrySet()) {
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

        // 5. Save binary .evo file
        String evoName = (modelName != null && !modelName.isEmpty()) ? modelName + ".evo" : "model.evo";
        saveToFile(dir.resolve(evoName));
    }

    public void saveToFile(Path path) throws IOException {
        recalculateManifestAndHash();
        EvoModelWriter writer = new EvoModelWriter();
        writer.writeModel(
                path,
                modelName,
                architectureDescriptor,
                tokenizerDescriptor,
                weightNames,
                weightShapes,
                weightDtypes,
                weightData,
                metadata
        );

        Path vocabJsonPath = path.getParent() != null ?
                path.getParent().resolve(path.getFileName().toString().replace(".evo", "_vocab.json")) :
                Paths.get(path.toString().replace(".evo", "_vocab.json"));
        saveVocabularyJson(vocabJsonPath);
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

        EvoModelReader reader = new EvoModelReader();
        reader.read(path);

        EvoModelArtifact artifact = new EvoModelArtifact();
        artifact.modelName = reader.getModelName();
        artifact.createdAt = reader.getCreatedAt();
        artifact.modelContentHash = reader.getModelContentHash();

        artifact.architectureDescriptor = reader.getArchitecture();
        artifact.tokenizerDescriptor = reader.getTokenizer();

        artifact.metadata.putAll(reader.getMetadata());

        artifact.manifest = new ArrayList<>(reader.getManifest());
        artifact.weightData = new ArrayList<>(reader.getTensorDataPayloads());

        artifact.weightNames.clear();
        artifact.weightShapes.clear();
        artifact.weightDtypes.clear();

        for (EvoTensorDescriptor desc : reader.getManifest()) {
            artifact.weightNames.add(desc.getCanonicalName());
            artifact.weightShapes.add(desc.getShape());
            artifact.weightDtypes.add(desc.getDtype());
        }

        return artifact;
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

        int vocabSize = 0, dModel = 0, numHeads = 0, numBlocks = 0, dff = 0, maxSeqLen = 0;

        if (Files.exists(manifestPath)) {
            try {
                String jsonStr = Files.readString(manifestPath);
                JSONObject obj = new JSONObject(jsonStr);
                if (obj.has("modelName")) artifact.modelName = obj.optString("modelName", artifact.modelName);
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
                JSONObject obj = new JSONObject(jsonStr);
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

        Map<String, Integer> tokenToId = new LinkedHashMap<>();
        if (Files.exists(tokenizerPath)) {
            try {
                String jsonStr = Files.readString(tokenizerPath);
                tokenToId = parseVocabFromJson(jsonStr);
            } catch (Exception ignored) {}
        }

        if (vocabSize <= 0 && !tokenToId.isEmpty()) {
            vocabSize = tokenToId.size();
        }

        artifact.architectureDescriptor = new EvoArchitectureDescriptor(
                vocabSize, dModel, numHeads, numBlocks, dff, maxSeqLen
        );
        artifact.tokenizerDescriptor = new EvoTokenizerDescriptor(vocabSize, tokenToId);

        EvoLlmModel tempModel = new EvoLlmModel(vocabSize, dModel, numHeads, numBlocks, dff, maxSeqLen);
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
        artifact.weightDtypes.clear();

        for (int i = 0; i < tempModel.parameters().size(); i++) {
            Tensor t = tempModel.parameters().get(i);
            float[] data = t.getData().clone();
            long[] shape = t.getShape().clone();
            artifact.weightData.add(data);
            artifact.weightShapes.add(shape);
            artifact.weightNames.add(artifact.generateWeightName(i));
            artifact.weightDtypes.add(EvoDtype.F32);
        }

        artifact.recalculateManifestAndHash();
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
        } catch (Exception ignored) {}
        return vocab;
    }

    private void saveVocabularyJson(Path path) throws IOException {
        Map<String, Object> vocabData = new LinkedHashMap<>();
        vocabData.put("vocab_size", getVocabSize());
        vocabData.put("bos_token", "<s>");
        vocabData.put("eos_token", "</s>");
        vocabData.put("unk_token", "<unk>");
        vocabData.put("vocab", tokenizerDescriptor.getTokenToId());

        JSONObject json = new JSONObject(vocabData);
        Files.writeString(path, json.toString(2));
    }

    // ============ MODEL RECREATION ============
    public EvoLlmModel createModel() {
        EvoLlmModel model = new EvoLlmModel(
                getVocabSize(),
                getDModel(),
                getNumHeads(),
                getNumBlocks(),
                getDff(),
                getMaxSeqLen()
        );

        List<Tensor> modelParams = model.parameters();
        if (modelParams.size() == weightData.size()) {
            for (int i = 0; i < modelParams.size(); i++) {
                Tensor t = modelParams.get(i);
                float[] data = weightData.get(i);
                if (data.length == t.getSize()) {
                    System.arraycopy(data, 0, t.getData(), 0, data.length);
                } else {
                    int copyLen = (int) Math.min(data.length, t.getSize());
                    System.arraycopy(data, 0, t.getData(), 0, copyLen);
                }
            }
        }

        if (getIdToToken() != null && !getIdToToken().isEmpty()) {
            model.getIdToToken().putAll(getIdToToken());
        }

        return model;
    }

    // ============ GETTERS / SETTERS ============
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }

    public String getModelContentHash() { return modelContentHash; }

    public int getVocabSize() { return architectureDescriptor != null ? architectureDescriptor.getVocabSize() : 0; }
    public int getDModel() { return architectureDescriptor != null ? architectureDescriptor.getDModel() : 0; }
    public int getNumHeads() { return architectureDescriptor != null ? architectureDescriptor.getNumHeads() : 0; }
    public int getNumBlocks() { return architectureDescriptor != null ? architectureDescriptor.getNumBlocks() : 0; }
    public int getDff() { return architectureDescriptor != null ? architectureDescriptor.getDff() : 0; }
    public int getMaxSeqLen() { return architectureDescriptor != null ? architectureDescriptor.getMaxSeqLen() : 0; }

    public EvoLlmArchitecture getArchitectureConfig() {
        return new EvoLlmArchitecture(getVocabSize(), getDModel(), getNumHeads(), getNumBlocks(), getDff(), getMaxSeqLen());
    }

    public EvoArchitectureDescriptor getArchitectureDescriptor() { return architectureDescriptor; }
    public EvoTokenizerDescriptor getTokenizerDescriptor() { return tokenizerDescriptor; }

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

    public Map<Integer, String> getIdToToken() { return tokenizerDescriptor != null ? tokenizerDescriptor.getIdToToken() : Collections.emptyMap(); }
    public Map<String, Integer> getTokenizerVocab() { return tokenizerDescriptor != null ? tokenizerDescriptor.getTokenToId() : Collections.emptyMap(); }

    public int getBosTokenId() { return tokenizerDescriptor != null ? tokenizerDescriptor.getBosTokenId() : 1; }
    public int getEosTokenId() { return tokenizerDescriptor != null ? tokenizerDescriptor.getEosTokenId() : 2; }
    public int getUnkTokenId() { return tokenizerDescriptor != null ? tokenizerDescriptor.getUnkTokenId() : 0; }
    public int getPadTokenId() { return tokenizerDescriptor != null ? tokenizerDescriptor.getPadTokenId() : 0; }

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

    public List<EvoTensorDescriptor> getManifest() { return manifest; }
    public List<float[]> getWeightData() { return weightData; }
    public List<long[]> getWeightShapes() { return weightShapes; }
    public List<String> getWeightNames() { return weightNames; }
}
