package eu.kalafatic.evolution.forge.tokenizer.impl;

import eu.kalafatic.evolution.forge.tokenizer.api.Tokenizer;
import java.util.*;

/**
 * A very simplified Byte Pair Encoding (BPE) tokenizer for educational purposes.
 * It merges most frequent character pairs iteratively.
 */
public class SimpleBPETokenizer implements Tokenizer {
    private Map<String, Integer> vocab = new LinkedHashMap<>();
    private Map<Integer, String> invVocab = new HashMap<>();

    public void train(String corpus, int targetVocabSize) {
        vocab.clear();
        vocab.put("<unk>", 0);
        vocab.put("<s>", 1);
        vocab.put("</s>", 2);
        vocab.put(" ", 3);

        int id = 4;
        if (targetVocabSize >= 260) {
            for (int b = 0; b < 256; b++) {
                String byteToken = String.format("<0x%02X>", b);
                vocab.put(byteToken, id++);
            }
        }

        if (corpus != null && !corpus.isEmpty()) {
            // 1. Initial characters from corpus
            for (char c : corpus.toCharArray()) {
                String s = String.valueOf(c);
                if (!vocab.containsKey(s) && vocab.size() < targetVocabSize) {
                    vocab.put(s, id++);
                }
            }

            // 2. Extract words from corpus and perform iterative BPE merges
            String[] rawWords = corpus.split("\\s+");
            List<List<String>> wordSymbols = new ArrayList<>();
            for (String w : rawWords) {
                if (w.isEmpty()) continue;
                List<String> syms = new ArrayList<>();
                for (char c : w.toCharArray()) {
                    syms.add(String.valueOf(c));
                }
                wordSymbols.add(syms);
            }

            // Perform iterative BPE pair merges
            while (vocab.size() < targetVocabSize) {
                Map<String, Integer> pairFreqs = new HashMap<>();
                for (List<String> syms : wordSymbols) {
                    for (int i = 0; i < syms.size() - 1; i++) {
                        String pair = syms.get(i) + syms.get(i + 1);
                        pairFreqs.put(pair, pairFreqs.getOrDefault(pair, 0) + 1);
                    }
                }

                if (pairFreqs.isEmpty()) break;

                String bestPair = null;
                int maxFreq = 0;
                for (Map.Entry<String, Integer> entry : pairFreqs.entrySet()) {
                    if (entry.getValue() > maxFreq && !vocab.containsKey(entry.getKey())) {
                        maxFreq = entry.getValue();
                        bestPair = entry.getKey();
                    }
                }

                if (bestPair == null || maxFreq < 1) break;

                vocab.put(bestPair, id++);

                for (List<String> syms : wordSymbols) {
                    int i = 0;
                    while (i < syms.size() - 1) {
                        if ((syms.get(i) + syms.get(i + 1)).equals(bestPair)) {
                            syms.set(i, bestPair);
                            syms.remove(i + 1);
                        } else {
                            i++;
                        }
                    }
                }
            }

            // 3. Add whole words if target vocabulary capacity remains
            for (String w : rawWords) {
                if (vocab.size() >= targetVocabSize) break;
                if (!w.isEmpty() && !vocab.containsKey(w)) {
                    vocab.put(w, id++);
                }
            }
        }

        // Common fallback words
        String[] commonWords = {"the", "and", "in", "is", "of", "to", "evolution", "ai", "model", "java", "code", "hi", "hello"};
        for (String word : commonWords) {
            if (vocab.size() < targetVocabSize && !vocab.containsKey(word)) {
                vocab.put(word, id++);
            }
        }

        updateInvVocab();
    }

    private void updateInvVocab() {
        invVocab.clear();
        vocab.forEach((k, v) -> invVocab.put(v, k));
    }

    @Override
    public List<Integer> encode(String text) {
        List<Integer> tokens = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return tokens;
        }

        // Group vocabulary keys by their starting character for O(1) starting prefix lookup.
        // Also keep them sorted by length descending so the first match we find is the longest match.
        Map<Character, List<String>> prefixMap = new HashMap<>();
        boolean hasByteTokens = false;
        for (Map.Entry<String, Integer> entry : vocab.entrySet()) {
            String key = entry.getKey();
            if (key == null || key.isEmpty()) continue;
            if (isByteToken(key)) {
                hasByteTokens = true;
                continue;
            }
            if (isSpecialToken(key)) continue;

            char firstChar = key.charAt(0);
            prefixMap.computeIfAbsent(firstChar, k -> new ArrayList<>()).add(key);
        }

        // Sort each list by length descending to ensure longest match is checked first
        for (List<String> list : prefixMap.values()) {
            list.sort((s1, s2) -> Integer.compare(s2.length(), s1.length()));
        }

        int i = 0;
        int len = text.length();
        while (i < len) {
            char currentChar = text.charAt(i);
            List<String> candidates = prefixMap.get(currentChar);
            String match = null;
            int matchLen = 0;

            if (candidates != null) {
                for (String v : candidates) {
                    if (text.startsWith(v, i)) {
                        match = v;
                        matchLen = v.length();
                        break; // Sorted by length descending, so first match is the longest!
                    }
                }
            }

            if (match != null) {
                tokens.add(vocab.get(match));
                i += matchLen;
            } else {
                // Character/subword not found in vocabulary.
                int codePoint = text.codePointAt(i);
                int charCount = Character.charCount(codePoint);

                if (hasByteTokens) {
                    String cpStr = text.substring(i, i + charCount);
                    byte[] cpBytes = cpStr.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                    for (byte b : cpBytes) {
                        String byteTok = String.format("<0x%02X>", b & 0xFF);
                        Integer byteId = vocab.get(byteTok);
                        if (byteId != null) {
                            tokens.add(byteId);
                        } else {
                            tokens.add(getUnkTokenId());
                        }
                    }
                } else {
                    tokens.add(getUnkTokenId());
                }
                i += charCount;
            }
        }
        return tokens;
    }

    public static class IncrementalDecoder {
        private final Map<Integer, String> invVocab;
        private final StringBuilder cumulativeText = new StringBuilder();
        private final java.io.ByteArrayOutputStream byteBuffer = new java.io.ByteArrayOutputStream();
        private final java.nio.charset.CharsetDecoder utf8Decoder = java.nio.charset.StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(java.nio.charset.CodingErrorAction.REPLACE)
                .onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPLACE);

        public IncrementalDecoder(Map<Integer, String> invVocab) {
            this.invVocab = invVocab != null ? invVocab : Collections.emptyMap();
        }

        /**
         * Accepts a single token ID and returns any newly decoded text snippet.
         */
        public String accept(int tokenId) {
            String tokStr = invVocab.getOrDefault(tokenId, "");
            if (tokStr == null || tokStr.isEmpty()) {
                return "";
            }

            if (isSpecialToken(tokStr)) {
                return "";
            }

            if (isByteToken(tokStr)) {
                byteBuffer.write(parseByteToken(tokStr));
                String newlyDecoded = decodePendingBytes(false);
                cumulativeText.append(newlyDecoded);
                return newlyDecoded;
            } else {
                // Non-byte subword token encountered.
                // Flush any pending accumulated bytes first.
                String flushedBytes = decodePendingBytes(true);
                cumulativeText.append(flushedBytes);
                cumulativeText.append(tokStr);
                return flushedBytes + tokStr;
            }
        }

        /**
         * Flushes remaining pending bytes at the end of generation.
         */
        public String flush() {
            String remaining = decodePendingBytes(true);
            cumulativeText.append(remaining);
            return remaining;
        }

        public String getCumulativeText() {
            return cumulativeText.toString();
        }

        private String decodePendingBytes(boolean endOfInput) {
            if (byteBuffer.size() == 0) {
                return "";
            }

            byte[] rawBytes = byteBuffer.toByteArray();
            java.nio.ByteBuffer in = java.nio.ByteBuffer.wrap(rawBytes);
            java.nio.CharBuffer out = java.nio.CharBuffer.allocate(rawBytes.length * 2 + 10);

            utf8Decoder.reset();
            java.nio.charset.CoderResult result = utf8Decoder.decode(in, out, endOfInput);
            if (endOfInput) {
                utf8Decoder.flush(out);
            }

            out.flip();
            String decodedChunk = out.toString();

            int unconsumed = in.remaining();
            if (unconsumed > 0 && !endOfInput) {
                byte[] remainingBytes = new byte[unconsumed];
                System.arraycopy(rawBytes, in.position(), remainingBytes, 0, unconsumed);
                byteBuffer.reset();
                byteBuffer.write(remainingBytes, 0, unconsumed);
            } else {
                byteBuffer.reset();
            }

            return decodedChunk;
        }
    }

    public IncrementalDecoder createIncrementalDecoder() {
        return new IncrementalDecoder(invVocab);
    }

    @Override
    public String decode(List<Integer> tokens) {
        if (tokens == null || tokens.isEmpty()) {
            return "";
        }
        IncrementalDecoder decoder = createIncrementalDecoder();
        for (Integer token : tokens) {
            if (token != null) {
                decoder.accept(token);
            }
        }
        decoder.flush();
        return decoder.getCumulativeText();
    }

    public static boolean isByteToken(String tok) {
        if (tok != null && tok.length() == 6 && (tok.startsWith("<0x") || tok.startsWith("<0X")) && tok.endsWith(">")) {
            try {
                int val = Integer.parseInt(tok.substring(3, 5), 16);
                return val >= 0 && val <= 255;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return false;
    }

    public static byte parseByteToken(String tok) {
        return (byte) Integer.parseInt(tok.substring(3, 5), 16);
    }

    public static boolean isSpecialToken(String tokStr) {
        if (tokStr == null || tokStr.isEmpty()) return false;
        return tokStr.equals("<s>") || tokStr.equals("</s>") || tokStr.equals("<unk>")
                || tokStr.equals("<pad>") || tokStr.equals("<eos>") || tokStr.equals("<bos>");
    }

    public int getBosTokenId() {
        if (vocab.containsKey("<s>")) return vocab.get("<s>");
        if (vocab.containsKey("<bos>")) return vocab.get("<bos>");
        return -1;
    }

    public int getEosTokenId() {
        if (vocab.containsKey("</s>")) return vocab.get("</s>");
        if (vocab.containsKey("<eos>")) return vocab.get("<eos>");
        return -1;
    }

    public int getUnkTokenId() {
        if (vocab.containsKey("<unk>")) return vocab.get("<unk>");
        return -1;
    }

    public int getPadTokenId() {
        if (vocab.containsKey("<pad>")) return vocab.get("<pad>");
        if (vocab.containsKey("<unk>")) return vocab.get("<unk>");
        return -1;
    }

    public String validateVocabulary() {
        StringBuilder sb = new StringBuilder();
        int size = vocab.size();
        sb.append(String.format("Vocabulary size: %d%n", size));

        int minId = Integer.MAX_VALUE;
        int maxId = Integer.MIN_VALUE;
        Set<Integer> uniqueIds = new HashSet<>();
        List<Integer> duplicateIds = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : vocab.entrySet()) {
            int id = entry.getValue();
            if (id < minId) minId = id;
            if (id > maxId) maxId = id;
            if (!uniqueIds.add(id)) {
                duplicateIds.add(id);
            }
        }

        sb.append(String.format("ID range: %d - %d%n", minId == Integer.MAX_VALUE ? 0 : minId, maxId == Integer.MIN_VALUE ? 0 : maxId));
        sb.append(String.format("Duplicate IDs: %d%n", duplicateIds.size()));

        int missingCount = 0;
        if (minId != Integer.MAX_VALUE && maxId != Integer.MIN_VALUE) {
            for (int i = minId; i <= maxId; i++) {
                if (!uniqueIds.contains(i)) {
                    missingCount++;
                }
            }
        }
        sb.append(String.format("Missing IDs in range: %d%n", missingCount));

        int byteTokenCount = 0;
        int malformedByteTokens = 0;
        int specialTokenCount = 0;

        for (Map.Entry<String, Integer> entry : vocab.entrySet()) {
            String token = entry.getKey();
            if (isByteToken(token)) {
                byteTokenCount++;
            } else if (token.startsWith("<0x") || token.startsWith("<0X")) {
                malformedByteTokens++;
            }
            if (isSpecialToken(token)) {
                specialTokenCount++;
            }
        }

        sb.append(String.format("Byte tokens: %d%n", byteTokenCount));
        sb.append(String.format("Malformed byte tokens: %d%n", malformedByteTokens));
        sb.append(String.format("Special tokens: %d%n", specialTokenCount));
        sb.append(String.format("BOS ID: %d, EOS ID: %d, PAD ID: %d, UNK ID: %d%n",
                getBosTokenId(), getEosTokenId(), getPadTokenId(), getUnkTokenId()));

        return sb.toString();
    }

    @Override
    public int getVocabSize() {
        return vocab.size();
    }

    public Map<String, Integer> getVocab() {
        return vocab;
    }

    public Map<Integer, String> getInvVocab() {
        return invVocab;
    }

    public void setVocabulary(Map<String, Integer> vocab) {
        this.vocab = vocab;
        updateInvVocab();
    }
}
