package eu.kalafatic.evolution.controller.tests;

import eu.kalafatic.evolution.forge.tokenizer.impl.SimpleBPETokenizer;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Regression tests for synthetic {@code <pad_N>} placeholder handling in the tokenizer.
 */
public class SimpleBPETokenizerPadTokenTest {

    @Test
    public void testPadPlaceholderIsSpecialToken() {
        assertTrue(SimpleBPETokenizer.isSpecialToken("<pad_2786>"));
        assertTrue(SimpleBPETokenizer.isSpecialToken("<pad_0>"));
        assertFalse(SimpleBPETokenizer.isSpecialToken("padding"));
        assertFalse(SimpleBPETokenizer.isSpecialToken("the"));
    }

    @Test
    public void testSmallCorpusPadsVocabularyToTarget() {
        SimpleBPETokenizer tokenizer = new SimpleBPETokenizer();
        tokenizer.train("a tiny corpus for testing", 100);
        assertEquals("Vocabulary must be padded to the requested target size", 100, tokenizer.getVocabSize());
        assertEquals("Real + padding entries must equal the total vocabulary size",
                100, tokenizer.getRealVocabSize() + tokenizer.getPaddingTokenCount());
        assertTrue("A tiny corpus must produce synthetic padding placeholders",
                tokenizer.getPaddingTokenCount() > 0);
    }

    @Test
    public void testDecodeNeverEmitsPadPlaceholderText() {
        SimpleBPETokenizer tokenizer = new SimpleBPETokenizer();
        tokenizer.train("hello world hello world", 64);
        int padId = -1;
        for (java.util.Map.Entry<String, Integer> e : tokenizer.getVocab().entrySet()) {
            if (e.getKey().startsWith("<pad_")) {
                padId = e.getValue();
                break;
            }
        }
        assertTrue("A tiny corpus must produce at least one <pad_N> placeholder", padId >= 0);
        // Decoding a <pad_N> id must produce empty text, not a literal placeholder string.
        String decoded = tokenizer.decode(java.util.List.of(padId));
        assertFalse("Decoded text must not contain a <pad_ literal", decoded.contains("<pad_"));
    }

    @Test
    public void testCompactToRealVocabularyRemovesPlaceholdersAndKeepsIdsContiguous() {
        SimpleBPETokenizer tokenizer = new SimpleBPETokenizer();
        tokenizer.train("a tiny corpus for testing", 256);
        int padded = tokenizer.getVocabSize();
        int real = tokenizer.getRealVocabSize();
        assertTrue("A tiny corpus must produce padding placeholders", tokenizer.getPaddingTokenCount() > 0);
        assertEquals(256, padded);

        String text = "a tiny corpus for testing";
        java.util.List<Integer> before = tokenizer.encode(text);

        int compacted = tokenizer.compactToRealVocabulary();
        assertEquals("Compacted size must equal the real (non-placeholder) vocabulary", real, compacted);
        assertEquals("getVocabSize must reflect the compacted size", compacted, tokenizer.getVocabSize());
        assertEquals("Compaction must remove all placeholders", 0, tokenizer.getPaddingTokenCount());

        // Encoding/decoding must be unaffected (placeholders are never emitted).
        java.util.List<Integer> after = tokenizer.encode(text);
        assertEquals("Encoding must be unchanged after compaction", before, after);
        assertEquals("Round-trip decode must be unchanged after compaction",
                text, tokenizer.decode(after).replace(SimpleBPETokenizer.METASPACE, " ").trim());

        // All remaining IDs must be contiguous and in range.
        java.util.Set<Integer> ids = new java.util.HashSet<>();
        for (java.util.Map.Entry<String, Integer> e : tokenizer.getVocab().entrySet()) {
            int id = e.getValue();
            assertFalse("Compacted token must not be a placeholder: " + e.getKey(), e.getKey().startsWith("<pad_"));
            assertTrue("ID out of range: " + id, id >= 0 && id < compacted);
            assertTrue("Duplicate ID: " + id, ids.add(id));
        }
    }
}
