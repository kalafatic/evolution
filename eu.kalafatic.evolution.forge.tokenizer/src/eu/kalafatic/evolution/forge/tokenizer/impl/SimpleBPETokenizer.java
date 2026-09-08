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
        // Keep them sorted by length descending so the first match we find is the longest match.
        // Exclude special tokens like <s>, </s>, <unk>, <pad>, and byte tokens <0xXX> from text candidate lookup.
        Map<Character, List<String>> prefixMap = new HashMap<>();
        for (String key : vocab.keySet()) {
            if (key == null || key.isEmpty()) continue;
            if (key.equals("<s>") || key.equals("</s>") || key.equals("<unk>") || key.equals("<pad>")) continue;
            if (key.length() == 6 && key.startsWith("<0x") && key.endsWith(">")) continue;
            char firstChar = key.charAt(0);
            prefixMap.computeIfAbsent(firstChar, k -> new ArrayList<>()).add(key);
        }

        for (List<String> list : prefixMap.values()) {
            list.sort((s1, s2) -> Integer.compare(s2.length(), s1.length()));
        }

        int i = 0;
        int len = text.length();
        int unkId = vocab.getOrDefault("<unk>", 0);

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
                        break; // Sorted by length descending, so first match is longest!
                    }
                }
            }

            if (match != null) {
                tokens.add(vocab.get(match));
                i += matchLen;
            } else {
                // Byte fallback handling: convert character to UTF-8 byte(s)
                byte[] bytes = String.valueOf(currentChar).getBytes(java.nio.charset.StandardCharsets.UTF_8);
                boolean allBytesAvailable = true;
                List<Integer> byteTokenIds = new ArrayList<>();
                for (byte b : bytes) {
                    String byteToken = String.format("<0x%02X>", b & 0xFF);
                    Integer byteId = vocab.get(byteToken);
                    if (byteId != null) {
                        byteTokenIds.add(byteId);
                    } else {
                        allBytesAvailable = false;
                        break;
                    }
                }

                if (allBytesAvailable && !byteTokenIds.isEmpty()) {
                    tokens.addAll(byteTokenIds);
                } else {
                    String charStr = String.valueOf(currentChar);
                    if (vocab.containsKey(charStr)) {
                        tokens.add(vocab.get(charStr));
                    } else {
                        tokens.add(unkId);
                    }
                }
                i++;
            }
        }
        return tokens;
    }

    @Override
    public String decode(List<Integer> tokens) {
        if (tokens == null || tokens.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        java.io.ByteArrayOutputStream byteBuffer = new java.io.ByteArrayOutputStream();

        for (Integer token : tokens) {
            String tokStr = invVocab.getOrDefault(token, "");
            if (tokStr == null || tokStr.isEmpty()) {
                continue;
            }

            if (tokStr.equals("<s>") || tokStr.equals("</s>") || tokStr.equals("<unk>") || tokStr.equals("<pad>")) {
                flushByteBuffer(byteBuffer, sb);
                continue;
            }

            // Check if token is byte fallback token <0xXX>
            if (tokStr.length() == 6 && tokStr.startsWith("<0x") && tokStr.endsWith(">")) {
                try {
                    int b = Integer.parseInt(tokStr.substring(4, 6), 16);
                    byteBuffer.write(b);
                } catch (NumberFormatException e) {
                    flushByteBuffer(byteBuffer, sb);
                    sb.append(tokStr);
                }
            } else {
                flushByteBuffer(byteBuffer, sb);
                sb.append(tokStr);
            }
        }

        flushByteBuffer(byteBuffer, sb);
        return sb.toString();
    }

    private void flushByteBuffer(java.io.ByteArrayOutputStream byteBuffer, StringBuilder sb) {
        if (byteBuffer != null && byteBuffer.size() > 0) {
            sb.append(byteBuffer.toString(java.nio.charset.StandardCharsets.UTF_8));
            byteBuffer.reset();
        }
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
