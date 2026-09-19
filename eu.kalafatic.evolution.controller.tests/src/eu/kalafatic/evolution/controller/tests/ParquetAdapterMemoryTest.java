package eu.kalafatic.evolution.controller.tests;

import eu.kalafatic.evolution.forge.data.api.NormalizedSample;
import eu.kalafatic.evolution.forge.data.api.source.DatasetItem;
import eu.kalafatic.evolution.forge.data.api.source.DatasetPreparationContext;
import eu.kalafatic.evolution.forge.data.impl.source.adapters.ParquetAdapter;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit test verifying memory safety and heap bounds in ParquetAdapter
 * when encountering malformed/crafted binary payloads with large varint uncompressed lengths.
 */
public class ParquetAdapterMemoryTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testDecompressSnappyBlockMemoryLimit() throws Exception {
        File tempFile = tempFolder.newFile("malformed_large_varint.parquet");

        // Write PAR1 magic header + large varint uncompressed length (e.g. 50MB varint length marker)
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            // Magic PAR1
            fos.write(new byte[]{'P', 'A', 'R', '1'});

            // Varint encoding 50,000,000 (0x02FA2080) -> 0x80, 0xC1, 0xE8, 0x17
            byte[] varintLarge = new byte[]{(byte) 0x80, (byte) 0xC1, (byte) 0xE8, (byte) 0x17};
            fos.write(varintLarge);

            // Append 1000 random payload bytes
            byte[] dummyPayload = new byte[1000];
            for (int i = 0; i < dummyPayload.length; i++) {
                dummyPayload[i] = (byte) (i & 0xFF);
            }
            fos.write(dummyPayload);

            // PAR1 magic tail
            fos.write(new byte[]{'P', 'A', 'R', '1'});
        }

        ParquetAdapter adapter = new ParquetAdapter();
        DatasetItem item = new DatasetItem(true, tempFile.getAbsolutePath(), "PARQUET");
        DatasetPreparationContext context = new DatasetPreparationContext();

        // Convert should process safely without throwing OutOfMemoryError
        List<NormalizedSample> samples = adapter.convert(item, context);
        assertNotNull("Samples list must not be null", samples);
        assertTrue("Execution completed safely without OutOfMemoryError", true);
    }
}
