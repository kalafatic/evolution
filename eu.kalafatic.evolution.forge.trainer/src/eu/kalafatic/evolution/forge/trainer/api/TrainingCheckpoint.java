package eu.kalafatic.evolution.forge.trainer.api;

import eu.kalafatic.evolution.forge.model.llm.EvoLlmArchitecture;
import eu.kalafatic.evolution.forge.model.llm.EvoLlmModel;
import eu.kalafatic.evolution.forge.math.api.Tensor;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

/**
 * Encapsulates complete, deterministic, and corruption-resistant training resumption state:
 * - Model weights and architecture descriptor
 * - Optimizer state: AdamW first moment (m), second moment (v), and step counter
 * - Training progression: 1-based completed epoch count, last training loss, validation loss, learning rate
 * - Run/session identity, random base seed, dataset fingerprint, and tokenizer vocabulary
 *
 * Employs atomic write-to-temp and atomic rename to guarantee integrity.
 */
public class TrainingCheckpoint implements Serializable {

    private static final long serialVersionUID = 2L;

    public static final String CHECKPOINT_MAGIC = "EVO_CHK_V2";

    private String runId;
    private String modelName;
    private int completedEpochs;
    private long totalOptimizerSteps;
    private float lastTrainLoss;
    private float lastValLoss;
    private float currentLr;
    private long baseSeed;
    private String datasetFingerprint;
    private long timestamp;

    // Architecture parameters
    private int vocabSize;
    private int dModel;
    private int numHeads;
    private int numBlocks;
    private int dff;
    private int maxSeqLen;

    // Model weight payloads (canonical order matching model.parameters())
    private List<float[]> modelWeights = new ArrayList<>();

    // Optimizer AdamW state: moment vectors per parameter tensor
    private List<float[]> adamWMoments = new ArrayList<>();
    private List<float[]> adamWVariances = new ArrayList<>();

    // Tokenizer vocabulary mapping
    private Map<String, Integer> tokenizerVocab = new LinkedHashMap<>();

    public TrainingCheckpoint() {
        this.timestamp = System.currentTimeMillis();
    }

    public static TrainingCheckpoint capture(
            String runId,
            String modelName,
            EvoLlmModel model,
            int completedEpochs,
            long optimizerSteps,
            float lastTrainLoss,
            float lastValLoss,
            float currentLr,
            long baseSeed,
            String datasetFingerprint,
            Map<String, float[]> optimizerM,
            Map<String, float[]> optimizerV,
            Map<String, Integer> vocab) {

        Objects.requireNonNull(model, "EvoLlmModel cannot be null");
        TrainingCheckpoint chk = new TrainingCheckpoint();
        chk.runId = runId != null ? runId : UUID.randomUUID().toString();
        chk.modelName = modelName != null ? modelName : "evo_model";
        chk.completedEpochs = completedEpochs;
        chk.totalOptimizerSteps = optimizerSteps;
        chk.lastTrainLoss = lastTrainLoss;
        chk.lastValLoss = lastValLoss;
        chk.currentLr = currentLr;
        chk.baseSeed = baseSeed;
        chk.datasetFingerprint = datasetFingerprint != null ? datasetFingerprint : "";
        chk.timestamp = System.currentTimeMillis();

        chk.vocabSize = model.getVocabSize();
        chk.dModel = model.getDModel();
        chk.numHeads = model.getNumHeads();
        chk.numBlocks = model.getNumBlocks();
        chk.dff = model.getDff();
        chk.maxSeqLen = model.getMaxSeqLen();

        List<Tensor> params = model.parameters();
        for (int i = 0; i < params.size(); i++) {
            Tensor p = params.get(i);
            chk.modelWeights.add(p.getData().clone());
            String paramKey = "p_" + i;
            if (optimizerM != null && optimizerM.containsKey(paramKey)) {
                chk.adamWMoments.add(optimizerM.get(paramKey).clone());
            } else {
                chk.adamWMoments.add(new float[p.getData().length]);
            }
            if (optimizerV != null && optimizerV.containsKey(paramKey)) {
                chk.adamWVariances.add(optimizerV.get(paramKey).clone());
            } else {
                chk.adamWVariances.add(new float[p.getData().length]);
            }
        }

        if (vocab != null) {
            chk.tokenizerVocab.putAll(vocab);
        }

        return chk;
    }

    public void restoreInto(EvoLlmModel targetModel, Map<String, float[]> outM, Map<String, float[]> outV) {
        validateCompatibility(targetModel);

        List<Tensor> params = targetModel.parameters();
        for (int i = 0; i < params.size(); i++) {
            Tensor p = params.get(i);
            float[] savedData = modelWeights.get(i);
            System.arraycopy(savedData, 0, p.getData(), 0, savedData.length);

            String paramKey = "p_" + i;
            if (outM != null && i < adamWMoments.size()) {
                outM.put(paramKey, adamWMoments.get(i).clone());
            }
            if (outV != null && i < adamWVariances.size()) {
                outV.put(paramKey, adamWVariances.get(i).clone());
            }
        }

        targetModel.getTrainingState().setEpoch(completedEpochs);
        targetModel.getTrainingState().setStep(totalOptimizerSteps);
        targetModel.getTrainingState().setLastLoss(lastTrainLoss);
        targetModel.getTrainingState().setLearningRate(currentLr);
    }

    public void validateCompatibility(EvoLlmModel targetModel) {
        Objects.requireNonNull(targetModel, "Target model cannot be null");
        if (targetModel.getVocabSize() != vocabSize ||
            targetModel.getDModel() != dModel ||
            targetModel.getNumHeads() != numHeads ||
            targetModel.getNumBlocks() != numBlocks ||
            targetModel.getDff() != dff) {
            throw new IllegalStateException(String.format(
                    "Incompatible checkpoint architecture: expected [vocab=%d, dModel=%d, heads=%d, blocks=%d, dff=%d], " +
                    "but target model has [vocab=%d, dModel=%d, heads=%d, blocks=%d, dff=%d]",
                    vocabSize, dModel, numHeads, numBlocks, dff,
                    targetModel.getVocabSize(), targetModel.getDModel(),
                    targetModel.getNumHeads(), targetModel.getNumBlocks(), targetModel.getDff()));
        }
        if (modelWeights.size() != targetModel.parameters().size()) {
            throw new IllegalStateException(String.format(
                    "Checkpoint parameter count mismatch: saved %d tensors, target model has %d tensors",
                    modelWeights.size(), targetModel.parameters().size()));
        }
    }

    public void saveAtomic(Path targetPath) throws IOException {
        Objects.requireNonNull(targetPath, "targetPath cannot be null");
        if (targetPath.getParent() != null) {
            Files.createDirectories(targetPath.getParent());
        }

        Path tempPath = targetPath.resolveSibling(targetPath.getFileName().toString() + ".tmp." + System.nanoTime());

        try (DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(tempPath.toFile())))) {
            dos.writeUTF(CHECKPOINT_MAGIC);
            dos.writeUTF(runId);
            dos.writeUTF(modelName);
            dos.writeInt(completedEpochs);
            dos.writeLong(totalOptimizerSteps);
            dos.writeFloat(lastTrainLoss);
            dos.writeFloat(lastValLoss);
            dos.writeFloat(currentLr);
            dos.writeLong(baseSeed);
            dos.writeUTF(datasetFingerprint);
            dos.writeLong(timestamp);

            dos.writeInt(vocabSize);
            dos.writeInt(dModel);
            dos.writeInt(numHeads);
            dos.writeInt(numBlocks);
            dos.writeInt(dff);
            dos.writeInt(maxSeqLen);

            // Weights
            dos.writeInt(modelWeights.size());
            for (float[] w : modelWeights) {
                dos.writeInt(w.length);
                for (float v : w) dos.writeFloat(v);
            }

            // Moments
            dos.writeInt(adamWMoments.size());
            for (float[] m : adamWMoments) {
                dos.writeInt(m.length);
                for (float v : m) dos.writeFloat(v);
            }
            dos.writeInt(adamWVariances.size());
            for (float[] v : adamWVariances) {
                dos.writeInt(v.length);
                for (float val : v) dos.writeFloat(val);
            }

            // Vocab
            dos.writeInt(tokenizerVocab.size());
            for (Map.Entry<String, Integer> entry : tokenizerVocab.entrySet()) {
                dos.writeUTF(entry.getKey());
                dos.writeInt(entry.getValue());
            }

            dos.flush();
        }

        try {
            Files.move(tempPath, targetPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            Files.move(tempPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public static TrainingCheckpoint load(Path checkpointPath) throws IOException {
        Objects.requireNonNull(checkpointPath, "checkpointPath cannot be null");
        if (!Files.exists(checkpointPath)) {
            throw new FileNotFoundException("Checkpoint file does not exist: " + checkpointPath);
        }

        try (DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(checkpointPath.toFile())))) {
            String magic = dis.readUTF();
            if (!CHECKPOINT_MAGIC.equals(magic)) {
                throw new IOException("Invalid checkpoint file header: expected " + CHECKPOINT_MAGIC + ", found " + magic);
            }

            TrainingCheckpoint chk = new TrainingCheckpoint();
            chk.runId = dis.readUTF();
            chk.modelName = dis.readUTF();
            chk.completedEpochs = dis.readInt();
            chk.totalOptimizerSteps = dis.readLong();
            chk.lastTrainLoss = dis.readFloat();
            chk.lastValLoss = dis.readFloat();
            chk.currentLr = dis.readFloat();
            chk.baseSeed = dis.readLong();
            chk.datasetFingerprint = dis.readUTF();
            chk.timestamp = dis.readLong();

            chk.vocabSize = dis.readInt();
            chk.dModel = dis.readInt();
            chk.numHeads = dis.readInt();
            chk.numBlocks = dis.readInt();
            chk.dff = dis.readInt();
            chk.maxSeqLen = dis.readInt();

            int weightsCount = dis.readInt();
            chk.modelWeights = new ArrayList<>(weightsCount);
            for (int i = 0; i < weightsCount; i++) {
                int len = dis.readInt();
                float[] data = new float[len];
                for (int j = 0; j < len; j++) data[j] = dis.readFloat();
                chk.modelWeights.add(data);
            }

            int mCount = dis.readInt();
            chk.adamWMoments = new ArrayList<>(mCount);
            for (int i = 0; i < mCount; i++) {
                int len = dis.readInt();
                float[] data = new float[len];
                for (int j = 0; j < len; j++) data[j] = dis.readFloat();
                chk.adamWMoments.add(data);
            }

            int vCount = dis.readInt();
            chk.adamWVariances = new ArrayList<>(vCount);
            for (int i = 0; i < vCount; i++) {
                int len = dis.readInt();
                float[] data = new float[len];
                for (int j = 0; j < len; j++) data[j] = dis.readFloat();
                chk.adamWVariances.add(data);
            }

            int vocabCount = dis.readInt();
            chk.tokenizerVocab = new LinkedHashMap<>(vocabCount);
            for (int i = 0; i < vocabCount; i++) {
                String token = dis.readUTF();
                int id = dis.readInt();
                chk.tokenizerVocab.put(token, id);
            }

            return chk;
        }
    }

    // Getters
    public String getRunId() { return runId; }
    public String getModelName() { return modelName; }
    public int getCompletedEpochs() { return completedEpochs; }
    public long getTotalOptimizerSteps() { return totalOptimizerSteps; }
    public float getLastTrainLoss() { return lastTrainLoss; }
    public float getLastValLoss() { return lastValLoss; }
    public float getCurrentLr() { return currentLr; }
    public long getBaseSeed() { return baseSeed; }
    public String getDatasetFingerprint() { return datasetFingerprint; }
    public long getTimestamp() { return timestamp; }

    public int getVocabSize() { return vocabSize; }
    public int getDModel() { return dModel; }
    public int getNumHeads() { return numHeads; }
    public int getNumBlocks() { return numBlocks; }
    public int getDff() { return dff; }
    public int getMaxSeqLen() { return maxSeqLen; }

    public List<float[]> getModelWeights() { return modelWeights; }
    public List<float[]> getAdamWMoments() { return adamWMoments; }
    public List<float[]> getAdamWVariances() { return adamWVariances; }
    public Map<String, Integer> getTokenizerVocab() { return tokenizerVocab; }
}
