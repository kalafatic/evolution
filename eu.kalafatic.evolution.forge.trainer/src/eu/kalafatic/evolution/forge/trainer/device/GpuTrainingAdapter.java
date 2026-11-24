package eu.kalafatic.evolution.forge.trainer.device;

import eu.kalafatic.evolution.forge.data.api.TrainingSample;
import eu.kalafatic.evolution.forge.math.api.Tensor;
import eu.kalafatic.evolution.forge.model.llm.EvoLlmModel;
import eu.kalafatic.evolution.forge.trainer.impl.llm.EvoLlmTrainer;
import eu.kalafatic.evolution.gpu.trainer.GpuTrainingEngine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Adapter that connects the canonical {@link EvoLlmModel} to the existing EVO-CUDA
 * backend ({@link GpuTrainingEngine}), so the real Forge training loop can run on GPU
 * while keeping the model, tokenizer and dataset formats unchanged.
 *
 * <p>The adapter flattens the model's {@link Tensor} parameters into the GPU engine's
 * flat weight layout (the parameter <em>order</em> is identical: embedding, then per
 * block [attn_norm, WQ, WK, WV, WO, ffn_norm, W1, W3, W2], then output_norm, then the
 * un-tied output head), runs forward/backward/AdamW on device, and writes the updated
 * weights back into the same model. It does not re-implement the model architecture —
 * the kernels are the existing {@link GpuTrainingEngine}.</p>
 *
 * <p>Training-loop semantics (shuffle seed, 10% validation split, micro-batch x
 * gradient accumulation, MEAN_PER_TOKEN gradient normalisation, warmup + cosine LR
 * schedule, rank-1 parameters excluded from weight decay) mirror {@link EvoLlmTrainer}
 * so CPU and GPU runs are numerically comparable.</p>
 */
public class GpuTrainingAdapter {

    private final EvoLlmModel model;
    private final GpuTrainingEngine.Config gpuCfg;
    private final List<Double> lossHistory = new ArrayList<>();
    private final long parameterCount;
    private final float initialLr = 1e-3f;
    private final float minLr = 5e-5f;
    private final float weightDecay = 0.01f;

    private int microBatchSize = 4;
    private int accumulationSteps = 2;
    private long baseSeed = 42L;
    private volatile boolean stopRequested = false;
    private EvoLlmTrainer.ProgressListener progressListener;
    private String lastFailureReason;
    private long forwardNanos;
    private long backwardNanos;
    private long optimizerNanos;
    private long optimizerStepsExecuted;
    private long transferNanos;

    public GpuTrainingAdapter(EvoLlmModel model) {
        if (model == null) {
            throw new IllegalArgumentException("EvoLlmModel cannot be null");
        }
        this.model = model;
        this.gpuCfg = buildConfig(model);
        this.parameterCount = model.parameters().stream().mapToLong(Tensor::getSize).sum();
    }

    private static GpuTrainingEngine.Config buildConfig(EvoLlmModel m) {
        GpuTrainingEngine.Config c = new GpuTrainingEngine.Config();
        c.vocabSize = m.getVocabSize();
        c.dModel = m.getDModel();
        c.numHeads = m.getNumHeads();
        c.numBlocks = m.getNumBlocks();
        c.dff = m.getDff();
        c.maxSeqLen = Math.max(1, m.getMaxSeqLen());
        c.useRoPE = m.getArchitecture().usesRoPE();
        return c;
    }

    // ------------------------------------------------------------------
    // Configuration (mirrors EvoLlmTrainer)
    // ------------------------------------------------------------------
    public void setMicroBatchSize(int microBatchSize) {
        this.microBatchSize = Math.max(1, microBatchSize);
    }

    public void setAccumulationSteps(int steps) {
        this.accumulationSteps = Math.max(1, steps);
    }

    public void setBaseSeed(long seed) {
        this.baseSeed = seed;
    }

    public void setProgressListener(EvoLlmTrainer.ProgressListener listener) {
        this.progressListener = listener;
    }

    public void requestStop() {
        this.stopRequested = true;
    }

    // ------------------------------------------------------------------
    // Introspection
    // ------------------------------------------------------------------
    public EvoLlmModel getModel() {
        return model;
    }

    public List<Double> getLossHistory() {
        return lossHistory;
    }

    public long getParameterCount() {
        return parameterCount;
    }

    public TrainingDevice getActualDevice() {
        return TrainingDevice.CUDA;
    }

    public String getBackendName() {
        return TrainingDevice.CUDA.backendName();
    }

    public String getLastFailureReason() {
        return lastFailureReason;
    }

    public long getForwardNanos() {
        return forwardNanos;
    }

    public long getBackwardNanos() {
        return backwardNanos;
    }

    public long getOptimizerNanos() {
        return optimizerNanos;
    }

    public long getOptimizerStepsExecuted() {
        return optimizerStepsExecuted;
    }

    public long getTransferNanos() {
        return transferNanos;
    }

    // ------------------------------------------------------------------
    // Weight transfer (EvoLlmModel <-> GPU flat layout)
    // ------------------------------------------------------------------
    public float[] flattenWeights() {
        List<Tensor> params = model.parameters();
        int n = 0;
        for (Tensor p : params) {
            n += p.getData().length;
        }
        float[] flat = new float[n];
        int off = 0;
        for (Tensor p : params) {
            float[] d = p.getData();
            System.arraycopy(d, 0, flat, off, d.length);
            off += d.length;
        }
        return flat;
    }

    public void restoreWeights(float[] flat) {
        List<Tensor> params = model.parameters();
        int off = 0;
        for (Tensor p : params) {
            float[] d = p.getData();
            System.arraycopy(flat, off, d, 0, d.length);
            off += d.length;
        }
    }

    private static float[] lossMaskToFloat(boolean[] lossMask) {
        float[] m = new float[lossMask.length];
        for (int i = 0; i < m.length; i++) {
            m[i] = lossMask[i] ? 1.0f : 0.0f;
        }
        return m;
    }

    // ------------------------------------------------------------------
    // Training (mirrors EvoLlmTrainer.train)
    // ------------------------------------------------------------------
    public void train(List<TrainingSample> samples, int epochs) {
        lossHistory.clear();
        lastFailureReason = null;
        forwardNanos = backwardNanos = optimizerNanos = transferNanos = 0;
        optimizerStepsExecuted = 0;

        if (samples == null || samples.isEmpty()) {
            return;
        }

        List<TrainingSample> dataset = new ArrayList<>(samples);
        Collections.shuffle(dataset, new Random(baseSeed));

        int valCount = (int) (dataset.size() * 0.1);
        List<TrainingSample> trainSamples = new ArrayList<>(
                dataset.subList(0, Math.max(1, dataset.size() - valCount)));
        List<TrainingSample> valSamples = valCount > 0
                ? new ArrayList<>(dataset.subList(dataset.size() - valCount, dataset.size()))
                : Collections.emptyList();

        List<TrainingSample> canonicalOrder = new ArrayList<>(trainSamples);
        int effectiveBatch = microBatchSize * accumulationSteps;
        int stepsPerEpoch = Math.max(1, (trainSamples.size() + effectiveBatch - 1) / effectiveBatch);
        int totalSteps = epochs * stepsPerEpoch;
        int warmupSteps = Math.max(1, (int) (totalSteps * 0.03f));

        try (GpuTrainingEngine.Session session = new GpuTrainingEngine(gpuCfg).openSession(gpuCfg)) {
            long tTransfer = System.nanoTime();
            float[] flat = flattenWeights();
            session.loadWeights(flat);
            transferNanos += (System.nanoTime() - tTransfer);

            int optStep = 0;
            for (int epoch = 0; epoch < epochs && !stopRequested; epoch++) {
                List<TrainingSample> epochSamples = new ArrayList<>(canonicalOrder);
                Collections.shuffle(epochSamples, new Random(baseSeed + epoch + 1));

                double epochTotalLoss = 0.0;
                long epochValidTokens = 0;
                long epochStart = System.nanoTime();

                for (int i = 0; i < epochSamples.size() && !stopRequested; i += effectiveBatch) {
                    int end = Math.min(i + effectiveBatch, epochSamples.size());

                    long windowTokens = 0;
                    for (int j = i; j < end; j++) {
                        for (boolean m : epochSamples.get(j).lossMask) {
                            if (m) {
                                windowTokens++;
                            }
                        }
                    }
                    if (windowTokens == 0) {
                        continue;
                    }

                    optStep++;
                    float lr = computeScheduledLr(optStep, totalSteps, warmupSteps);
                    session.setLearningRate(lr);

                    double windowLoss = 0.0;
                    long fwdNanos = 0;
                    long bwdNanos = 0;
                    for (int j = i; j < end; j++) {
                        TrainingSample s = epochSamples.get(j);
                        float[] mask = lossMaskToFloat(s.lossMask);
                        int t = s.inputIds.length;
                        long f0 = System.nanoTime();
                        float raw = session.accumulate(s.inputIds, s.labels, mask, t, (float) windowTokens);
                        long f1 = System.nanoTime();
                        fwdNanos += (f1 - f0);
                        bwdNanos += (f1 - f0); // forward+backward happen together inside accumulate()
                        windowLoss += raw;
                    }
                    forwardNanos += fwdNanos;
                    backwardNanos += bwdNanos;

                    long o0 = System.nanoTime();
                    session.optimizerStep();
                    optimizerNanos += (System.nanoTime() - o0);
                    optimizerStepsExecuted++;

                    epochTotalLoss += windowLoss;
                    epochValidTokens += windowTokens;

                    double avgWindowLoss = windowValidLoss(windowLoss, windowTokens);
                    if (progressListener != null) {
                        progressListener.onProgressMetrics(epoch, epochs, optStep, totalSteps,
                                epochValidTokens, (long) trainSamples.size() * 1, 0.0, 0.0, 0.0, avgWindowLoss, lr);
                    }
                }

                double avgTrainLoss = epochValidTokens > 0 ? epochTotalLoss / epochValidTokens : 0.0;
                lossHistory.add(avgTrainLoss);
                model.getTrainingState().setEpoch(epoch + 1);
                model.getTrainingState().setStep(optStep);
                model.getTrainingState().setLastLoss((float) avgTrainLoss);

                double valLoss = valSamples.isEmpty() ? Double.NaN : evaluateValidation(valSamples);
                if (progressListener != null) {
                    progressListener.onEpochCompleted(epoch + 1, epochs, avgTrainLoss, valLoss,
                            (System.nanoTime() - epochStart) / 1_000_000L);
                }
            }

            long tDownload = System.nanoTime();
            session.downloadWeights(flat);
            restoreWeights(flat);
            transferNanos += (System.nanoTime() - tDownload);
        } catch (Throwable t) {
            lastFailureReason = t.toString();
            throw new RuntimeException("EVO-CUDA training failed: " + t.getMessage(), t);
        }
    }

    private static double windowValidLoss(double windowLoss, long windowTokens) {
        return windowTokens > 0 ? windowLoss / windowTokens : 0.0;
    }

    private float computeScheduledLr(int step, int totalSteps, int warmupSteps) {
        if (step <= warmupSteps) {
            return initialLr * ((float) step / Math.max(1, warmupSteps));
        }
        float progress = (float) (step - warmupSteps) / Math.max(1, totalSteps - warmupSteps);
        progress = Math.max(0.0f, Math.min(1.0f, progress));
        float cosineDecay = 0.5f * (1.0f + (float) Math.cos(Math.PI * progress));
        return minLr + (initialLr - minLr) * cosineDecay;
    }

    /**
     * Validation NLL/token computed on the CPU model after training (the weights are
     * restored into {@link #model}), so this reuses the canonical forward pass rather
     * than duplicating a forward-only GPU kernel.
     */
    public double evaluateValidation(List<TrainingSample> valSamples) {
        if (valSamples == null || valSamples.isEmpty()) {
            return 0.0;
        }
        double totalLoss = 0.0;
        long totalValid = 0;
        for (TrainingSample s : valSamples) {
            Tensor logits = model.forward(s.inputIds, s.getAttentionMaskAsFloat());
            float[] logitsData = logits.getData();
            int vocabSize = (int) logits.getShape()[1];
            for (int t = 0; t < s.inputIds.length; t++) {
                if (!s.lossMask[t]) {
                    continue;
                }
                totalValid++;
                int off = t * vocabSize;
                int target = s.labels[t];
                float maxLogit = Float.NEGATIVE_INFINITY;
                for (int v = 0; v < vocabSize; v++) {
                    maxLogit = Math.max(maxLogit, logitsData[off + v]);
                }
                float sumExp = 0.0f;
                for (int v = 0; v < vocabSize; v++) {
                    sumExp += (float) Math.exp(logitsData[off + v] - maxLogit);
                }
                totalLoss += (maxLogit + (float) Math.log(sumExp) - logitsData[off + target]);
            }
        }
        return totalValid > 0 ? (totalLoss / totalValid) : 0.0;
    }
}
