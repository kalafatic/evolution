package eu.kalafatic.evolution.gpu.diag;

/**
 * Isolated GPU vs CPU matrix multiplication benchmark for performance verification.
 * Explicitly distinguishes host CPU execution from native GPU CUDA acceleration.
 */
public class GpuBenchmark {

    public static class BenchmarkResult {
        private String executionMode;
        private int matrixSize;
        private long cpuDurationMs;
        private long gpuDurationMs = -1;
        private double speedupRatio = 1.0;
        private String note;
        private String technicalBlocker;

        public String getExecutionMode() { return executionMode; }
        public void setExecutionMode(String executionMode) { this.executionMode = executionMode; }

        public int getMatrixSize() { return matrixSize; }
        public void setMatrixSize(int matrixSize) { this.matrixSize = matrixSize; }

        public long getCpuDurationMs() { return cpuDurationMs; }
        public void setCpuDurationMs(long cpuDurationMs) { this.cpuDurationMs = cpuDurationMs; }

        public long getGpuDurationMs() { return gpuDurationMs; }
        public void setGpuDurationMs(long gpuDurationMs) { this.gpuDurationMs = gpuDurationMs; }

        public double getSpeedupRatio() { return speedupRatio; }
        public void setSpeedupRatio(double speedupRatio) { this.speedupRatio = speedupRatio; }

        public String getNote() { return note; }
        public void setNote(String note) { this.note = note; }

        public String getTechnicalBlocker() { return technicalBlocker; }
        public void setTechnicalBlocker(String technicalBlocker) { this.technicalBlocker = technicalBlocker; }
    }

    public BenchmarkResult runBenchmark(boolean gpuComputeCapable, int matrixSize) {
        BenchmarkResult res = new BenchmarkResult();
        res.setMatrixSize(matrixSize);

        float[][] a = createRandomMatrix(matrixSize);
        float[][] b = createRandomMatrix(matrixSize);

        long startCpu = System.nanoTime();
        float[][] cCpu = multiplyCpu(a, b, matrixSize);
        long endCpu = System.nanoTime();
        res.setCpuDurationMs((endCpu - startCpu) / 1_000_000);

        if (gpuComputeCapable) {
            res.setGpuDurationMs(-1);
            res.setExecutionMode("GPU_CUDA_KERNEL_REQUIRED");
            res.setSpeedupRatio(1.0);
            res.setNote("CUDA driver libraries detected, but actual GPU tensor acceleration requires compiled CUDA PTX/cuBLAS native kernel binaries.");
            res.setTechnicalBlocker("Java 21 bytecode cannot execute on NVIDIA GPU execution units directly. GPU kernel dispatch requires compiling C++/CUDA (.cu -> .ptx/.so) kernels and binding them via FFM/JNI.");
        } else {
            res.setGpuDurationMs(-1);
            res.setExecutionMode("CPU_FALLBACK");
            res.setSpeedupRatio(1.0);
            res.setNote("GPU compute hardware / CUDA runtime unavailable; benchmark ran strictly on CPU.");
        }

        return res;
    }

    private float[][] createRandomMatrix(int n) {
        float[][] m = new float[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                m[i][j] = (float) Math.random();
            }
        }
        return m;
    }

    private float[][] multiplyCpu(float[][] a, float[][] b, int n) {
        float[][] c = new float[n][n];
        for (int i = 0; i < n; i++) {
            for (int k = 0; k < n; k++) {
                float aVal = a[i][k];
                for (int j = 0; j < n; j++) {
                    c[i][j] += aVal * b[k][j];
                }
            }
        }
        return c;
    }
}
