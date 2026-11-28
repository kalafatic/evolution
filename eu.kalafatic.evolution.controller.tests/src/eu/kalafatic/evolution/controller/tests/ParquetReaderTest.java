package eu.kalafatic.evolution.controller.tests;

import eu.kalafatic.evolution.forge.data.api.NormalizedSample;
import eu.kalafatic.evolution.forge.data.api.TrainingSampleType;
import eu.kalafatic.evolution.forge.data.api.source.DatasetInspection;
import eu.kalafatic.evolution.forge.data.api.source.DatasetItem;
import eu.kalafatic.evolution.forge.data.api.source.DatasetPreparationContext;
import eu.kalafatic.evolution.forge.data.impl.source.adapters.ParquetAdapter;
import eu.kalafatic.evolution.forge.data.impl.source.adapters.ParquetReader;

import org.json.JSONObject;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Regression tests for the real {@link ParquetReader} codec and the rewritten
 * {@link ParquetAdapter}. Uses real HuggingFace Parquet datasets (not mocked byte streams)
 * when present; corrupted-file, schema and codec-behaviour tests are always run.
 */
public class ParquetReaderTest {

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    private static final File OASST_TRAIN =
            new File("/home/petr/workspace/forge-input/OpenAssist/oasst1/train-00000-of-00001-b42a775f407cee45.parquet");
    private static final File SMOLTALK =
            new File("/home/petr/workspace/forge-input/smoltalk/0000.parquet");
    private static final File COSMOPEDIA =
            new File("/home/petr/workspace/forge-input/cosmopedia/0000.parquet");
    private static final File ALPACA =
            new File("/home/petr/workspace/forge-input/alpaca/data/train-00000-of-00001-a09b74b3ef9c3b56.parquet");
    private static final File WIKITEXT =
            new File("/home/petr/workspace/forge-input/wikitext/0000.parquet");

    // ------------------------------------------------------------------
    // Corrupted / non-parquet inputs (always run)
    // ------------------------------------------------------------------

    @Test
    public void testReadSchemaRejectsNonParquetFile() throws Exception {
        File f = temp.newFile("notes.txt");
        try (FileOutputStream out = new FileOutputStream(f)) {
            out.write("not a parquet file".getBytes(StandardCharsets.UTF_8));
        }
        try {
            ParquetReader.readSchema(f);
            fail("Expected IOException for a non-parquet file");
        } catch (java.io.IOException expected) {
            assertTrue(expected.getMessage().contains("Not a valid Parquet"));
        }
    }

    @Test
    public void testReadSchemaRejectsTruncatedParquet() throws Exception {
        File f = temp.newFile("junk.parquet");
        try (FileOutputStream out = new FileOutputStream(f)) {
            out.write("PAR1".getBytes(StandardCharsets.US_ASCII));
            out.write(new byte[32]);
            out.write("PAR1".getBytes(StandardCharsets.US_ASCII));
            out.write(new byte[]{0x04, 0x00, 0x00, 0x00});
        }
        try {
            ParquetReader.readSchema(f);
            fail("Expected IOException for a truncated PAR1 file");
        } catch (java.io.IOException expected) {
            // expected
        }
    }

    @Test
    public void testAdapterInspectRejectsGarbageParquet() throws Exception {
        File f = temp.newFile("junk.parquet");
        try (FileOutputStream out = new FileOutputStream(f)) {
            out.write("PAR1".getBytes(StandardCharsets.US_ASCII));
            out.write(new byte[64]);
        }
        ParquetAdapter adapter = new ParquetAdapter();
        DatasetInspection inspection = adapter.inspect(new DatasetItem(true, f.getAbsolutePath(), "FILE"));
        assertFalse("A garbage .parquet must not report VALID", inspection.isSupported());
    }

    // ------------------------------------------------------------------
    // Real dataset tests (skipped when datasets absent)
    // ------------------------------------------------------------------

    @Test
    public void testReadOasstSchemaAndRows() throws Exception {
        Assume.assumeTrue("OASST1 parquet not present", OASST_TRAIN.exists());
        ParquetReader.SchemaInfo schema = ParquetReader.readSchema(OASST_TRAIN);
        assertTrue(schema.numRows > 0);
        assertTrue(schema.rowGroups > 0);
        assertFalse(schema.columns.isEmpty());

        // 84,437 rows in the canonical OASST1 training shard.
        assertEquals(84437L, schema.numRows);

        AtomicInteger count = new AtomicInteger();
        final boolean[] sawText = {false};
        final boolean[] sawRole = {false};
        final boolean[] sawBool = {false};
        ParquetReader.forEachRow(OASST_TRAIN, row -> {
            count.incrementAndGet();
            if (row.has("text") && !row.isNull("text")) sawText[0] = true;
            if (row.has("role")) sawRole[0] = true;
            if (row.has("deleted") && !row.isNull("deleted")) sawBool[0] = true;
        });
        assertEquals(84437, count.get());
        assertTrue("Expected text column", sawText[0]);
        assertTrue("Expected role column", sawRole[0]);
        assertTrue("Expected boolean (deleted) column", sawBool[0]);
    }

    @Test
    public void testReadNestedMessagesStruct() throws Exception {
        Assume.assumeTrue("SmolTalk parquet not present", SMOLTALK.exists());
        ParquetReader.SchemaInfo schema = ParquetReader.readSchema(SMOLTALK);
        assertTrue(schema.numRows > 0);

        final int[] conversations = {0};
        final boolean[] sawContent = {false};
        final boolean[] sawRole = {false};
        ParquetReader.forEachRow(SMOLTALK, row -> {
            if (row.has("messages.list.element.content")) {
                sawContent[0] = true;
                if (row.optJSONArray("messages.list.element.content").length() >= 2) {
                    conversations[0]++;
                }
            }
            if (row.has("messages.list.element.role")) sawRole[0] = true;
        });
        assertTrue("Expected nested message content", sawContent[0]);
        assertTrue("Expected nested message role", sawRole[0]);
        assertTrue("Expected multi-turn conversations", conversations[0] > 0);
    }

    @Test
    public void testReadInt64Column() throws Exception {
        Assume.assumeTrue("Cosmopedia parquet not present", COSMOPEDIA.exists());
        ParquetReader.SchemaInfo schema = ParquetReader.readSchema(COSMOPEDIA);
        boolean hasInt64 = false;
        for (ParquetReader.ColumnInfo ci : schema.columns) {
            if (ci.type == 2) hasInt64 = true;
        }
        assertTrue("Cosmopedia has an INT64 (text_token_length) column", hasInt64);

        final boolean[] sawNumber = {false};
        final boolean[] sawText = {false};
        ParquetReader.forEachRow(COSMOPEDIA, row -> {
            if (row.has("text_token_length")) sawNumber[0] = true;
            if (row.has("text")) sawText[0] = true;
        });
        assertTrue(sawNumber[0]);
        assertTrue(sawText[0]);
    }

    @Test
    public void testAdapterProducesInstructionSamples() throws Exception {
        Assume.assumeTrue("Alpaca parquet not present", ALPACA.exists());
        ParquetAdapter adapter = new ParquetAdapter();
        DatasetItem item = new DatasetItem(true, ALPACA.getAbsolutePath(), "FILE");
        DatasetInspection inspection = adapter.inspect(item);
        assertTrue("Alpaca must report VALID", inspection.isSupported());

        List<NormalizedSample> samples = adapter.convert(item, new DatasetPreparationContext(2_000_000_000L, m -> {}));
        assertFalse(samples.isEmpty());
        int instructions = 0;
        for (NormalizedSample s : samples) {
            if (s.getType() == TrainingSampleType.INSTRUCTION) instructions++;
        }
        assertTrue("Expected instruction samples", instructions > 0);
    }

    @Test
    public void testAdapterProducesConversationSamples() throws Exception {
        Assume.assumeTrue("SmolTalk parquet not present", SMOLTALK.exists());
        ParquetAdapter adapter = new ParquetAdapter();
        List<NormalizedSample> samples = adapter.convert(
                new DatasetItem(true, SMOLTALK.getAbsolutePath(), "FILE"),
                new DatasetPreparationContext(2_000_000_000L, m -> {}));
        assertFalse(samples.isEmpty());
        int conversations = 0;
        for (NormalizedSample s : samples) {
            if (s.getType() == TrainingSampleType.CONVERSATION) conversations++;
        }
        assertTrue("Expected conversation samples from nested messages", conversations > 0);
    }

    @Test
    public void testAdapterProducesTextSamples() throws Exception {
        Assume.assumeTrue("Wikitext parquet not present", WIKITEXT.exists());
        ParquetAdapter adapter = new ParquetAdapter();
        List<NormalizedSample> samples = adapter.convert(
                new DatasetItem(true, WIKITEXT.getAbsolutePath(), "FILE"),
                new DatasetPreparationContext(2_000_000_000L, m -> {}));
        assertFalse(samples.isEmpty());
        assertNotNull(samples.get(0).getText());
    }
}
