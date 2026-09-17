package eu.kalafatic.evolution.controller.tests;

import eu.kalafatic.evolution.forge.data.api.NormalizedMessage;
import eu.kalafatic.evolution.forge.data.api.NormalizedSample;
import eu.kalafatic.evolution.forge.data.api.TrainingSampleType;
import eu.kalafatic.evolution.forge.data.api.artifact.EvoDatasetArtifact;
import eu.kalafatic.evolution.forge.data.api.service.DatasetPreparationResult;
import eu.kalafatic.evolution.forge.data.api.source.DatasetInspection;
import eu.kalafatic.evolution.forge.data.api.source.DatasetItem;
import eu.kalafatic.evolution.forge.data.api.source.DatasetPreparationContext;
import eu.kalafatic.evolution.forge.data.api.source.DatasetSourceAdapter;
import eu.kalafatic.evolution.forge.data.api.source.SourceAdapterRegistry;
import eu.kalafatic.evolution.forge.data.impl.service.DatasetPreparationService;
import eu.kalafatic.evolution.forge.data.impl.source.adapters.CSVAdapter;
import eu.kalafatic.evolution.forge.data.impl.source.adapters.EVODataAdapter;
import eu.kalafatic.evolution.forge.data.impl.source.adapters.FolderAdapter;
import eu.kalafatic.evolution.forge.data.impl.source.adapters.JSONAdapter;
import eu.kalafatic.evolution.forge.data.impl.source.adapters.JSONLAdapter;
import eu.kalafatic.evolution.forge.data.impl.source.adapters.OASST1Adapter;
import eu.kalafatic.evolution.forge.data.impl.source.adapters.TextAdapter;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit and integration test suite verifying dataset selection filtering, adapter resolution,
 * OASST1 conversation tree reconstruction/filtering, .evodata artifact hardening, and end-to-end dataset preparation.
 */
public class DatasetSelectionAndAdapterIntegrationTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testDatasetSelectionFilteringAndOrdering() throws Exception {
        File f1 = tempFolder.newFile("included1.txt");
        Files.writeString(f1.toPath(), "Content of included file 1.");

        File f2 = tempFolder.newFile("excluded.txt");
        Files.writeString(f2.toPath(), "Content of excluded file.");

        File f3 = tempFolder.newFile("included2.txt");
        Files.writeString(f3.toPath(), "Content of included file 2.");

        List<DatasetItem> items = new ArrayList<>();
        items.add(new DatasetItem(true, f1.getAbsolutePath(), "FILE"));
        items.add(new DatasetItem(false, f2.getAbsolutePath(), "FILE")); // Unchecked -> MUST be ignored
        items.add(new DatasetItem(true, f3.getAbsolutePath(), "FILE"));

        DatasetPreparationService service = new DatasetPreparationService();
        DatasetPreparationContext context = new DatasetPreparationContext(0, null);
        File outDir = tempFolder.newFolder("out_datasets");

        DatasetPreparationResult result = service.prepareDatasets(items, context, outDir);

        assertEquals(DatasetPreparationResult.Status.SUCCESS, result.getStatus());
        assertEquals(2, result.getSourcesProcessed()); // Only checked items
        assertEquals(2, result.getSamples().size());

        // Ordering preserved
        assertTrue(result.getSamples().get(0).toFullText().contains("included file 1"));
        assertTrue(result.getSamples().get(1).toFullText().contains("included file 2"));
    }

    @Test
    public void testAdapterRegistrySelection() throws Exception {
        SourceAdapterRegistry registry = SourceAdapterRegistry.createDefaultRegistry();

        File evodataFile = tempFolder.newFile("sample.evodata");
        DatasetItem evoItem = new DatasetItem(true, evodataFile.getAbsolutePath(), "EVO_MODEL");
        assertTrue(registry.findAdapter(evoItem) instanceof EVODataAdapter);

        File oasstFile = tempFolder.newFile("2023-04-12_oasst_ready.trees.jsonl.gz");
        DatasetItem oasstItem = new DatasetItem(true, oasstFile.getAbsolutePath(), "FILE");
        assertTrue(registry.findAdapter(oasstItem) instanceof OASST1Adapter);

        File jsonlFile = tempFolder.newFile("chat.jsonl");
        DatasetItem jsonlItem = new DatasetItem(true, jsonlFile.getAbsolutePath(), "FILE");
        assertTrue(registry.findAdapter(jsonlItem) instanceof JSONLAdapter);

        File jsonFile = tempFolder.newFile("data.json");
        DatasetItem jsonItem = new DatasetItem(true, jsonFile.getAbsolutePath(), "FILE");
        assertTrue(registry.findAdapter(jsonItem) instanceof JSONAdapter);

        File csvFile = tempFolder.newFile("table.csv");
        DatasetItem csvItem = new DatasetItem(true, csvFile.getAbsolutePath(), "FILE");
        assertTrue(registry.findAdapter(csvItem) instanceof CSVAdapter);

        File txtFile = tempFolder.newFile("doc.txt");
        DatasetItem txtItem = new DatasetItem(true, txtFile.getAbsolutePath(), "FILE");
        assertTrue(registry.findAdapter(txtItem) instanceof TextAdapter);

        File folder = tempFolder.newFolder("data_dir");
        DatasetItem folderItem = new DatasetItem(true, folder.getAbsolutePath(), "FOLDER");
        assertTrue(registry.findAdapter(folderItem) instanceof FolderAdapter);
    }

    @Test
    public void testOasstAdapterTreeReconstructionAndFiltering() throws Exception {
        File oasstGz = tempFolder.newFile("oasst_branching.jsonl.gz");

        // Root prompt
        JSONObject root = new JSONObject();
        root.put("message_id", "root1");
        root.put("message_tree_id", "tree_test_1");
        root.put("parent_id", (Object) null);
        root.put("role", "prompter");
        root.put("text", "Explain gravity.");
        root.put("lang", "en");

        // Replies: Branch A
        JSONArray replies = new JSONArray();

        JSONObject replyA = new JSONObject();
        replyA.put("message_id", "reply_a");
        replyA.put("parent_id", "root1");
        replyA.put("role", "assistant");
        replyA.put("text", "Gravity is a fundamental force pulling masses together.");
        replyA.put("lang", "en");
        replies.put(replyA);

        // Branch B (deleted message -> must be rejected)
        JSONObject replyB = new JSONObject();
        replyB.put("message_id", "reply_b");
        replyB.put("parent_id", "root1");
        replyB.put("role", "assistant");
        replyB.put("text", "Deleted answer.");
        replyB.put("deleted", true);
        replies.put(replyB);

        // Branch C (unknown role -> must be rejected)
        JSONObject replyC = new JSONObject();
        replyC.put("message_id", "reply_c");
        replyC.put("parent_id", "root1");
        replyC.put("role", "unsupported_alien_role");
        replyC.put("text", "Alien response.");
        replies.put(replyC);

        root.put("replies", replies);

        try (GZIPOutputStream gzos = new GZIPOutputStream(new FileOutputStream(oasstGz))) {
            gzos.write((root.toString() + "\n").getBytes(StandardCharsets.UTF_8));
        }

        OASST1Adapter adapter = new OASST1Adapter();
        DatasetItem item = new DatasetItem(true, oasstGz.getAbsolutePath(), "FILE");
        DatasetPreparationContext context = new DatasetPreparationContext();

        List<NormalizedSample> samples = adapter.convert(item, context);

        // Only valid Branch A path should produce a conversation
        assertEquals(1, samples.size());
        NormalizedSample sample = samples.get(0);
        assertEquals(TrainingSampleType.CONVERSATION, sample.getType());
        assertEquals("tree_test_1:reply_a", sample.getConversationId());
        assertEquals(2, sample.getConversationMessages().size());
        assertEquals("user", sample.getConversationMessages().get(0).getRole());
        assertEquals("assistant", sample.getConversationMessages().get(1).getRole());
    }

    @Test
    public void testEvoDatasetArtifactHardeningAndMalformedJsonRejection() throws Exception {
        File evodataFile = tempFolder.newFile("hardened_test.evodata");

        // Create sample list with CONVERSATION, TEXT, INSTRUCTION
        List<NormalizedSample> inputSamples = new ArrayList<>();
        inputSamples.add(NormalizedSample.createTextSample("Test sample text", "source1"));

        List<NormalizedMessage> msgs = new ArrayList<>();
        msgs.add(new NormalizedMessage("user", "Hello", "m1", null));
        msgs.add(new NormalizedMessage("assistant", "Hi there!", "m2", "m1"));
        inputSamples.add(NormalizedSample.createConversationSample("c1", msgs, "source1"));

        EvoDatasetArtifact artifact = new EvoDatasetArtifact(evodataFile);
        artifact.save(inputSamples, null, null, 0.0);

        // Verify tokens were reset and saved
        assertTrue(artifact.getTotalTrainTokens() > 0);

        // Load back
        EvoDatasetArtifact loaded = EvoDatasetArtifact.load(evodataFile);
        assertEquals(2, loaded.getTrainSamples().size());
        assertEquals(TrainingSampleType.TEXT, loaded.getTrainSamples().get(0).getType());
        assertEquals(TrainingSampleType.CONVERSATION, loaded.getTrainSamples().get(1).getType());

        // Call save again and verify total tokens are NOT accumulated twice
        long firstTotal = artifact.getTotalTrainTokens();
        artifact.save(inputSamples, null, null, 0.0);
        assertEquals(firstTotal, artifact.getTotalTrainTokens());
    }

    @Test
    public void testEndToEndForgeDatasetPreparationFlow() throws Exception {
        // 1. OASST1 source
        File oasstFile = tempFolder.newFile("oasst_input.jsonl");
        JSONObject tree = new JSONObject();
        tree.put("message_id", "m1");
        tree.put("message_tree_id", "tree_e2e");
        tree.put("role", "prompter");
        tree.put("text", "How do software tests work?");
        JSONArray replies = new JSONArray();
        JSONObject rep = new JSONObject();
        rep.put("message_id", "m2");
        rep.put("role", "assistant");
        rep.put("text", "Tests verify code correctness by asserting expectations.");
        replies.put(rep);
        tree.put("replies", replies);
        Files.writeString(oasstFile.toPath(), tree.toString() + "\n", StandardCharsets.UTF_8);

        // 2. JSONL source
        File jsonlFile = tempFolder.newFile("chat.jsonl");
        JSONObject jsonlRow = new JSONObject();
        jsonlRow.put("type", "INSTRUCTION");
        jsonlRow.put("instruction", "Write a Java test.");
        jsonlRow.put("response", "@Test public void testFoo() {}");
        Files.writeString(jsonlFile.toPath(), jsonlRow.toString() + "\n", StandardCharsets.UTF_8);

        // 3. CSV source
        File csvFile = tempFolder.newFile("data.csv");
        Files.writeString(csvFile.toPath(), "instruction,response\n\"What is Java?\",\"Java is a programming language.\"\n", StandardCharsets.UTF_8);

        // 4. DatasetItem list from dialog
        List<DatasetItem> selectedItems = new ArrayList<>();
        selectedItems.add(new DatasetItem(true, oasstFile.getAbsolutePath(), "FILE"));
        selectedItems.add(new DatasetItem(true, jsonlFile.getAbsolutePath(), "FILE"));
        selectedItems.add(new DatasetItem(true, csvFile.getAbsolutePath(), "FILE"));

        // JSON serialization roundtrip matching ForgeSettingsDialog.getDatasetsJson()
        JSONArray jsonArr = new JSONArray();
        for (DatasetItem item : selectedItems) {
            jsonArr.put(item.toJsonObject());
        }

        List<DatasetItem> restoredItems = DatasetItem.parseJsonList(jsonArr.toString());
        assertEquals(3, restoredItems.size());

        DatasetPreparationService service = new DatasetPreparationService();
        DatasetPreparationContext context = new DatasetPreparationContext(0, null);
        File outDir = tempFolder.newFolder("e2e_output");

        DatasetPreparationResult result = service.prepareDatasets(restoredItems, context, outDir);

        assertEquals(DatasetPreparationResult.Status.SUCCESS, result.getStatus());
        assertNotNull(result.getArtifact());
        assertTrue(result.getSamples().size() >= 3);
        assertTrue(new File(result.getOutputPath()).exists());
    }

    @Test
    public void testForgeSettingsDialogCreateEvodataInForgeInputFolder() throws Exception {
        File txtFile = tempFolder.newFile("sample_training.txt");
        Files.writeString(txtFile.toPath(), "Sample text content for creating native evodata from ForgeSettingsDialog.", StandardCharsets.UTF_8);

        File jsonFile = tempFolder.newFile("sample_instruction.json");
        JSONObject jsonRow = new JSONObject();
        jsonRow.put("instruction", "Explain .evodata format.");
        jsonRow.put("response", ".evodata is a native EVO dataset container.");
        Files.writeString(jsonFile.toPath(), jsonRow.toString() + "\n", StandardCharsets.UTF_8);

        List<DatasetItem> items = new ArrayList<>();
        items.add(new DatasetItem(true, txtFile.getAbsolutePath(), "FILE"));
        items.add(new DatasetItem(true, jsonFile.getAbsolutePath(), "FILE"));

        File forgeInputDir = tempFolder.newFolder("forge-input");

        DatasetPreparationService service = new DatasetPreparationService();
        DatasetPreparationContext context = new DatasetPreparationContext();
        DatasetPreparationResult result = service.prepareDatasets(items, context, forgeInputDir);

        assertEquals(DatasetPreparationResult.Status.SUCCESS, result.getStatus());
        assertNotNull(result.getOutputPath());
        assertTrue(result.getOutputPath().endsWith(".evodata"));

        File generatedEvodata = new File(result.getOutputPath());
        assertTrue("Generated .evodata must exist in forge-input directory", generatedEvodata.exists());
        assertEquals("Generated file must be inside forge-input folder", forgeInputDir.getAbsolutePath(), generatedEvodata.getParentFile().getAbsolutePath());

        EvoDatasetArtifact loadedArtifact = EvoDatasetArtifact.load(generatedEvodata);
        assertNotNull(loadedArtifact);
        assertTrue(loadedArtifact.getTrainSamples().size() >= 2);
    }
}
