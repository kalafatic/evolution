package eu.kalafatic.evolution.forge.tokenizer.impl;

import eu.kalafatic.evolution.forge.tokenizer.api.Tokenizer;
import java.util.*;

public class WhitespaceTokenizer implements Tokenizer {
    private Map<String, Integer> vocab = new HashMap<>();
    private Map<Integer, String> invVocab = new HashMap<>();

    public void setVocabulary(Map<String, Integer> vocab) {
        this.vocab = vocab;
        this.invVocab = new HashMap<>();
        vocab.forEach((k, v) -> invVocab.put(v, k));
    }

    @Override
    public List<Integer> encode(String text) {
        String[] words = text.split("\\s+");
        List<Integer> tokens = new ArrayList<>();
        for (String word : words) {
            tokens.add(vocab.getOrDefault(word, vocab.getOrDefault("<UNK>", 1)));
        }
        return tokens;
    }

    @Override
    public String decode(List<Integer> tokens) {
        StringBuilder sb = new StringBuilder();
        for (Integer token : tokens) {
            sb.append(invVocab.getOrDefault(token, "<UNK>")).append(" ");
        }
        return sb.toString().trim();
    }

    @Override
    public int getVocabSize() {
        return vocab.size();
    }
}
