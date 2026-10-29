package eu.kalafatic.evolution.gpu.diag;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Structured GPU diagnostic report combining device detection, CUDA compute verification, and benchmarking.
 */
public class GpuDiagnosticReport {

    private String timestamp = Instant.now().toString();
    private NvidiaGpuDetector.GpuDeviceInfo deviceInfo;
    private CudaComputeVerifier.CudaComputeInfo computeInfo;
    private GpuBenchmark.BenchmarkResult benchmarkResult;
    private List<String> diagnosticSummary = new ArrayList<>();

    public String getTimestamp() { return timestamp; }

    public NvidiaGpuDetector.GpuDeviceInfo getDeviceInfo() { return deviceInfo; }
    public void setDeviceInfo(NvidiaGpuDetector.GpuDeviceInfo deviceInfo) { this.deviceInfo = deviceInfo; }

    public CudaComputeVerifier.CudaComputeInfo getComputeInfo() { return computeInfo; }
    public void setComputeInfo(CudaComputeVerifier.CudaComputeInfo computeInfo) { this.computeInfo = computeInfo; }

    public GpuBenchmark.BenchmarkResult getBenchmarkResult() { return benchmarkResult; }
    public void setBenchmarkResult(GpuBenchmark.BenchmarkResult benchmarkResult) { this.benchmarkResult = benchmarkResult; }

    public List<String> getDiagnosticSummary() { return diagnosticSummary; }
    public void addSummary(String line) { this.diagnosticSummary.add(line); }

    public String toJsonString() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        return mapper.writeValueAsString(this);
    }

    public void saveJson(File file) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.writeValue(file, this);
    }

    public String toTextFormatted() {
        StringBuilder sb = new StringBuilder();
        sb.append("=========================================================\n");
        sb.append("           EVO INDEPENDENT GPU DIAGNOSTIC REPORT          \n");
        sb.append("=========================================================\n");
        sb.append("Timestamp: ").append(timestamp).append("\n\n");

        sb.append("--- [A] DEVICE DETECTION (NVIDIA GPU) ---\n");
        if (deviceInfo != null) {
            sb.append("NVIDIA GPU Available : ").append(deviceInfo.isAvailable() ? "YES" : "NO").append("\n");
            sb.append("Device Name          : ").append(deviceInfo.getDeviceName()).append("\n");
            sb.append("Driver Version       : ").append(deviceInfo.getDriverVersion()).append("\n");
            sb.append("CUDA Driver Version  : ").append(deviceInfo.getCudaDriverVersion()).append("\n");
            sb.append("Total VRAM           : ").append(deviceInfo.getTotalVramMb()).append(" MB\n");
            sb.append("Used VRAM            : ").append(deviceInfo.getUsedVramMb()).append(" MB\n");
            sb.append("Free VRAM            : ").append(deviceInfo.getFreeVramMb()).append(" MB\n");
            sb.append("GPU Utilization      : ").append(deviceInfo.getGpuUtilizationPercent()).append(" %\n");
            if (!deviceInfo.getConfigErrors().isEmpty()) {
                sb.append("Config Errors        : ").append(deviceInfo.getConfigErrors()).append("\n");
            }
        } else {
            sb.append("Device info not collected.\n");
        }

        sb.append("\n--- [B] COMPUTE VERIFICATION (CUDA INTERFACE) ---\n");
        if (computeInfo != null) {
            sb.append("CUDA Driver Library  : ").append(computeInfo.isDriverLibraryFound() ? "FOUND (" + computeInfo.getDriverLibraryPath() + ")" : "NOT FOUND").append("\n");
            sb.append("CUDA Runtime Library : ").append(computeInfo.isRuntimeLibraryFound() ? "FOUND (" + computeInfo.getRuntimeLibraryPath() + ")" : "NOT FOUND").append("\n");
            sb.append("Compute Verification : ").append(computeInfo.isComputeCapable() ? "VERIFIED (CUDA Capable)" : "FAILED (CPU Fallback)").append("\n");
            sb.append("Compute Device Type  : ").append(computeInfo.getComputeDeviceType()).append("\n");
            if (!computeInfo.getErrorMessages().isEmpty()) {
                sb.append("Verification Errors  : ").append(computeInfo.getErrorMessages()).append("\n");
            }
        } else {
            sb.append("Compute info not collected.\n");
        }

        sb.append("\n--- [C] PERFORMANCE BENCHMARK ---\n");
        if (benchmarkResult != null) {
            sb.append("Execution Mode       : ").append(benchmarkResult.getExecutionMode()).append("\n");
            sb.append("Matrix Size          : ").append(benchmarkResult.getMatrixSize()).append(" x ").append(benchmarkResult.getMatrixSize()).append("\n");
            sb.append("CPU Compute Time     : ").append(benchmarkResult.getCpuDurationMs()).append(" ms\n");
            sb.append("GPU Compute Time     : ").append(benchmarkResult.getGpuDurationMs()).append(" ms\n");
            sb.append("Note                 : ").append(benchmarkResult.getNote()).append("\n");
        } else {
            sb.append("Benchmark not executed.\n");
        }

        sb.append("\n--- [SUMMARY & DIAGNOSTICS] ---\n");
        for (String s : diagnosticSummary) {
            sb.append("• ").append(s).append("\n");
        }
        sb.append("=========================================================\n");

        return sb.toString();
    }
}
