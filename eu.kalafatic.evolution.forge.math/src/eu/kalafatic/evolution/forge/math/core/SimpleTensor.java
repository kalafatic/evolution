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
    public Tensor exp() {
        float[] resultData = new float[(int) size];
        for (int i = 0; i < size; i++) {
            resultData[i] = (float) Math.exp(data[i]);
        }
        return new SimpleTensor(shape, resultData);
    }

    @Override
    public Tensor log() {
        float[] resultData = new float[(int) size];
        for (int i = 0; i < size; i++) {
            resultData[i] = (float) Math.log(data[i]);
        }
        return new SimpleTensor(shape, resultData);
    }

    @Override
    public Tensor sqrt() {
        float[] resultData = new float[(int) size];
        for (int i = 0; i < size; i++) {
            resultData[i] = (float) Math.sqrt(data[i]);
        }
        return new SimpleTensor(shape, resultData);
    }

    @Override
    public Tensor pow(float exponent) {
        float[] resultData = new float[(int) size];
        for (int i = 0; i < size; i++) {
            resultData[i] = (float) Math.pow(data[i], exponent);
        }
        return new SimpleTensor(shape, resultData);
    }

    @Override
    public float sum() {
        float total = 0;
        for (float val : data) {
            total += val;
        }
        return total;
    }

    @Override
    public Tensor softmax() {
        // Simple softmax implementation for rank 1 or 2
        float[] resultData = new float[(int) size];
        if (rank == 1) {
            float max = Float.NEGATIVE_INFINITY;
            for (float val : data) if (val > max) max = val;
            float sum = 0;
            for (int i = 0; i < size; i++) {
                resultData[i] = (float) Math.exp(data[i] - max);
                sum += resultData[i];
            }
            for (int i = 0; i < size; i++) resultData[i] /= sum;
        } else if (rank == 2) {
            int rows = (int) shape[0];
            int cols = (int) shape[1];
            for (int r = 0; r < rows; r++) {
                float max = Float.NEGATIVE_INFINITY;
                for (int c = 0; c < cols; c++) {
                    float val = data[r * cols + c];
                    if (val > max) max = val;
                }
                float sum = 0;
                for (int c = 0; c < cols; c++) {
                    resultData[r * cols + c] = (float) Math.exp(data[r * cols + c] - max);
                    sum += resultData[r * cols + c];
                }
                for (int c = 0; c < cols; c++) resultData[r * cols + c] /= sum;
            }
        } else {
            throw new UnsupportedOperationException("Softmax only supported for rank 1 or 2");
        }
        return new SimpleTensor(shape, resultData);
    }

    @Override
    public Tensor layerNorm() {
        float[] resultData = new float[(int) size];
        if (rank == 2) {
            int rows = (int) shape[0];
            int cols = (int) shape[1];
            float eps = 1e-5f;
            for (int r = 0; r < rows; r++) {
                float mean = 0;
                for (int c = 0; c < cols; c++) mean += data[r * cols + c];
                mean /= cols;
                float var = 0;
                for (int c = 0; c < cols; c++) {
                    float diff = data[r * cols + c] - mean;
                    var += diff * diff;
                }
                var /= cols;
                float std = (float) Math.sqrt(var + eps);
                for (int c = 0; c < cols; c++) {
                    resultData[r * cols + c] = (data[r * cols + c] - mean) / std;
                }
            }
        } else {
            throw new UnsupportedOperationException("LayerNorm only supported for rank 2");
        }
        return new SimpleTensor(shape, resultData);
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
