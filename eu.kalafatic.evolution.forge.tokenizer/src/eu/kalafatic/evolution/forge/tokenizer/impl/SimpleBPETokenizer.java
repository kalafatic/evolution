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
        vocab.put("<PAD>", 0);
        vocab.put("<UNK>", 1);
        vocab.put("<BOS>", 2);
        vocab.put("<EOS>", 3);

        // Initial characters
        Set<String> chars = new HashSet<>();
        for (char c : corpus.toCharArray()) {
            chars.add(String.valueOf(c));
        }
        int id = 4;
        for (String c : chars) {
            vocab.put(c, id++);
        }

        // Simulating BPE merges
        // In a real implementation, we would count pairs and merge them.
        // For this demo, we'll just use the initial character-level vocab plus some common words.
        String[] commonWords = {"the", "and", "in", "is", "of", "to", "evolution", "ai"};
        for (String word : commonWords) {
            if (vocab.size() < targetVocabSize) {
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
                tokens.add(vocab.get("<UNK>"));
                i++;
            }
        }
        return tokens;
    }

    @Override
    public String decode(List<Integer> tokens) {
        StringBuilder sb = new StringBuilder();
        for (Integer token : tokens) {
            sb.append(invVocab.getOrDefault(token, ""));
        }
        return sb.toString();
    }

    @Override
    public int getVocabSize() {
        return vocab.size();
    }

    public void setVocabulary(Map<String, Integer> vocab) {
        this.vocab = vocab;
        updateInvVocab();
    }
}
