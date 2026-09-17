package eu.kalafatic.evolution.controller.tests;

import eu.kalafatic.evolution.forge.data.api.NormalizedSample;
import eu.kalafatic.evolution.forge.data.api.artifact.EvoDatasetArtifact;
import eu.kalafatic.evolution.forge.data.api.service.DatasetPreparationResult;
import eu.kalafatic.evolution.forge.data.api.source.DatasetInspection;
import eu.kalafatic.evolution.forge.data.api.source.DatasetItem;
import eu.kalafatic.evolution.forge.data.api.source.DatasetPreparationContext;
import eu.kalafatic.evolution.forge.data.api.source.SourceAdapterRegistry;
import eu.kalafatic.evolution.forge.data.impl.service.DatasetPreparationService;
import eu.kalafatic.evolution.forge.data.impl.source.adapters.FolderAdapter;

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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Comprehensive test suite verifying the Intelligent Universal Dataset -> EVO Native .evodata Pipeline.
 * Tests cover folder discovery, metadata exclusion, heterogeneous merging, deduplication accounting,
 * failure isolation, target size limits, metadata-only rejection, and cache invalidation.
 */
public class UniversalDatasetPreparationPipelineTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void test1_AlpacaLocalFolderWithMetadataExclusion() throws Exception {
        File alpacaDir = tempFolder.newFolder("alpaca_dataset");

        // 1. Valid Parquet dataset file
        File parquetFile = new File(alpacaDir, "train-00000-of-00001.parquet");
        String jsonRecord1 = "{\"instruction\": \"Explain quantum computing.\", \"output\": \"Quantum computing uses qubits.\"}";
        String jsonRecord2 = "{\"instruction\": \"What is an algorithm?\", \"output\": \"An algorithm is a set of step-by-step instructions.\"}";
        Files.writeString(parquetFile.toPath(), jsonRecord1 + "\n" + jsonRecord2 + "\n", StandardCharsets.UTF_8);

        // 2. Metadata files
        File readme = new File(alpacaDir, "README.md");
        Files.writeString(readme.toPath(), "# Alpaca Dataset\nThis is dataset documentation.", StandardCharsets.UTF_8);

        File gitattributes = new File(alpacaDir, ".gitattributes");
        Files.writeString(gitattributes.toPath(), "* text=auto\n", StandardCharsets.UTF_8);

        File datasetInfo = new File(alpacaDir, "dataset_info.json");
        Files.writeString(datasetInfo.toPath(), "{\"dataset_name\": \"alpaca\", \"splits\": [\"train\"]}", StandardCharsets.UTF_8);

        // Test inspection
        FolderAdapter folderAdapter = new FolderAdapter(SourceAdapterRegistry.createDefaultRegistry());
        DatasetItem folderItem = new DatasetItem(true, alpacaDir.getAbsolutePath(), "FOLDER");
        DatasetInspection inspection = folderAdapter.inspect(folderItem);

        assertTrue("Folder must be reported as supported", inspection.isSupported());
        assertTrue("Estimated bytes must be greater than 0", inspection.getEstimatedBytes() > 0);

        // Test dataset preparation
        DatasetPreparationService service = new DatasetPreparationService();
        DatasetPreparationContext context = new DatasetPreparationContext();
        File outDir = tempFolder.newFolder("out_alpaca");

        List<DatasetItem> items = new ArrayList<>();
        items.add(folderItem);

        DatasetPreparationResult result = service.prepareDatasets(items, context, outDir);

        assertEquals(DatasetPreparationResult.Status.SUCCESS, result.getStatus());
        assertNotNull(result.getArtifact());
        assertTrue(result.getSamples().size() >= 2);

        // Verify metadata content is NOT included in samples
        for (NormalizedSample sample : result.getSamples()) {
            String fullText = sample.toFullText();
            assertFalse("Sample must not contain README documentation", fullText.contains("This is dataset documentation"));
            assertFalse("Sample must not contain .gitattributes content", fullText.contains("* text=auto"));
        }
    }

    @Test
    public void test2_HeterogeneousSourcesMergedIntoOneEvodata() throws Exception {
        // 1. JSONL source
        File jsonlFile = tempFolder.newFile("source_chat.jsonl");
        JSONObject jsonlRow = new JSONObject();
        jsonlRow.put("instruction", "Write a Java class.");
        jsonlRow.put("response", "public class Foo {}");
        Files.writeString(jsonlFile.toPath(), jsonlRow.toString() + "\n", StandardCharsets.UTF_8);

        // 2. Parquet source
        File parquetFile = tempFolder.newFile("source_data.parquet");
        JSONObject parquetRow = new JSONObject();
        parquetRow.put("prompt", "Explain JVM.");
        parquetRow.put("completion", "JVM executes Java bytecode.");
        Files.writeString(parquetFile.toPath(), parquetRow.toString() + "\n", StandardCharsets.UTF_8);

        // 3. Plain Text source
        File txtFile = tempFolder.newFile("source_corpus.txt");
        Files.writeString(txtFile.toPath(), "Java is a multi-platform object-oriented language.", StandardCharsets.UTF_8);

        List<DatasetItem> items = new ArrayList<>();
        items.add(new DatasetItem(true, jsonlFile.getAbsolutePath(), "FILE"));
        items.add(new DatasetItem(true, parquetFile.getAbsolutePath(), "FILE"));
        items.add(new DatasetItem(true, txtFile.getAbsolutePath(), "FILE"));

        DatasetPreparationService service = new DatasetPreparationService();
        DatasetPreparationContext context = new DatasetPreparationContext();
        File outDir = tempFolder.newFolder("out_hetero");

        DatasetPreparationResult result = service.prepareDatasets(items, context, outDir);

        assertEquals(DatasetPreparationResult.Status.SUCCESS, result.getStatus());
        assertNotNull(result.getArtifact());
        assertTrue("Single .evodata artifact must contain samples from all 3 sources", result.getSamples().size() >= 3);
        assertEquals(3, result.getSuccessfulSources().size());
    }

    @Test
    public void test3_DuplicateSamplesAccountingInvariant() throws Exception {
        File txtFile = tempFolder.newFile("duplicates.txt");
        String content = "Unique sample line 1.\nUnique sample line 1.\nUnique sample line 2.\n";
        Files.writeString(txtFile.toPath(), content, StandardCharsets.UTF_8);

        File jsonlFile = tempFolder.newFile("duplicates.jsonl");
        JSONObject row = new JSONObject();
        row.put("instruction", "Hello");
        row.put("response", "World");
        // Write identical record twice
        Files.writeString(jsonlFile.toPath(), row.toString() + "\n" + row.toString() + "\n", StandardCharsets.UTF_8);

        List<DatasetItem> items = new ArrayList<>();
        items.add(new DatasetItem(true, txtFile.getAbsolutePath(), "FILE"));
        items.add(new DatasetItem(true, jsonlFile.getAbsolutePath(), "FILE"));

        DatasetPreparationService service = new DatasetPreparationService();
        DatasetPreparationContext context = new DatasetPreparationContext();
        File outDir = tempFolder.newFolder("out_dedupe");

        DatasetPreparationResult result = service.prepareDatasets(items, context, outDir);

        assertEquals(DatasetPreparationResult.Status.SUCCESS, result.getStatus());
        long read = result.getRecordsRead();
        long accepted = result.getRecordsAccepted();
        long rejected = result.getRecordsRejected();
        long duplicates = result.getDuplicateCount();

        assertEquals("Accounting invariant MUST hold: recordsRead = accepted + rejected + duplicates",
                read, accepted + rejected + duplicates);
        assertTrue("Duplicate count must be > 0", duplicates > 0);
    }

    @Test
    public void test4_SourceFailureIsolation() throws Exception {
        // Source A: Valid text file
        File validFile1 = tempFolder.newFile("valid1.txt");
        Files.writeString(validFile1.toPath(), "Valid content source A.", StandardCharsets.UTF_8);

        // Source B: Non-existent file
        File missingFile = new File(tempFolder.getRoot(), "non_existent_file.jsonl");

        // Source C: Valid JSONL file
        File validFile2 = tempFolder.newFile("valid2.jsonl");
        JSONObject jsonRow = new JSONObject();
        jsonRow.put("text", "Valid content source C.");
        Files.writeString(validFile2.toPath(), jsonRow.toString() + "\n", StandardCharsets.UTF_8);

        List<DatasetItem> items = new ArrayList<>();
        items.add(new DatasetItem(true, validFile1.getAbsolutePath(), "FILE"));
        items.add(new DatasetItem(true, missingFile.getAbsolutePath(), "FILE"));
        items.add(new DatasetItem(true, validFile2.getAbsolutePath(), "FILE"));

        DatasetPreparationService service = new DatasetPreparationService();
        DatasetPreparationContext context = new DatasetPreparationContext();
        File outDir = tempFolder.newFolder("out_isolation");

        DatasetPreparationResult result = service.prepareDatasets(items, context, outDir);

        // Status should be PARTIAL because 2 succeeded and 1 failed
        assertEquals(DatasetPreparationResult.Status.PARTIAL, result.getStatus());
        assertEquals(2, result.getSuccessfulSources().size());
        assertEquals(1, result.getFailedSources().size());
        assertNotNull(result.getArtifact());
        assertTrue("Valid sources A and C must be compiled into .evodata", result.getSamples().size() >= 2);
    }

    @Test
    public void test5_InsufficientSourceDataVsTargetBytes() throws Exception {
        File txtFile = tempFolder.newFile("small_source.txt");
        Files.writeString(txtFile.toPath(), "Small training text content.", StandardCharsets.UTF_8);

        List<DatasetItem> items = new ArrayList<>();
        items.add(new DatasetItem(true, txtFile.getAbsolutePath(), "FILE"));

        // Set target to 500 MB (524288000 bytes)
        long target500MB = 500L * 1024L * 1024L;
        DatasetPreparationService service = new DatasetPreparationService();
        DatasetPreparationContext context = new DatasetPreparationContext(target500MB, null);
        File outDir = tempFolder.newFolder("out_insufficient");

        DatasetPreparationResult result = service.prepareDatasets(items, context, outDir);

        assertEquals(DatasetPreparationResult.Status.SUCCESS, result.getStatus());
        assertNotNull(result.getArtifact());
        assertEquals("Artifact status must indicate INSUFFICIENT_SOURCE_DATA when under target bytes",
                EvoDatasetArtifact.Status.INSUFFICIENT_SOURCE_DATA, result.getArtifact().getStatus());
        assertTrue(new File(result.getOutputPath()).exists());
    }

    @Test
    public void test6_TargetReachedTruncation() throws Exception {
        File txtFile = tempFolder.newFile("corpus.txt");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 50; i++) {
            sb.append("This is training sample line ").append(i).append(" with substantial content.\n");
        }
        Files.writeString(txtFile.toPath(), sb.toString(), StandardCharsets.UTF_8);

        List<DatasetItem> items = new ArrayList<>();
        items.add(new DatasetItem(true, txtFile.getAbsolutePath(), "FILE"));

        // Set small target limit (100 bytes)
        long smallTarget = 100L;
        DatasetPreparationService service = new DatasetPreparationService();
        DatasetPreparationContext context = new DatasetPreparationContext(smallTarget, null);
        File outDir = tempFolder.newFolder("out_target_reached");

        DatasetPreparationResult result = service.prepareDatasets(items, context, outDir);

        assertEquals(DatasetPreparationResult.Status.SUCCESS, result.getStatus());
        assertNotNull(result.getArtifact());
        assertTrue("Truncation must restrict sample count when target is reached", result.getSamples().size() < 50);
        assertEquals("Artifact status must be READY when target size is reached or exceeded",
                EvoDatasetArtifact.Status.READY, result.getArtifact().getStatus());
    }

    @Test
    public void test7_MetadataOnlyFolderRejection() throws Exception {
        File metaDir = tempFolder.newFolder("metadata_only_dir");
        File readme = new File(metaDir, "README.md");
        Files.writeString(readme.toPath(), "# Metadata Only Repository", StandardCharsets.UTF_8);

        File gitignore = new File(metaDir, ".gitignore");
        Files.writeString(gitignore.toPath(), "target/\n.settings/\n", StandardCharsets.UTF_8);

        File gitattr = new File(metaDir, ".gitattributes");
        Files.writeString(gitattr.toPath(), "* text=auto\n", StandardCharsets.UTF_8);

        List<DatasetItem> items = new ArrayList<>();
        items.add(new DatasetItem(true, metaDir.getAbsolutePath(), "FOLDER"));

        DatasetPreparationService service = new DatasetPreparationService();
        DatasetPreparationContext context = new DatasetPreparationContext();
        File outDir = tempFolder.newFolder("out_meta_only");

        DatasetPreparationResult result = service.prepareDatasets(items, context, outDir);

        assertEquals("Metadata-only folder must produce FAILED status (0 usable samples)",
                DatasetPreparationResult.Status.FAILED, result.getStatus());
    }

    @Test
    public void test8_CacheInvalidationOnFileModification() throws Exception {
        File dataDir = tempFolder.newFolder("cache_test_dir");
        File jsonFile = new File(dataDir, "data.jsonl");
        JSONObject obj1 = new JSONObject();
        obj1.put("text", "Initial content version 1.");
        Files.writeString(jsonFile.toPath(), obj1.toString() + "\n", StandardCharsets.UTF_8);

        List<DatasetItem> items = new ArrayList<>();
        items.add(new DatasetItem(true, dataDir.getAbsolutePath(), "FOLDER"));

        DatasetPreparationService service = new DatasetPreparationService();
        DatasetPreparationContext context = new DatasetPreparationContext();
        File outDir = tempFolder.newFolder("out_cache");

        // First preparation
        DatasetPreparationResult result1 = service.prepareDatasets(items, context, outDir);
        assertEquals(DatasetPreparationResult.Status.SUCCESS, result1.getStatus());
        assertEquals(1, result1.getSamples().size());
        assertTrue(result1.getSamples().get(0).toFullText().contains("version 1"));

        // Modify file inside folder
        JSONObject obj2 = new JSONObject();
        obj2.put("text", "Updated content version 2.");
        Files.writeString(jsonFile.toPath(), obj1.toString() + "\n" + obj2.toString() + "\n", StandardCharsets.UTF_8);
        jsonFile.setLastModified(System.currentTimeMillis() + 2000);

        // Second preparation
        DatasetPreparationResult result2 = service.prepareDatasets(items, context, outDir);
        assertEquals(DatasetPreparationResult.Status.SUCCESS, result2.getStatus());
        assertEquals("Cache must be invalidated when child file in folder is modified", 2, result2.getSamples().size());
    }
}
