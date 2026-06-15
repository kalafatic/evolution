package eu.kalafatic.evolution.forge.math.api;

public interface Tensor {
    long[] getShape();
    float[] getData();
    float getValue(long... indices);
    void setValue(float value, long... indices);

    Tensor add(Tensor other);
    Tensor sub(Tensor other);
    Tensor mul(Tensor other);
    Tensor div(Tensor other);

    Tensor matmul(Tensor other);
    Tensor transpose();

    int getRank();
    long getSize();
}
