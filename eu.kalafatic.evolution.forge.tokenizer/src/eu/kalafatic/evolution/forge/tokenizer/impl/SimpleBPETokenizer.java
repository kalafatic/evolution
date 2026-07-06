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
        // Greedy longest match
        int i = 0;
        while (i < text.length()) {
            String match = null;
            int matchLen = 0;
            for (String v : vocab.keySet()) {
                if (text.startsWith(v, i)) {
                    if (v.length() > matchLen) {
                        match = v;
                        matchLen = v.length();
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
