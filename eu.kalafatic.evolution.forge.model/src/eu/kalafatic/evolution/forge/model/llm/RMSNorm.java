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
        int size = (int) x.getSize();
        int dim = (int) shape[shape.length - 1]; // Last dimension
        
        // Calculate RMS
        float sumSq = 0.0f;
        for (int i = 0; i < size; i++) {
            sumSq += input[i] * input[i];
        }
        float rms = (float) Math.sqrt(sumSq / size + epsilon);
        
        // Normalize and multiply by weight
        Tensor output = new SimpleTensor(shape);
        float[] outData = output.getData();
        float[] weightData = weight.getData();
        
        for (int i = 0; i < size; i++) {
            int idx = i % dim;
            outData[i] = (input[i] / rms) * weightData[idx];
        }
        
        return output;
    }
    
    public Tensor backward(Tensor dOutput) {
        if (lastX == null) return dOutput;
        
        float[] input = lastX.getData();
        float[] grad = dOutput.getData();
        long[] shape = lastX.getShape();
        int size = (int) lastX.getSize();
        int dim = (int) shape[shape.length - 1];
        
        // Calculate RMS of input
        float sumSq = 0.0f;
        for (int i = 0; i < size; i++) {
            sumSq += input[i] * input[i];
        }
        float rms = (float) Math.sqrt(sumSq / size + epsilon);
        
        // Gradient for weight
        float[] weightGrad = weight.getGrad();
        for (int i = 0; i < size; i++) {
            int idx = i % dim;
            weightGrad[idx] += grad[i] * (input[i] / rms);
        }
        
        // Gradient for input
        Tensor dX = new SimpleTensor(shape);
        float[] dXData = dX.getData();
        float[] weightData = weight.getData();
        
        // Simplified gradient (full derivation omitted for brevity)
        for (int i = 0; i < size; i++) {
            int idx = i % dim;
            dXData[i] = grad[i] * weightData[idx] / rms;
        }
        
        return dX;
    }
    
    public Tensor getWeight() { return weight; }
    public int getDimension() { return dimension; }
    public float getEpsilon() { return epsilon; }
}