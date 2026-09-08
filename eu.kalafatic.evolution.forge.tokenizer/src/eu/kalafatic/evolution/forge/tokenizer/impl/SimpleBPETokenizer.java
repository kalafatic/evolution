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
        for (String key : vocab.keySet()) {
            if (key == null || key.isEmpty()) continue;
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
                tokens.add(vocab.get("<unk>"));
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
            if (token == null) continue;
            String tokStr = invVocab.getOrDefault(token, "");
            if (tokStr == null || tokStr.isEmpty()) continue;

            if (isSpecialToken(tokStr)) {
                continue;
            }

            if (isByteToken(tokStr)) {
                byteBuffer.write(parseByteToken(tokStr));
            } else {
                if (byteBuffer.size() > 0) {
                    sb.append(new String(byteBuffer.toByteArray(), java.nio.charset.StandardCharsets.UTF_8));
                    byteBuffer.reset();
                }
                sb.append(tokStr);
            }
        }

        if (byteBuffer.size() > 0) {
            sb.append(new String(byteBuffer.toByteArray(), java.nio.charset.StandardCharsets.UTF_8));
        }

        return sb.toString();
    }

    public static boolean isByteToken(String tok) {
        if (tok != null && tok.length() == 6 && tok.startsWith("<0x") && tok.endsWith(">")) {
            try {
                Integer.parseInt(tok.substring(3, 5), 16);
                return true;
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
        return tokStr.equals("<s>") || tokStr.equals("</s>") || tokStr.equals("<unk>") || tokStr.equals("<pad>");
    }

    public int getBosTokenId() {
        return vocab.getOrDefault("<s>", 1);
    }

    public int getEosTokenId() {
        return vocab.getOrDefault("</s>", 2);
    }

    public int getUnkTokenId() {
        return vocab.getOrDefault("<unk>", 0);
    }

    public int getPadTokenId() {
        return vocab.getOrDefault("<pad>", 0);
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
