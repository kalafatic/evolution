package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.*;

import java.nio.charset.StandardCharsets;
import java.util.*;

import org.junit.Before;
import org.junit.Test;

import eu.kalafatic.evolution.forge.tokenizer.impl.SimpleBPETokenizer;

public class SimpleBPETokenizerDecoderTest {

    private SimpleBPETokenizer tokenizer;
    private Map<String, Integer> mockVocab;

    @Before
    public void setUp() {
        tokenizer = new SimpleBPETokenizer();
        mockVocab = new LinkedHashMap<>();

        // Special tokens
        mockVocab.put("<unk>", 0);
        mockVocab.put("<s>", 1);
        mockVocab.put("</s>", 2);
        mockVocab.put("<pad>", 3);

        // Byte fallback tokens (0x00 to 0xFF mapped to IDs 4 to 259)
        int id = 4;
        for (int b = 0; b < 256; b++) {
            mockVocab.put(String.format("<0x%02X>", b), id++);
        }

        // Subword tokens
        mockVocab.put("hello", id++);
        mockVocab.put("world", id++);
        mockVocab.put("Hello", id++);

        tokenizer.setVocabulary(mockVocab);
    }

    @Test
    public void testPlainAsciiDecoding() {
        List<Integer> tokenIds = List.of(
            mockVocab.get("hello"),
            mockVocab.get("world")
        );

        String decoded = tokenizer.decode(tokenIds);
        assertEquals("helloworld", decoded);
    }

    @Test
    public void testSpecialTokensOmittedInDecoding() {
        List<Integer> tokenIds = List.of(
            mockVocab.get("<s>"),
            mockVocab.get("hello"),
            mockVocab.get("<unk>"),
            mockVocab.get("world"),
            mockVocab.get("</s>")
        );

        String decoded = tokenizer.decode(tokenIds);
        assertEquals("helloworld", decoded);
    }

    @Test
    public void testByteFallbackSequencesDecoding() {
        // "Hello světe" - 'světe' contains UTF-8 multi-byte characters
        String targetText = "Hello světe";
        byte[] bytes = targetText.getBytes(StandardCharsets.UTF_8);

        List<Integer> tokenIds = new ArrayList<>();
        for (byte b : bytes) {
            int bInt = b & 0xFF;
            String byteTokStr = String.format("<0x%02X>", bInt);
            tokenIds.add(mockVocab.get(byteTokStr));
        }

        String decoded = tokenizer.decode(tokenIds);
        assertEquals("Hello světe", decoded);
    }

    @Test
    public void testMixedTextAndByteFallbackDecoding() {
        // Token "hello" followed by bytes for " světe " followed by token "world"
        byte[] middleBytes = " světe ".getBytes(StandardCharsets.UTF_8);

        List<Integer> tokenIds = new ArrayList<>();
        tokenIds.add(mockVocab.get("hello"));
        for (byte b : middleBytes) {
            int bInt = b & 0xFF;
            String byteTokStr = String.format("<0x%02X>", bInt);
            tokenIds.add(mockVocab.get(byteTokStr));
        }
        tokenIds.add(mockVocab.get("world"));

        String decoded = tokenizer.decode(tokenIds);
        assertEquals("hello světe world", decoded);
    }

    @Test
    public void testMultiByteUtf8SplitAcrossTokens() {
        // UTF-8 smiley emoji: 😀 (0xF0 0x9F 0x98 0x90)
        byte[] emojiBytes = new byte[] { (byte) 0xF0, (byte) 0x9F, (byte) 0x98, (byte) 0x90 };

        List<Integer> tokenIds = new ArrayList<>();
        for (byte b : emojiBytes) {
            int bInt = b & 0xFF;
            String byteTokStr = String.format("<0x%02X>", bInt);
            tokenIds.add(mockVocab.get(byteTokStr));
        }

        String decoded = tokenizer.decode(tokenIds);
        assertEquals("😀", decoded);
    }

    @Test
    public void testSpecialTokenGetters() {
        assertEquals(1, tokenizer.getBosTokenId());
        assertEquals(2, tokenizer.getEosTokenId());
        assertEquals(0, tokenizer.getUnkTokenId());
        assertEquals(3, tokenizer.getPadTokenId());
    }
}
