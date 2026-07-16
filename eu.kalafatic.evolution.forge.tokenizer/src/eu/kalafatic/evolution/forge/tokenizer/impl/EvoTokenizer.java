package eu.kalafatic.evolution.forge.tokenizer.impl;

import eu.kalafatic.evolution.forge.tokenizer.api.Tokenizer;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class EvoTokenizer implements Tokenizer {
    private Map<String, Integer> vocab = new LinkedHashMap<>();
    private Map<Integer, String> invVocab = new HashMap<>();

    public EvoTokenizer() {
        // Default minimal initialization
        vocab.put("<PAD>", 0);
        vocab.put("<UNK>", 1);
        vocab.put("<BOS>", 2);
        vocab.put("<EOS>", 3);
        updateInvVocab();
    }

    public void trainOnDocuments(List<Path> markdownFiles, int targetVocabSize) {
        vocab.clear();
        vocab.put("<PAD>", 0);
        vocab.put("<UNK>", 1);
        vocab.put("<BOS>", 2);
        vocab.put("<EOS>", 3);

        // Aggregate text
        StringBuilder corpusBuilder = new StringBuilder();
        for (Path file : markdownFiles) {
            try {
                if (Files.exists(file)) {
                    corpusBuilder.append(Files.readString(file)).append(" ");
                }
            } catch (IOException e) {
                // Ignore
            }
        }
        String corpus = corpusBuilder.toString();

        // Extract unique words and chars
        Set<String> uniqueTokens = new LinkedHashSet<>();
        // Add single characters
        for (char c : corpus.toCharArray()) {
            uniqueTokens.add(String.valueOf(c));
        }

        // Add words
        String[] words = corpus.split("\\s+");
        for (String w : words) {
            if (!w.isEmpty()) {
                uniqueTokens.add(w);
            }
        }

        int id = 4;
        for (String token : uniqueTokens) {
            if (vocab.size() >= targetVocabSize) break;
            if (!vocab.containsKey(token)) {
                vocab.put(token, id++);
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
        int i = 0;
        while (i < text.length()) {
            String longestMatch = null;
            int longestLen = 0;
            // Greedy matching
            for (String token : vocab.keySet()) {
                if (text.startsWith(token, i)) {
                    if (token.length() > longestLen) {
                        longestMatch = token;
                        longestLen = token.length();
                    }
                }
            }
            if (longestMatch != null) {
                tokens.add(vocab.get(longestMatch));
                i += longestLen;
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

    public Map<String, Integer> getVocab() {
        return vocab;
    }

    public void save(Path path) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n  \"vocab\": {\n");
        int idx = 0;
        for (Map.Entry<String, Integer> entry : vocab.entrySet()) {
            sb.append("    \"").append(escapeJson(entry.getKey())).append("\": ").append(entry.getValue());
            if (idx < vocab.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
            idx++;
        }
        sb.append("  }\n}");
        Files.writeString(path, sb.toString());
    }

    public void load(Path path) throws IOException {
        String content = Files.readString(path);
        int vocabStart = content.indexOf("\"vocab\": {");
        if (vocabStart != -1) {
            int start = content.indexOf("{", vocabStart + 8);
            int end = content.lastIndexOf("}");
            if (start != -1 && end != -1) {
                String vocabContent = content.substring(start + 1, end).trim();
                vocab.clear();
                for (String line : vocabContent.split("\n")) {
                    line = line.trim();
                    if (line.isEmpty()) continue;
                    if (line.endsWith(",")) {
                        line = line.substring(0, line.length() - 1).trim();
                    }
                    int colonIdx = line.indexOf(":");
                    if (colonIdx != -1) {
                        String token = line.substring(0, colonIdx).trim();
                        if (token.startsWith("\"") && token.endsWith("\"")) {
                            token = token.substring(1, token.length() - 1);
                        }
                        token = unescapeJson(token);
                        try {
                            int val = Integer.parseInt(line.substring(colonIdx + 1).trim());
                            vocab.put(token, val);
                        } catch (NumberFormatException e) {
                            // Skip or log
                        }
                    }
                }
            }
        }
        updateInvVocab();
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }

    private String unescapeJson(String s) {
        return s.replace("\\\"", "\"").replace("\\n", "\n").replace("\\r", "\r").replace("\\\\", "\\");
    }
}
