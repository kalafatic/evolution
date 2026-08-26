package eu.kalafatic.evolution.controller.tests;

import eu.kalafatic.evolution.forge.agent.export.OllamaExporter;
import eu.kalafatic.evolution.forge.agent.gguf.*;
import eu.kalafatic.evolution.forge.model.llm.EvoLlmArchitecture;
import eu.kalafatic.evolution.forge.model.llm.EvoLlmModel;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive Unit, Round-trip, and Self-corruption test suite for GGUFReader and GGUFValidator.
 * Tests all 11 required corruption scenarios explicitly.
 */
public class GGUFReaderAndValidatorTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private Path exportDir;
    private Path validGgufPath;
    private EvoLlmModel tinyModel;
    private Map<Integer, String> tinyVocab;

    @Before
    public void setUp() throws Exception {
        exportDir = tempFolder.newFolder("gguf_test_export").toPath();

        // Construct tiny deterministic model fixture for fast testing
        // Vocab=16, DModel=8, Heads=2, Blocks=1, DFF=16, MaxSeqLen=32
        EvoLlmArchitecture arch = new EvoLlmArchitecture(16, 8, 2, 1, 16, 32);
        tinyModel = new EvoLlmModel(arch);

        // Populate vocabulary
        tinyVocab = new LinkedHashMap<>();
        tinyVocab.put(0, "<unk>");
        tinyVocab.put(1, "<s>");
        tinyVocab.put(2, "</s>");
        tinyVocab.put(3, " ");
        for (int i = 4; i < 16; i++) {
            tinyVocab.put(i, "token_" + i);
        }

        // Export tiny GGUF file
        OllamaExporter exporter = new OllamaExporter();
        exporter.export("tiny-evo", exportDir, tinyModel, tinyVocab);

        validGgufPath = exportDir.resolve("exports/ollama/evo.gguf");
        assertTrue("Exported GGUF file must exist", Files.exists(validGgufPath));
        assertTrue("GGUF file size must be > 0", Files.size(validGgufPath) > 0);
    }

    @Test
    public void testValidGGUFReaderParsing() throws Exception {
        try (GGUFReader reader = new GGUFReader(validGgufPath)) {
            assertNotNull("Header should not be null", reader.getHeader());
            assertEquals("Magic must be GGUF", "GGUF", reader.getHeader().getMagic());
            assertTrue("Version must be 2 or 3", reader.getHeader().getVersion() == 2 || reader.getHeader().getVersion() == 3);

            assertTrue("Metadata count > 0", reader.getHeader().getMetadataCount() > 0);
            assertTrue("Tensor count > 0", reader.getHeader().getTensorCount() > 0);

            // Check metadata
            Map<String, GGUFMetadata> metaMap = reader.getMetadataMap();
            assertTrue(metaMap.containsKey("general.architecture"));
            assertEquals("llama", metaMap.get("general.architecture").getValue());
            assertEquals(16L, ((Number) metaMap.get("llama.vocab_size").getValue()).longValue());
            assertEquals(8L, ((Number) metaMap.get("llama.embedding_length").getValue()).longValue());

            // Check tensor info
            Map<String, GGUFTensorInfo> tensorMap = reader.getTensorMap();
            assertTrue(tensorMap.containsKey("token_embd.weight"));
            assertTrue(tensorMap.containsKey("blk.0.attn_q.weight"));
            assertTrue(tensorMap.containsKey("output.weight"));

            GGUFTensorInfo embInfo = tensorMap.get("token_embd.weight");
            assertEquals(GGMLType.F32, embInfo.getGgmlType());
            assertEquals(128, embInfo.getElementCount()); // 16 * 8
            assertEquals(512, embInfo.getByteSize()); // 128 * 4

            // Test float reading
            float[] embFloats = reader.readTensorFloatData("token_embd.weight");
            assertEquals(128, embFloats.length);
        }
    }

    @Test
    public void testValidGGUFValidatorPass() throws Exception {
        GGUFValidationReport report = GGUFValidator.validate(validGgufPath, tinyModel, tinyVocab);
        System.out.println(report.generateSummary());

        assertTrue("Validation report must be valid for pristine GGUF", report.isValid());
        assertTrue("Structure must be valid", report.isStructureValid());
        assertTrue("Metadata must be valid", report.isMetadataValid());
        assertTrue("Tensors must be valid", report.isTensorsValid());
        assertTrue("Semantics must be valid", report.isSemanticsValid());
        assertTrue("Errors list should be empty", report.getErrors().isEmpty());
    }

    // 1. Corrupt magic
    @Test
    public void testCorruption1CorruptMagic() throws Exception {
        Path corrupted = copyFile(validGgufPath, "corrupt_1_magic.gguf");
        try (RandomAccessFile raf = new RandomAccessFile(corrupted.toFile(), "rw")) {
            raf.seek(0);
            raf.write("BADF".getBytes());
        }

        GGUFValidationReport report = GGUFValidator.validate(corrupted, tinyModel, tinyVocab);
        assertFalse("Validator must reject corrupted magic", report.isValid());
        assertFalse("Structure must be invalid", report.isStructureValid());
        assertTrue("Errors must contain magic error", report.getErrors().stream().anyMatch(e -> e.contains("Invalid magic header")));
    }

    // 2. Corrupt version
    @Test
    public void testCorruption2CorruptVersion() throws Exception {
        Path corrupted = copyFile(validGgufPath, "corrupt_2_version.gguf");
        try (RandomAccessFile raf = new RandomAccessFile(corrupted.toFile(), "rw")) {
            raf.seek(4); // Version offset = 4
            raf.writeInt(Integer.reverseBytes(99)); // Write 99 in Little-Endian
        }

        GGUFValidationReport report = GGUFValidator.validate(corrupted, tinyModel, tinyVocab);
        assertFalse("Validator must reject invalid version", report.isValid());
        assertTrue("Errors must mention unsupported version", report.getErrors().stream().anyMatch(e -> e.contains("Unsupported version")));
    }

    // 3. Corrupt metadata count
    @Test
    public void testCorruption3CorruptMetadataCount() throws Exception {
        Path corrupted = copyFile(validGgufPath, "corrupt_3_metacount.gguf");
        try (RandomAccessFile raf = new RandomAccessFile(corrupted.toFile(), "rw")) {
            raf.seek(16); // Metadata count offset = 16
            raf.writeLong(Long.reverseBytes(9999L));
        }

        GGUFValidationReport report = GGUFValidator.validate(corrupted, tinyModel, tinyVocab);
        assertFalse("Validator must reject metadata count mismatch", report.isValid());
    }

    // 4. Corrupt tensor count
    @Test
    public void testCorruption4CorruptTensorCount() throws Exception {
        Path corrupted = copyFile(validGgufPath, "corrupt_4_tensorcount.gguf");
        try (RandomAccessFile raf = new RandomAccessFile(corrupted.toFile(), "rw")) {
            raf.seek(8); // Tensor count offset = 8
            raf.writeLong(Long.reverseBytes(9999L));
        }

        GGUFValidationReport report = GGUFValidator.validate(corrupted, tinyModel, tinyVocab);
        assertFalse("Validator must reject tensor count mismatch", report.isValid());
        assertTrue("Errors must mention tensor count mismatch", report.getErrors().stream().anyMatch(e -> e.contains("Tensor count mismatch")));
    }

    // 5. Corrupt tensor offset
    @Test
    public void testCorruption5CorruptTensorOffset() throws Exception {
        Path corrupted = copyFile(validGgufPath, "corrupt_5_tensoroffset.gguf");
        try (GGUFReader reader = new GGUFReader(validGgufPath)) {
            GGUFTensorInfo info = reader.getTensorMap().get("token_embd.weight");
            long descOffset = info.getDescriptorOffset();

            // Descriptor structure: string (8 + len) + nDims (4) + dims (nDims * 8) + typeId (4) + ggufTensorOffset (8)
            long offsetFieldPos = descOffset + 8 + info.getName().length() + 4 + (info.getDimensions().length * 8) + 4;

            try (RandomAccessFile raf = new RandomAccessFile(corrupted.toFile(), "rw")) {
                raf.seek(offsetFieldPos);
                raf.writeLong(Long.reverseBytes(9_999_999L)); // Set tensor offset past file size
            }
        }

        GGUFValidationReport report = GGUFValidator.validate(corrupted, tinyModel, tinyVocab);
        assertFalse("Validator must reject tensor offset outside file bounds", report.isValid());
    }

    // 6. Corrupt tensor dimension
    @Test
    public void testCorruption6CorruptTensorDimension() throws Exception {
        Path corrupted = copyFile(validGgufPath, "corrupt_6_tensordim.gguf");
        try (GGUFReader reader = new GGUFReader(validGgufPath)) {
            GGUFTensorInfo info = reader.getTensorMap().get("token_embd.weight");
            long descOffset = info.getDescriptorOffset();
            long dim0Pos = descOffset + 8 + info.getName().length() + 4; // Position of first dimension

            try (RandomAccessFile raf = new RandomAccessFile(corrupted.toFile(), "rw")) {
                raf.seek(dim0Pos);
                raf.writeLong(Long.reverseBytes(512L)); // Mutate shape from [8, 16] to [512, 16]
            }
        }

        GGUFValidationReport report = GGUFValidator.validate(corrupted, tinyModel, tinyVocab);
        assertFalse("Validator must reject tensor dimension mismatch", report.isValid());
        assertTrue("Errors must mention tensor shape mismatch", report.getErrors().stream().anyMatch(e -> e.contains("Tensor shape mismatch")));
    }

    // 7. Truncate file
    @Test
    public void testCorruption7TruncateFile() throws Exception {
        Path corrupted = copyFile(validGgufPath, "corrupt_7_truncated.gguf");
        long size = Files.size(corrupted);
        try (RandomAccessFile raf = new RandomAccessFile(corrupted.toFile(), "rw")) {
            raf.setLength(size / 2); // Truncate half the file
        }

        GGUFValidationReport report = GGUFValidator.validate(corrupted, tinyModel, tinyVocab);
        assertFalse("Validator must reject truncated file", report.isValid());
        assertFalse("Structure must be invalid", report.isStructureValid());
    }

    // 8. Modify tensor name
    @Test
    public void testCorruption8ModifyTensorName() throws Exception {
        Path corrupted = copyFile(validGgufPath, "corrupt_8_tensorname.gguf");
        try (GGUFReader reader = new GGUFReader(validGgufPath)) {
            GGUFTensorInfo info = reader.getTensorMap().get("token_embd.weight");
            long descOffset = info.getDescriptorOffset();

            try (RandomAccessFile raf = new RandomAccessFile(corrupted.toFile(), "rw")) {
                raf.seek(descOffset + 8); // Offset right after length prefix
                raf.write("BAD_TENSOR_NAME".getBytes()); // Overwrite tensor name bytes
            }
        }

        GGUFValidationReport report = GGUFValidator.validate(corrupted, tinyModel, tinyVocab);
        assertFalse("Validator must reject missing or unexpected tensor names", report.isValid());
    }

    // 9. Modify metadata type
    @Test
    public void testCorruption9ModifyMetadataType() throws Exception {
        Path corrupted = copyFile(validGgufPath, "corrupt_9_metatype.gguf");
        try (GGUFReader reader = new GGUFReader(validGgufPath)) {
            GGUFMetadata meta = reader.getMetadataMap().get("general.architecture");
            long typePos = meta.getOffset() + 8 + meta.getKey().length();

            try (RandomAccessFile raf = new RandomAccessFile(corrupted.toFile(), "rw")) {
                raf.seek(typePos);
                raf.writeInt(Integer.reverseBytes(4)); // Change String type (8) to INT32 type (4)
            }
        }

        GGUFValidationReport report = GGUFValidator.validate(corrupted, tinyModel, tinyVocab);
        assertFalse("Validator must reject metadata type modification", report.isValid());
    }

    // 10. Modify tokenizer array length
    @Test
    public void testCorruption10ModifyTokenizerArrayLength() throws Exception {
        Path corrupted = copyFile(validGgufPath, "corrupt_10_tokenizerlen.gguf");
        try (GGUFReader reader = new GGUFReader(validGgufPath)) {
            GGUFMetadata meta = reader.getMetadataMap().get("tokenizer.ggml.tokens");
            long arrLenPos = meta.getOffset() + 8 + meta.getKey().length() + 4 + 4; // After key + type + elemType

            try (RandomAccessFile raf = new RandomAccessFile(corrupted.toFile(), "rw")) {
                raf.seek(arrLenPos);
                raf.writeLong(Long.reverseBytes(5L)); // Mutate array length from 16 to 5
            }
        }

        GGUFValidationReport report = GGUFValidator.validate(corrupted, tinyModel, tinyVocab);
        assertFalse("Validator must reject tokenizer array length mismatch", report.isValid());
        assertTrue("Errors must mention array length mismatch", report.getErrors().stream().anyMatch(e -> e.contains("Tokenizer tokens array length mismatch")));
    }

    // 11. Create overlapping tensor ranges
    @Test
    public void testCorruption11OverlappingTensorRanges() throws Exception {
        Path corrupted = copyFile(validGgufPath, "corrupt_11_overlap.gguf");
        try (GGUFReader reader = new GGUFReader(validGgufPath)) {
            GGUFTensorInfo t2Info = reader.getTensorList().get(1); // Second tensor descriptor
            long descOffset = t2Info.getDescriptorOffset();

            long offsetFieldPos = descOffset + 8 + t2Info.getName().length() + 4 + (t2Info.getDimensions().length * 8) + 4;

            try (RandomAccessFile raf = new RandomAccessFile(corrupted.toFile(), "rw")) {
                raf.seek(offsetFieldPos);
                raf.writeLong(Long.reverseBytes(0L)); // Set second tensor's offset to 0, overlapping with tensor 1
            }
        }

        GGUFValidationReport report = GGUFValidator.validate(corrupted, tinyModel, tinyVocab);
        assertFalse("Validator must detect overlapping tensor ranges", report.isValid());
        assertTrue("Errors must mention overlapping tensor payload", report.getErrors().stream().anyMatch(e -> e.contains("Overlapping tensor payload detected")));
    }

    private Path copyFile(Path source, String newName) throws Exception {
        Path dest = source.getParent().resolve(newName);
        Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING);
        return dest;
    }
}
