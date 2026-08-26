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
        
        // Xavier/He Normal Initialization
        java.util.Random r = new java.util.Random();
        float std = (float) (1.0 / Math.sqrt(dModel));
        for (Tensor t : new Tensor[]{WQ, WK, WV, WO}) {
            float[] data = t.getData();
            for (int i = 0; i < data.length; i++) {
                data[i] = (float) (r.nextGaussian() * std);
            }
        }
    }
    
    private Tensor lastX;
    private Tensor lastSoftmax;
    private Tensor lastAttention;

    public Tensor forward(Tensor x) {
        this.lastX = x;
        this.lastQ = x.matmul(WQ);
        this.lastK = x.matmul(WK);
        this.lastV = x.matmul(WV);
        
        Tensor scores = lastQ.matmul(lastK.transpose());
        float scale = 1.0f / (float) Math.sqrt(headDim);

        float[] scoresData = scores.getData();
        int seqLen = (int) x.getShape()[0];
        
        Tensor softmaxProbs = new SimpleTensor(seqLen, seqLen);
        float[] softData = softmaxProbs.getData();

        for (int r = 0; r < seqLen; r++) {
            int offset = r * seqLen;
            float maxVal = Float.NEGATIVE_INFINITY;
            for (int c = 0; c < seqLen; c++) {
                scoresData[offset + c] *= scale;
                if (scoresData[offset + c] > maxVal) {
                    maxVal = scoresData[offset + c];
                }
            }
            float sum = 0.0f;
            for (int c = 0; c < seqLen; c++) {
                softData[offset + c] = (float) Math.exp(scoresData[offset + c] - maxVal);
                sum += softData[offset + c];
            }
            for (int c = 0; c < seqLen; c++) {
                softData[offset + c] /= sum;
            }
        }
        this.lastSoftmax = softmaxProbs;
        
        this.lastAttention = softmaxProbs.matmul(lastV);
        return lastAttention.matmul(WO);
    }
    
    public Tensor backward(Tensor dOutput) {
        if (lastX == null) return dOutput;

        // 1. dWO = lastAttention^T * dOutput
        Tensor dWO = lastAttention.transpose().matmul(dOutput);
        float[] woGrad = WO.getGrad();
        float[] dWOData = dWO.getData();
        for (int i = 0; i < woGrad.length; i++) {
            woGrad[i] += dWOData[i];
        }

        // 2. dAttention = dOutput * WO^T
        Tensor dAttn = dOutput.matmul(WO.transpose());

        // 3. dWV = lastSoftmax^T * dAttn
        Tensor dWV = lastSoftmax.transpose().matmul(dAttn);
        float[] wvGrad = WV.getGrad();
        float[] dWVData = dWV.getData();
        for (int i = 0; i < wvGrad.length; i++) {
            wvGrad[i] += dWVData[i];
        }

        // 4. dSoftmax = dAttn * lastV^T
        Tensor dSoftmax = dAttn.matmul(lastV.transpose());
        float[] dSoftData = dSoftmax.getData();
        float[] softData = lastSoftmax.getData();
        int seqLen = (int) lastX.getShape()[0];

        Tensor dScores = new SimpleTensor(seqLen, seqLen);
        float[] dScoresData = dScores.getData();
        float scale = 1.0f / (float) Math.sqrt(headDim);

        for (int r = 0; r < seqLen; r++) {
            int offset = r * seqLen;
            float dot = 0.0f;
            for (int c = 0; c < seqLen; c++) {
                dot += dSoftData[offset + c] * softData[offset + c];
            }
            for (int c = 0; c < seqLen; c++) {
                float s = softData[offset + c];
                float dS = dSoftData[offset + c];
                dScoresData[offset + c] = s * (dS - dot) * scale;
            }
        }

        // 5. dQ = dScores * lastK, dK = dScores^T * lastQ
        Tensor dQ = dScores.matmul(lastK);
        Tensor dK = dScores.transpose().matmul(lastQ);

        // 6. dWQ = lastX^T * dQ, dWK = lastX^T * dK
        Tensor dWQ = lastX.transpose().matmul(dQ);
        float[] wqGrad = WQ.getGrad();
        float[] dWQData = dWQ.getData();
        for (int i = 0; i < wqGrad.length; i++) {
            wqGrad[i] += dWQData[i];
        }

        Tensor dWK = lastX.transpose().matmul(dK);
        float[] wkGrad = WK.getGrad();
        float[] dWKData = dWK.getData();
        for (int i = 0; i < wkGrad.length; i++) {
            wkGrad[i] += dWKData[i];
        }

        // 7. dX = dQ * WQ^T + dK * WK^T + dAttn * WV^T
        Tensor dXQ = dQ.matmul(WQ.transpose());
        Tensor dXK = dK.matmul(WK.transpose());
        Tensor dXV = dAttn.matmul(WV.transpose());
        return dXQ.add(dXK).add(dXV);
    }
    
    public Tensor getWQ() { return WQ; }
    public Tensor getWK() { return WK; }
    public Tensor getWV() { return WV; }
    public Tensor getWO() { return WO; }
}