package eu.kalafatic.evolution.forge.data.impl;

import java.util.*;
import java.util.stream.Collectors;

public class VocabularyBuilder {
    public Map<String, Integer> buildVocabulary(List<String> tokens, int minFreq) {
        Map<String, Integer> freq = new HashMap<>();
        for (String token : tokens) {
            freq.put(token, freq.getOrDefault(token, 0) + 1);
        }

        List<String> vocabList = freq.entrySet().stream()
            .filter(e -> e.getValue() >= minFreq)
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());

        Map<String, Integer> vocab = new LinkedHashMap<>();
        vocab.put("<PAD>", 0);
        vocab.put("<UNK>", 1);
        vocab.put("<BOS>", 2);
        vocab.put("<EOS>", 3);

        int id = 4;
        for (String token : vocabList) {
            if (!vocab.containsKey(token)) {
                vocab.put(token, id++);
            }
        }
        return vocab;
    }
}
