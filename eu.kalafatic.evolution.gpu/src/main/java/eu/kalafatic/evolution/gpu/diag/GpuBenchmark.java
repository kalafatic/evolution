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

        float[] a = createRandomMatrix(matrixSize);
        float[] b = createRandomMatrix(matrixSize);

        // CPU matmul (single-threaded naive, as a baseline).
        long startCpu = System.nanoTime();
        float[] cCpu = multiplyCpu(a, b, matrixSize);
        long endCpu = System.nanoTime();
        res.setCpuDurationMs((endCpu - startCpu) / 1_000_000);

        if (!gpuComputeCapable) {
            res.setGpuDurationMs(-1);
            res.setExecutionMode("CPU_FALLBACK");
            res.setSpeedupRatio(1.0);
            res.setNote("GPU compute hardware / CUDA runtime unavailable; benchmark ran strictly on CPU.");
            return res;
        }

        // Real GPU matmul via the CUDA driver API + NVRTC (genuine GPU execution).
        try {
            eu.kalafatic.evolution.gpu.cuda.CudaDevice dev = eu.kalafatic.evolution.gpu.cuda.CudaDevice.tryCreate();
            if (dev == null) throw new IllegalStateException("CUDA device not initialised");
            String src = "extern \"C\" __global__ void g(const float* A, const float* B, float* C, int n){"
                    + " int i=blockIdx.x*blockDim.x+threadIdx.x; if(i>=n*n)return; int r=i/n,c=i%n;"
                    + " float s=0; for(int k=0;k<n;k++)s+=A[r*n+k]*B[k*n+c]; C[i]=s; }";
            var mod = dev.compile("bench_gemm", src);
            long dA = dev.alloc(a.length * 4L), dB = dev.alloc(b.length * 4L), dC = dev.alloc(cCpu.length * 4L);
            dev.memcpyHtoD(dA, a);
            dev.memcpyHtoD(dB, b);
            int reps = 10;
            long startGpu = System.nanoTime();
            for (int i = 0; i < reps; i++) {
                mod.function("g").launch(matrixSize * matrixSize,
                        java.util.List.of(dA, dB, dC, matrixSize));
            }
            dev.synchronize();
            long endGpu = System.nanoTime();
            long gpuTotalMs = (endGpu - startGpu) / 1_000_000;
            res.setGpuDurationMs(gpuTotalMs);
            res.setExecutionMode("GPU_CUDA_KERNEL");
            res.setSpeedupRatio(((double) res.getCpuDurationMs() * reps) / Math.max(1, gpuTotalMs));
            res.setNote("Genuine CUDA kernel matmul (" + reps + " iterations) vs single-threaded CPU matmul.");
            res.setTechnicalBlocker(null);
        } catch (Throwable t) {
            res.setGpuDurationMs(-1);
            res.setExecutionMode("GPU_KERNEL_FAILED");
            res.setSpeedupRatio(1.0);
            res.setNote("GPU matmul failed: " + t);
            res.setTechnicalBlocker(t.toString());
        }

        return res;
    }

    private float[] createRandomMatrix(int n) {
        float[] m = new float[n * n];
        for (int i = 0; i < m.length; i++) {
            m[i] = (float) Math.random();
        }
        return m;
    }

    private float[] multiplyCpu(float[] a, float[] b, int n) {
        float[] c = new float[n * n];
        for (int i = 0; i < n; i++) {
            int io = i * n;
            for (int k = 0; k < n; k++) {
                float aVal = a[i * n + k];
                if (aVal == 0.0f) continue;
                int ko = k * n;
                for (int j = 0; j < n; j++) {
                    c[io + j] += aVal * b[ko + j];
                }
            }
        }
        return c;
    }
}
