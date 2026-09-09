package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.*;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import eu.kalafatic.evolution.forge.data.api.NormalizedSample;
import eu.kalafatic.evolution.forge.data.api.TrainingSampleType;
import eu.kalafatic.evolution.forge.data.api.artifact.EvoDatasetArtifact;
import eu.kalafatic.evolution.forge.data.api.source.DatasetSourceConfig;
import eu.kalafatic.evolution.forge.data.api.source.DatasetSourceStats;
import eu.kalafatic.evolution.forge.data.impl.pipeline.DataCleaner;
import eu.kalafatic.evolution.forge.data.impl.pipeline.DatasetDeduplicator;
import eu.kalafatic.evolution.forge.data.impl.pipeline.DatasetSampler;
import eu.kalafatic.evolution.forge.data.impl.pipeline.TrainingSampleQualityScorer;
import eu.kalafatic.evolution.forge.data.impl.source.HuggingFaceDatasetSource;
import eu.kalafatic.evolution.forge.data.impl.source.LocalDatasetSource;

public class TrainingDataPreparationPipelineTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private File sampleTextFile;
    private File sampleDir;

    @Before
    public void setUp() throws Exception {
        sampleDir = tempFolder.newFolder("corpus");
        sampleTextFile = new File(sampleDir, "sample1.txt");
        Files.writeString(sampleTextFile.toPath(), "This is a clean training sentence for EVO Forge dataset preparation test.\nDuplicate checks and quality filtering apply.");

        File sampleTextFile2 = new File(sampleDir, "sample2.txt");
        Files.writeString(sampleTextFile2.toPath(), "This is a clean training sentence for EVO Forge dataset preparation test.\nDuplicate checks and quality filtering apply.");

        File javaFile = new File(sampleDir, "Sample.java");
        Files.writeString(javaFile.toPath(), "public class Sample { public static void main(String[] args) { System.out.println(\"EVO Code\"); } }");
    }

    @Test
    public void testNormalizedSampleAndTypes() {
        NormalizedSample sample = NormalizedSample.createTextSample("Hello EVO", "test-src");
        assertEquals(TrainingSampleType.TEXT, sample.getType());
        assertEquals("Hello EVO", sample.toFullText());
        assertNotNull(sample.getHash());

        NormalizedSample codeSample = new NormalizedSample(TrainingSampleType.CODE, "public class A {}");
        assertEquals(TrainingSampleType.CODE, codeSample.getType());
    }

    @Test
    public void testLocalDatasetSourceBoundedRead() throws Exception {
        DatasetSourceConfig config = new DatasetSourceConfig("LOCAL", sampleDir.getAbsolutePath());
        config.setMaxSamples(5);

        try (LocalDatasetSource source = new LocalDatasetSource(config)) {
            source.initialize();
            List<NormalizedSample> items = new ArrayList<>();
            while (source.hasNext()) {
                items.add(source.next());
            }
            assertTrue("Should read samples from local dir", items.size() > 0);
            assertTrue("Total samples read recorded in stats", source.getStats().getTotalSamplesRead() > 0);
        }
    }

    @Test
    public void testHuggingFaceDatasetSourceMockFallback() throws Exception {
        DatasetSourceConfig config = new DatasetSourceConfig("HUGGING_FACE", "wikitext");
        config.setMaxSamples(3);

        try (HuggingFaceDatasetSource source = new HuggingFaceDatasetSource(config)) {
            source.initialize();
            List<NormalizedSample> items = new ArrayList<>();
            while (source.hasNext()) {
                items.add(source.next());
            }
            assertEquals("Bounded HF source should produce max 3 samples in test context", 3, items.size());
        }
    }

    @Test
    public void testCleanerDeduplicatorAndQualityScorer() {
        DataCleaner cleaner = new DataCleaner();
        DatasetDeduplicator deduplicator = new DatasetDeduplicator(true);
        TrainingSampleQualityScorer scorer = new TrainingSampleQualityScorer(0.4);

        NormalizedSample rawSample = NormalizedSample.createTextSample("<p>  Clean text   with HTML tags.  </p>\n\n\n\nNew line.", "src1");
        NormalizedSample cleaned = cleaner.clean(rawSample);

        assertNotNull("Cleaner should return cleaned sample", cleaned);
        assertFalse("HTML should be stripped", cleaned.toFullText().contains("<p>"));

        assertTrue("Sample should pass quality scorer", scorer.isAcceptable(cleaned));

        assertFalse("First registration should not be duplicate", deduplicator.isDuplicate(cleaned));
        deduplicator.register(cleaned);

        NormalizedSample duplicateSample = NormalizedSample.createTextSample(cleaned.toFullText(), "src2");
        assertTrue("Subsequent identical sample should be detected as duplicate", deduplicator.isDuplicate(duplicateSample));
    }

    @Test
    public void testReservoirSampler() {
        DatasetSampler sampler = new DatasetSampler(DatasetSampler.Strategy.RESERVOIR, 5);
        List<NormalizedSample> candidates = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            candidates.add(NormalizedSample.createTextSample("Sample #" + i, "src"));
        }

        List<NormalizedSample> sampled = sampler.sample(candidates);
        assertEquals("Reservoir sampling should bound result size to 5", 5, sampled.size());
    }

    @Test
    public void testEvoDatasetArtifactRoundTrip() throws Exception {
        File artifactFile = new File(tempFolder.getRoot(), "test_dataset.evodata");
        EvoDatasetArtifact artifact = new EvoDatasetArtifact(artifactFile);

        List<NormalizedSample> samples = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            samples.add(NormalizedSample.createTextSample("Sample sentence #" + i + " for EVO artifact testing.", "src"));
        }

        DatasetSourceConfig config = new DatasetSourceConfig("LOCAL", sampleDir.getAbsolutePath());
        DatasetSourceStats stats = new DatasetSourceStats();

        artifact.save(samples, config, stats, 0.2);

        assertTrue("Artifact file .evodata must exist on disk", artifactFile.exists());
        assertEquals(EvoDatasetArtifact.Status.READY, artifact.getStatus());

        EvoDatasetArtifact loaded = EvoDatasetArtifact.load(artifactFile);
        assertEquals(EvoDatasetArtifact.Status.READY, loaded.getStatus());
        assertTrue("Train samples should be recovered", loaded.getTrainSamples().size() > 0);
        assertTrue("Report text should be generated", loaded.buildReportText().contains("EVO DATA PREPARATION RESULT REPORT"));
    }
}
