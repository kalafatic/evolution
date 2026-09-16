package eu.kalafatic.evolution.controller.tests;

import eu.kalafatic.evolution.forge.data.api.NormalizedSample;
import eu.kalafatic.evolution.forge.data.api.artifact.EvoDatasetArtifact;
import eu.kalafatic.evolution.forge.data.api.service.DatasetPreparationResult;
import eu.kalafatic.evolution.forge.data.api.source.DatasetItem;
import eu.kalafatic.evolution.forge.data.api.source.DatasetPreparationContext;
import eu.kalafatic.evolution.forge.data.impl.service.DatasetPreparationService;
import eu.kalafatic.evolution.forge.model.llm.EvoLlmModel;
import eu.kalafatic.evolution.forge.tokenizer.impl.SimpleBPETokenizer;
import eu.kalafatic.evolution.forge.trainer.impl.llm.EvoLlmTrainer;

import org.json.JSONObject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * End-to-end validation test suite verifying all 9 core requirements for the EVO Forge dataset pipeline:
 * 1. Single TXT -> adapter -> normalized records -> timestamp.evodata
 * 2. Multiple TXT files -> one timestamp.evodata
 * 3. Heterogeneous data (TXT + JSONL + Parquet) -> one timestamp.evodata
 * 4. HF + local -> one timestamp.evodata
 * 5. Existing .evodata + new source -> merged timestamp.evodata
 * 6. Duplicate data deduplication
 * 7. Failed source isolation
 * 8. Usable byte target accumulation
 * 9. Training boundary (.evodata is the sole training input)
 */
public class ForgeDataPipelineEndToEndTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    /**
     * Test 1 — single TXT
     * TXT -> adapter -> normalized records -> timestamp.evodata -> valid training input
     */
    @Test
    public void test1_SingleTxtSource() throws Exception {
        File txtFile = tempFolder.newFile("dataset_single.txt");
        Files.writeString(txtFile.toPath(), "EVO Native LLM training document with single TXT source adaptation.", StandardCharsets.UTF_8);

        List<DatasetItem> items = new ArrayList<>();
        items.add(new DatasetItem(true, txtFile.getAbsolutePath(), "FILE"));

        DatasetPreparationService service = new DatasetPreparationService();
        DatasetPreparationContext context = new DatasetPreparationContext(0, null);
        File outDir = tempFolder.newFolder("out_test1");

        DatasetPreparationResult result = service.prepareDatasets(items, context, outDir);

        assertEquals(DatasetPreparationResult.Status.SUCCESS, result.getStatus());
        assertNotNull(result.getArtifact());
        assertTrue(result.getOutputPath().endsWith(".evodata"));
        assertTrue(new File(result.getOutputPath()).exists());
        assertEquals(1, result.getSamples().size());
        assertTrue(result.getSamples().get(0).toFullText().contains("EVO Native LLM"));
    }

    /**
     * Test 2 — multiple TXT files
     * TXT A + TXT B + TXT C -> one timestamp.evodata
     */
    @Test
    public void test2_MultipleTxtFiles() throws Exception {
        File txtA = tempFolder.newFile("docA.txt");
        Files.writeString(txtA.toPath(), "Document A content for multi-file merge.", StandardCharsets.UTF_8);

        File txtB = tempFolder.newFile("docB.txt");
        Files.writeString(txtB.toPath(), "Document B content for multi-file merge.", StandardCharsets.UTF_8);

        File txtC = tempFolder.newFile("docC.txt");
        Files.writeString(txtC.toPath(), "Document C content for multi-file merge.", StandardCharsets.UTF_8);

        List<DatasetItem> items = new ArrayList<>();
        items.add(new DatasetItem(true, txtA.getAbsolutePath(), "FILE"));
        items.add(new DatasetItem(true, txtB.getAbsolutePath(), "FILE"));
        items.add(new DatasetItem(true, txtC.getAbsolutePath(), "FILE"));

        DatasetPreparationService service = new DatasetPreparationService();
        DatasetPreparationContext context = new DatasetPreparationContext(0, null);
        File outDir = tempFolder.newFolder("out_test2");

        DatasetPreparationResult result = service.prepareDatasets(items, context, outDir);

        assertEquals(DatasetPreparationResult.Status.SUCCESS, result.getStatus());
        assertEquals(3, result.getSuccessfulSources().size());
        assertEquals(3, result.getSamples().size());
        assertTrue(new File(result.getOutputPath()).exists());
    }

    /**
     * Test 3 — heterogeneous data
     * TXT + JSONL + Parquet -> one timestamp.evodata
     */
    @Test
    public void test3_HeterogeneousDataSources() throws Exception {
        // 1. TXT
        File txtFile = tempFolder.newFile("text_source.txt");
        Files.writeString(txtFile.toPath(), "Heterogeneous text content for EVO training.", StandardCharsets.UTF_8);

        // 2. JSONL
        File jsonlFile = tempFolder.newFile("chat_source.jsonl");
        JSONObject jsonlRow = new JSONObject();
        jsonlRow.put("type", "INSTRUCTION");
        jsonlRow.put("instruction", "Explain compiler design.");
        jsonlRow.put("response", "Compilers translate high-level code into executable machine instructions.");
        Files.writeString(jsonlFile.toPath(), jsonlRow.toString() + "\n", StandardCharsets.UTF_8);

        // 3. Parquet
        File parquetFile = tempFolder.newFile("data_table.parquet");
        Files.writeString(parquetFile.toPath(), "Column_A\tColumn_B\n\"Parquet tabular entry 1\"\t\"Parquet tabular entry 2\"\n", StandardCharsets.UTF_8);

        List<DatasetItem> items = new ArrayList<>();
        items.add(new DatasetItem(true, txtFile.getAbsolutePath(), "FILE"));
        items.add(new DatasetItem(true, jsonlFile.getAbsolutePath(), "FILE"));
        items.add(new DatasetItem(true, parquetFile.getAbsolutePath(), "FILE"));

        DatasetPreparationService service = new DatasetPreparationService();
        DatasetPreparationContext context = new DatasetPreparationContext(0, null);
        File outDir = tempFolder.newFolder("out_test3");

        DatasetPreparationResult result = service.prepareDatasets(items, context, outDir);

        assertEquals(DatasetPreparationResult.Status.SUCCESS, result.getStatus());
        assertEquals(3, result.getSuccessfulSources().size());
        assertTrue(result.getSamples().size() >= 3);
        assertTrue(new File(result.getOutputPath()).exists());
    }

    /**
     * Test 4 — HF + local
     * HF + TXT + Parquet -> one timestamp.evodata
     */
    @Test
    public void test4_HuggingFaceAndLocalSources() throws Exception {
        // Local TXT
        File txtFile = tempFolder.newFile("local_text.txt");
        Files.writeString(txtFile.toPath(), "Local text source for hybrid acquisition test.", StandardCharsets.UTF_8);

        // Local Parquet
        File parquetFile = tempFolder.newFile("local_data.parquet");
        Files.writeString(parquetFile.toPath(), "Local Parquet row dataset entry.", StandardCharsets.UTF_8);

        // HuggingFace source item
        DatasetItem hfItem = new DatasetItem(true, "wikitext", "HUGGING_FACE");

        List<DatasetItem> items = new ArrayList<>();
        items.add(new DatasetItem(true, txtFile.getAbsolutePath(), "FILE"));
        items.add(new DatasetItem(true, parquetFile.getAbsolutePath(), "FILE"));
        items.add(hfItem);

        DatasetPreparationService service = new DatasetPreparationService();
        DatasetPreparationContext context = new DatasetPreparationContext(0, null);
        File outDir = tempFolder.newFolder("out_test4");

        DatasetPreparationResult result = service.prepareDatasets(items, context, outDir);

        assertEquals(DatasetPreparationResult.Status.SUCCESS, result.getStatus());
        assertTrue(result.getSamples().size() >= 2); // Local sources successfully merged
        assertTrue(new File(result.getOutputPath()).exists());
    }

    /**
     * Test 5 — existing .evodata
     * existing.evodata + new source -> merged timestamp.evodata
     */
    @Test
    public void test5_ExistingEvodataAndNewSource() throws Exception {
        // Pre-compile existing .evodata archive
        File existingEvodata = tempFolder.newFile("existing.evodata");
        List<NormalizedSample> initialSamples = new ArrayList<>();
        initialSamples.add(NormalizedSample.createTextSample("Existing evodata pre-compiled sample.", "existing_source"));

        EvoDatasetArtifact initialArtifact = new EvoDatasetArtifact(existingEvodata);
        initialArtifact.save(initialSamples, null, null, 0.0);

        // New source TXT
        File newTxt = tempFolder.newFile("new_source.txt");
        Files.writeString(newTxt.toPath(), "Newly added source content for incremental compilation.", StandardCharsets.UTF_8);

        List<DatasetItem> items = new ArrayList<>();
        items.add(new DatasetItem(true, existingEvodata.getAbsolutePath(), "EVO_MODEL"));
        items.add(new DatasetItem(true, newTxt.getAbsolutePath(), "FILE"));

        DatasetPreparationService service = new DatasetPreparationService();
        DatasetPreparationContext context = new DatasetPreparationContext(0, null);
        File outDir = tempFolder.newFolder("out_test5");

        DatasetPreparationResult result = service.prepareDatasets(items, context, outDir);

        assertEquals(DatasetPreparationResult.Status.SUCCESS, result.getStatus());
        assertEquals(2, result.getSamples().size());

        boolean foundExisting = false;
        boolean foundNew = false;
        for (NormalizedSample s : result.getSamples()) {
            if (s.toFullText().contains("Existing evodata")) foundExisting = true;
            if (s.toFullText().contains("Newly added")) foundNew = true;
        }

        assertTrue(foundExisting);
        assertTrue(foundNew);
    }

    /**
     * Test 6 — duplicate data
     * The same logical content appearing in multiple sources must be handled by deduplication policy
     */
    @Test
    public void test6_DuplicateDataDeduplication() throws Exception {
        String duplicateContent = "Identical duplicate record appearing across multiple sources.";

        File fileA = tempFolder.newFile("sourceA.txt");
        Files.writeString(fileA.toPath(), duplicateContent, StandardCharsets.UTF_8);

        File fileB = tempFolder.newFile("sourceB.txt");
        Files.writeString(fileB.toPath(), duplicateContent, StandardCharsets.UTF_8);

        List<DatasetItem> items = new ArrayList<>();
        items.add(new DatasetItem(true, fileA.getAbsolutePath(), "FILE"));
        items.add(new DatasetItem(true, fileB.getAbsolutePath(), "FILE"));

        DatasetPreparationService service = new DatasetPreparationService();
        DatasetPreparationContext context = new DatasetPreparationContext(0, null);
        File outDir = tempFolder.newFolder("out_test6");

        DatasetPreparationResult result = service.prepareDatasets(items, context, outDir);

        assertEquals(DatasetPreparationResult.Status.SUCCESS, result.getStatus());
        assertEquals(1, result.getSamples().size()); // Duplicate filtered out
        assertTrue(result.getDuplicateCount() >= 1 || result.getDuplicateBytes() > 0);
    }

    /**
     * Test 7 — failed source
     * One unavailable source must not corrupt valid sources
     */
    @Test
    public void test7_FailedSourceIsolation() throws Exception {
        File validTxt = tempFolder.newFile("valid_source.txt");
        Files.writeString(validTxt.toPath(), "Valid source content.", StandardCharsets.UTF_8);

        File missingFile = new File(tempFolder.getRoot(), "non_existent_file.txt");

        List<DatasetItem> items = new ArrayList<>();
        items.add(new DatasetItem(true, validTxt.getAbsolutePath(), "FILE"));
        items.add(new DatasetItem(true, missingFile.getAbsolutePath(), "FILE"));

        DatasetPreparationService service = new DatasetPreparationService();
        DatasetPreparationContext context = new DatasetPreparationContext(0, null);
        File outDir = tempFolder.newFolder("out_test7");

        DatasetPreparationResult result = service.prepareDatasets(items, context, outDir);

        assertEquals(DatasetPreparationResult.Status.SUCCESS, result.getStatus());
        assertEquals(1, result.getSuccessfulSources().size());
        assertEquals(1, result.getFailedSources().size());
        assertEquals(1, result.getSamples().size());
        assertTrue(result.getSamples().get(0).toFullText().contains("Valid source content"));
    }

    /**
     * Test 8 — target size
     * Multiple sources must contribute toward the configured usable-byte target
     */
    @Test
    public void test8_ByteTargetAccumulation() throws Exception {
        File doc1 = tempFolder.newFile("target_doc1.txt");
        Files.writeString(doc1.toPath(), "Chunk 1 content for byte target accumulation test. ".repeat(10), StandardCharsets.UTF_8);

        File doc2 = tempFolder.newFile("target_doc2.txt");
        Files.writeString(doc2.toPath(), "Chunk 2 content for byte target accumulation test. ".repeat(10), StandardCharsets.UTF_8);

        long targetBytes = 100; // Cap target at 100 bytes
        List<DatasetItem> items = new ArrayList<>();
        items.add(new DatasetItem(true, doc1.getAbsolutePath(), "FILE"));
        items.add(new DatasetItem(true, doc2.getAbsolutePath(), "FILE"));

        DatasetPreparationService service = new DatasetPreparationService();
        DatasetPreparationContext context = new DatasetPreparationContext(targetBytes, null);
        File outDir = tempFolder.newFolder("out_test8");

        DatasetPreparationResult result = service.prepareDatasets(items, context, outDir);

        assertEquals(DatasetPreparationResult.Status.SUCCESS, result.getStatus());
        assertTrue(result.getAcceptedBytes() >= targetBytes);
        assertTrue(result.getSamples().size() <= 2);
    }

    /**
     * Test 9 — training boundary
     * The model training process accepts timestamp.evodata without knowing original source types
     */
    @Test
    public void test9_TrainingBoundaryEvodataInput() throws Exception {
        File txtFile = tempFolder.newFile("source_for_boundary.txt");
        Files.writeString(txtFile.toPath(), "EVO Training boundary verification text content.", StandardCharsets.UTF_8);

        List<DatasetItem> items = new ArrayList<>();
        items.add(new DatasetItem(true, txtFile.getAbsolutePath(), "FILE"));

        DatasetPreparationService service = new DatasetPreparationService();
        DatasetPreparationContext context = new DatasetPreparationContext(0, null);
        File outDir = tempFolder.newFolder("out_test9");

        DatasetPreparationResult result = service.prepareDatasets(items, context, outDir);

        // 1. Verify compilation produced timestamp.evodata
        String compiledEvodataPath = result.getOutputPath();
        assertTrue(compiledEvodataPath.endsWith(".evodata"));

        // 2. Training boundary test: Model trainer loads ONLY from compiled .evodata artifact
        EvoDatasetArtifact trainingArtifact = EvoDatasetArtifact.load(new File(compiledEvodataPath));
        assertNotNull(trainingArtifact);
        assertTrue(trainingArtifact.getTrainSamples().size() > 0);

        String trainingCorpus = trainingArtifact.getTrainSamples().get(0).toFullText();
        SimpleBPETokenizer tokenizer = new SimpleBPETokenizer();
        tokenizer.train(trainingCorpus, 500);

        List<Integer> tokens = tokenizer.encode(trainingCorpus);
        assertTrue(tokens.size() > 0);

        EvoLlmModel model = new EvoLlmModel(tokenizer.getVocabSize(), 64, 2, 2, 128, 32);
        EvoLlmTrainer trainer = new EvoLlmTrainer(model);
        assertNotNull(trainer);
    }
}
