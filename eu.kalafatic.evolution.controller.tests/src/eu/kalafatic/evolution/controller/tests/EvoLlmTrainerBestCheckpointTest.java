package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import eu.kalafatic.evolution.forge.model.llm.EvoLlmArchitecture;
import eu.kalafatic.evolution.forge.model.llm.EvoLlmModel;
import eu.kalafatic.evolution.forge.tokenizer.impl.SimpleBPETokenizer;
import eu.kalafatic.evolution.forge.trainer.api.TrainingResult;
import eu.kalafatic.evolution.forge.trainer.impl.llm.EvoLlmTrainer;

/**
 * Verifies validation-monitored best-checkpoint tracking in {@link EvoLlmTrainer}:
 * the trainer records the epoch with the lowest validation loss and can restore it,
 * so callers can select the best-generalizing model instead of the final (overfit) one.
 */
public class EvoLlmTrainerBestCheckpointTest {

    @Test
    public void testBestCheckpointTrackedAndRestorable() {
        String corpus = "the quick brown fox jumps over the lazy dog. " +
                "artificial intelligence and machine learning are transforming software. " +
                "a computer processes instructions and stores information in memory. " +
                "the capital of France is Paris and it is a major European city. " +
                "programming languages such as Java and Python are widely used. " +
                "water infrastructure and agricultural systems need maintenance.";

        SimpleBPETokenizer tokenizer = new SimpleBPETokenizer();
        tokenizer.train(corpus, 128);

        EvoLlmModel model = new EvoLlmModel(tokenizer.getVocabSize(), 64, 2, 2, 128, 48,
                EvoLlmArchitecture.POSITIONAL_ROPE);
        model.getIdToToken().putAll(tokenizer.getInvVocab());

        // Build training samples from the corpus (sliding windows).
        List<Integer> all = tokenizer.encode(corpus);
        List<int[]> samples = new ArrayList<>();
        int seqLen = 16;
        for (int i = 0; i + seqLen <= all.size(); i += seqLen / 2) {
            int[] w = new int[seqLen];
            for (int j = 0; j < seqLen; j++) w[j] = all.get(i + j);
            samples.add(w);
        }
        assertTrue(samples.size() >= 4);

        EvoLlmTrainer trainer = new EvoLlmTrainer(model, EvoLlmTrainer.TrainingProfile.EVO_FAST);
        trainer.setMicroBatchSize(4);
        trainer.setAccumulationSteps(1);
        trainer.setBaseSeed(42);
        trainer.setValidationSplitRatio(0.2f);

        TrainingResult result = trainer.train(samples, 12);

        assertTrue(result.getCompletedEpochs() > 0);
        // With a validation split configured, a best checkpoint must be recorded.
        assertTrue("best checkpoint must be recorded when validation split is set",
                trainer.hasBestCheckpoint());
        assertTrue("best epoch must be within [1, epochs]", trainer.getBestEpoch() >= 1);
        assertTrue("best val loss must be finite", trainer.getBestValLoss() < Double.POSITIVE_INFINITY);
        // The best validation loss must never exceed the final validation loss.
        assertTrue("best val loss <= final val loss",
                trainer.getBestValLoss() <= result.getValLoss() + 1e-6);

        // Restoring the best checkpoint must succeed and not throw.
        assertTrue("restoreBestCheckpoint must return true", trainer.restoreBestCheckpoint());
    }

    @Test
    public void testNoBestCheckpointWithoutValidationSplit() {
        SimpleBPETokenizer tokenizer = new SimpleBPETokenizer();
        tokenizer.train("hello world evolution native inference", 64);
        EvoLlmModel model = new EvoLlmModel(tokenizer.getVocabSize(), 32, 2, 1, 64, 32,
                EvoLlmArchitecture.POSITIONAL_ROPE);
        model.getIdToToken().putAll(tokenizer.getInvVocab());

        List<Integer> all = tokenizer.encode("hello world evolution native inference hello world");
        List<int[]> samples = new ArrayList<>();
        for (int i = 0; i + 8 <= all.size(); i += 4) {
            int[] w = new int[8];
            for (int j = 0; j < 8; j++) w[j] = all.get(i + j);
            samples.add(w);
        }

        EvoLlmTrainer trainer = new EvoLlmTrainer(model, EvoLlmTrainer.TrainingProfile.EVO_FAST);
        trainer.setMicroBatchSize(4);
        trainer.setBaseSeed(42);
        trainer.setValidationSplitRatio(0.0f); // no validation split

        trainer.train(samples, 3);

        assertFalse("no best checkpoint without a validation split", trainer.hasBestCheckpoint());
        assertEquals(-1, trainer.getBestEpoch());
        assertFalse("restore is a no-op without a best checkpoint", trainer.restoreBestCheckpoint());
    }
}
