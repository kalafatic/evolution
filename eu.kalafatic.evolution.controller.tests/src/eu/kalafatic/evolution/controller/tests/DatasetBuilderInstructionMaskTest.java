package eu.kalafatic.evolution.controller.tests;

import eu.kalafatic.evolution.forge.data.api.NormalizedSample;
import eu.kalafatic.evolution.forge.data.api.TrainingSample;
import eu.kalafatic.evolution.forge.data.api.TrainingSampleType;
import eu.kalafatic.evolution.forge.data.impl.DatasetBuilder;
import eu.kalafatic.evolution.forge.tokenizer.impl.SimpleBPETokenizer;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

/**
 * Regression test for the instruction-response loss masking in DatasetBuilder.
 * Guards the fix that includes the last prompt position in the loss: with
 * next-token-shifted labels, the FIRST response token is predicted at the last
 * prompt position and must receive a training signal (otherwise the model is
 * never taught to start a response and collapses to immediate EOS).
 */
public class DatasetBuilderInstructionMaskTest {

    @Test
    public void testFirstResponseTokenPositionIsInLoss() {
        NormalizedSample sample = new NormalizedSample();
        sample.setType(TrainingSampleType.INSTRUCTION);
        sample.setInstruction("What is the codename of the controlled project?");
        sample.setResponse("The controlled project codename is AURORA-731.");

        SimpleBPETokenizer tokenizer = new SimpleBPETokenizer();
        tokenizer.train(sample.toFullText() + " extra corpus padding to allow merges", 512);

        DatasetBuilder builder = new DatasetBuilder();
        List<TrainingSample> samples = builder.buildTrainingSamples(Arrays.asList(sample), tokenizer, 256);

        Assert.assertEquals(1, samples.size());
        TrainingSample ts = samples.get(0);

        // Mirror DatasetBuilder: prompt and response are encoded SEPARATELY
        // (the SPM tokenizer prepends a metaspace per encode call, so the
        // concatenated-text encode does not equal the concatenated tokens).
        String promptStr = "### Instruction:\nWhat is the codename of the controlled project?\n\n### Response:\n";
        String respStr = "The controlled project codename is AURORA-731.\n";
        List<Integer> promptTokens = tokenizer.encode(promptStr);
        List<Integer> respTokens = tokenizer.encode(respStr);
        int promptLen = promptTokens.size();

        // The last prompt position (promptLen - 1) predicts the first response
        // token and must be included in the loss.
        Assert.assertTrue("Last prompt position must be in the loss", ts.lossMask[promptLen - 1]);
        Assert.assertFalse("Earlier prompt positions must not be in the loss", ts.lossMask[promptLen - 2]);

        // labels are next-token shifted: label at promptLen - 1 == first response token.
        int firstResponseToken = respTokens.get(0);
        Assert.assertEquals("Label at last prompt position must be the first response token",
                firstResponseToken, ts.labels[promptLen - 1]);

        // Response body positions remain in the loss.
        Assert.assertTrue(ts.lossMask[promptLen]);
        Assert.assertTrue(ts.lossMask[ts.inputIds.length - 1]);
    }

    @Test
    public void testContinuousTextSamplesUnchanged() {
        NormalizedSample sample = NormalizedSample.createTextSample("Some continuous text for training purposes.", "test");
        SimpleBPETokenizer tokenizer = new SimpleBPETokenizer();
        tokenizer.train("Some continuous text for training purposes. corpus corpus corpus", 512);
        DatasetBuilder builder = new DatasetBuilder();
        List<TrainingSample> samples = builder.buildTrainingSamples(Arrays.asList(sample), tokenizer, 256);
        Assert.assertEquals(1, samples.size());
        TrainingSample ts = samples.get(0);
        for (int i = 0; i < ts.inputIds.length; i++) {
            Assert.assertTrue("Continuous text positions must all be in the loss", ts.lossMask[i]);
        }
    }
}
