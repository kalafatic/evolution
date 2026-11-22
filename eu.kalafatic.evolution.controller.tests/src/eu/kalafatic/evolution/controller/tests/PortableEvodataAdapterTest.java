package eu.kalafatic.evolution.controller.tests;

import eu.kalafatic.evolution.forge.data.api.NormalizedSample;
import eu.kalafatic.evolution.forge.data.api.TrainingSampleType;
import eu.kalafatic.evolution.forge.data.api.source.DatasetItem;
import eu.kalafatic.evolution.forge.data.api.source.DatasetPreparationContext;
import eu.kalafatic.evolution.forge.data.impl.source.adapters.EVODataAdapter;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

/**
 * Regression tests for the portable JSONL .evodata fixture ingestion in
 * EVODataAdapter: records with the documented fields (content, metadata,
 * source, sourceId, documentId, language, type, timestamp, quality) must be
 * converted 1:1 into NormalizedSamples with preserved provenance.
 */
public class PortableEvodataAdapterTest {

    private File writeFixture() throws Exception {
        File dir = Files.createTempDirectory("portable-evodata-test").toFile();
        File f = new File(dir, "01_basic_facts.evodata");
        String line = "{\"content\": \"### Instruction\\nWhat is the codename of the controlled project?\\n\\n### Response\\nThe controlled project codename is AURORA-731.\", "
                + "\"metadata\": {\"recordId\": \"FACT-001\", \"category\": \"fact\", \"split\": \"train\", \"tags\": [\"synthetic\", \"atomic_fact\"]}, "
                + "\"source\": \"forge-controlled-synthetic-fixture\", \"sourceId\": \"FACT-001\", \"documentId\": \"FORGE-VAL-FACT\", "
                + "\"language\": \"en\", \"type\": \"instruction_response\", \"timestamp\": \"2026-11-21T00:00:00Z\", \"quality\": 1.0}";
        Files.write(f.toPath(), line.getBytes(StandardCharsets.UTF_8));
        return f;
    }

    @Test
    public void testPortableJsonlFixtureConvertsToOneSample() throws Exception {
        File f = writeFixture();
        DatasetItem item = new DatasetItem(true, f.getAbsolutePath(), "FILE");
        EVODataAdapter adapter = new EVODataAdapter();
        Assert.assertTrue(adapter.supports(item));

        List<NormalizedSample> samples = adapter.convert(item, new DatasetPreparationContext(0, m -> {}));
        Assert.assertEquals(1, samples.size());

        NormalizedSample s = samples.get(0);
        Assert.assertEquals(TrainingSampleType.INSTRUCTION, s.getType());
        Assert.assertEquals("What is the codename of the controlled project?", s.getInstruction());
        Assert.assertEquals("The controlled project codename is AURORA-731.", s.getResponse());
        Assert.assertEquals("FACT-001", s.getId());
        Assert.assertEquals("forge-controlled-synthetic-fixture", s.getSource());
        Assert.assertEquals("en", s.getLanguage());
        Assert.assertEquals(1.0, s.getQualityScore(), 1e-9);
        Assert.assertEquals("fact", s.getCategory());
        Assert.assertEquals("FACT-001", s.getMetadata().get("recordId"));
        Assert.assertEquals("train", s.getMetadata().get("split"));
        Assert.assertEquals("FORGE-VAL-FACT", s.getMetadata().get("documentId"));
        Assert.assertEquals("2026-11-21T00:00:00Z", s.getMetadata().get("timestamp"));
        Assert.assertEquals("instruction_response", s.getMetadata().get("recordType"));
        Assert.assertTrue(s.getMetadata().get("tags") instanceof List);
        Assert.assertTrue(s.toFullText().contains("AURORA-731"));
    }

    @Test
    public void testNativeZipEvodataStillLoads() throws Exception {
        // A plain text file (non-ZIP) must go through the portable path; this
        // test guards that the ZIP detection keeps the native archive path intact
        // for the canonical container format.
        File dir = Files.createTempDirectory("portable-evodata-test2").toFile();
        File txt = new File(dir, "plain.evodata");
        Files.write(txt.toPath(), "not json".getBytes(StandardCharsets.UTF_8));
        DatasetItem item = new DatasetItem(true, txt.getAbsolutePath(), "FILE");
        EVODataAdapter adapter = new EVODataAdapter();
        // Non-ZIP + malformed JSON: no samples, no exception.
        List<NormalizedSample> samples = adapter.convert(item, new DatasetPreparationContext(0, m -> {}));
        Assert.assertNotNull(samples);
    }

    @Test
    public void testContentWithoutResponseMarkerPreservedAsText() {
        String[] parts = EVODataAdapter.splitInstructionResponse("Just some plain content.");
        Assert.assertEquals("Just some plain content.", parts[0]);
        Assert.assertEquals("", parts[1]);
    }
}
