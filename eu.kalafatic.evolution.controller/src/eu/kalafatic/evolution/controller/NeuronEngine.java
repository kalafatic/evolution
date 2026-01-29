package eu.kalafatic.evolution.controller;

import eu.kalafatic.evolution.model.orchestration.NeuronType;
import java.util.Random;
import java.util.stream.IntStream;

public class NeuronEngine {

    private static final int EMBED_DIM = 8;
    private static final int HIDDEN_DIM = 16;

    public String runModel(NeuronType type, String modelName, String prompt) {
        double[] input = embed(prompt);
        if (prompt == null || prompt.isEmpty()) prompt = "empty prompt";

        switch (type) {
            case MLP:
                return runMLP(modelName, input);
            case CNN:
                return runCNN(modelName, prompt);
            case RNN:
                return runRNN(modelName, prompt);
            case LSTM:
                return runLSTM(modelName, prompt);
            case TRANSFORMER:
                return runTransformer(modelName, prompt);
            default:
                return "Unknown model type: " + type;
        }
    }

    private String runMLP(String model, double[] x) {
        Random rand = new Random(model.hashCode());
        double[][] W = generateMatrix(HIDDEN_DIM, EMBED_DIM, rand);
        double[] b = generateVector(HIDDEN_DIM, rand);

        double[] h = relu(add(matMul(W, x), b));

        double[][] W2 = generateMatrix(4, HIDDEN_DIM, rand);
        double[] out = softmax(matMul(W2, h));

        return String.format("[NeuronAI MLP - %s] Processed vector of size %d. Output probabilities: %s",
                model, x.length, formatArray(out));
    }

    private String runCNN(String model, String prompt) {
        double[][] sequence = embedSequence(prompt);
        Random rand = new Random(model.hashCode());
        int kernelSize = 3;
        double[][] kernel = generateMatrix(kernelSize, EMBED_DIM, rand);

        // 1D Convolution simulation
        double[] pooled = new double[EMBED_DIM];
        for (int i = 0; i <= sequence.length - kernelSize; i++) {
            for (int d = 0; d < EMBED_DIM; d++) {
                double val = 0;
                for (int k = 0; k < kernelSize; k++) {
                    val += sequence[i + k][d] * kernel[k][d];
                }
                pooled[d] = Math.max(pooled[d], relu(val)); // Max pooling
            }
        }

        return String.format("[NeuronAI CNN - %s] Convolved sequence of token length %d. Feature map max: %.4f",
                model, sequence.length, max(pooled));
    }

    private String runRNN(String model, String prompt) {
        double[][] sequence = embedSequence(prompt);
        Random rand = new Random(model.hashCode());
        double[][] Wxh = generateMatrix(HIDDEN_DIM, EMBED_DIM, rand);
        double[][] Whh = generateMatrix(HIDDEN_DIM, HIDDEN_DIM, rand);
        double[] h = new double[HIDDEN_DIM];

        for (double[] x_t : sequence) {
            h = tanh(add(matMul(Wxh, x_t), matMul(Whh, h)));
        }

        return String.format("[NeuronAI RNN - %s] Recurrence completed. Final hidden state norm: %.4f",
                model, norm(h));
    }

    private String runLSTM(String model, String prompt) {
        double[][] sequence = embedSequence(prompt);
        Random rand = new Random(model.hashCode());
        // Gates: input, forget, output, cell
        double[][] Wi = generateMatrix(HIDDEN_DIM, EMBED_DIM + HIDDEN_DIM, rand);
        double[][] Wf = generateMatrix(HIDDEN_DIM, EMBED_DIM + HIDDEN_DIM, rand);
        double[][] Wo = generateMatrix(HIDDEN_DIM, EMBED_DIM + HIDDEN_DIM, rand);
        double[][] Wc = generateMatrix(HIDDEN_DIM, EMBED_DIM + HIDDEN_DIM, rand);

        double[] h = new double[HIDDEN_DIM];
        double[] c = new double[HIDDEN_DIM];

        for (double[] x_t : sequence) {
            double[] concat = concat(x_t, h);
            double[] i_gate = sigmoid(matMul(Wi, concat));
            double[] f_gate = sigmoid(matMul(Wf, concat));
            double[] o_gate = sigmoid(matMul(Wo, concat));
            double[] g_gate = tanh(matMul(Wc, concat));

            c = add(multiply(f_gate, c), multiply(i_gate, g_gate));
            h = multiply(o_gate, tanh(c));
        }

        return String.format("[NeuronAI LSTM - %s] Gated memory updated. Final cell state mean: %.4f",
                model, mean(c));
    }

    private String runTransformer(String model, String prompt) {
        double[][] X = embedSequence(prompt);
        Random rand = new Random(model.hashCode());
        int d_k = EMBED_DIM;
        double[][] Wq = generateMatrix(d_k, EMBED_DIM, rand);
        double[][] Wk = generateMatrix(d_k, EMBED_DIM, rand);
        double[][] Wv = generateMatrix(d_k, EMBED_DIM, rand);

        // Self-Attention simplified
        double[][] Q = matMulBatch(Wq, X);
        double[][] K = matMulBatch(Wk, X);
        double[][] V = matMulBatch(Wv, X);

        double[][] scores = new double[X.length][X.length];
        for (int i = 0; i < X.length; i++) {
            for (int j = 0; j < X.length; j++) {
                scores[i][j] = dot(Q[i], K[j]) / Math.sqrt(d_k);
            }
            scores[i] = softmax(scores[i]);
        }

        double[][] out = new double[X.length][d_k];
        for (int i = 0; i < X.length; i++) {
            for (int j = 0; j < X.length; j++) {
                for (int d = 0; d < d_k; d++) {
                    out[i][d] += scores[i][j] * V[j][d];
                }
            }
        }

        return String.format("[NeuronAI Transformer - %s] Self-attention applied over %d tokens. Output energy: %.4f",
                model, X.length, energy(out));
    }

    // --- Math Utilities ---

    private double[] embed(String text) {
        double[] v = new double[EMBED_DIM];
        for (int i = 0; i < text.length(); i++) {
            v[i % EMBED_DIM] += (double) text.charAt(i) / 255.0;
        }
        return tanh(v);
    }

    private double[][] embedSequence(String text) {
        String[] words = text.split("\\s+");
        double[][] seq = new double[Math.max(1, words.length)][EMBED_DIM];
        for (int i = 0; i < words.length; i++) {
            seq[i] = embed(words[i]);
        }
        return seq;
    }

    private double[] matMul(double[][] W, double[] x) {
        double[] out = new double[W.length];
        for (int i = 0; i < W.length; i++) {
            out[i] = dot(W[i], x);
        }
        return out;
    }

    private double[][] matMulBatch(double[][] W, double[][] X) {
        double[][] out = new double[X.length][W.length];
        for (int i = 0; i < X.length; i++) {
            out[i] = matMul(W, X[i]);
        }
        return out;
    }

    private double dot(double[] a, double[] b) {
        double sum = 0;
        for (int i = 0; i < a.length; i++) sum += a[i] * b[i];
        return sum;
    }

    private double[] add(double[] a, double[] b) {
        double[] out = new double[a.length];
        for (int i = 0; i < a.length; i++) out[i] = a[i] + b[i];
        return out;
    }

    private double[] multiply(double[] a, double[] b) {
        double[] out = new double[a.length];
        for (int i = 0; i < a.length; i++) out[i] = a[i] * b[i];
        return out;
    }

    private double[] relu(double[] x) {
        return IntStream.range(0, x.length).mapToDouble(i -> Math.max(0, x[i])).toArray();
    }

    private double relu(double x) { return Math.max(0, x); }

    private double[] tanh(double[] x) {
        return IntStream.range(0, x.length).mapToDouble(i -> Math.tanh(x[i])).toArray();
    }

    private double[] sigmoid(double[] x) {
        return IntStream.range(0, x.length).mapToDouble(i -> 1.0 / (1.0 + Math.exp(-x[i]))).toArray();
    }

    private double[] softmax(double[] x) {
        double max = Double.NEGATIVE_INFINITY;
        for (double v : x) if (v > max) max = v;
        double sum = 0;
        double[] out = new double[x.length];
        for (int i = 0; i < x.length; i++) {
            out[i] = Math.exp(x[i] - max);
            sum += out[i];
        }
        for (int i = 0; i < x.length; i++) out[i] /= sum;
        return out;
    }

    private double[][] generateMatrix(int rows, int cols, Random rand) {
        double[][] m = new double[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) m[i][j] = rand.nextGaussian() * 0.1;
        }
        return m;
    }

    private double[] generateVector(int size, Random rand) {
        double[] v = new double[size];
        for (int i = 0; i < size; i++) v[i] = rand.nextGaussian() * 0.1;
        return v;
    }

    private double[] concat(double[] a, double[] b) {
        double[] res = new double[a.length + b.length];
        System.arraycopy(a, 0, res, 0, a.length);
        System.arraycopy(b, 0, res, a.length, b.length);
        return res;
    }

    private String formatArray(double[] arr) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < arr.length; i++) {
            sb.append(String.format("%.4f", arr[i]));
            if (i < arr.length - 1) sb.append(", ");
        }
        return sb.append("]").toString();
    }

    private double max(double[] arr) {
        double m = Double.NEGATIVE_INFINITY;
        for (double v : arr) if (v > m) m = v;
        return m;
    }

    private double norm(double[] v) {
        double s = 0;
        for (double x : v) s += x * x;
        return Math.sqrt(s);
    }

    private double mean(double[] v) {
        double s = 0;
        for (double x : v) s += x;
        return s / Math.max(1, v.length);
    }

    private double energy(double[][] m) {
        double s = 0;
        for (double[] row : m) for (double v : row) s += v * v;
        return s;
    }
}
