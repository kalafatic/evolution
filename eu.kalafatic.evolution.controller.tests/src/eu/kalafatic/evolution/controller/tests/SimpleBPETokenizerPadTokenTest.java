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
}
