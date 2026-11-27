package eu.kalafatic.evolution.gpu.trainer;

import eu.kalafatic.evolution.gpu.cuda.CudaDevice;
import org.junit.Assume;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertTrue;

/**
 * Real-GPU smoke test for the SMALL Forge profile (48.9M parameters) that previously
 * failed with {@code cuMemAlloc failed: out of memory}.
 *
 * <p>This proves that a genuinely sized SMALL-profile workload — persistent weight /
 * gradient / AdamW-moment / decay-mask buffers plus forward+backward activation scratch
 * for the full 15k vocabulary — completes on the GPU, updates parameters and finite loss,
 * and releases all device memory. It is skipped when no CUDA device is present.</p>
 */
public class GpuSmallProfileSmokeTest {

    @Test
    public void smallProfileTrainsOnGpuWithoutOom() {
        Assume.assumeTrue("CUDA device not available", CudaDevice.isAvailable());

        // SMALL profile (ModelSizePreset.SMALL): vocab 15000, dModel 512, 8 heads, 8 blocks,
        // dff 2048. Sequence length is capped at 128 to keep the O(T^2) attention backward
        // tractable in a unit test; the 48.9M-parameter tensor allocations are identical.
        GpuTrainingEngine.Config cfg = new GpuTrainingEngine.Config();
        cfg.vocabSize = 15000;
        cfg.dModel = 512;
        cfg.numHeads = 8;
        cfg.numBlocks = 8;
        cfg.dff = 2048;
        cfg.maxSeqLen = 128;
        cfg.epochs = 1;
        cfg.learningRate = 1e-3f;
        cfg.seed = 42;

        GpuTrainingEngine engine = new GpuTrainingEngine(cfg);
        assertTrue("SMALL profile parameter count must be ~48.9M", engine.getTotalParams() == 48_923_136);

        float[] initial = engine.initWeights(cfg);

        long[] before = CudaDevice.tryCreate().memGetInfo();

        // Small controlled dataset (8 samples × 64 tokens).
        Random r = new Random(7);
        java.util.List<GpuTrainingEngine.Sample> samples = new java.util.ArrayList<>();
        for (int s = 0; s < 8; s++) {
            int[] tok = new int[64];
            for (int i = 0; i < 64; i++) tok[i] = r.nextInt(cfg.vocabSize);
            samples.add(new GpuTrainingEngine.Sample(tok));
        }

        GpuTrainingEngine.Result res = engine.train(cfg, samples);

        assertTrue("SMALL-profile training must run on GPU (got: " + res.failureReason + ")", res.gpuUsed);
        assertTrue("loss history must be populated", !res.lossHistory.isEmpty());
        double first = res.lossHistory.get(0);
        double last = res.finalLoss;
        assertTrue("loss must be finite (first=" + first + ", last=" + last + ")",
                Double.isFinite(first) && Double.isFinite(last));

        // Parameters must actually change (real optimizer updates, not a no-op).
        double change = 0.0;
        for (int i = 0; i < initial.length; i++) change += Math.abs(initial[i] - res.flatWeights[i]);
        assertTrue("weights must change during training", change > 0.0);

        // Device memory must be released after the run (tolerate driver cache noise).
        long[] after = CudaDevice.tryCreate().memGetInfo();
        long leaked = before[0] - after[0];
        assertTrue("device memory not released after SMALL-profile run (leaked ~" + leaked + " bytes)",
                leaked < 128L * 1024 * 1024);
    }
}
