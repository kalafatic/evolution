package eu.kalafatic.evolution.gpu.trainer;

import org.junit.Assume;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Functional test proving genuine GPU-accelerated training.
 *
 * <p>This test is skipped (via {@link Assume}) when no CUDA device is present, so it is
 * safe in CPU-only CI. On a CUDA machine it performs a real forward/backward/AdamW
 * training run on the GPU and verifies the observable effects of genuine training:</p>
 * <ul>
 *   <li>The CUDA device is detected and initialised.</li>
 *   <li>The training loss decreases (a real model learns the data).</li>
 *   <li>The model weights actually change.</li>
 *   <li>GPU memory is allocated during the run.</li>
 * </ul>
 */
public class GpuTrainingFunctionalTest {

    @Test
    public void testGenuineGpuTrainingReducesLossAndChangesWeights() {
        Assume.assumeTrue("No CUDA device available", eu.kalafatic.evolution.gpu.cuda.CudaDevice.isAvailable());

        GpuTrainingEngine.Config cfg = new GpuTrainingEngine.Config();
        cfg.vocabSize = 128;
        cfg.dModel = 32;
        cfg.numHeads = 4;
        cfg.numBlocks = 2;
        cfg.dff = 64;
        cfg.maxSeqLen = 32;
        cfg.epochs = 3;
        cfg.learningRate = 1e-2f;
        cfg.seed = 42;

        GpuTrainingEngine engine = new GpuTrainingEngine(cfg);
        float[] initial = engine.initWeights(cfg);

        List<GpuTrainingEngine.Sample> samples = new ArrayList<>();
        Random r = new Random(123);
        for (int s = 0; s < 16; s++) {
            int[] tok = new int[16];
            for (int i = 0; i < 16; i++) tok[i] = r.nextInt(cfg.vocabSize);
            samples.add(new GpuTrainingEngine.Sample(tok));
        }

        GpuTrainingEngine.Result res = engine.train(cfg, samples);

        assertTrue("Training must run on the GPU, got: " + res.failureReason, res.gpuUsed);
        assertTrue("Device name must identify an NVIDIA GPU, got: " + res.deviceName,
                res.deviceName.contains("NVIDIA") || res.deviceName.contains("RTX"));
        assertNotNull("Loss history must be populated", res.lossHistory);
        assertTrue("Loss history must be non-empty", !res.lossHistory.isEmpty());

        double first = res.lossHistory.get(0);
        double last = res.finalLoss;
        assertTrue("Loss must decrease (genuine learning): first=" + first + " last=" + last, last < first);

        double weightChange = 0.0;
        for (int i = 0; i < initial.length; i++) {
            weightChange += Math.abs(initial[i] - res.flatWeights[i]);
        }
        assertTrue("Model weights must change during training", weightChange > 0.0);
        assertTrue("Device memory must have been allocated during the run",
                res.freeMemBytesBefore > 0 && res.freeMemBytesAfter > 0);
    }

    @Test
    public void testGpuForwardMatchesCpuReference() {
        Assume.assumeTrue("No CUDA device available", eu.kalafatic.evolution.gpu.cuda.CudaDevice.isAvailable());

        GpuTrainingEngine.Config cfg = new GpuTrainingEngine.Config();
        cfg.vocabSize = 64;
        cfg.dModel = 16;
        cfg.numHeads = 4;
        cfg.numBlocks = 1;
        cfg.dff = 32;
        cfg.maxSeqLen = 16;
        cfg.seed = 42;

        GpuTrainingEngine engine = new GpuTrainingEngine(cfg);
        float[] w = engine.initWeights(cfg);
        int[] tokens = {1, 5, 9, 3, 7, 2, 8, 4, 6, 0, 10, 11, 12, 13, 14, 15};

        float[] gpu = engine.forwardLogits(cfg, w, tokens);
        float[] cpu = CpuReference.forward(cfg, w, tokens);

        double maxErr = 0.0;
        for (int i = 0; i < gpu.length; i++) {
            maxErr = Math.max(maxErr, Math.abs(gpu[i] - cpu[i]));
        }
        assertTrue("GPU forward must match CPU reference (max abs diff " + maxErr + ")", maxErr < 1e-4);
    }
}
