package eu.kalafatic.evolution.forge.tokenizer.impl;

import eu.kalafatic.evolution.forge.tokenizer.api.Tokenizer;
import java.util.*;

public class WordTokenizer implements Tokenizer {
    private Map<String, Integer> vocab = new HashMap<>();
    private Map<Integer, String> invVocab = new HashMap<>();

    public void setVocabulary(Map<String, Integer> vocab) {
        this.vocab = vocab;
        this.invVocab = new HashMap<>();
        vocab.forEach((k, v) -> invVocab.put(v, k));
    }

    @Override
    public List<Integer> encode(String text) {
        // Simple word-level tokenizer that keeps punctuation
        List<String> words = new ArrayList<>();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\w+|[^\\w\\s]").matcher(text);
        while (m.find()) {
            words.add(m.group());
        }
        
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
            sb.append(invVocab.getOrDefault(token, "<UNK>"));
            // Add space if it's a word, but this is a simplified version
            sb.append(" ");
        }
        return sb.toString().trim();
    }

    @Override
    public int getVocabSize() {
        return vocab.size();
    }
}
