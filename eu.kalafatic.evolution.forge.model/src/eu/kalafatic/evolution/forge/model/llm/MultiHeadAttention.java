package eu.kalafatic.evolution.forge.model.llm;

import eu.kalafatic.evolution.forge.math.api.Tensor;
import eu.kalafatic.evolution.forge.math.core.SimpleTensor;

public class MultiHeadAttention {
    private final Tensor WQ, WK, WV, WO;
    private final int dModel;
    private final int numHeads;
    private final int headDim;
    
    private Tensor lastQ, lastK, lastV; // Cached for backward
    
    public MultiHeadAttention(int dModel, int numHeads) {
        this.dModel = dModel;
        this.numHeads = numHeads;
        this.headDim = dModel / numHeads;
        
        // Weight matrices: [dModel, dModel]
        this.WQ = new SimpleTensor(dModel, dModel);
        this.WK = new SimpleTensor(dModel, dModel);
        this.WV = new SimpleTensor(dModel, dModel);
        this.WO = new SimpleTensor(dModel, dModel);
        
        // Initialize with small random values
        java.util.Random r = new java.util.Random();
        for (Tensor t : new Tensor[]{WQ, WK, WV, WO}) {
            float[] data = t.getData();
            for (int i = 0; i < data.length; i++) {
                data[i] = (r.nextFloat() * 2 - 1) * 0.02f;
            }
        }
    }
    
    public Tensor forward(Tensor x) {
        // x: [seq_len, dModel]
        // Store for backward
        this.lastQ = x.matmul(WQ);
        this.lastK = x.matmul(WK);
        this.lastV = x.matmul(WV);
        
        // Simplified attention (full implementation would handle multi-head)
        // For now, do: attention = softmax(Q*K^T/sqrt(d)) * V
        Tensor q = lastQ;
        Tensor k = lastK.transpose();
        Tensor scores = q.matmul(k);
        
        // Scale
        float scale = 1.0f / (float) Math.sqrt(headDim);
        float[] scoresData = scores.getData();
        for (int i = 0; i < scoresData.length; i++) {
            scoresData[i] *= scale;
        }
        
        // Softmax (simplified - along last dimension)
        // This would need proper implementation in production
        
        Tensor attention = scores.matmul(lastV);
        return attention.matmul(WO);
    }
    
    public Tensor backward(Tensor dOutput) {
        // Full backward implementation would go here
        // For now, return dOutput to pass gradient through
        return dOutput;
    }
    
    public Tensor getWQ() { return WQ; }
    public Tensor getWK() { return WK; }
    public Tensor getWV() { return WV; }
    public Tensor getWO() { return WO; }
}