package eu.kalafatic.evolution.forge.model.protocol;

import org.json.JSONObject;

/**
 * Descriptive architecture descriptor for models encoded in the EVO Native Model Protocol.
 */
public class EvoArchitectureDescriptor {

    private String architectureFamily = "evo_llm";
    private int vocabSize;
    private int dModel;
    private int numHeads;
    private int numKvHeads;
    private int numBlocks;
    private int dff;
    private int maxSeqLen;
    private String normType = "rms_norm";
    private float normEps = 1e-5f;
    private String activationType = "swiglu";
    private String positionalEncoding = "rope";
    private float ropeTheta = 10000.0f;
    private boolean embeddingTied = false;

    public EvoArchitectureDescriptor() {}

    public EvoArchitectureDescriptor(int vocabSize, int dModel, int numHeads, int numBlocks, int dff, int maxSeqLen) {
        this.vocabSize = vocabSize;
        this.dModel = dModel;
        this.numHeads = numHeads;
        this.numKvHeads = numHeads;
        this.numBlocks = numBlocks;
        this.dff = dff;
        this.maxSeqLen = maxSeqLen;
    }

    public JSONObject toJsonObject() {
        JSONObject obj = new JSONObject();
        obj.put("architecture_family", architectureFamily);
        obj.put("vocab_size", vocabSize);
        obj.put("d_model", dModel);
        obj.put("num_heads", numHeads);
        obj.put("num_kv_heads", numKvHeads);
        obj.put("num_blocks", numBlocks);
        obj.put("dff", dff);
        obj.put("max_seq_len", maxSeqLen);
        obj.put("norm_type", normType);
        obj.put("norm_eps", normEps);
        obj.put("activation_type", activationType);
        obj.put("positional_encoding", positionalEncoding);
        obj.put("rope_theta", ropeTheta);
        obj.put("embedding_tied", embeddingTied);
        return obj;
    }

    public static EvoArchitectureDescriptor fromJsonObject(JSONObject obj) {
        EvoArchitectureDescriptor desc = new EvoArchitectureDescriptor();
        if (obj.has("architecture_family")) desc.architectureFamily = obj.getString("architecture_family");
        if (obj.has("vocab_size")) desc.vocabSize = obj.getInt("vocab_size");
        else if (obj.has("vocabSize")) desc.vocabSize = obj.getInt("vocabSize");

        if (obj.has("d_model")) desc.dModel = obj.getInt("d_model");
        else if (obj.has("dModel")) desc.dModel = obj.getInt("dModel");

        if (obj.has("num_heads")) desc.numHeads = obj.getInt("num_heads");
        else if (obj.has("numHeads")) desc.numHeads = obj.getInt("numHeads");

        if (obj.has("num_kv_heads")) desc.numKvHeads = obj.getInt("num_kv_heads");
        else desc.numKvHeads = desc.numHeads;

        if (obj.has("num_blocks")) desc.numBlocks = obj.getInt("num_blocks");
        else if (obj.has("numBlocks")) desc.numBlocks = obj.getInt("numBlocks");

        if (obj.has("dff")) desc.dff = obj.getInt("dff");

        if (obj.has("max_seq_len")) desc.maxSeqLen = obj.getInt("max_seq_len");
        else if (obj.has("maxSeqLen")) desc.maxSeqLen = obj.getInt("maxSeqLen");

        if (obj.has("norm_type")) desc.normType = obj.getString("norm_type");
        if (obj.has("norm_eps")) desc.normEps = (float) obj.getDouble("norm_eps");
        if (obj.has("activation_type")) desc.activationType = obj.getString("activation_type");
        if (obj.has("positional_encoding")) desc.positionalEncoding = obj.getString("positional_encoding");
        if (obj.has("rope_theta")) desc.ropeTheta = (float) obj.getDouble("rope_theta");
        if (obj.has("embedding_tied")) desc.embeddingTied = obj.getBoolean("embedding_tied");

        return desc;
    }

    public String getArchitectureFamily() { return architectureFamily; }
    public void setArchitectureFamily(String architectureFamily) { this.architectureFamily = architectureFamily; }

    public int getVocabSize() { return vocabSize; }
    public void setVocabSize(int vocabSize) { this.vocabSize = vocabSize; }

    public int getDModel() { return dModel; }
    public void setDModel(int dModel) { this.dModel = dModel; }

    public int getNumHeads() { return numHeads; }
    public void setNumHeads(int numHeads) { this.numHeads = numHeads; }

    public int getNumKvHeads() { return numKvHeads; }
    public void setNumKvHeads(int numKvHeads) { this.numKvHeads = numKvHeads; }

    public int getNumBlocks() { return numBlocks; }
    public void setNumBlocks(int numBlocks) { this.numBlocks = numBlocks; }

    public int getDff() { return dff; }
    public void setDff(int dff) { this.dff = dff; }

    public int getMaxSeqLen() { return maxSeqLen; }
    public void setMaxSeqLen(int maxSeqLen) { this.maxSeqLen = maxSeqLen; }

    public String getNormType() { return normType; }
    public void setNormType(String normType) { this.normType = normType; }

    public float getNormEps() { return normEps; }
    public void setNormEps(float normEps) { this.normEps = normEps; }

    public String getActivationType() { return activationType; }
    public void setActivationType(String activationType) { this.activationType = activationType; }

    public String getPositionalEncoding() { return positionalEncoding; }
    public void setPositionalEncoding(String positionalEncoding) { this.positionalEncoding = positionalEncoding; }

    public float getRopeTheta() { return ropeTheta; }
    public void setRopeTheta(float ropeTheta) { this.ropeTheta = ropeTheta; }

    public boolean isEmbeddingTied() { return embeddingTied; }
    public void setEmbeddingTied(boolean embeddingTied) { this.embeddingTied = embeddingTied; }
}
