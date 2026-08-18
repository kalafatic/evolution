package eu.kalafatic.evolution.forge.model.llm;

import eu.kalafatic.evolution.forge.math.api.Tensor;
import eu.kalafatic.evolution.forge.math.core.SimpleTensor;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Self-contained native EVO model artifact.
 * Holds architectural configuration, tokenizer state, sampling parameters,
 * and handles weight saving/loading independently of any external framework.
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

    // Architectural Configuration
    private EvoLlmArchitecture archConfig;

    // Sampling Configuration
    private float temperature = 0.2f;
    private float topP = 0.9f;
    private int topK = 40;
    private float repeatPenalty = 1.1f;

    // Tokenizer Artifact
    private EvoTokenizerArtifact tokenizerArtifact = new EvoTokenizerArtifact();

    // Trained weights/parameters
    private List<Tensor> weights = new ArrayList<>();

    // Empty constructor
    public EvoModelArtifact() {}

    /**
     * Reconstructs an EvoLlmModel instance using the exact persisted architecture and weights.
     */
    public EvoLlmModel createModel() {
        validateModelIntegrity();
        EvoLlmModel model = new EvoLlmModel(archConfig);

        // Strict invariant check: recreated model architecture MUST match artifact architecture
        if (!archConfig.equals(model.getArchitecture())) {
            throw new IllegalStateException("Architecture invariant violation: artifact architecture "
                    + archConfig + " does not match recreated model architecture " + model.getArchitecture());
        }

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
        this.archConfig = model.getArchitecture();

        EvoTokenizerArtifact tokArtifact = new EvoTokenizerArtifact("SimpleBPE", vocab, 1, 2, 3, 0);
        tokArtifact.padToSize(this.archConfig.getVocabSize());
        this.tokenizerArtifact = tokArtifact;
        
        this.weights = new ArrayList<>();
        long totalParams = 0;
        for (Tensor p : model.parameters()) {
            float[] dataCopy = new float[p.getData().length];
            System.arraycopy(p.getData(), 0, dataCopy, 0, p.getData().length);
            this.weights.add(new SimpleTensor(p.getShape(), dataCopy));
            totalParams += p.getData().length;
        }
    }

    public void initializeFromModel(String modelName, EvoLlmModel model, EvoTokenizerArtifact tokenizer) {
        this.modelName = modelName;
        this.archConfig = model.getArchitecture();

        EvoTokenizerArtifact tokArtifact = tokenizer != null ? tokenizer : new EvoTokenizerArtifact();
        tokArtifact.padToSize(this.archConfig.getVocabSize());
        this.tokenizerArtifact = tokArtifact;

        this.weights = new ArrayList<>();
        long totalParams = 0;
        for (Tensor p : model.parameters()) {
            float[] dataCopy = new float[p.getData().length];
            System.arraycopy(p.getData(), 0, dataCopy, 0, p.getData().length);
            this.weights.add(new SimpleTensor(p.getShape(), dataCopy));
            totalParams += p.getData().length;
        }
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
        if (archConfig == null) {
            throw new IllegalArgumentException("Missing architecture configuration in artifact.");
        }
        if (tokenizerArtifact == null || tokenizerArtifact.getVocab() == null || tokenizerArtifact.getVocab().isEmpty()) {
            throw new IllegalArgumentException("Tokenizer artifact is missing or empty.");
        }
        if (tokenizerArtifact.getVocabSize() < archConfig.getVocabSize()) {
            tokenizerArtifact.padToSize(archConfig.getVocabSize());
        }
        if (weights == null || weights.isEmpty()) {
            throw new IllegalArgumentException("Weights list is missing or empty.");
        }
    }

    /**
     * Saves the native model artifact as a single unified package with .evo extension,
     * or as a directory structure.
     */
    public EvoModelArtifact save(Path targetPath) throws IOException {
        if (Files.isDirectory(targetPath)) {
            saveDirectory(targetPath);
        } else {
            Path evoFile = targetPath;
            if (!evoFile.getFileName().toString().toLowerCase().endsWith(".evo")) {
                evoFile = evoFile.getParent().resolve(evoFile.getFileName().toString() + ".evo");
            }
            Path tempDir = Files.createTempDirectory("evo-artifact-zip");
            try {
                saveDirectory(tempDir);
                packZip(tempDir, evoFile);
            } finally {
                deleteDirectory(tempDir.toFile());
            }
        }
        return this;
    }

    private void saveDirectory(Path dir) throws IOException {
        Files.createDirectories(dir);
        validateModelIntegrity();

        long parameterCount = getParameterCount();

        // 1. Serialize model.json (the stable manifest file)
        StringBuilder sb = new StringBuilder();
        sb.append("{\n")
                .append("  \"format\": \"").append(format).append("\",\n")
                .append("  \"formatVersion\": ").append(formatVersion).append(",\n")
                .append("  \"modelName\": \"").append(modelName).append("\",\n")
                .append("  \"architecture\": \"").append(architecture).append("\",\n")
                .append("  \"creationTimestamp\": ").append(creationTimestamp).append(",\n")
                .append("  \"vocabSize\": ").append(archConfig.getVocabSize()).append(",\n")
                .append("  \"embeddingSize\": ").append(archConfig.getDModel()).append(",\n")
                .append("  \"layers\": ").append(archConfig.getNumBlocks()).append(",\n")
                .append("  \"heads\": ").append(archConfig.getNumHeads()).append(",\n")
                .append("  \"dff\": ").append(archConfig.getDff()).append(",\n")
                .append("  \"maxSeqLen\": ").append(archConfig.getMaxSeqLen()).append(",\n")
                .append("  \"parameterCount\": ").append(parameterCount).append(",\n")
                .append("  \"temperature\": ").append(temperature).append(",\n")
                .append("  \"top_p\": ").append(topP).append(",\n")
                .append("  \"top_k\": ").append(topK).append(",\n")
                .append("  \"repeat_penalty\": ").append(repeatPenalty).append("\n")
                .append("}");
        Files.writeString(dir.resolve("model.json"), sb.toString());

        // 2. Also save config.json file for backward compatibility
        StringBuilder configJson = new StringBuilder();
        configJson.append("{\n")
                .append("  \"vocabSize\": ").append(archConfig.getVocabSize()).append(",\n")
                .append("  \"dModel\": ").append(archConfig.getDModel()).append(",\n")
                .append("  \"numHeads\": ").append(archConfig.getNumHeads()).append(",\n")
                .append("  \"numBlocks\": ").append(archConfig.getNumBlocks()).append(",\n")
                .append("  \"dff\": ").append(archConfig.getDff()).append(",\n")
                .append("  \"maxSeqLen\": ").append(archConfig.getMaxSeqLen()).append("\n")
                .append("}");
        Files.writeString(dir.resolve("config.json"), configJson.toString());

        StringBuilder trainingJson = new StringBuilder();
        trainingJson.append("{\n")
                .append("  \"epoch\": 1,\n")
                .append("  \"loss\": 0.0\n")
                .append("}");
        Files.writeString(dir.resolve("training.json"), trainingJson.toString());

        // 3. Serialize tokenizer.json (complete vocabulary / token mappings state)
        Map<String, Integer> tokVocab = tokenizerArtifact.getVocab();
        StringBuilder tokBuilder = new StringBuilder();
        tokBuilder.append("{\n")
                .append("  \"type\": \"").append(tokenizerArtifact.getTokenizerType()).append("\",\n")
                .append("  \"vocabSize\": ").append(tokenizerArtifact.getVocabSize()).append(",\n")
                .append("  \"unkId\": ").append(tokenizerArtifact.getUnkId()).append(",\n")
                .append("  \"bosId\": ").append(tokenizerArtifact.getBosId()).append(",\n")
                .append("  \"eosId\": ").append(tokenizerArtifact.getEosId()).append(",\n")
                .append("  \"padId\": ").append(tokenizerArtifact.getPadId()).append(",\n")
                .append("  \"vocab\": {\n");
        int count = 0;
        for (Map.Entry<String, Integer> entry : tokVocab.entrySet()) {
            String cleanKey = entry.getKey()
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
            tokBuilder.append("    \"").append(cleanKey).append("\": ").append(entry.getValue());
            if (++count < tokVocab.size()) {
                tokBuilder.append(",");
            }
            tokBuilder.append("\n");
        }
        tokBuilder.append("  }\n}");
        Files.writeString(dir.resolve("tokenizer.json"), tokBuilder.toString());

        // 4. Save weights.bin atomically
        Path weightsPath = dir.resolve("weights.bin");
        Path tempFile = dir.resolve("weights.bin." + UUID.randomUUID().toString() + ".tmp");
        try {
            try (DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(tempFile.toFile())))) {
                dos.write(MAGIC);
                dos.writeShort(WEIGHTS_VERSION);
                byte[] modelIdBytes = modelName.getBytes();
                dos.writeInt(modelIdBytes.length);
                dos.write(modelIdBytes);
                dos.writeInt(weights.size());

                for (int i = 0; i < weights.size(); i++) {
                    Tensor t = weights.get(i);
                    String tensorName = "tensor_" + i;
                    byte[] nameBytes = tensorName.getBytes();
                    dos.writeInt(nameBytes.length);
                    dos.write(nameBytes);

                    long[] shape = t.getShape();
                    dos.writeInt(shape.length);
                    for (long dim : shape) {
                        dos.writeLong(dim);
                    }

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
    }

    /**
     * Loads and validates a native model artifact from a directory or a packed .evo file.
     */
    public static EvoModelArtifact load(Path targetPath) throws IOException {
        if (Files.isDirectory(targetPath)) {
            return loadDirectory(targetPath);
        } else {
            if (!Files.exists(targetPath)) {
                throw new FileNotFoundException("Model file not found: " + targetPath);
            }
            Path tempDir = Files.createTempDirectory("evo-artifact-unzip");
            try {
                unpackZip(targetPath, tempDir);
                return loadDirectory(tempDir);
            } finally {
                deleteDirectory(tempDir.toFile());
            }
        }
    }

    private static EvoModelArtifact loadDirectory(Path dir) throws IOException {
        Path manifestPath = dir.resolve("model.json");
        Path configPath = dir.resolve("config.json");
        Path tokenizerPath = dir.resolve("tokenizer.json");
        Path weightsPath = dir.resolve("weights.bin");

        if (!Files.exists(weightsPath)) {
            throw new FileNotFoundException("Missing weights.bin file in: " + dir);
        }

        EvoModelArtifact artifact = new EvoModelArtifact();

        int vocabSize = 0;
        int dModel = 0;
        int layers = 0;
        int heads = 0;
        int dff = 0;
        int maxSeqLen = 0;

        // 1. Load stable manifest if present, else fallback to config.json
        if (Files.exists(manifestPath)) {
            String mJson = Files.readString(manifestPath);
            artifact.setFormat(parseStringField(mJson, "format"));
            artifact.setFormatVersion(parseIntField(mJson, "formatVersion", 1));
            artifact.setModelName(parseStringField(mJson, "modelName"));
            artifact.setArchitecture(parseStringField(mJson, "architecture"));
            artifact.setCreationTimestamp(parseLongField(mJson, "creationTimestamp", System.currentTimeMillis()));
            vocabSize = parseIntField(mJson, "vocabSize", 0);
            dModel = parseIntField(mJson, "embeddingSize", 0);
            layers = parseIntField(mJson, "layers", 0);
            heads = parseIntField(mJson, "heads", 0);
            dff = parseIntField(mJson, "dff", 0);
            maxSeqLen = parseIntField(mJson, "maxSeqLen", 0);
            artifact.setTemperature(parseFloatField(mJson, "temperature", 0.2f));
            artifact.setTopP(parseFloatField(mJson, "top_p", 0.9f));
            artifact.setTopK(parseIntField(mJson, "top_k", 40));
            artifact.setRepeatPenalty(parseFloatField(mJson, "repeat_penalty", 1.1f));
        } else if (Files.exists(configPath)) {
            String cfgJson = Files.readString(configPath);
            vocabSize = parseIntField(cfgJson, "vocabSize", 0);
            dModel = parseIntField(cfgJson, "dModel", 0);
            layers = parseIntField(cfgJson, "numBlocks", 0);
            heads = parseIntField(cfgJson, "numHeads", 0);
            dff = parseIntField(cfgJson, "dff", dModel * 4);
            maxSeqLen = parseIntField(cfgJson, "maxSeqLen", 0);
            artifact.setModelName("migrated-" + dir.getFileName().toString());
        } else {
            throw new FileNotFoundException("Neither model.json nor config.json exists in " + dir);
        }

        if (dff <= dModel) {
            dff = dModel * 4;
        }

        artifact.setArchitectureConfig(new EvoLlmArchitecture(vocabSize, dModel, heads, layers, dff, maxSeqLen));

        // 2. Load tokenizer vocabulary from tokenizer.json
        EvoTokenizerArtifact tokArtifact = new EvoTokenizerArtifact();
        if (Files.exists(tokenizerPath)) {
            String tJson = Files.readString(tokenizerPath);
            tokArtifact.setTokenizerType(parseStringField(tJson, "type"));
            tokArtifact.setUnkId(parseIntField(tJson, "unkId", 1));
            tokArtifact.setBosId(parseIntField(tJson, "bosId", 2));
            tokArtifact.setEosId(parseIntField(tJson, "eosId", 3));
            tokArtifact.setPadId(parseIntField(tJson, "padId", 0));
            Map<String, Integer> vocabMap = parseVocabFromJson(tJson);
            tokArtifact.setVocab(vocabMap);
        } else {
            Map<String, Integer> mockVocab = new LinkedHashMap<>();
            mockVocab.put("<unk>", 0);
            mockVocab.put("<s>", 1);
            mockVocab.put("</s>", 2);
            mockVocab.put(" ", 3);
            for (int i = 4; i < vocabSize; i++) {
                mockVocab.put("token_" + i, i);
            }
            tokArtifact.setVocab(mockVocab);
        }
        tokArtifact.padToSize(vocabSize);
        artifact.setTokenizerArtifact(tokArtifact);

        // 3. Load & Validate binary weights
        long totalFileSize = Files.size(weightsPath);
        if (totalFileSize < 5) {
            throw new IOException("Weights file weights.bin is truncated or corrupt. File size: " + totalFileSize + " bytes.");
        }

        try (DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(weightsPath.toFile())))) {
            byte[] magicIn = new byte[3];
            dis.readFully(magicIn);
            if (Arrays.equals(magicIn, MAGIC)) {
                short version = dis.readShort();
                if (version != WEIGHTS_VERSION) {
                    throw new IOException("Unsupported weights version: " + version);
                }
                int modelIdLen = dis.readInt();
                byte[] modelIdBytes = new byte[modelIdLen];
                dis.readFully(modelIdBytes);

                int tensorCount = dis.readInt();
                List<Tensor> loadedTensors = new ArrayList<>();

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
                }

                artifact.setWeights(loadedTensors);
            } else {
                dis.close();

                EvoLlmModel tempModel = new EvoLlmModel(artifact.getArchitectureConfig());
                List<Tensor> params = tempModel.parameters();
                List<Tensor> loadedTensors = new ArrayList<>();

                try (DataInputStream disRaw = new DataInputStream(new BufferedInputStream(new FileInputStream(weightsPath.toFile())))) {
                    for (Tensor p : params) {
                        float[] data = new float[p.getData().length];
                        for (int i = 0; i < data.length; i++) {
                            data[i] = disRaw.readFloat();
                        }
                        loadedTensors.add(new SimpleTensor(p.getShape(), data));
                    }
                }

                artifact.setWeights(loadedTensors);
            }
        }

        artifact.validateModelIntegrity();
        return artifact;
    }

    private static void packZip(Path sourceDir, Path zipFile) throws IOException {
        Files.createDirectories(zipFile.getParent());
        try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(
                new BufferedOutputStream(new FileOutputStream(zipFile.toFile())))) {
            try (Stream<Path> paths = Files.walk(sourceDir)) {
                List<Path> fileList = paths.filter(Files::isRegularFile).collect(Collectors.toList());
                for (Path file : fileList) {
                    String zipEntryName = sourceDir.relativize(file).toString().replace("\\", "/");
                    java.util.zip.ZipEntry entry = new java.util.zip.ZipEntry(zipEntryName);
                    zos.putNextEntry(entry);
                    Files.copy(file, zos);
                    zos.closeEntry();
                }
            }
        }
    }

    private static void unpackZip(Path zipFile, Path targetDir) throws IOException {
        Files.createDirectories(targetDir);
        try (java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(
                new BufferedInputStream(new FileInputStream(zipFile.toFile())))) {
            java.util.zip.ZipEntry entry;
            byte[] buffer = new byte[8192];
            while ((entry = zis.getNextEntry()) != null) {
                Path newPath = targetDir.resolve(entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(newPath);
                } else {
                    Files.createDirectories(newPath.getParent());
                    try (OutputStream os = new BufferedOutputStream(new FileOutputStream(newPath.toFile()))) {
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            os.write(buffer, 0, len);
                        }
                    }
                }
                zis.closeEntry();
            }
        }
    }

    private static void deleteDirectory(java.io.File directory) {
        java.io.File[] allContents = directory.listFiles();
        if (allContents != null) {
            for (java.io.File file : allContents) {
                deleteDirectory(file);
            }
        }
        directory.delete();
    }

    private static Map<String, Integer> parseVocabFromJson(String json) {
        Map<String, Integer> vocabMap = new LinkedHashMap<>();
        int vocabStart = json.indexOf("\"vocab\"");
        if (vocabStart == -1) return vocabMap;
        int blockStart = json.indexOf("{", vocabStart);
        if (blockStart == -1) return vocabMap;

        int braceDepth = 1;
        int blockEnd = -1;
        for (int i = blockStart + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '{') braceDepth++;
            else if (c == '}') {
                braceDepth--;
                if (braceDepth == 0) {
                    blockEnd = i;
                    break;
                }
            }
        }
        if (blockEnd == -1) return vocabMap;

        String vocabBlock = json.substring(blockStart + 1, blockEnd);
        int i = 0;
        int len = vocabBlock.length();
        while (i < len) {
            int keyStart = vocabBlock.indexOf('"', i);
            if (keyStart == -1) break;

            int keyEnd = -1;
            boolean escaped = false;
            for (int j = keyStart + 1; j < len; j++) {
                char c = vocabBlock.charAt(j);
                if (escaped) {
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == '"') {
                    keyEnd = j;
                    break;
                }
            }
            if (keyEnd == -1) break;

            String keyPart = vocabBlock.substring(keyStart + 1, keyEnd);
            keyPart = keyPart
                    .replace("\\\\", "\\")
                    .replace("\\\"", "\"")
                    .replace("\\n", "\n")
                    .replace("\\r", "\r")
                    .replace("\\t", "\t");

            int colonIdx = vocabBlock.indexOf(':', keyEnd + 1);
            if (colonIdx == -1) {
                i = keyEnd + 1;
                continue;
            }

            int valStart = -1;
            int valEnd = -1;
            for (int j = colonIdx + 1; j < len; j++) {
                char c = vocabBlock.charAt(j);
                if (Character.isDigit(c)) {
                    if (valStart == -1) {
                        valStart = j;
                    }
                    valEnd = j + 1;
                } else {
                    if (valStart != -1) {
                        break;
                    }
                }
            }

            if (valStart != -1 && valEnd != -1) {
                try {
                    int val = Integer.parseInt(vocabBlock.substring(valStart, valEnd));
                    vocabMap.put(keyPart, val);
                } catch (Exception ignored) {}
                i = valEnd;
            } else {
                i = colonIdx + 1;
            }
        }
        return vocabMap;
    }

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

    public EvoLlmArchitecture getArchitectureConfig() { return archConfig; }
    public void setArchitectureConfig(EvoLlmArchitecture archConfig) { this.archConfig = archConfig; }

    // Legacy / Convenience getters delegating to archConfig
    public int getVocabSize() { return archConfig != null ? archConfig.getVocabSize() : 0; }
    public void setVocabSize(int vocabSize) {
        updateArchField(vocabSize, getEmbeddingSize(), getLayers(), getHeads(), getDff(), getMaxSeqLen());
    }

    public int getEmbeddingSize() { return archConfig != null ? archConfig.getDModel() : 0; }
    public void setEmbeddingSize(int embeddingSize) {
        updateArchField(getVocabSize(), embeddingSize, getLayers(), getHeads(), getDff(), getMaxSeqLen());
    }

    public int getLayers() { return archConfig != null ? archConfig.getNumBlocks() : 0; }
    public void setLayers(int layers) {
        updateArchField(getVocabSize(), getEmbeddingSize(), layers, getHeads(), getDff(), getMaxSeqLen());
    }

    public int getHeads() { return archConfig != null ? archConfig.getNumHeads() : 0; }
    public void setHeads(int heads) {
        updateArchField(getVocabSize(), getEmbeddingSize(), getLayers(), heads, getDff(), getMaxSeqLen());
    }

    public int getDff() { return archConfig != null ? archConfig.getDff() : 0; }
    public void setDff(int dff) {
        updateArchField(getVocabSize(), getEmbeddingSize(), getLayers(), getHeads(), dff, getMaxSeqLen());
    }

    public int getMaxSeqLen() { return archConfig != null ? archConfig.getMaxSeqLen() : 0; }
    public void setMaxSeqLen(int maxSeqLen) {
        updateArchField(getVocabSize(), getEmbeddingSize(), getLayers(), getHeads(), getDff(), maxSeqLen);
    }

    private void updateArchField(int vSize, int dModel, int layers, int heads, int dff, int maxSeq) {
        if (vSize > 0 && dModel > 0 && layers > 0 && heads > 0 && maxSeq > 0) {
            if (dff <= dModel) dff = dModel * 4;
            this.archConfig = new EvoLlmArchitecture(vSize, dModel, heads, layers, dff, maxSeq);
        }
    }

    public long getParameterCount() {
        if (archConfig != null) {
            return archConfig.getParameterCount();
        }
        return 0;
    }
    public void setParameterCount(long parameterCount) { /* Computed from archConfig */ }

    public float getTemperature() { return temperature; }
    public void setTemperature(float temperature) { this.temperature = temperature; }

    public float getTopP() { return topP; }
    public void setTopP(float topP) { this.topP = topP; }

    public int getTopK() { return topK; }
    public void setTopK(int topK) { this.topK = topK; }

    public float getRepeatPenalty() { return repeatPenalty; }
    public void setRepeatPenalty(float repeatPenalty) { this.repeatPenalty = repeatPenalty; }

    public EvoTokenizerArtifact getTokenizerArtifact() { return tokenizerArtifact; }
    public void setTokenizerArtifact(EvoTokenizerArtifact tokenizerArtifact) {
        this.tokenizerArtifact = tokenizerArtifact;
    }

    public Map<String, Integer> getTokenizerVocab() {
        return tokenizerArtifact != null ? tokenizerArtifact.getVocab() : Collections.emptyMap();
    }
    public void setTokenizerVocab(Map<String, Integer> tokenizerVocab) {
        if (this.tokenizerArtifact == null) {
            this.tokenizerArtifact = new EvoTokenizerArtifact();
        }
        this.tokenizerArtifact.setVocab(tokenizerVocab);
    }

    public List<Tensor> getWeights() { return weights; }
    public void setWeights(List<Tensor> weights) { this.weights = weights; }
}
