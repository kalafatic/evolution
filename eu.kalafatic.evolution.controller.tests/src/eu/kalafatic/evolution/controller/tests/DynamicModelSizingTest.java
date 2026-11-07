package eu.kalafatic.evolution.controller.tests;

import eu.kalafatic.evolution.forge.controller.service.impl.agents.ForgeTrainingEstimationAgent;
import org.junit.Assert;
import org.junit.Test;

/**
 * Regression tests for dynamic model capacity sizing and hardware-aware capacity calculation.
 */
public class DynamicModelSizingTest {

    @Test
    public void testDynamicSizingSmallDatasetNano() {
        ForgeTrainingEstimationAgent agent = ForgeTrainingEstimationAgent.getInstance();
        ForgeTrainingEstimationAgent.DynamicSizingResult res = agent.calculateDynamicModelSizing(20_000L, 1000, 2048);

        Assert.assertEquals(128, res.dModel);
        Assert.assertEquals(3, res.numBlocks);
        Assert.assertEquals(4, res.numHeads);
        Assert.assertEquals(256, res.dff);
        Assert.assertEquals(128, res.maxSeqLen);
        Assert.assertTrue(res.estimatedParameters > 0);
        Assert.assertTrue(res.rationale.contains("Nano"));
    }

    @Test
    public void testDynamicSizingMicroAndTiny() {
        ForgeTrainingEstimationAgent agent = ForgeTrainingEstimationAgent.getInstance();

        // 200k tokens -> Micro
        ForgeTrainingEstimationAgent.DynamicSizingResult micro = agent.calculateDynamicModelSizing(200_000L, 2000, 4096);
        Assert.assertEquals(256, micro.dModel);
        Assert.assertEquals(4, micro.numBlocks);
        Assert.assertEquals(8, micro.numHeads);
        Assert.assertEquals(512, micro.dff);
        Assert.assertTrue(micro.rationale.contains("Micro"));

        // 1M tokens -> Tiny
        ForgeTrainingEstimationAgent.DynamicSizingResult tiny = agent.calculateDynamicModelSizing(1_000_000L, 8000, 4096);
        Assert.assertEquals(384, tiny.dModel);
        Assert.assertEquals(6, tiny.numBlocks);
        Assert.assertEquals(8, tiny.numHeads);
        Assert.assertEquals(1024, tiny.dff);
        Assert.assertTrue(tiny.rationale.contains("Tiny"));
    }

    @Test
    public void testDynamicSizingRestrictedMemoryBudgetFallback() {
        ForgeTrainingEstimationAgent agent = ForgeTrainingEstimationAgent.getInstance();
        // Even with 5M tokens, if memory budget is restricted (<1GB), sizing scales down to Nano
        ForgeTrainingEstimationAgent.DynamicSizingResult res = agent.calculateDynamicModelSizing(5_000_000L, 4096, 512);
        Assert.assertEquals(128, res.dModel);
        Assert.assertEquals(3, res.numBlocks);
        Assert.assertTrue(res.rationale.contains("restricted memory budget"));
    }
}
