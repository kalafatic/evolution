package eu.kalafatic.evolution.forge.model.llm;

import eu.kalafatic.evolution.forge.math.api.Tensor;
import eu.kalafatic.evolution.forge.math.core.SimpleTensor;

public class RMSNorm {
    private final Tensor weight;
    private final int dimension;
    private final float epsilon;
    private Tensor lastX; // For backward pass
    
    public RMSNorm(int dimension) {
        this(dimension, 1e-5f);
    }
    
    public RMSNorm(int dimension, float epsilon) {
        this.dimension = dimension;
        this.epsilon = epsilon;
        this.weight = new SimpleTensor(dimension);
        // Initialize to 1.0 (standard for RMSNorm)
        float[] data = weight.getData();
        for (int i = 0; i < data.length; i++) {
            data[i] = 1.0f;
        }
    }
    
    public Tensor forward(Tensor x) {
        this.lastX = x;
        float[] input = x.getData();
        long[] shape = x.getShape();
        int rows = (int) shape[0];
        int dim = (int) shape[1];
        
        Tensor output = new SimpleTensor(shape);
        float[] outData = output.getData();
        float[] weightData = weight.getData();
        
        for (int r = 0; r < rows; r++) {
            int offset = r * dim;
            float sumSq = 0.0f;
            for (int i = 0; i < dim; i++) {
                float val = input[offset + i];
                sumSq += val * val;
            }
            float rms = (float) Math.sqrt(sumSq / dim + epsilon);
            for (int i = 0; i < dim; i++) {
                outData[offset + i] = (input[offset + i] / rms) * weightData[i];
            }
        }
        
        return output;
    }
    
    public Tensor backward(Tensor dOutput) {
        if (lastX == null) return dOutput;
        
        float[] input = lastX.getData();
        float[] grad = dOutput.getData();
        long[] shape = lastX.getShape();
        int rows = (int) shape[0];
        int dim = (int) shape[1];
        
        Tensor dX = new SimpleTensor(shape);
        float[] dXData = dX.getData();
        float[] weightData = weight.getData();
        float[] weightGrad = weight.getGrad();
        
        for (int r = 0; r < rows; r++) {
            int offset = r * dim;
            float sumSq = 0.0f;
            for (int i = 0; i < dim; i++) {
                float val = input[offset + i];
                sumSq += val * val;
            }
            float rms = (float) Math.sqrt(sumSq / dim + epsilon);

            float dot = 0.0f;
            for (int i = 0; i < dim; i++) {
                float normX = input[offset + i] / rms;
                float dNorm = grad[offset + i] * weightData[i];
                dot += dNorm * normX;
                weightGrad[i] += grad[offset + i] * normX;
            }

            for (int i = 0; i < dim; i++) {
                float normX = input[offset + i] / rms;
                float dNorm = grad[offset + i] * weightData[i];
                dXData[offset + i] = (dNorm - normX * dot / dim) / rms;
            }
        }
        
        return dX;
    }
    
    public Tensor getWeight() { return weight; }
    public int getDimension() { return dimension; }
    public float getEpsilon() { return epsilon; }
}