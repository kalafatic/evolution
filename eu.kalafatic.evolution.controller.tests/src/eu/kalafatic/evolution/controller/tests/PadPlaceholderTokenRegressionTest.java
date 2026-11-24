package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.*;

import java.io.File;
import java.nio.file.Path;
import java.util.*;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import eu.kalafatic.evolution.forge.math.api.Tensor;
import eu.kalafatic.evolution.forge.model.inference.InferenceRequest;
import eu.kalafatic.evolution.forge.model.inference.InferenceResult;
import eu.kalafatic.evolution.forge.model.inference.ReferenceEvoInferenceEngine;
import eu.kalafatic.evolution.forge.model.llm.EvoLlmArchitecture;
import eu.kalafatic.evolution.forge.model.llm.EvoLlmModel;
import eu.kalafatic.evolution.forge.model.llm.EvoModelArtifact;
import eu.kalafatic.evolution.forge.tokenizer.impl.SimpleBPETokenizer;

/**
 * Regression suite for the EVO_NATIVE_V2 tokenizer/decoding integrity defect where
 * inference emitted synthetic {@code <pad_N>} placeholder tokens instead of language.
 *
 * <p>The placeholder tokens are produced by {@link SimpleBPETokenizer#train} to pad the
 * vocabulary up to its requested target size. They never occur in training data, so their
 * embedding and output-projection rows remain at random initialization and an undertrained
 * model samples them almost exclusively. {@link ReferenceEvoInferenceEngine} now excludes
 * these tokens from sampling.</p>
 */
public class PadPlaceholderTokenRegressionTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private ReferenceEvoInferenceEngine engine;
    private SimpleBPETokenizer tokenizer;

    private static final String CORPUS =
            "The quick brown fox jumps over the lazy dog. " +
            "Evolution is a native inference platform for language models. " +
            "The inspection interval for a station describes the time between required inspections.";

    @Before
    public void setUp() {
        engine = new ReferenceEvoInferenceEngine();
        tokenizer = new SimpleBPETokenizer();
        // Small corpus + large target vocab forces the tokenizer to pad the
        // vocabulary with synthetic <pad_N> placeholders, reproducing the
        // production EVO_NATIVE_V2 condition.
        tokenizer.train(CORPUS, 512);
    }

    // ============ 1. Vocabulary encode/decode round-trip ============

    @Test
    public void testVocabularyEncodeDecodeRoundTrip() {
        String sentence = "The inspection interval for a station describes the time between required inspections.";
        List<Integer> encoded = tokenizer.encode(sentence);
        assertFalse("Encoding must not be empty", encoded.isEmpty());

        String decoded = tokenizer.decode(encoded);
        assertEquals("Round-trip decode must reconstruct the original sentence",
                sentence, decoded.replace(SimpleBPETokenizer.METASPACE, " ").trim());
    }

    @Test
    public void testIndividualVocabularyEntriesRoundTrip() {
        // Every vocabulary entry must encode back to itself and decode to itself.
        for (Map.Entry<String, Integer> entry : tokenizer.getVocab().entrySet()) {
            String token = entry.getKey();
            if (token.isEmpty()) {
                continue;
            }
            // Byte tokens and placeholders are not standalone natural text; skip
            // them for the round-trip check but verify their ID mapping below.
            if (SimpleBPETokenizer.isByteToken(token) || token.startsWith("<pad_")) {
                continue;
            }
            List<Integer> ids = tokenizer.encode(token.replace(SimpleBPETokenizer.METASPACE, " "));
            assertFalse("Token must encode non-empty: " + token, ids.isEmpty());
            assertTrue("Token " + token + " encoded to out-of-range ID",
                    ids.get(0) >= 0 && ids.get(0) < tokenizer.getVocabSize());
        }
    }

    // ============ 2. Known training sentence reconstruction ============

    @Test
    public void testKnownTrainingSentenceReconstruction() {
        String sentence = "The quick brown fox jumps over the lazy dog.";
        List<Integer> ids = tokenizer.encode(sentence);
        String decoded = tokenizer.decode(ids);
        assertEquals(sentence, decoded.replace(SimpleBPETokenizer.METASPACE, " ").trim());
    }

    // ============ 3. Special-token identification ============

    @Test
    public void testSpecialTokenIdsMatchTrainingConvention() {
        assertEquals("UNK must be id 0", 0, tokenizer.getUnkTokenId());
        assertEquals("BOS must be id 1", 1, tokenizer.getBosTokenId());
        assertEquals("EOS must be id 2", 2, tokenizer.getEosTokenId());
        assertEquals("<unk>", tokenizer.getInvVocab().get(0));
        assertEquals("<s>", tokenizer.getInvVocab().get(1));
        assertEquals("</s>", tokenizer.getInvVocab().get(2));
    }

    @Test
    public void testVocabularyIdsAreUniqueAndInRange() {
        Map<String, Integer> vocab = tokenizer.getVocab();
        Set<Integer> ids = new HashSet<>();
        int padCount = 0;
        for (Map.Entry<String, Integer> entry : vocab.entrySet()) {
            int id = entry.getValue();
            assertTrue("Token ID must be in [0, vocabSize): " + entry.getKey(),
                    id >= 0 && id < vocab.size());
            assertTrue("Duplicate token ID " + id + " for " + entry.getKey(), ids.add(id));
            if (entry.getKey().startsWith("<pad_")) {
                padCount++;
            }
        }
        assertEquals("Vocabulary must be padded to exactly the target size", 512, vocab.size());
        assertTrue("Expected synthetic <pad_N> placeholders to be present (reproducing the defect)", padCount > 0);
    }

    // ============ 4. EOS termination is recognized by the engine ============

    @Test
    public void testEosTerminationIsRecognized() {
        int vocabSize = tokenizer.getVocabSize();
        // RoPE model so no absolute positional encoding is added outside attention;
        // zeroed attention/FFN weights turn each block into an identity passthrough.
        EvoLlmModel model = new EvoLlmModel(vocabSize, 8, 2, 1, 16, 16, EvoLlmArchitecture.POSITIONAL_ROPE);
        model.getIdToToken().putAll(tokenizer.getInvVocab());

        // Force the greedy argmax to be the EOS token (id 2) via the output projection.
        forceGreedyArgmax(model, tokenizer.getEosTokenId());

        InferenceRequest req = InferenceRequest.builder()
                .inputIds(new int[]{1})
                .maxTokens(8)
                .temperature(0.0f)
                .build();

        InferenceResult result = engine.generate(model, req, tokenizer);
        assertEquals("EOS must remain selectable as the first generated token",
                tokenizer.getEosTokenId(), result.getGeneratedTokenIds()[0]);
        assertEquals("Generation must stop on EOS when EOS is the argmax",
                InferenceResult.TerminationReason.EOS_REACHED, result.getTerminationReason());
    }

    @Test
    public void testUnkDoesNotTerminateGeneration() {
        int vocabSize = tokenizer.getVocabSize();
        EvoLlmModel model = new EvoLlmModel(vocabSize, 8, 2, 1, 16, 16, EvoLlmArchitecture.POSITIONAL_ROPE);
        model.getIdToToken().putAll(tokenizer.getInvVocab());

        // Force the greedy argmax onto <unk> (id 0). <unk> must NOT terminate
        // generation; the loop must keep decoding to maxTokens.
        forceGreedyArgmax(model, tokenizer.getUnkTokenId());

        InferenceRequest req = InferenceRequest.builder()
                .inputIds(new int[]{1})
                .maxTokens(8)
                .temperature(0.0f)
                .build();

        InferenceResult result = engine.generate(model, req, tokenizer);
        assertEquals("Generation must not stop early on <unk>",
                InferenceResult.TerminationReason.MAX_TOKENS_REACHED, result.getTerminationReason());
        assertEquals("All 8 steps must produce tokens (unk must not terminate)",
                8, result.getGeneratedTokenCount());
    }

    @Test
    public void testBosCannotBeEmittedDuringGeneration() {
        int vocabSize = tokenizer.getVocabSize();
        EvoLlmModel model = new EvoLlmModel(vocabSize, 8, 2, 1, 16, 16, EvoLlmArchitecture.POSITIONAL_ROPE);
        model.getIdToToken().putAll(tokenizer.getInvVocab());

        // Force the greedy argmax onto BOS (<s> = id 1). BOS is valid only for
        // prompt initialization and must never be emitted during decoding.
        int bosId = tokenizer.getBosTokenId();
        forceGreedyArgmax(model, bosId);

        InferenceRequest req = InferenceRequest.builder()
                .inputIds(new int[]{1})
                .maxTokens(8)
                .temperature(0.0f)
                .build();

        InferenceResult result = engine.generate(model, req, tokenizer);
        assertTrue(result.getGeneratedTokenCount() > 0);
        for (int id : result.getGeneratedTokenIds()) {
            assertNotEquals("BOS token must never be emitted during generation", bosId, id);
        }
    }

    @Test
    public void testNoPlaceholderThroughAllSamplingStrategies() {
        int vocabSize = tokenizer.getVocabSize();
        int padId = findFirstPadPlaceholderId(tokenizer);
        assertTrue(padId >= 0);
        int bosId = tokenizer.getBosTokenId();

        // Three distinct sampling configurations. Each builds a fresh model that
        // forces the greedy argmax onto the <pad_N> placeholder; the mask must keep
        // it (and BOS) out of the distribution regardless of temperature/top-k/top-p.
        float[][] configs = {
                {0.8f, 40, 0.9f},   // temperature + top-k + top-p
                {1.0f, 0, 1.0f},    // pure temperature
                {0.8f, 10, 0.95f}   // temperature + aggressive top-k + top-p
        };

        for (float[] cfg : configs) {
            EvoLlmModel model = new EvoLlmModel(vocabSize, 8, 2, 1, 16, 16, EvoLlmArchitecture.POSITIONAL_ROPE);
            model.getIdToToken().putAll(tokenizer.getInvVocab());
            forceGreedyArgmax(model, padId);

            InferenceRequest req = InferenceRequest.builder()
                    .inputIds(new int[]{1})
                    .maxTokens(48)
                    .temperature(cfg[0])
                    .topK((int) cfg[1])
                    .topP(cfg[2])
                    .build();

            InferenceResult result = engine.generate(model, req, tokenizer);
            for (int id : result.getGeneratedTokenIds()) {
                assertNotEquals("pad placeholder emitted under sampling config " + Arrays.toString(cfg), padId, id);
                assertNotEquals("BOS emitted under sampling config " + Arrays.toString(cfg), bosId, id);
            }
        }
    }

    @Test
    public void testByteFallbackTokensRemainFunctional() {
        // Characters outside the training corpus fall back to per-byte <0xXX> tokens.
        String text = "café Ω≈ç√";
        List<Integer> ids = tokenizer.encode(text);
        assertFalse(ids.isEmpty());

        boolean sawByteToken = false;
        for (int id : ids) {
            String tok = tokenizer.getInvVocab().get(id);
            if (tok != null && SimpleBPETokenizer.isByteToken(tok)) {
                sawByteToken = true;
                break;
            }
        }
        assertTrue("Encoding out-of-corpus text must use byte-fallback tokens", sawByteToken);

        // Byte-fallback decode must reconstruct the non-ASCII characters.
        String decoded = tokenizer.decode(ids).replace(SimpleBPETokenizer.METASPACE, " ").trim();
        assertEquals("Byte-fallback decode must reconstruct the original text", text, decoded);
    }

    @Test
    public void testStreamingListenerObeysRestrictions() {
        int vocabSize = tokenizer.getVocabSize();
        int padId = findFirstPadPlaceholderId(tokenizer);
        assertTrue(padId >= 0);
        int bosId = tokenizer.getBosTokenId();

        EvoLlmModel model = new EvoLlmModel(vocabSize, 8, 2, 1, 16, 16, EvoLlmArchitecture.POSITIONAL_ROPE);
        model.getIdToToken().putAll(tokenizer.getInvVocab());
        forceGreedyArgmax(model, padId);

        List<Integer> streamedIds = new ArrayList<>();
        ReferenceEvoInferenceEngine.TokenStreamListener listener =
                (tokenId, tokenText) -> streamedIds.add(tokenId);

        InferenceRequest req = InferenceRequest.builder()
                .inputIds(new int[]{1})
                .maxTokens(32)
                .temperature(0.0f)
                .build();

        InferenceResult result = engine.generateWithListener(model, req, tokenizer, listener);
        assertFalse("Streaming listener must receive generated tokens", streamedIds.isEmpty());
        for (int id : streamedIds) {
            assertNotEquals("Streaming path emitted pad placeholder", padId, id);
            assertNotEquals("Streaming path emitted BOS", bosId, id);
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testAllForbiddenVocabularyFailsExplicitly() {
        // A vocabulary where every token is forbidden (<s> BOS + <pad_1> placeholder)
        // must produce a controlled error rather than silently sampling an invalid token.
        Map<String, Integer> vocab = new LinkedHashMap<>();
        vocab.put("<s>", 0);
        vocab.put("<pad_1>", 1);

        SimpleBPETokenizer bpe = new SimpleBPETokenizer();
        bpe.setVocabulary(vocab);

        EvoLlmModel model = new EvoLlmModel(2, 8, 2, 1, 16, 16, EvoLlmArchitecture.POSITIONAL_ROPE);
        model.getIdToToken().putAll(bpe.getInvVocab());

        InferenceRequest req = InferenceRequest.builder()
                .inputIds(new int[]{0})
                .maxTokens(4)
                .temperature(0.0f)
                .build();

        engine.generate(model, req, bpe);
    }

    // ============ 5. Short conversational inference must not emit placeholders ============

    @Test
    public void testGenerationNeverEmitsPadPlaceholderTokens() {
        int vocabSize = tokenizer.getVocabSize();
        EvoLlmModel model = new EvoLlmModel(vocabSize, 8, 2, 1, 16, 16, EvoLlmArchitecture.POSITIONAL_ROPE);
        model.getIdToToken().putAll(tokenizer.getInvVocab());

        // Force the greedy argmax onto a synthetic <pad_N> placeholder. Without the
        // sampling mask the engine would emit it immediately; with the mask it must
        // be redirected onto a real vocabulary token.
        int padId = findFirstPadPlaceholderId(tokenizer);
        assertTrue("Expected at least one <pad_N> placeholder for this test", padId >= 0);
        forceGreedyArgmax(model, padId);

        InferenceRequest req = InferenceRequest.builder()
                .inputIds(new int[]{1})
                .maxTokens(32)
                .temperature(0.0f)
                .build();

        InferenceResult result = engine.generate(model, req, tokenizer);
        assertTrue("Generation must produce at least one token", result.getGeneratedTokenCount() > 0);

        for (int id : result.getGeneratedTokenIds()) {
            String tokenStr = model.getIdToToken().get(id);
            assertFalse("Generated token must not be a synthetic <pad_N> placeholder: " + tokenStr,
                    tokenStr != null && tokenStr.startsWith("<pad_"));
        }
        assertFalse("Generated text must not contain a <pad_N> literal",
                result.getGeneratedText().contains("<pad_"));
    }

    @Test
    public void testShortConversationalInferenceProducesRealTokens() {
        // Realistic undertrained-model scenario: random weights, padded vocabulary.
        int vocabSize = tokenizer.getVocabSize();
        EvoLlmModel model = new EvoLlmModel(vocabSize, 16, 2, 1, 32, 16, EvoLlmArchitecture.POSITIONAL_ROPE);
        model.getIdToToken().putAll(tokenizer.getInvVocab());

        InferenceRequest req = InferenceRequest.builder()
                .prompt("Evolution is a native inference platform")
                .maxTokens(40)
                .temperature(0.0f)
                .build();

        InferenceResult result = engine.generate(model, req, tokenizer);
        assertTrue(result.getGeneratedTokenCount() > 0);

        Set<Integer> padIds = collectPadPlaceholderIds(tokenizer);
        for (int id : result.getGeneratedTokenIds()) {
            assertFalse("Greedy generation sampled a placeholder token id " + id, padIds.contains(id));
        }
    }

    // ============ 6. Training-time vs inference-time token ID consistency ============

    @Test
    public void testTrainingAndInferenceTokenIdsAreIdentical() throws Exception {
        int vocabSize = tokenizer.getVocabSize();
        EvoLlmModel model = new EvoLlmModel(vocabSize, 16, 2, 1, 32, 16);
        model.getIdToToken().putAll(tokenizer.getInvVocab());

        // Export using the training vocabulary, then reload and compare.
        EvoModelArtifact artifact = new EvoModelArtifact();
        artifact.initializeFromModel("tokenizer-integrity", model, tokenizer.getVocab());

        File dir = tempFolder.newFolder("tokenizer-integrity");
        Path evoFile = dir.toPath().resolve("model.evo");
        artifact.saveToFile(evoFile);

        EvoModelArtifact loaded = EvoModelArtifact.load(evoFile);
        Map<String, Integer> trainingVocab = tokenizer.getVocab();
        Map<String, Integer> inferenceVocab = loaded.getTokenizerVocab();

        assertEquals("Serialized vocabulary size must match the training vocabulary",
                trainingVocab.size(), inferenceVocab.size());

        // Token IDs must be identical for every entry (no reordering / no reassignment).
        for (Map.Entry<String, Integer> entry : trainingVocab.entrySet()) {
            Integer inferenceId = inferenceVocab.get(entry.getKey());
            assertNotNull("Inference vocabulary missing training token: " + entry.getKey(), inferenceId);
            assertEquals("Token ID changed between training and inference for: " + entry.getKey(),
                    entry.getValue(), inferenceId);
        }

        // The special-token IDs must survive the round-trip exactly.
        assertEquals(tokenizer.getUnkTokenId(), loaded.getUnkTokenId());
        assertEquals(tokenizer.getBosTokenId(), loaded.getBosTokenId());
        assertEquals(tokenizer.getEosTokenId(), loaded.getEosTokenId());
    }

    // ============ 7. Tokenizer round-trip & vocabulary utilization ============

    @Test
    public void testEncodeDecodeRoundTripHasNoLeadingMetaspace() {
        // decode(encode(x)) must equal x exactly — the SentencePiece leading
        // metaspace (add_space_prefix) must be stripped, not leaked as a space.
        for (String text : new String[] {
                "hi", "hello", "Hello! How are you?",
                "The quick brown fox jumps over the lazy dog.",
                "Evolution is a native inference platform."}) {
            List<Integer> ids = tokenizer.encode(text);
            String decoded = tokenizer.decode(ids);
            assertEquals("Round-trip must not leak a leading metaspace for: " + text,
                    text, decoded);
        }
    }

    @Test
    public void testVocabularyUtilizationReporting() {
        int total = tokenizer.getVocabSize();
        int real = tokenizer.getRealVocabSize();
        int pad = tokenizer.getPaddingTokenCount();
        assertEquals("real + pad must equal total vocabulary", total, real + pad);
        assertTrue("A small corpus must produce some placeholder padding", pad > 0);
        assertEquals("Real vocabulary must exclude all <pad_N> placeholders", real, total - pad);
    }

    @Test
    public void testSufficientCorpusLearnsFullVocabularyWithoutPadding() {
        // A sufficiently large corpus must fill the target vocabulary with real
        // subword units instead of synthetic <pad_N> placeholders.
        String corpus = "the quick brown fox jumps over the lazy dog. " +
                "artificial intelligence and machine learning are transforming software engineering. " +
                "a computer processes instructions and stores information in memory. " +
                "the capital of France is Paris and it is a major European city. " +
                "programming languages such as Java and Python are widely used by developers. " +
                "evolution is a platform for native language model inference and training. " +
                "the inspection interval describes the time between required inspections. " +
                "water infrastructure and agricultural systems require regular maintenance schedules.";
        SimpleBPETokenizer bpe = new SimpleBPETokenizer();
        bpe.train(corpus, 128);
        // The corpus is large enough that the tokenizer should not need any padding.
        assertEquals("Sufficient corpus must not produce <pad_N> placeholders",
                0, bpe.getPaddingTokenCount());
        assertEquals(128, bpe.getRealVocabSize());
        assertEquals(128, bpe.getVocabSize());
    }

    // ============ helpers ============

    /**
     * Overwrites the model so that greedy decoding selects {@code targetId} as the
     * argmax. Uses a RoPE model with zeroed attention/FFN weights (identity passthrough),
     * a constant embedding, and an output projection whose only non-zero column is
     * {@code targetId}. Deterministic and independent of weight initialization.
     */
    private static void forceGreedyArgmax(EvoLlmModel model, int targetId) {
        List<Tensor> params = model.parameters();
        // params order: [0]=embedding, [1]=attn_norm, [2..5]=attn_q/k/v/output,
        // [6]=ffn_norm, [7..9]=ffn_gate/up/down, [10]=output_norm, [11]=lmHead
        int dModel = model.getDModel();

        // Constant embedding -> constant activations through the passthrough blocks.
        Arrays.fill(params.get(0).getData(), 1.0f);
        // Zero the attention and FFN weights so the single block becomes a passthrough.
        for (int i = 2; i <= 5; i++) {
            Arrays.fill(params.get(i).getData(), 0.0f);
        }
        for (int i = 7; i <= 9; i++) {
            Arrays.fill(params.get(i).getData(), 0.0f);
        }
        // Output projection: only the target column is non-zero.
        float[] lmHead = params.get(params.size() - 1).getData();
        Arrays.fill(lmHead, 0.0f);
        int vocabSize = model.getVocabSize();
        for (int d = 0; d < dModel; d++) {
            lmHead[d * vocabSize + targetId] = 1000.0f;
        }
    }

    private static int findFirstPadPlaceholderId(SimpleBPETokenizer tokenizer) {
        for (Map.Entry<String, Integer> entry : tokenizer.getVocab().entrySet()) {
            if (entry.getKey().startsWith("<pad_")) {
                return entry.getValue();
            }
        }
        return -1;
    }

    private static Set<Integer> collectPadPlaceholderIds(SimpleBPETokenizer tokenizer) {
        Set<Integer> padIds = new HashSet<>();
        for (Map.Entry<String, Integer> entry : tokenizer.getVocab().entrySet()) {
            if (entry.getKey().startsWith("<pad_")) {
                padIds.add(entry.getValue());
            }
        }
        return padIds;
    }
}
