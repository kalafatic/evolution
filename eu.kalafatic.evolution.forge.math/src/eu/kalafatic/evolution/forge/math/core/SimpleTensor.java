package eu.kalafatic.evolution.forge.math.core;

import eu.kalafatic.evolution.forge.math.api.Tensor;

public class SimpleTensor implements Tensor {
    private final long[] shape;
    private final float[] data;
    private final int rank;
    private final long size;

    public SimpleTensor(long... shape) {
        this.shape = shape;
        this.rank = shape.length;
        long totalSize = 1;
        for (long dim : shape) {
            totalSize *= dim;
        }
        this.size = totalSize;
        this.data = new float[(int) size];
    }

    public SimpleTensor(long[] shape, float[] data) {
        this.shape = shape;
        this.rank = shape.length;
        this.data = data;
        this.size = data.length;
    }

    @Override
    public long[] getShape() {
        return shape;
    }

    @Override
    public float[] getData() {
        return data;
    }

    @Override
    public float getValue(long... indices) {
        return data[getFlatIndex(indices)];
    }

    @Override
    public void setValue(float value, long... indices) {
        data[getFlatIndex(indices)] = value;
    }

    private int getFlatIndex(long... indices) {
        int index = 0;
        int multiplier = 1;
        for (int i = rank - 1; i >= 0; i--) {
            index += indices[i] * multiplier;
            multiplier *= shape[i];
        }
        return index;
    }

    @Override
    public Tensor add(Tensor other) {
        float[] resultData = new float[(int) size];
        float[] otherData = other.getData();
        for (int i = 0; i < size; i++) {
            resultData[i] = this.data[i] + otherData[i];
        }
        return new SimpleTensor(shape, resultData);
    }

    @Override
    public Tensor sub(Tensor other) {
        float[] resultData = new float[(int) size];
        float[] otherData = other.getData();
        for (int i = 0; i < size; i++) {
            resultData[i] = this.data[i] - otherData[i];
        }
        return new SimpleTensor(shape, resultData);
    }

    @Override
    public Tensor mul(Tensor other) {
        float[] resultData = new float[(int) size];
        float[] otherData = other.getData();
        for (int i = 0; i < size; i++) {
            resultData[i] = this.data[i] * otherData[i];
        }
        return new SimpleTensor(shape, resultData);
    }

    @Override
    public Tensor div(Tensor other) {
        float[] resultData = new float[(int) size];
        float[] otherData = other.getData();
        for (int i = 0; i < size; i++) {
            resultData[i] = this.data[i] / otherData[i];
        }
        return new SimpleTensor(shape, resultData);
    }

    @Override
    public Tensor matmul(Tensor other) {
        if (this.rank != 2 || other.getRank() != 2) {
            throw new UnsupportedOperationException("Matmul only supported for rank 2 tensors");
        }
        if (this.shape[1] != other.getShape()[0]) {
            throw new IllegalArgumentException("Incompatible shapes for matmul");
        }

        int m = (int) this.shape[0];
        int n = (int) this.shape[1];
        int p = (int) other.getShape()[1];

        float[] resultData = new float[m * p];
        float[] otherData = other.getData();

        for (int i = 0; i < m; i++) {
            for (int k = 0; k < n; k++) {
                float a = this.data[i * n + k];
                for (int j = 0; j < p; j++) {
                    resultData[i * p + j] += a * otherData[k * p + j];
                }
            }
        }
        return new SimpleTensor(new long[]{m, p}, resultData);
    }

    @Override
    public Tensor transpose() {
        if (this.rank != 2) {
            throw new UnsupportedOperationException("Transpose only supported for rank 2 tensors");
        }
        int m = (int) this.shape[0];
        int n = (int) this.shape[1];
        float[] resultData = new float[m * n];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                resultData[j * m + i] = this.data[i * n + j];
            }
        }
        return new SimpleTensor(new long[]{n, m}, resultData);
    }

    @Override
    public int getRank() {
        return rank;
    }

    @Override
    public long getSize() {
        return size;
    }
}
