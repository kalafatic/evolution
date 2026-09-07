package eu.kalafatic.evolution.forge.model.protocol;

import org.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Tokenizer descriptor and vocabulary configuration for EVO Native Model Protocol.
 */
public class EvoTokenizerDescriptor {

    private String tokenizerType = "BPE";
    private int vocabSize;
    private int bosTokenId = 1;
    private int eosTokenId = 2;
    private int unkTokenId = 0;
    private int padTokenId = 0;
    private String vocabularyChecksum = "";

    private Map<Integer, String> idToToken = new LinkedHashMap<>();
    private Map<String, Integer> tokenToId = new LinkedHashMap<>();

    public EvoTokenizerDescriptor() {}

    public EvoTokenizerDescriptor(int vocabSize, Map<String, Integer> tokenToId) {
        this.vocabSize = vocabSize;
        if (tokenToId != null) {
            this.tokenToId = new LinkedHashMap<>(tokenToId);
            this.idToToken = new LinkedHashMap<>();
            for (Map.Entry<String, Integer> entry : tokenToId.entrySet()) {
                this.idToToken.put(entry.getValue(), entry.getKey());
            }
        }
        ensureSpecialTokens();
    }

    public void ensureSpecialTokens() {
        if (tokenToId.containsKey("<s>")) bosTokenId = tokenToId.get("<s>");
        if (tokenToId.containsKey("</s>")) eosTokenId = tokenToId.get("</s>");
        if (tokenToId.containsKey("<unk>")) unkTokenId = tokenToId.get("<unk>");
        if (tokenToId.containsKey("<pad>")) padTokenId = tokenToId.get("<pad>");
    }

    public JSONObject toJsonObject() {
        JSONObject obj = new JSONObject();
        obj.put("tokenizer_type", tokenizerType);
        obj.put("vocab_size", vocabSize);
        obj.put("bos_token_id", bosTokenId);
        obj.put("eos_token_id", eosTokenId);
        obj.put("unk_token_id", unkTokenId);
        obj.put("pad_token_id", padTokenId);
        obj.put("vocabulary_checksum", vocabularyChecksum);
        return obj;
    }

    public static EvoTokenizerDescriptor fromJsonObject(JSONObject obj) {
        EvoTokenizerDescriptor desc = new EvoTokenizerDescriptor();
        if (obj.has("tokenizer_type")) desc.tokenizerType = obj.getString("tokenizer_type");
        if (obj.has("vocab_size")) desc.vocabSize = obj.getInt("vocab_size");
        if (obj.has("bos_token_id")) desc.bosTokenId = obj.getInt("bos_token_id");
        if (obj.has("eos_token_id")) desc.eosTokenId = obj.getInt("eos_token_id");
        if (obj.has("unk_token_id")) desc.unkTokenId = obj.getInt("unk_token_id");
        if (obj.has("pad_token_id")) desc.padTokenId = obj.getInt("pad_token_id");
        if (obj.has("vocabulary_checksum")) desc.vocabularyChecksum = obj.getString("vocabulary_checksum");
        return desc;
    }

    public String getTokenizerType() { return tokenizerType; }
    public void setTokenizerType(String tokenizerType) { this.tokenizerType = tokenizerType; }

    public int getVocabSize() { return vocabSize; }
    public void setVocabSize(int vocabSize) { this.vocabSize = vocabSize; }

    public int getBosTokenId() { return bosTokenId; }
    public void setBosTokenId(int bosTokenId) { this.bosTokenId = bosTokenId; }

    public int getEosTokenId() { return eosTokenId; }
    public void setEosTokenId(int eosTokenId) { this.eosTokenId = eosTokenId; }

    public int getUnkTokenId() { return unkTokenId; }
    public void setUnkTokenId(int unkTokenId) { this.unkTokenId = unkTokenId; }

    public int getPadTokenId() { return padTokenId; }
    public void setPadTokenId(int padTokenId) { this.padTokenId = padTokenId; }

    public String getVocabularyChecksum() { return vocabularyChecksum; }
    public void setVocabularyChecksum(String vocabularyChecksum) { this.vocabularyChecksum = vocabularyChecksum; }

    public Map<Integer, String> getIdToToken() { return idToToken; }
    public void setIdToToken(Map<Integer, String> idToToken) { this.idToToken = idToToken; }

    public Map<String, Integer> getTokenToId() { return tokenToId; }
    public void setTokenToId(Map<String, Integer> tokenToId) { this.tokenToId = tokenToId; }
}
