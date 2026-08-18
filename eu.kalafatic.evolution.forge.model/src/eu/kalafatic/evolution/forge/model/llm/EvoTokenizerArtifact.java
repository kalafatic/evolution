package eu.kalafatic.evolution.forge.model.llm;

import java.io.Serializable;
import java.util.*;

/**
 * Tokenizer artifact containing token vocabulary, special token IDs,
 * tokenizer type, and serialization helpers.
 */
public class EvoTokenizerArtifact implements Serializable {

    private static final long serialVersionUID = 1L;

    private String tokenizerType = "SimpleBPE";
    private Map<String, Integer> vocab = new LinkedHashMap<>();
    private transient Map<Integer, String> invVocab = null;

    private int unkId = 0;
    private int bosId = 1;
    private int eosId = 2;
    private int padId = 3;

    public EvoTokenizerArtifact() {}

    public EvoTokenizerArtifact(Map<String, Integer> vocab) {
        this("SimpleBPE", vocab, 0, 1, 2, 3);
    }

    public EvoTokenizerArtifact(String tokenizerType, Map<String, Integer> vocab, int unkId, int bosId, int eosId, int padId) {
        this.tokenizerType = tokenizerType != null ? tokenizerType : "SimpleBPE";
        if (vocab != null) {
            this.vocab = new LinkedHashMap<>(vocab);
        }
        this.unkId = unkId;
        this.bosId = bosId;
        this.eosId = eosId;
        this.padId = padId;
        rebuildInvVocab();
    }

    public void rebuildInvVocab() {
        if (invVocab == null) {
            invVocab = new HashMap<>();
        } else {
            invVocab.clear();
        }
        if (vocab != null) {
            vocab.forEach((k, v) -> invVocab.put(v, k));
        }
    }

    public Map<Integer, String> getInvVocab() {
        if (invVocab == null || invVocab.size() != vocab.size()) {
            rebuildInvVocab();
        }
        return invVocab;
    }

    public void padToSize(int targetSize) {
        if (vocab.size() >= targetSize) {
            return;
        }
        int currentMaxId = -1;
        for (Integer id : vocab.values()) {
            if (id > currentMaxId) {
                currentMaxId = id;
            }
        }
        int nextId = Math.max(currentMaxId + 1, vocab.size());
        while (vocab.size() < targetSize) {
            while (vocab.containsValue(nextId)) {
                nextId++;
            }
            String padToken = "token_" + nextId;
            vocab.put(padToken, nextId);
            nextId++;
        }
        rebuildInvVocab();
    }

    public int getVocabSize() {
        return vocab != null ? vocab.size() : 0;
    }

    public String getTokenizerType() { return tokenizerType; }
    public void setTokenizerType(String tokenizerType) { this.tokenizerType = tokenizerType; }

    public Map<String, Integer> getVocab() { return vocab; }
    public void setVocab(Map<String, Integer> vocab) {
        this.vocab = vocab != null ? new LinkedHashMap<>(vocab) : new LinkedHashMap<>();
        rebuildInvVocab();
    }

    public int getUnkId() { return unkId; }
    public void setUnkId(int unkId) { this.unkId = unkId; }

    public int getBosId() { return bosId; }
    public void setBosId(int bosId) { this.bosId = bosId; }

    public int getEosId() { return eosId; }
    public void setEosId(int eosId) { this.eosId = eosId; }

    public int getPadId() { return padId; }
    public void setPadId(int padId) { this.padId = padId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EvoTokenizerArtifact that = (EvoTokenizerArtifact) o;
        return unkId == that.unkId &&
                bosId == that.bosId &&
                eosId == that.eosId &&
                padId == that.padId &&
                Objects.equals(tokenizerType, that.tokenizerType) &&
                Objects.equals(vocab, that.vocab);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tokenizerType, vocab, unkId, bosId, eosId, padId);
    }
}
