package eu.kalafatic.evolution.forge.tokenizer.api;

import java.util.List;

public interface Tokenizer {
    List<Integer> encode(String text);
    String decode(List<Integer> tokens);
    int getVocabSize();
}
