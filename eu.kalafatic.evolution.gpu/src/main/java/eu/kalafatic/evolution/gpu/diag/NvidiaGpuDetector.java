package eu.kalafatic.evolution.gpu.diag;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Isolated NVIDIA GPU device detector using system process execution (nvidia-smi).
 * Includes strict timeouts, exit code checking, error capturing, and fallback driver path checks.
 */
public class NvidiaGpuDetector {

    public static class GpuDeviceInfo {
        private boolean available;
        private String deviceName = "Unknown";
        private long totalVramMb = 0;
        private long usedVramMb = 0;
        private long freeVramMb = 0;
        private String driverVersion = "Unknown";
        private String cudaDriverVersion = "Unknown";
        private int gpuUtilizationPercent = 0;
        private List<String> configErrors = new ArrayList<>();
        private String rawDiagnostics = "";

        public boolean isAvailable() { return available; }
        public void setAvailable(boolean available) { this.available = available; }

        public String getDeviceName() { return deviceName; }
        public void setDeviceName(String deviceName) { this.deviceName = deviceName; }

        public long getTotalVramMb() { return totalVramMb; }
        public void setTotalVramMb(long totalVramMb) { this.totalVramMb = totalVramMb; }

        public long getUsedVramMb() { return usedVramMb; }
        public void setUsedVramMb(long usedVramMb) { this.usedVramMb = usedVramMb; }

        public long getFreeVramMb() { return freeVramMb; }
        public void setFreeVramMb(long freeVramMb) { this.freeVramMb = freeVramMb; }

        public String getDriverVersion() { return driverVersion; }
        public void setDriverVersion(String driverVersion) { this.driverVersion = driverVersion; }

        public String getCudaDriverVersion() { return cudaDriverVersion; }
        public void setCudaDriverVersion(String cudaDriverVersion) { this.cudaDriverVersion = cudaDriverVersion; }

        public int getGpuUtilizationPercent() { return gpuUtilizationPercent; }
        public void setGpuUtilizationPercent(int gpuUtilizationPercent) { this.gpuUtilizationPercent = gpuUtilizationPercent; }

        public List<String> getConfigErrors() { return configErrors; }
        public void addError(String error) { this.configErrors.add(error); }

        public String getRawDiagnostics() { return rawDiagnostics; }
        public void setRawDiagnostics(String rawDiagnostics) { this.rawDiagnostics = rawDiagnostics; }
    }

    public GpuDeviceInfo detectGpu() {
        GpuDeviceInfo info = new GpuDeviceInfo();

        String nvidiaSmiPath = findNvidiaSmiExecutable();
        if (nvidiaSmiPath == null) {
            info.setAvailable(false);
            info.addError("nvidia-smi executable not found in standard system PATH or binaries.");
            checkLinuxDriverPath(info);
            return info;
        }

        try {
            ProcessBuilder pb = new ProcessBuilder(
                    nvidiaSmiPath,
                    "--query-gpu=name,memory.total,memory.used,memory.free,driver_version,utilization.gpu",
                    "--format=csv,noheader,nounits"
            );
            pb.redirectErrorStream(false);
            Process process = pb.start();

            StringBuilder stdOut = new StringBuilder();
            StringBuilder stdErr = new StringBuilder();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    stdOut.append(line).append("\n");
                }
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    stdErr.append(line).append("\n");
                }
            }

            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                info.setAvailable(false);
                info.addError("nvidia-smi process timed out after 5 seconds.");
                return info;
            }

            int exitCode = process.exitValue();
            info.setRawDiagnostics("Exit code: " + exitCode + "\nStdOut:\n" + stdOut + "\nStdErr:\n" + stdErr);

            if (exitCode != 0) {
                info.setAvailable(false);
                info.addError("nvidia-smi returned non-zero exit code: " + exitCode + " (" + stdErr.toString().trim() + ")");
                return info;
            }

            String output = stdOut.toString().trim();
            if (output.isEmpty()) {
                info.setAvailable(false);
                info.addError("nvidia-smi returned empty query output.");
                return info;
            }

            String[] tokens = output.split(",");
            if (tokens.length >= 6) {
                info.setDeviceName(tokens[0].trim());
                info.setTotalVramMb(parseLong(tokens[1].trim()));
                info.setUsedVramMb(parseLong(tokens[2].trim()));
                info.setFreeVramMb(parseLong(tokens[3].trim()));
                info.setDriverVersion(tokens[4].trim());
                info.setGpuUtilizationPercent(parseInt(tokens[5].trim()));
                info.setAvailable(true);

                detectCudaVersionFromNvidiaSmi(nvidiaSmiPath, info);
            } else {
                info.setAvailable(false);
                info.addError("nvidia-smi returned unexpected token count: " + tokens.length);
            }

        } catch (Exception e) {
            info.setAvailable(false);
            info.addError("Exception during GPU detection: " + e.getMessage());
        }

        return info;
    }

    private void detectCudaVersionFromNvidiaSmi(String nvidiaSmiPath, GpuDeviceInfo info) {
        try {
            ProcessBuilder pb = new ProcessBuilder(nvidiaSmiPath);
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("CUDA Version:")) {
                        int idx = line.indexOf("CUDA Version:");
                        String cudaVer = line.substring(idx + "CUDA Version:".length()).trim();
                        int spaceIdx = cudaVer.indexOf(' ');
                        if (spaceIdx > 0) {
                            cudaVer = cudaVer.substring(0, spaceIdx);
                        }
                        info.setCudaDriverVersion(cudaVer);
                        break;
                    }
                }
            }
            process.waitFor(3, TimeUnit.SECONDS);
        } catch (Exception ignored) {
        }
    }

    private String findNvidiaSmiExecutable() {
        String[] candidatePaths = {
                "/usr/bin/nvidia-smi",
                "/usr/local/bin/nvidia-smi",
                "C:\\Windows\\System32\\nvidia-smi.exe",
                "C:\\Program Files\\NVIDIA Corporation\\NVSMI\\nvidia-smi.exe"
        };

        for (String p : candidatePaths) {
            File f = new File(p);
            if (f.exists() && f.canExecute()) {
                return f.getAbsolutePath();
            }
        }

        String pathEnv = System.getenv("PATH");
        if (pathEnv != null) {
            String delimiter = File.pathSeparator;
            for (String dir : pathEnv.split(delimiter)) {
                File file = new File(dir, System.getProperty("os.name").toLowerCase().contains("win") ? "nvidia-smi.exe" : "nvidia-smi");
                if (file.exists() && file.canExecute()) {
                    return file.getAbsolutePath();
                }
            }
        }

        return null;
    }

    private void checkLinuxDriverPath(GpuDeviceInfo info) {
        try {
            Path nvidiaProc = Path.of("/proc/driver/nvidia");
            if (Files.exists(nvidiaProc)) {
                info.addError("NVIDIA Linux driver entry found in /proc/driver/nvidia, but nvidia-smi tool was not accessible.");
            }
        } catch (Exception ignored) {
        }
    }

    private long parseLong(String val) {
        try {
            return Long.parseLong(val);
        } catch (Exception e) {
            return 0;
        }
    }

    private int parseInt(String val) {
        try {
            return Integer.parseInt(val);
        } catch (Exception e) {
            return 0;
        }
    }
}
