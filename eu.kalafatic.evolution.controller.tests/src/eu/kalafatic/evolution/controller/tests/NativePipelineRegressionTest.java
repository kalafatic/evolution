package eu.kalafatic.evolution.controller.tests;

import eu.kalafatic.evolution.forge.data.api.NormalizedSample;
import eu.kalafatic.evolution.forge.data.api.TrainingBatch;
import eu.kalafatic.evolution.forge.data.api.artifact.EvoDatasetArtifact;
import eu.kalafatic.evolution.forge.data.api.source.DatasetSourceConfig;
import eu.kalafatic.evolution.forge.data.api.source.DatasetSourceStats;
import eu.kalafatic.evolution.forge.math.api.Tensor;
import eu.kalafatic.evolution.forge.math.core.SimpleTensor;
import eu.kalafatic.evolution.forge.model.inference.EvoModelValidator;
import eu.kalafatic.evolution.forge.model.inference.InferenceRequest;
import eu.kalafatic.evolution.forge.model.inference.InferenceResult;
import eu.kalafatic.evolution.forge.model.inference.KVCache;
import eu.kalafatic.evolution.forge.model.inference.ReferenceEvoInferenceEngine;
import eu.kalafatic.evolution.forge.model.llm.EvoLlmArchitecture;
import eu.kalafatic.evolution.forge.model.llm.EvoLlmModel;
import eu.kalafatic.evolution.forge.model.llm.EvoModelArtifact;
import eu.kalafatic.evolution.forge.model.llm.EvoModelExporter;
import eu.kalafatic.evolution.forge.model.llm.EvoModelQuantizer;
import eu.kalafatic.evolution.forge.model.llm.ModelSnapshot;
import eu.kalafatic.evolution.forge.model.llm.QuantizationConfig;
import eu.kalafatic.evolution.forge.model.protocol.EvoDtype;
import eu.kalafatic.evolution.forge.model.protocol.EvoModelReader;
import eu.kalafatic.evolution.forge.trainer.api.LossReduction;
import eu.kalafatic.evolution.forge.trainer.impl.llm.EvoLlmTrainer;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

/**
 * Regression tests for the native LLM pipeline correctness fixes:
 * tensor validation, attention masks, KV-cache positional encoding, loss reduction,
 * .evo protocol integrity, exporter weight preservation, quantizer storage honesty,
 * inference cancellation, .evodata integrity and checkpoint moment keying.
 */
public class NativePipelineRegressionTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private static EvoLlmModel tinyModel() {
        return new EvoLlmModel(new EvoLlmArchitecture(64, 16, 4, 2, 32, 128));
    }

    // ================= Tensor engine validation =================

    @Test(expected = IllegalArgumentException.class)
    public void tensorConstructorRejectsNegativeDimension() {
        new SimpleTensor(2, -3);
    }

    @Test(expected = IllegalArgumentException.class)
    public void tensorConstructorRejectsZeroDimension() {
        new SimpleTensor(0, 4);
    }

    @Test(expected = IllegalArgumentException.class)
    public void tensorConstructorRejectsShapeDataMismatch() {
        new SimpleTensor(new long[]{2, 3}, new float[7]);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void tensorIndexOutOfBoundsThrows() {
        Tensor t = new SimpleTensor(2, 3);
        t.getValue(0, 5);
    }

    @Test(expected = IllegalArgumentException.class)
    public void tensorWrongIndexRankThrows() {
        Tensor t = new SimpleTensor(2, 3);
        t.getValue(1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void tensorElementwiseShapeMismatchThrows() {
        Tensor a = new SimpleTensor(2, 3);
        Tensor b = new SimpleTensor(3, 2);
        a.add(b);
    }

    @Test
    public void tensorMatmulMatchesReference() {
        float[] ad = {1, 2, 3, 4};
        float[] bd = {5, 6, 7, 8, 9, 10}; // [2,3] = rows {5,6,7} and {8,9,10}
        Tensor a = new SimpleTensor(new long[]{2, 2}, ad);
        Tensor b = new SimpleTensor(new long[]{2, 3}, bd);
        Tensor c = a.matmul(b);
        assertArrayEquals(new long[]{2, 3}, c.getShape());
        float[] expected = {1 * 5 + 2 * 8, 1 * 6 + 2 * 9, 1 * 7 + 2 * 10,
                3 * 5 + 4 * 8, 3 * 6 + 4 * 9, 3 * 7 + 4 * 10};
        assertArrayEquals(expected, c.getData(), 1e-6f);
    }

    // ================= Attention mask plumbing =================

    @Test
    public void attentionMaskPreventsAttendanceOnPaddingPositions() {
        EvoLlmModel model = tinyModel();
        int[] idsA = {5, 10, 15, 20};
        int[] idsB = {5, 10, 99, 20};
        float[] mask = {1.0f, 1.0f, 0.0f, 1.0f}; // position 2 is padding

        Tensor logitsA = model.forward(idsA, mask);
        Tensor logitsB = model.forward(idsB, mask);

        // Query position 3 must not attend to masked position 2: its logits must be
        // identical regardless of the token at position 2.
        int vocab = model.getVocabSize();
        float[] da = logitsA.getData();
        float[] db = logitsB.getData();
        int offset = 3 * vocab;
        for (int v = 0; v < vocab; v++) {
            assertEquals("masked position must not influence query 3 logits", da[offset + v], db[offset + v], 1e-5f);
        }
        // Sanity: without mask the same perturbation WOULD change the logits.
        Tensor logitsC = model.forward(idsA);
        Tensor logitsD = model.forward(idsB);
        float[] dc = logitsC.getData();
        float[] dd = logitsD.getData();
        boolean differs = false;
        for (int v = 0; v < vocab; v++) {
            if (Math.abs(dc[offset + v] - dd[offset + v]) > 1e-5f) {
                differs = true;
                break;
            }
        }
        assertTrue("unmasked attention must be sensitive to the perturbed token", differs);
    }

    @Test
    public void fullyMaskedSequenceProducesFiniteLogits() {
        EvoLlmModel model = tinyModel();
        int[] ids = {5, 10, 15, 20};
        float[] allMasked = {0.0f, 0.0f, 0.0f, 0.0f};
        Tensor logits = model.forward(ids, allMasked);
        for (float v : logits.getData()) {
            assertFalse("logits must be finite under full mask", Float.isNaN(v) || Float.isInfinite(v));
        }
    }

    @Test
    public void maskedPositionGradientIsZero() {
        EvoLlmModel model = tinyModel();
        int[] ids = {5, 10, 15, 20};
        float[] mask = {1.0f, 1.0f, 0.0f, 1.0f};
        Tensor logits = model.forward(ids, mask);
        int vocab = model.getVocabSize();

        Tensor dLogits = new SimpleTensor(4, vocab);
        float[] dl = dLogits.getData();
        dl[3 * vocab + 5] = 1.0f; // non-zero gradient only on query row 3
        model.backward(dLogits);

        Tensor emb = model.getModelParameters().get("token_embd.weight");
        float[] grad = emb.getGrad();
        int dModel = model.getDModel();
        int offsetToken15 = 15 * dModel;
        for (int d = 0; d < dModel; d++) {
            assertEquals("gradient for masked token must be zero", 0.0f, grad[offsetToken15 + d], 1e-6f);
        }
    }

    // ================= KV-cache positional encoding =================

    @Test
    public void autoregressiveGenerationMatchesBatchedForwardPositionalEncoding() {
        EvoLlmModel model = tinyModel();
        int[] seq = {5, 10, 15};

        Tensor directLogits = model.forward(seq);
        int vocab = model.getVocabSize();
        float[] direct = directLogits.getData();
        int lastRowOffset = 2 * vocab;

        KVCache cache = new KVCache(model.getNumBlocks());
        model.forwardWithCache(new int[]{5, 10}, cache);
        Tensor stepLogits = model.forwardWithCache(new int[]{15}, cache);
        float[] step = stepLogits.getData();

        for (int v = 0; v < vocab; v++) {
            assertEquals("autoregressive step must use the correct positional encoding",
                    direct[lastRowOffset + v], step[v], 1e-4f);
        }
    }

    // ================= Loss reduction =================

    @Test
    public void sumLossReductionProducesBatchSizeIndependentGradients() throws Exception {
        EvoLlmModel modelA = tinyModel();
        EvoLlmModel modelB = tinyModel();
        copyWeights(modelA, modelB);

        EvoLlmTrainer trainerA = new EvoLlmTrainer(modelA, EvoLlmTrainer.TrainingProfile.EVO_FAST);
        trainerA.setLossReduction(LossReduction.SUM);
        trainerA.setMicroBatchSize(1);
        trainerA.setAccumulationSteps(1);

        EvoLlmTrainer trainerB = new EvoLlmTrainer(modelB, EvoLlmTrainer.TrainingProfile.EVO_FAST);
        trainerB.setLossReduction(LossReduction.SUM);
        trainerB.setMicroBatchSize(4);
        trainerB.setAccumulationSteps(1);

        List<TrainingBatch> batches1 = makeBatches(4, 1);
        List<TrainingBatch> batches4 = makeBatches(4, 4);

        double lossA = 0;
        for (TrainingBatch b : batches1) {
            lossA += trainerA.processMicroBatch(b, countValidTokens(b));
        }
        double lossB = 0;
        for (TrainingBatch b : batches4) {
            lossB += trainerB.processMicroBatch(b, countValidTokens(b));
        }

        assertEquals("SUM loss must be batch-size independent", lossA, lossB, 1e-4);

        Tensor pA = modelA.getModelParameters().get("token_embd.weight");
        Tensor pB = modelB.getModelParameters().get("token_embd.weight");
        float[] gA = pA.getGrad();
        float[] gB = pB.getGrad();
        for (int i = 0; i < gA.length; i++) {
            assertEquals("SUM gradient must be batch-size independent", gA[i], gB[i], 1e-4f);
        }
    }

    private static List<TrainingBatch> makeBatches(int totalRows, int batchSize) {
        List<TrainingBatch> batches = new ArrayList<>();
        for (int start = 0; start < totalRows; start += batchSize) {
            int end = Math.min(start + batchSize, totalRows);
            int n = end - start;
            int[][] inputs = new int[n][];
            int[][] labels = new int[n][];
            float[][] masks = new float[n][];
            float[][] attMasks = new float[n][];
            for (int i = 0; i < n; i++) {
                int row = start + i;
                inputs[i] = new int[]{1 + row, 2 + row, 3 + row};
                labels[i] = new int[]{2 + row, 3 + row, 4 + row};
                masks[i] = new float[]{1, 1, 1};
                attMasks[i] = new float[]{1, 1, 1};
            }
            batches.add(new TrainingBatch(inputs, labels, masks, attMasks));
        }
        return batches;
    }

    private static long countValidTokens(TrainingBatch b) {
        long n = 0;
        for (float[] row : b.lossMask) {
            for (float m : row) {
                if (m > 0) n++;
            }
        }
        return n;
    }

    private static void copyWeights(EvoLlmModel from, EvoLlmModel to) {
        for (String name : from.getModelParameters().names()) {
            Tensor src = from.getModelParameters().get(name);
            Tensor dst = to.getModelParameters().get(name);
            System.arraycopy(src.getData(), 0, dst.getData(), 0, src.getData().length);
        }
    }

    // ================= .evo protocol integrity =================

    @Test
    public void evoFileHeaderHashAndSectionCountAreCorrect() throws Exception {
        File dir = tempFolder.newFolder("hdr");
        EvoLlmModel model = tinyModel();
        EvoModelArtifact artifact = new EvoModelArtifact();
        artifact.initializeFromModel("header_test", model, java.util.Collections.emptyMap());
        Path evo = dir.toPath().resolve("header_test.evo");
        artifact.saveToFile(evo);

        EvoModelReader reader = new EvoModelReader();
        reader.read(evo);
        reader.verifyIntegrity();

        assertEquals(64, reader.getModelContentHash().length());
        String storedFull = reader.getMetadata().get("evo_content_hash");
        assertNotNull("full content hash must be stored in metadata", storedFull);
        assertEquals(storedFull, reader.getModelContentHash());
    }

    @Test
    public void corruptedEvoPayloadFailsLoadWithIntegrityError() throws Exception {
        File dir = tempFolder.newFolder("corrupt");
        EvoLlmModel model = tinyModel();
        EvoModelArtifact artifact = new EvoModelArtifact();
        artifact.initializeFromModel("corrupt_test", model, java.util.Collections.emptyMap());
        Path evo = dir.toPath().resolve("corrupt_test.evo");
        artifact.saveToFile(evo);

        byte[] bytes = Files.readAllBytes(evo);
        // Corrupt a byte deep inside the tensor payload area (after the header+JSON sections)
        int target = bytes.length - 100;
        bytes[target] ^= 0x5A;
        Files.write(evo, bytes);

        try {
            EvoModelArtifact.load(evo);
            fail("corrupted model must not load");
        } catch (java.io.IOException e) {
            assertTrue("integrity failure must be reported", e.getMessage().contains("EVO_INTEGRITY_ERROR"));
        }
    }

    @Test
    public void evoRoundTripPreservesLogitsExactly() throws Exception {
        File dir = tempFolder.newFolder("rt");
        EvoLlmModel model = tinyModel();
        int[] seq = {5, 10, 15};
        Tensor before = model.forward(seq);

        EvoModelArtifact artifact = new EvoModelArtifact();
        artifact.initializeFromModel("rt_test", model, java.util.Collections.emptyMap());
        Path evo = dir.toPath().resolve("rt_test.evo");
        artifact.saveToFile(evo);

        EvoModelArtifact loaded = EvoModelArtifact.load(evo);
        EvoLlmModel restored = loaded.createModel();
        Tensor after = restored.forward(seq);
        assertArrayEquals("logits must survive the round trip exactly", before.getData(), after.getData(), 1e-6f);
    }

    // ================= Exporter snapshot weight preservation =================

    @Test
    public void defaultExporterSnapshotPreservesTrainedWeights() throws Exception {
        File dir = tempFolder.newFolder("exp");
        EvoLlmModel model = tinyModel();
        ModelSnapshot snapshot = model.createSnapshot();

        final EvoModelArtifact[] captured = new EvoModelArtifact[1];
        EvoModelExporter exporter = (artifact, outputDirectory) -> captured[0] = artifact;
        exporter.exportSnapshot(snapshot, dir.toPath());

        assertNotNull(captured[0]);
        EvoLlmModel exported = captured[0].createModel();
        for (String name : model.getModelParameters().names()) {
            Tensor src = model.getModelParameters().get(name);
            Tensor dst = exported.getModelParameters().get(name);
            assertArrayEquals("exported weights must equal the trained snapshot weights for " + name,
                    src.getData(), dst.getData(), 1e-6f);
        }
    }

    // ================= Quantizer storage honesty =================

    @Test
    public void f16QuantizedArtifactStoresRealF16DtypeAndShrinks() throws Exception {
        File dir = tempFolder.newFolder("quant");
        EvoLlmModel model = tinyModel();
        EvoModelArtifact source = new EvoModelArtifact();
        source.initializeFromModel("quant_src", model, java.util.Collections.emptyMap());

        EvoModelQuantizer quantizer = new EvoModelQuantizer();
        EvoModelQuantizer.QuantizationResult result = quantizer.quantizeArtifact(
                source, new QuantizationConfig(QuantizationConfig.QuantizationScheme.F16));

        Path f32Path = dir.toPath().resolve("src.evo");
        Path f16Path = dir.toPath().resolve("f16.evo");
        source.saveToFile(f32Path);
        result.getQuantizedArtifact().saveToFile(f16Path);

        long f32Size = Files.size(f32Path);
        long f16Size = Files.size(f16Path);
        assertTrue("F16 payload must be smaller than F32 (f32=" + f32Size + ", f16=" + f16Size + ")",
                f16Size < f32Size);

        EvoModelArtifact loaded = EvoModelArtifact.load(f16Path);
        for (EvoDtype dtype : loaded.getWeightDtypes()) {
            assertEquals("stored dtype must be F16", EvoDtype.F16, dtype);
        }
        for (eu.kalafatic.evolution.forge.model.protocol.EvoTensorDescriptor desc : loaded.getManifest()) {
            assertEquals("manifest dtype must be F16", EvoDtype.F16, desc.getDtype());
        }

        EvoLlmModel restored = loaded.createModel();
        EvoLlmModel original = source.createModel();
        for (String name : restored.getModelParameters().names()) {
            Tensor a = original.getModelParameters().get(name);
            Tensor b = restored.getModelParameters().get(name);
            float[] da = a.getData();
            float[] db = b.getData();
            for (int i = 0; i < da.length; i++) {
                assertEquals("F16 round-trip tolerance", da[i], db[i], Math.max(1e-2f, Math.abs(da[i]) * 0.01f));
            }
        }
    }

    @Test
    public void int8QuantizedArtifactRoundTripsWithScale() throws Exception {
        File dir = tempFolder.newFolder("quant8");
        EvoLlmModel model = tinyModel();
        EvoModelArtifact source = new EvoModelArtifact();
        source.initializeFromModel("quant8_src", model, java.util.Collections.emptyMap());

        EvoModelQuantizer quantizer = new EvoModelQuantizer();
        EvoModelQuantizer.QuantizationResult result = quantizer.quantizeArtifact(
                source, new QuantizationConfig(QuantizationConfig.QuantizationScheme.INT8));

        Path int8Path = dir.toPath().resolve("int8.evo");
        result.getQuantizedArtifact().saveToFile(int8Path);

        EvoModelArtifact loaded = EvoModelArtifact.load(int8Path);
        for (EvoDtype dtype : loaded.getWeightDtypes()) {
            assertEquals("stored dtype must be INT8", EvoDtype.INT8, dtype);
        }
        EvoModelValidator.validateEvoFile(int8Path);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void int4QuantizationIsHonestlyUnsupported() {
        EvoLlmModel model = tinyModel();
        EvoModelArtifact source = new EvoModelArtifact();
        source.initializeFromModel("quant4_src", model, java.util.Collections.emptyMap());
        new EvoModelQuantizer().quantizeArtifact(
                source, new QuantizationConfig(QuantizationConfig.QuantizationScheme.INT4));
    }

    // ================= Inference cancellation =================

    @Test
    public void generationCancellationTerminatesPromptly() throws Exception {
        EvoLlmModel model = tinyModel();
        ReferenceEvoInferenceEngine engine = new ReferenceEvoInferenceEngine();
        AtomicBoolean cancel = new AtomicBoolean(false);
        InferenceRequest request = InferenceRequest.builder()
                .inputIds(new int[]{5, 10, 15})
                .maxTokens(100000)
                .temperature(0.9f)
                .topK(64)
                .cancellationFlag(cancel)
                .build();

        final InferenceResult[] result = new InferenceResult[1];
        // Set the cancellation flag before the generation loop starts: the engine
        // must honor it deterministically on the first iteration.
        cancel.set(true);
        Thread worker = new Thread(() -> result[0] = engine.generate(model, request, null));
        worker.start();
        worker.join(10000);

        assertFalse("generation must terminate", worker.isAlive());
        assertNotNull(result[0]);
        assertEquals(InferenceResult.TerminationReason.CANCELLED, result[0].getTerminationReason());
        assertTrue("cancelled generation must be fast", result[0].getExecutionTimeMs() < 10000);
        assertEquals(0, result[0].getGeneratedTokenCount());
    }

    // ================= .evodata integrity / identity / determinism =================

    @Test
    public void evodataRecordsTokenizerIdentityAndRoundTrips() throws Exception {
        File dir = tempFolder.newFolder("evodata1");
        File file = new File(dir, "test.evodata");
        EvoDatasetArtifact artifact = new EvoDatasetArtifact(file);
        artifact.setTokenizerIdentity("SimpleBPETokenizer", "2.0", 300,
                Arrays.asList("<unk>", "<s>", "</s>"));

        List<NormalizedSample> samples = new ArrayList<>();
        samples.add(makeSample("s1", "The quick brown fox jumps."));
        samples.add(makeSample("s2", "Second sample about training data."));
        artifact.save(samples, new DatasetSourceConfig("LOCAL", "test-corpus"),
                new DatasetSourceStats(), 0.1);

        EvoDatasetArtifact loaded = EvoDatasetArtifact.load(file);
        assertEquals("tokenizer type must round-trip", "SimpleBPETokenizer", loaded.getTokenizerType());
        assertEquals("tokenizer version must round-trip", "2.0", loaded.getTokenizerVersion());
        assertEquals("tokenizer vocab must round-trip", 300, loaded.getTokenizerVocabSize());
        assertEquals("status must be READY", EvoDatasetArtifact.Status.READY, loaded.getStatus());
        assertEquals(2, loaded.getSamples().size());
        assertEquals("", loaded.getLoadDiagnostics());
    }

    @Test
    public void corruptedEvodataIsDetectedAndMarkedCorrupted() throws Exception {
        File dir = tempFolder.newFolder("evodata2");
        File file = new File(dir, "test.evodata");
        EvoDatasetArtifact artifact = new EvoDatasetArtifact(file);
        List<NormalizedSample> samples = new ArrayList<>();
        samples.add(makeSample("s1", "Sample content number one."));
        artifact.save(samples, new DatasetSourceConfig("LOCAL", "test"),
                new DatasetSourceStats(), 0.0);

        // Rebuild the archive with corrupted data.jsonl content but the original
        // (now stale) checksums in metadata.json — a genuine bit-rot simulation.
        rewriteZipEntry(file, "data.jsonl", "{\"id\":\"s1\",\"type\":\"TEXT\",\"text\":\"CORRUPTED CONTENT\"}\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));

        EvoDatasetArtifact loaded = EvoDatasetArtifact.load(file);
        assertEquals("corrupted dataset must be marked CORRUPTED",
                EvoDatasetArtifact.Status.CORRUPTED, loaded.getStatus());
        assertTrue("corruption diagnostics must be recorded", loaded.getLoadDiagnostics().contains("checksum"));
    }

    @Test
    public void evodataIsByteReproducible() throws Exception {
        File dirA = tempFolder.newFolder("reproA");
        File dirB = tempFolder.newFolder("reproB");
        List<NormalizedSample> samples = new ArrayList<>();
        samples.add(makeSample("s1", "Deterministic sample one."));
        samples.add(makeSample("s2", "Deterministic sample two."));

        File fileA = new File(dirA, "test.evodata");
        EvoDatasetArtifact artifactA = new EvoDatasetArtifact(fileA);
        artifactA.save(samples, new DatasetSourceConfig("LOCAL", "test"),
                new DatasetSourceStats(), 0.2);

        File fileB = new File(dirB, "test.evodata");
        EvoDatasetArtifact artifactB = new EvoDatasetArtifact(fileB);
        artifactB.save(samples, new DatasetSourceConfig("LOCAL", "test"),
                new DatasetSourceStats(), 0.2);

        assertArrayEquals("identical inputs must produce identical artifact bytes",
                Files.readAllBytes(fileA.toPath()), Files.readAllBytes(fileB.toPath()));
    }

    @Test
    public void unsupportedEvodataFormatVersionIsRejected() throws Exception {
        File dir = tempFolder.newFolder("evodata4");
        File file = new File(dir, "test.evodata");
        EvoDatasetArtifact artifact = new EvoDatasetArtifact(file);
        artifact.save(java.util.Collections.singletonList(makeSample("s1", "Content.")),
                new DatasetSourceConfig("LOCAL", "test"), new DatasetSourceStats(), 0.0);

        // Rebuild the archive with an unsupported format version in metadata.json
        String meta = new String(readZipEntry(file, "metadata.json"), java.nio.charset.StandardCharsets.UTF_8);
        String updated = meta.replace("\"formatVersion\": \"1.1\"", "\"formatVersion\": \"9.9\"");
        rewriteZipEntry(file, "metadata.json", updated.getBytes(java.nio.charset.StandardCharsets.UTF_8));

        EvoDatasetArtifact loaded = EvoDatasetArtifact.load(file);
        assertEquals("unsupported format version must be rejected",
                EvoDatasetArtifact.Status.CORRUPTED, loaded.getStatus());
        assertTrue(loaded.getLoadDiagnostics().contains("format version"));
    }

    private static byte[] readZipEntry(File zipFile, String entryName) throws Exception {
        try (java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(new java.io.FileInputStream(zipFile))) {
            java.util.zip.ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entryName.equals(entry.getName())) {
                    return zis.readAllBytes();
                }
            }
        }
        throw new IllegalStateException("Entry not found: " + entryName);
    }

    private static void rewriteZipEntry(File zipFile, String entryName, byte[] newContent) throws Exception {
        Map<String, byte[]> entries = new java.util.LinkedHashMap<>();
        try (java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(new java.io.FileInputStream(zipFile))) {
            java.util.zip.ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                byte[] content = zis.readAllBytes();
                if (entryName.equals(entry.getName())) {
                    content = newContent;
                }
                entries.put(entry.getName(), content);
            }
        }
        File temp = new File(zipFile.getAbsolutePath() + ".tmp");
        try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(new java.io.FileOutputStream(temp))) {
            for (Map.Entry<String, byte[]> e : entries.entrySet()) {
                java.util.zip.ZipEntry ze = new java.util.zip.ZipEntry(e.getKey());
                zos.putNextEntry(ze);
                zos.write(e.getValue());
                zos.closeEntry();
            }
        }
        Files.move(temp.toPath(), zipFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }

    private static NormalizedSample makeSample(String id, String text) {
        NormalizedSample s = new NormalizedSample();
        s.setId(id);
        s.setText(text);
        s.recalculateCountsAndHash();
        return s;
    }

    // ================= Checkpoint moment keying =================

    @Test
    public void checkpointMomentsUseCanonicalTensorNames() throws Exception {
        EvoLlmModel model = tinyModel();
        EvoLlmTrainer trainer = new EvoLlmTrainer(model, EvoLlmTrainer.TrainingProfile.EVO_FAST);
        trainer.setMicroBatchSize(1);
        trainer.setAccumulationSteps(1);
        trainer.setValidationSplitRatio(0.0f);

        eu.kalafatic.evolution.forge.data.api.TrainingSample sample =
                new eu.kalafatic.evolution.forge.data.api.TrainingSample(
                        new int[]{5, 10, 15}, new int[]{10, 15, 20},
                        new boolean[]{true, true, true}, new float[]{1, 1, 1});
        trainer.train(java.util.Collections.singletonList(sample), 1);

        eu.kalafatic.evolution.forge.trainer.api.TrainingCheckpoint chk = trainer.captureCheckpoint(
                "run1", "model1", 1, 1.0f, 1.1f, "sha256:abc",
                new java.util.HashMap<>());

        EvoLlmModel freshModel = tinyModel();
        Map<String, float[]> restoredM = new java.util.HashMap<>();
        Map<String, float[]> restoredV = new java.util.HashMap<>();
        chk.restoreInto(freshModel, restoredM, restoredV);

        boolean hasCanonical = false;
        for (String key : restoredM.keySet()) {
            if (key.startsWith("n_")) {
                hasCanonical = true;
                break;
            }
        }
        assertTrue("checkpoint moments must be keyed by canonical tensor names", hasCanonical);
    }
}
