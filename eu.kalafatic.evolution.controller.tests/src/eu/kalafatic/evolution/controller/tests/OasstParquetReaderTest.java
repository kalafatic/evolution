package eu.kalafatic.evolution.controller.tests;

import eu.kalafatic.evolution.forge.data.api.NormalizedMessage;
import eu.kalafatic.evolution.forge.data.api.NormalizedSample;
import eu.kalafatic.evolution.forge.data.api.TrainingSampleType;
import eu.kalafatic.evolution.forge.data.api.source.DatasetInspection;
import eu.kalafatic.evolution.forge.data.api.source.DatasetItem;
import eu.kalafatic.evolution.forge.data.api.source.DatasetPreparationContext;
import eu.kalafatic.evolution.forge.data.impl.source.adapters.OASST1Adapter;
import eu.kalafatic.evolution.forge.data.impl.source.adapters.OasstParquetReader;

import org.json.JSONObject;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Regression tests for the OASST1 Parquet reader and the repaired OASST1Adapter.
 */
public class OasstParquetReaderTest {

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    private static final File OASST_DIR = new File("/home/petr/workspace/forge-input/OpenAssist/oasst1");

    @Test
    public void testIsOasstParquetRejectsNonParquetFile() throws Exception {
        File f = temp.newFile("notes.txt");
        try (FileOutputStream out = new FileOutputStream(f)) {
            out.write("not a parquet file".getBytes(StandardCharsets.UTF_8));
        }
        assertFalse("Plain text file must not be identified as OASST1 parquet",
                OasstParquetReader.isOasstParquet(f));
    }

    @Test
    public void testIsOasstParquetRejectsGarbageParquet() throws Exception {
        File f = temp.newFile("junk.parquet");
        try (FileOutputStream out = new FileOutputStream(f)) {
            out.write("PAR1".getBytes(StandardCharsets.US_ASCII));
            out.write(new byte[64]);
            out.write("PAR1".getBytes(StandardCharsets.US_ASCII));
            out.write(new byte[]{0x04, 0x00, 0x00, 0x00});
        }
        assertFalse("Garbage PAR1 file must not be identified as OASST1 parquet",
                OasstParquetReader.isOasstParquet(f));
    }

    @Test
    public void testAdapterSupportsRejectsGarbageParquet() throws Exception {
        File f = temp.newFile("junk.parquet");
        try (FileOutputStream out = new FileOutputStream(f)) {
            out.write("PAR1".getBytes(StandardCharsets.US_ASCII));
            out.write(new byte[64]);
        }
        OASST1Adapter adapter = new OASST1Adapter();
        assertFalse("OASST1Adapter must not claim a non-OASST parquet file",
                adapter.supports(new DatasetItem(true, f.getAbsolutePath(), "FILE")));
    }

    @Test
    public void testAdapterInspectEmptyDirectoryIsInvalid() throws Exception {
        File dir = temp.newFolder("empty_oasst1");
        OASST1Adapter adapter = new OASST1Adapter();
        DatasetItem item = new DatasetItem(true, dir.getAbsolutePath(), "FOLDER");
        assertTrue("A directory named oasst1 is claimed by the adapter", adapter.supports(item));
        DatasetInspection inspection = adapter.inspect(item);
        assertFalse("An empty directory must never report VALID", inspection.isSupported());
    }

    @Test
    public void testFlatMessagesRoleNormalization() {
        List<JSONObject> raw = new ArrayList<>();
        raw.add(msg("m1", null, "prompter", "What is the capital of France?", "t1"));
        raw.add(msg("m2", "m1", "assistant", "Paris.", "t1"));
        List<NormalizedSample> samples =
                OASST1Adapter.convertFlatMessages(raw, "test", new DatasetPreparationContext());
        assertFalse("Expected at least one conversation", samples.isEmpty());
        NormalizedSample s = samples.get(0);
        assertEquals(TrainingSampleType.CONVERSATION, s.getType());
        List<NormalizedMessage> messages = s.getConversationMessages();
        assertEquals(2, messages.size());
        assertEquals("user", messages.get(0).getRole());
        assertEquals("assistant", messages.get(1).getRole());
    }

    @Test
    public void testReadRealOasstParquetWhenPresent() {
        Assume.assumeTrue("OASST1 dataset directory not present; skipping",
                OASST_DIR.exists() && OASST_DIR.isDirectory());

        OASST1Adapter adapter = new OASST1Adapter();
        DatasetItem item = new DatasetItem(true, OASST_DIR.getAbsolutePath(), "FOLDER");
        assertTrue("Adapter must support the real OASST1 directory", adapter.supports(item));

        DatasetInspection inspection = adapter.inspect(item);
        assertTrue("Real OASST1 directory must report VALID", inspection.isSupported());
        assertTrue("Expected at least one data file", inspection.getEstimatedBytes() > 0);

        List<NormalizedSample> samples;
        try {
            samples = adapter.convert(item, new DatasetPreparationContext(500_000_000L, msg -> {}));
        } catch (Exception ex) {
            throw new AssertionError("Convert failed", ex);
        }
        assertFalse("Real OASST1 dataset must produce samples", samples.isEmpty());

        int conversations = 0;
        int messages = 0;
        boolean sawUser = false;
        boolean sawAssistant = false;
        for (NormalizedSample s : samples) {
            if (s.getType() == TrainingSampleType.CONVERSATION) conversations++;
            if (s.getConversationMessages() != null) {
                messages += s.getConversationMessages().size();
                for (NormalizedMessage m : s.getConversationMessages()) {
                    if ("user".equals(m.getRole())) sawUser = true;
                    if ("assistant".equals(m.getRole())) sawAssistant = true;
                }
            }
        }
        assertTrue("Expected non-zero conversations", conversations > 0);
        assertTrue("Expected non-zero messages", messages > 0);
        assertTrue("Expected user messages (prompter normalized)", sawUser);
        assertTrue("Expected assistant messages", sawAssistant);
    }

    private static JSONObject msg(String id, String parentId, String role, String text, String treeId) {
        JSONObject o = new JSONObject();
        o.put("message_id", id);
        if (parentId != null) o.put("parent_id", parentId);
        o.put("role", role);
        o.put("text", text);
        o.put("message_tree_id", treeId);
        o.put("lang", "en");
        o.put("deleted", false);
        return o;
    }
}
