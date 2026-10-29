package eu.kalafatic.evolution.gpu;

import eu.kalafatic.evolution.gpu.diag.CudaComputeVerifier;
import eu.kalafatic.evolution.gpu.diag.GpuBenchmark;
import eu.kalafatic.evolution.gpu.diag.GpuDiagnosticReport;
import eu.kalafatic.evolution.gpu.diag.NvidiaGpuDetector;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Main standalone CLI entry point for NVIDIA GPU diagnostics.
 */
public class GpuDiagnosticMain {

    public static void main(String[] args) {
        System.out.println("[EVO-GPU-DIAG] Initializing isolated NVIDIA GPU Diagnostic Process...");

        boolean outputJson = false;
        String outputDirPath = "gpu-output";

        for (int i = 0; i < args.length; i++) {
            if ("--json".equalsIgnoreCase(args[i])) {
                outputJson = true;
            } else if ("--output-dir".equalsIgnoreCase(args[i]) && i + 1 < args.length) {
                outputDirPath = args[i + 1];
            }
        }

        try {
            NvidiaGpuDetector detector = new NvidiaGpuDetector();
            NvidiaGpuDetector.GpuDeviceInfo deviceInfo = detector.detectGpu();

            CudaComputeVerifier verifier = new CudaComputeVerifier();
            CudaComputeVerifier.CudaComputeInfo computeInfo = verifier.verifyCudaCompute();

            GpuBenchmark benchmark = new GpuBenchmark();
            GpuBenchmark.BenchmarkResult benchResult = benchmark.runBenchmark(computeInfo.isComputeCapable(), 256);

            GpuDiagnosticReport report = new GpuDiagnosticReport();
            report.setDeviceInfo(deviceInfo);
            report.setComputeInfo(computeInfo);
            report.setBenchmarkResult(benchResult);

            if (deviceInfo.isAvailable()) {
                report.addSummary("NVIDIA GPU detected: " + deviceInfo.getDeviceName() + " (" + deviceInfo.getTotalVramMb() + " MB VRAM)");
            } else {
                report.addSummary("NVIDIA GPU not detected via nvidia-smi.");
            }

            if (computeInfo.isComputeCapable()) {
                report.addSummary("CUDA compute verification SUCCESSFUL: " + computeInfo.getComputeDeviceType());
            } else {
                report.addSummary("CUDA compute verification FAILED or unavailable. Isolated process will use CPU FALLBACK mode.");
            }

            if (deviceInfo.isAvailable() && !computeInfo.isComputeCapable()) {
                report.addSummary("CRITICAL DIAGNOSTIC NOTE: GPU device is physically present, but CUDA execution runtime/libraries are missing or incompatible. GPU detection DOES NOT imply GPU execution capability!");
            }

            String textReport = report.toTextFormatted();
            System.out.println("\n" + textReport);

            File outDir = new File(outputDirPath);
            if (!outDir.exists()) {
                outDir.mkdirs();
            }

            File reportTextFile = new File(outDir, "gpu_diagnostic_report.txt");
            try (FileWriter writer = new FileWriter(reportTextFile)) {
                writer.write(textReport);
            }
            System.out.println("[EVO-GPU-DIAG] Saved text report to: " + reportTextFile.getAbsolutePath());

            if (outputJson) {
                File reportJsonFile = new File(outDir, "gpu_diagnostic_report.json");
                report.saveJson(reportJsonFile);
                System.out.println("[EVO-GPU-DIAG] Saved JSON report to: " + reportJsonFile.getAbsolutePath());
            }

        } catch (Exception e) {
            System.err.println("[EVO-GPU-DIAG] Diagnostic execution error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
