package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.*;

import java.io.File;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import eu.kalafatic.evolution.forge.data.api.NormalizedSample;
import eu.kalafatic.evolution.forge.data.api.discovery.DataSourceCandidate;
import eu.kalafatic.evolution.forge.data.api.discovery.TrainingDataSourceDiscovery;
import eu.kalafatic.evolution.forge.data.api.discovery.TrainingDataSourceRanker;
import eu.kalafatic.evolution.forge.data.api.evaluation.TrainingDataPreferenceEvaluation;
import eu.kalafatic.evolution.forge.data.api.evaluation.TrainingDataPreferenceEvaluator;
import eu.kalafatic.evolution.forge.data.api.planner.TrainingDataAcquisitionPlan;
import eu.kalafatic.evolution.forge.data.api.planner.TrainingDataAcquisitionPlanner;
import eu.kalafatic.evolution.forge.data.api.preference.RequirementLevel;
import eu.kalafatic.evolution.forge.data.api.preference.TrainingDataPreferences;
import eu.kalafatic.evolution.forge.data.api.service.TrainingDataAcquisitionRequest;
import eu.kalafatic.evolution.forge.data.api.service.TrainingDataAcquisitionResult;
import eu.kalafatic.evolution.forge.data.api.service.TrainingDataAcquisitionService;
import eu.kalafatic.evolution.forge.data.api.source.DatasetSource;
import eu.kalafatic.evolution.forge.data.api.source.DatasetSourceConfig;
import eu.kalafatic.evolution.forge.data.api.source.DatasetSourceStats;
import eu.kalafatic.evolution.forge.data.impl.discovery.DefaultSourceRanker;
import eu.kalafatic.evolution.forge.data.impl.discovery.HuggingFaceSourceDiscovery;
import eu.kalafatic.evolution.forge.data.impl.evaluation.DefaultPreferenceEvaluator;
import eu.kalafatic.evolution.forge.data.impl.planner.DefaultAcquisitionPlanner;
import eu.kalafatic.evolution.forge.data.impl.pipeline.DataCleaner;
import eu.kalafatic.evolution.forge.data.impl.pipeline.DatasetDeduplicator;
import eu.kalafatic.evolution.forge.data.impl.pipeline.TrainingSampleQualityScorer;
import eu.kalafatic.evolution.forge.data.impl.service.TrainingDataAcquisitionServiceImpl;

public class PreferenceDrivenAcquisitionTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testPreferenceModelConstruction() {
        TrainingDataPreferences prefs = TrainingDataPreferences.builder()
                .minimumUsableBytes(100_000)
                .requiredLanguage("en")
                .capabilityObjective("chat")
                .addDomain("evolution")
                .addDomain("programming")
                .addDomain("AI")
                .sizeRequirementLevel(RequirementLevel.HARD)
                .build();

        assertEquals(100_000, prefs.getMinimumUsableBytes());
        assertEquals("en", prefs.getRequiredLanguage());
        assertEquals("chat", prefs.getCapabilityObjective());
        assertEquals(3, prefs.getPrimaryDomains().size());
        assertEquals(RequirementLevel.HARD, prefs.getSizeRequirementLevel());
    }

    @Test
    public void testDiscoveryAndRanking() {
        TrainingDataPreferences prefs = TrainingDataPreferences.builder()
                .minimumUsableBytes(1_000_000)
                .addDomain("evolution")
                .addDomain("programming")
                .build();

        TrainingDataSourceDiscovery discovery = new HuggingFaceSourceDiscovery();
        List<DataSourceCandidate> discovered = discovery.discover(prefs);
        assertFalse(discovered.isEmpty());

        TrainingDataSourceRanker ranker = new DefaultSourceRanker();
        List<DataSourceCandidate> ranked = ranker.rank(discovered, prefs);
        assertNotNull(ranked);
        assertEquals(discovered.size(), ranked.size());
    }

    @Test
    public void testAcquisitionPlannerProportionalAllocation() {
        TrainingDataPreferences prefs = TrainingDataPreferences.builder()
                .minimumUsableBytes(300_000)
                .addDomain("evolution")
                .addDomain("programming")
                .addDomain("AI")
                .build();

        TrainingDataSourceDiscovery discovery = new HuggingFaceSourceDiscovery();
        List<DataSourceCandidate> candidates = discovery.discover(prefs);

        TrainingDataAcquisitionPlanner planner = new DefaultAcquisitionPlanner();
        TrainingDataAcquisitionPlan plan = planner.plan(prefs, candidates);

        assertNotNull(plan);
        assertFalse(plan.getAllocatedTargetBytesPerSource().isEmpty());
    }

    @Test
    public void testPreferenceSatisfactionEvaluation() {
        TrainingDataPreferences prefs = TrainingDataPreferences.builder()
                .minimumUsableBytes(1000)
                .sizeRequirementLevel(RequirementLevel.HARD)
                .build();

        DatasetSourceStats statsSatisfied = new DatasetSourceStats();
        statsSatisfied.setAcceptedBytes(1500);

        TrainingDataPreferenceEvaluator evaluator = new DefaultPreferenceEvaluator();
        TrainingDataPreferenceEvaluation evalSatisfied = evaluator.evaluate(prefs, statsSatisfied);

        assertTrue(evalSatisfied.isAllHardRequirementsSatisfied());
        assertEquals(TrainingDataPreferenceEvaluation.SatisfactionStatus.SATISFIED, evalSatisfied.getOverallStatus());

        DatasetSourceStats statsUnsatisfied = new DatasetSourceStats();
        statsUnsatisfied.setAcceptedBytes(500);
        TrainingDataPreferenceEvaluation evalUnsatisfied = evaluator.evaluate(prefs, statsUnsatisfied);

        assertFalse(evalUnsatisfied.isAllHardRequirementsSatisfied());
        assertEquals(TrainingDataPreferenceEvaluation.SatisfactionStatus.NOT_SATISFIED, evalUnsatisfied.getOverallStatus());
    }

    @Test
    public void testAdaptiveAcquisitionEndToEnd() throws Exception {
        DatasetSource src1 = createMockSource("src1", 300); // 300 bytes
        DatasetSource src2 = createMockSource("src2", 800); // 800 bytes

        TrainingDataPreferences prefs = TrainingDataPreferences.builder()
                .minimumUsableBytes(1000)
                .build();

        TrainingDataAcquisitionRequest req = new TrainingDataAcquisitionRequest()
                .setPreferences(prefs)
                .addSource(src1)
                .addSource(src2);

        TrainingDataAcquisitionService service = new TrainingDataAcquisitionServiceImpl();
        TrainingDataAcquisitionResult result = service.acquireDataset(req);

        assertTrue("Multi-source adaptive acquisition should combine sources to reach target", result.getUsableContentBytes() >= 1000);
        assertTrue("Target reached should be true", result.isTargetReached());
        assertEquals(TrainingDataAcquisitionResult.Status.READY, result.getStatus());
        assertNotNull(result.getPreferenceEvaluation());
        assertTrue(result.getPreferenceEvaluation().isAllHardRequirementsSatisfied());
    }

    @Test
    public void testSmartDiscoveryFallbackExpansionWhenInitialSourceExhausted() throws Exception {
        // Initial source has only 200 bytes (e.g. 3MB vs 500MB scenario)
        DatasetSource smallSrc = createMockSource("small_src", 200);

        // Target requirement is 1000 bytes
        TrainingDataPreferences prefs = TrainingDataPreferences.builder()
                .minimumUsableBytes(1000)
                .addDomain("chat")
                .build();

        DatasetSource fallbackSrc = createMockSource("fallback_expanded_src", 1200);

        TrainingDataSourceDiscovery mockDiscovery = p -> List.of(
                new DataSourceCandidate("fallback_expanded_src", "MOCK", fallbackSrc, "en", List.of("chat"), 1200, 0.9)
        );

        TrainingDataAcquisitionServiceImpl service = new TrainingDataAcquisitionServiceImpl(
                new DataCleaner(),
                new TrainingSampleQualityScorer(0.5),
                new DatasetDeduplicator(true),
                new DefaultPreferenceEvaluator(),
                mockDiscovery
        );

        TrainingDataAcquisitionRequest req = new TrainingDataAcquisitionRequest()
                .setPreferences(prefs)
                .addSource(smallSrc);

        TrainingDataAcquisitionResult result = service.acquireDataset(req);

        assertTrue("Smart discovery fallback expansion should acquire additional similar datasets to satisfy 1000 byte target",
                result.getUsableContentBytes() >= 1000);
        assertTrue("Target reached must be true after fallback expansion", result.isTargetReached());
        assertEquals(TrainingDataAcquisitionResult.Status.READY, result.getStatus());
    }

    private DatasetSource createMockSource(String name, long targetBytes) {
        return new DatasetSource() {
            private int count = 0;
            private final DatasetSourceConfig config = new DatasetSourceConfig("MOCK", name);
            private final DatasetSourceStats stats = new DatasetSourceStats();

            @Override public String getSourceName() { return name; }
            @Override public DatasetSourceConfig getConfig() { return config; }
            @Override public DatasetSourceStats getStats() { return stats; }
            @Override public void initialize() {}

            @Override
            public boolean hasNext() {
                return count * 100 < targetBytes;
            }

            @Override
            public NormalizedSample next() {
                count++;
                return NormalizedSample.createTextSample("Sample turn #" + count + " for mock source " + name + " test.", name);
            }

            @Override public void close() {}
        };
    }
}
