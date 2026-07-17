package eu.kalafatic.evolution.forge.math.api;

public interface Tensor {
    long[] getShape();
    float[] getData();
    float[] getGrad();
    void zeroGrad();
    float getValue(long... indices);
    void setValue(float value, long... indices);

    Tensor add(Tensor other);
    Tensor sub(Tensor other);
    Tensor mul(Tensor other);
    Tensor div(Tensor other);

    Tensor matmul(Tensor other);
    Tensor transpose();

    Tensor exp();
    Tensor log();
    Tensor sqrt();
    Tensor pow(float exponent);
    float sum();
    Tensor softmax();
    Tensor layerNorm();

    int getRank();
    long getSize();
}
