package eu.kalafatic.evolution.forge.tokenizer.core;

import eu.kalafatic.evolution.forge.tokenizer.api.Tokenizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SimpleTokenizer implements Tokenizer {
    private final Map<String, Integer> charToToken = new HashMap<>();
    private final Map<Integer, String> tokenToChar = new HashMap<>();

    public SimpleTokenizer() {
        for (int i = 0; i < 256; i++) {
            String c = String.valueOf((char) i);
            charToToken.put(c, i);
            tokenToChar.put(i, c);
        }
    }

    @Override
    public List<Integer> encode(String text) {
        List<Integer> tokens = new ArrayList<>();
        for (char c : text.toCharArray()) {
            tokens.add(charToToken.getOrDefault(String.valueOf(c), 0));
        }
        return tokens;
    }

    @Override
    public String decode(List<Integer> tokens) {
        StringBuilder sb = new StringBuilder();
        for (int token : tokens) {
            sb.append(tokenToChar.getOrDefault(token, ""));
        }
        return sb.toString();
    }

    @Override
    public int getVocabSize() {
        return charToToken.size();
    }
}
