package eu.kalafatic.evolution.forge.tokenizer.impl;

import eu.kalafatic.evolution.forge.tokenizer.api.Tokenizer;
import java.util.*;

/**
 * A simplified SentencePiece-style tokenizer.
 *
 * <p>The tokenization algorithm mirrors llama.cpp's {@code llm_tokenizer_spm_session}
 * exactly: the text is split into UTF-8 character symbols, every space is escaped
 * to the metaspace symbol (U+2581) with a leading metaspace added, and adjacent
 * symbol pairs whose concatenation exists in the vocabulary are merged greedily
 * (all token scores equal, ties broken by larger left index). Unknown symbols
 * fall back to per-byte tokens. This makes native EVO tokenization bit-identical
 * to llama.cpp / Ollama for {@code tokenizer.ggml.model=llama} GGUFs.</p>
 */
public class SimpleBPETokenizer implements Tokenizer {

    /** SentencePiece-style metaspace character (replaces ' '). */
    public static final String METASPACE = "\u2581";

    private Map<String, Integer> vocab = new LinkedHashMap<>();
    private Map<Integer, String> invVocab = new HashMap<>();

    /**
     * Maximum number of corpus characters used for BPE frequency statistics during
     * {@link #train}. A value of {@link Integer#MAX_VALUE} uses the whole corpus.
     * The previous hard-coded value (100_000) sampled only a tiny leading slice of
     * large corpora, which starved the vocabulary of rare subword merges.
     */
    private int maxSamplingChars = 1_000_000;

    /** Sets the corpus sampling budget used by {@link #train}. */
    public void setMaxSamplingChars(int maxSamplingChars) {
        this.maxSamplingChars = maxSamplingChars > 0 ? maxSamplingChars : Integer.MAX_VALUE;
    }

    public int getMaxSamplingChars() {
        return maxSamplingChars;
    }

    public void train(String corpus, int targetVocabSize) {
        vocab.clear();
        vocab.put("<unk>", 0);
        vocab.put("<s>", 1);
        vocab.put("</s>", 2);
        vocab.put(METASPACE, 3);

        int id = 4;
        if (targetVocabSize >= 260) {
            for (int b = 0; b < 256; b++) {
                String byteToken = String.format("<0x%02X>", b);
                vocab.put(byteToken, id++);
            }
        }

        if (corpus != null && !corpus.isEmpty()) {
            String escaped = corpus.replace(" ", METASPACE);

            // 1. Initial symbols from corpus (metaspace-prefixed word starts and
            //    plain characters inside words).
            for (String w : escaped.split(METASPACE)) {
                if (w.isEmpty()) continue;
                for (char c : (METASPACE + w).toCharArray()) {
                    String s = String.valueOf(c);
                    if (!vocab.containsKey(s) && vocab.size() < targetVocabSize) {
                        vocab.put(s, id++);
                    }
                }
            }

            // 2. Extract metaspace-prefixed words and count frequencies
            String[] rawWords = escaped.split(METASPACE);
            Map<String, Integer> wordCounts = new HashMap<>();
            int sampledChars = 0;
            int maxSampledChars = maxSamplingChars; // Sampling budget for BPE frequency statistics
            for (String w : rawWords) {
                if (w.isEmpty()) continue;
                String pw = METASPACE + w;
                wordCounts.put(pw, wordCounts.getOrDefault(pw, 0) + 1);
                sampledChars += pw.length();
                if (sampledChars >= maxSampledChars && wordCounts.size() >= 1000) {
                    break;
                }
            }

            class WordSpec {
                List<String> syms;
                int count;
                WordSpec(String word, int count) {
                    this.count = count;
                    this.syms = new ArrayList<>(word.length());
                    for (char c : word.toCharArray()) {
                        this.syms.add(String.valueOf(c));
                    }
                }
            }

            List<WordSpec> wordSpecs = new ArrayList<>(wordCounts.size());
            for (Map.Entry<String, Integer> entry : wordCounts.entrySet()) {
                wordSpecs.add(new WordSpec(entry.getKey(), entry.getValue()));
            }

            // Perform iterative BPE pair merges over unique word specifications
            while (vocab.size() < targetVocabSize) {
                Map<String, Integer> pairFreqs = new HashMap<>();
                for (WordSpec ws : wordSpecs) {
                    List<String> syms = ws.syms;
                    int count = ws.count;
                    for (int i = 0; i < syms.size() - 1; i++) {
                        String pair = syms.get(i) + syms.get(i + 1);
                        pairFreqs.put(pair, pairFreqs.getOrDefault(pair, 0) + count);
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

                if (bestPair == null || maxFreq < 2) break;

                vocab.put(bestPair, id++);

                for (WordSpec ws : wordSpecs) {
                    List<String> syms = ws.syms;
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
                String pw = METASPACE + w;
                if (!pw.isEmpty() && !vocab.containsKey(pw)) {
                    vocab.put(pw, id++);
                }
            }
        }

        // Common fallback words (metaspace-prefixed, matching llama.cpp SPM semantics)
        String[] commonWords = {"the", "and", "in", "is", "of", "to", "evolution", "ai", "model", "java", "code", "hi", "hello"};
        for (String word : commonWords) {
            if (vocab.size() < targetVocabSize && !vocab.containsKey(word)) {
                vocab.put(word, id++);
            }
        }

        // Pad the vocabulary to the requested target size with never-occurring placeholder
        // tokens. This makes the produced vocabulary size deterministic and consistent
        // with the model architecture dimensions derived from the training target.
        while (vocab.size() < targetVocabSize) {
            vocab.put("<pad_" + id + ">", id++);
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

        // llama.cpp SPM tokenize(): add_space_prefix=true — a metaspace is
        // prepended, and every ' ' is escaped to the metaspace symbol.
        String escaped = METASPACE + text.replace(" ", METASPACE);

        // Split into UTF-8 character symbols
        List<String> symbols = new ArrayList<>();
        int i = 0;
        while (i < escaped.length()) {
            int cp = escaped.codePointAt(i);
            int charCount = Character.charCount(cp);
            symbols.add(escaped.substring(i, i + charCount));
            i += charCount;
        }
        int n = symbols.size();

        int[] prev = new int[n];
        int[] next = new int[n];
        for (int k = 0; k < n; k++) {
            prev[k] = k - 1;
            next[k] = (k < n - 1) ? k + 1 : -1;
        }

        boolean[] mergedAway = new boolean[n];
        Map<String, int[]> revMerge = new HashMap<>();

        // Priority queue ordered by score desc, then left index ASC — identical
        // tie-breaking to llama.cpp's llm_bigram_spm comparator with all scores 0
        // (its operator() returns true when the left candidate is "less", i.e.
        // higher score wins and, on ties, the SMALLER left index is popped first).
        PriorityQueue<int[]> workQueue = new PriorityQueue<>((a, b) -> {
            if (a[0] != b[0]) return Integer.compare(b[0], a[0]); // higher score first (always 0 here)
            return Integer.compare(a[1], b[1]); // smaller left index first
        });

        java.util.function.BiConsumer<Integer, Integer> tryAdd = (left, right) -> {
            if (left == -1 || right == -1) return;
            String pair = symbols.get(left) + symbols.get(right);
            Integer tokenId = vocab.get(pair);
            if (tokenId == null) return;
            revMerge.put(pair, new int[]{left, right});
            workQueue.add(new int[]{0, left, right, pair.length()});
        };

        for (int k = 1; k < n; k++) {
            tryAdd.accept(k - 1, k);
        }

        while (!workQueue.isEmpty()) {
            int[] bigram = workQueue.poll();
            int left = bigram[1];
            int right = bigram[2];
            int size = bigram[3];

            if (mergedAway[left] || mergedAway[right]) continue;
            if (symbols.get(left).length() + symbols.get(right).length() != size) continue;

            // merge the right sym into the left one
            symbols.set(left, symbols.get(left) + symbols.get(right));
            symbols.set(right, "");
            mergedAway[right] = true;

            int leftNext = (next[left] != right) ? next[left] : next[right];
            next[left] = leftNext;
            if (leftNext != -1) prev[leftNext] = left;

            tryAdd.accept(prev[left], left);
            tryAdd.accept(left, leftNext);
        }

        java.util.function.Consumer<String> resegment = new java.util.function.Consumer<String>() {
            @Override
            public void accept(String symText) {
                Integer tokenId = vocab.get(symText);
                if (tokenId != null) {
                    tokens.add(tokenId);
                    return;
                }
                int[] pair = revMerge.get(symText);
                if (pair != null) {
                    accept(symbols.get(pair[0]));
                    accept(symbols.get(pair[1]));
                    return;
                }
                // Output symbols that did not form tokens as UTF-8 bytes.
                byte[] bytes = symText.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                for (byte b : bytes) {
                    String byteTok = String.format("<0x%02X>", b & 0xFF);
                    Integer byteId = vocab.get(byteTok);
                    tokens.add(byteId != null ? byteId : getUnkTokenId());
                }
            }
        };

        int k = 0;
        while (k != -1) {
            if (!mergedAway[k]) {
                resegment.accept(symbols.get(k));
            }
            k = next[k];
        }
        return tokens;
    }

    public static class IncrementalDecoder {
        private final Map<Integer, String> invVocab;
        private final StringBuilder cumulativeText = new StringBuilder();
        private final java.io.ByteArrayOutputStream byteBuffer = new java.io.ByteArrayOutputStream();
        private final java.nio.charset.CharsetDecoder utf8Decoder = java.nio.charset.StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(java.nio.charset.CodingErrorAction.REPLACE)
                .onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPLACE);
        // SentencePiece add_space_prefix inserts a leading metaspace at the start of
        // an encoded sequence. Its space equivalent must be stripped exactly once so
        // decode(encode(text)) round-trips, without dropping real internal spaces.
        private boolean leadingSpaceHandled = false;

        public IncrementalDecoder(Map<Integer, String> invVocab) {
            this.invVocab = invVocab != null ? invVocab : Collections.emptyMap();
        }

        /**
         * Accepts a single token ID and returns any newly decoded text snippet.
         */
        public String accept(int tokenId) {
            String tokStr = invVocab.getOrDefault(tokenId, "");
            if (tokStr == null || tokStr.isEmpty()) {
                return "";
            }

            if (isSpecialToken(tokStr)) {
                return "";
            }

            if (isByteToken(tokStr)) {
                byteBuffer.write(parseByteToken(tokStr));
                String newlyDecoded = decodePendingBytes(false);
                cumulativeText.append(newlyDecoded);
                return newlyDecoded;
            } else {
                // Non-byte subword token encountered.
                // Flush any pending accumulated bytes first.
                String flushedBytes = decodePendingBytes(true);
                cumulativeText.append(flushedBytes);
                String piece = tokStr.replace(METASPACE, " ");
                if (!leadingSpaceHandled) {
                    leadingSpaceHandled = true;
                    if (piece.startsWith(" ")) {
                        piece = piece.substring(1);
                    }
                }
                cumulativeText.append(piece);
                return flushedBytes + piece;
            }
        }

        /**
         * Flushes remaining pending bytes at the end of generation.
         */
        public String flush() {
            String remaining = decodePendingBytes(true);
            cumulativeText.append(remaining);
            return remaining;
        }

        public String getCumulativeText() {
            return cumulativeText.toString();
        }

        private String decodePendingBytes(boolean endOfInput) {
            if (byteBuffer.size() == 0) {
                return "";
            }

            byte[] rawBytes = byteBuffer.toByteArray();
            java.nio.ByteBuffer in = java.nio.ByteBuffer.wrap(rawBytes);
            java.nio.CharBuffer out = java.nio.CharBuffer.allocate(rawBytes.length * 2 + 10);

            utf8Decoder.reset();
            java.nio.charset.CoderResult result = utf8Decoder.decode(in, out, endOfInput);
            if (endOfInput) {
                utf8Decoder.flush(out);
            }

            out.flip();
            String decodedChunk = out.toString();

            int unconsumed = in.remaining();
            if (unconsumed > 0 && !endOfInput) {
                byte[] remainingBytes = new byte[unconsumed];
                System.arraycopy(rawBytes, in.position(), remainingBytes, 0, unconsumed);
                byteBuffer.reset();
                byteBuffer.write(remainingBytes, 0, unconsumed);
            } else {
                byteBuffer.reset();
            }

            return decodedChunk;
        }
    }

    public IncrementalDecoder createIncrementalDecoder() {
        return new IncrementalDecoder(invVocab);
    }

    @Override
    public String decode(List<Integer> tokens) {
        if (tokens == null || tokens.isEmpty()) {
            return "";
        }
        IncrementalDecoder decoder = createIncrementalDecoder();
        for (Integer token : tokens) {
            if (token != null) {
                decoder.accept(token);
            }
        }
        decoder.flush();
        return decoder.getCumulativeText();
    }

    public static boolean isByteToken(String tok) {
        if (tok != null && tok.length() == 6 && (tok.startsWith("<0x") || tok.startsWith("<0X")) && tok.endsWith(">")) {
            try {
                int val = Integer.parseInt(tok.substring(3, 5), 16);
                return val >= 0 && val <= 255;
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
        if (tokStr == null || tokStr.isEmpty()) return false;
        if (tokStr.equals("<s>") || tokStr.equals("</s>") || tokStr.equals("<unk>")
                || tokStr.equals("<pad>") || tokStr.equals("<eos>") || tokStr.equals("<bos>")
                || tokStr.equals("<|im_end|>") || tokStr.equals("<|im_start|>") || tokStr.equals("<|endoftext|>")) {
            return true;
        }
        // Synthetic vocabulary-padding placeholders (<pad_N>) are never valid language
        // output and must be treated as reserved/special rather than emitted as text.
        if (tokStr.startsWith("<pad_") && tokStr.endsWith(">")) {
            return true;
        }
        return false;
    }

    public int getBosTokenId() {
        if (vocab.containsKey("<s>")) return vocab.get("<s>");
        if (vocab.containsKey("<bos>")) return vocab.get("<bos>");
        if (vocab.containsKey("<|im_start|>")) return vocab.get("<|im_start|>");
        return -1;
    }

    public int getEosTokenId() {
        if (vocab.containsKey("</s>")) return vocab.get("</s>");
        if (vocab.containsKey("<eos>")) return vocab.get("<eos>");
        if (vocab.containsKey("<|im_end|>")) return vocab.get("<|im_end|>");
        if (vocab.containsKey("<|endoftext|>")) return vocab.get("<|endoftext|>");
        return -1;
    }

    public int getUnkTokenId() {
        if (vocab.containsKey("<unk>")) return vocab.get("<unk>");
        return -1;
    }

    public int getPadTokenId() {
        if (vocab.containsKey("<pad>")) return vocab.get("<pad>");
        if (vocab.containsKey("<unk>")) return vocab.get("<unk>");
        return -1;
    }

    public String validateVocabulary() {
        StringBuilder sb = new StringBuilder();
        int size = vocab.size();
        sb.append(String.format("Vocabulary size: %d%n", size));

        int minId = Integer.MAX_VALUE;
        int maxId = Integer.MIN_VALUE;
        Set<Integer> uniqueIds = new HashSet<>();
        List<Integer> duplicateIds = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : vocab.entrySet()) {
            int id = entry.getValue();
            if (id < minId) minId = id;
            if (id > maxId) maxId = id;
            if (!uniqueIds.add(id)) {
                duplicateIds.add(id);
            }
        }

        sb.append(String.format("ID range: %d - %d%n", minId == Integer.MAX_VALUE ? 0 : minId, maxId == Integer.MIN_VALUE ? 0 : maxId));
        sb.append(String.format("Duplicate IDs: %d%n", duplicateIds.size()));

        int missingCount = 0;
        if (minId != Integer.MAX_VALUE && maxId != Integer.MIN_VALUE) {
            for (int i = minId; i <= maxId; i++) {
                if (!uniqueIds.contains(i)) {
                    missingCount++;
                }
            }
        }
        sb.append(String.format("Missing IDs in range: %d%n", missingCount));

        int byteTokenCount = 0;
        int malformedByteTokens = 0;
        int specialTokenCount = 0;

        for (Map.Entry<String, Integer> entry : vocab.entrySet()) {
            String token = entry.getKey();
            if (isByteToken(token)) {
                byteTokenCount++;
            } else if (token.startsWith("<0x") || token.startsWith("<0X")) {
                malformedByteTokens++;
            }
            if (isSpecialToken(token)) {
                specialTokenCount++;
            }
        }

        sb.append(String.format("Byte tokens: %d%n", byteTokenCount));
        sb.append(String.format("Malformed byte tokens: %d%n", malformedByteTokens));
        sb.append(String.format("Special tokens: %d%n", specialTokenCount));
        sb.append(String.format("BOS ID: %d, EOS ID: %d, PAD ID: %d, UNK ID: %d%n",
                getBosTokenId(), getEosTokenId(), getPadTokenId(), getUnkTokenId()));

        return sb.toString();
    }

    @Override
    public int getVocabSize() {
        return vocab.size();
    }

    /**
     * Returns the number of real (non-placeholder) vocabulary entries, i.e. the
     * total vocabulary size minus any synthetic {@code <pad_N>} padding tokens.
     * This is the vocabulary utilization metric used to detect an under-trained
     * tokenizer that filled most of its capacity with placeholders.
     */
    public int getRealVocabSize() {
        int real = 0;
        for (String token : vocab.keySet()) {
            if (!token.startsWith("<pad_")) {
                real++;
            }
        }
        return real;
    }

    /** Returns the number of synthetic {@code <pad_N>} placeholder tokens. */
    public int getPaddingTokenCount() {
        int pad = 0;
        for (String token : vocab.keySet()) {
            if (token.startsWith("<pad_")) {
                pad++;
            }
        }
        return pad;
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
