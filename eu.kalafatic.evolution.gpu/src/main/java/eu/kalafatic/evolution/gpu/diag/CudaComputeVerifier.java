package eu.kalafatic.evolution.gpu.diag;

import java.io.File;
import java.lang.foreign.Arena;
import java.lang.foreign.SymbolLookup;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Isolated verifier for CUDA compute interface capability in Java 21.
 * Evaluates CUDA Driver API dynamic libraries (libcuda.so / nvcuda.dll) and CUDA Runtime API libraries (libcudart.so / cudart.dll)
 * using Java 21 Foreign Function & Memory API (FFM) dynamic symbol lookup.
 */
public class CudaComputeVerifier {

    public static class CudaComputeInfo {
        private boolean driverLibraryFound;
        private boolean runtimeLibraryFound;
        private boolean computeCapable;
        private String driverLibraryPath = "Not Found";
        private String runtimeLibraryPath = "Not Found";
        private String computeDeviceType = "CPU_ONLY";
        private List<String> errorMessages = new ArrayList<>();

        public boolean isDriverLibraryFound() { return driverLibraryFound; }
        public void setDriverLibraryFound(boolean driverLibraryFound) { this.driverLibraryFound = driverLibraryFound; }

        public boolean isRuntimeLibraryFound() { return runtimeLibraryFound; }
        public void setRuntimeLibraryFound(boolean runtimeLibraryFound) { this.runtimeLibraryFound = runtimeLibraryFound; }

        public boolean isComputeCapable() { return computeCapable; }
        public void setComputeCapable(boolean computeCapable) { this.computeCapable = computeCapable; }

        public String getDriverLibraryPath() { return driverLibraryPath; }
        public void setDriverLibraryPath(String driverLibraryPath) { this.driverLibraryPath = driverLibraryPath; }

        public String getRuntimeLibraryPath() { return runtimeLibraryPath; }
        public void setRuntimeLibraryPath(String runtimeLibraryPath) { this.runtimeLibraryPath = runtimeLibraryPath; }

        public String getComputeDeviceType() { return computeDeviceType; }
        public void setComputeDeviceType(String computeDeviceType) { this.computeDeviceType = computeDeviceType; }

        public List<String> getErrorMessages() { return errorMessages; }
        public void addError(String error) { this.errorMessages.add(error); }
    }

    public CudaComputeInfo verifyCudaCompute() {
        CudaComputeInfo info = new CudaComputeInfo();
        boolean isWin = System.getProperty("os.name").toLowerCase().contains("win");

        String driverLibName = isWin ? "nvcuda.dll" : "libcuda.so";
        String runtimeLibName = isWin ? "cudart64_12.dll" : "libcudart.so";

        String driverPath = findSystemLibrary(driverLibName, isWin);
        if (driverPath != null) {
            info.setDriverLibraryFound(true);
            info.setDriverLibraryPath(driverPath);
        } else {
            info.addError("CUDA Driver API library (" + driverLibName + ") was not found on system.");
        }

        String runtimePath = findSystemLibrary(runtimeLibName, isWin);
        if (runtimePath == null && isWin) {
            runtimePath = findWindowsCudartFallback();
        }
        if (runtimePath != null) {
            info.setRuntimeLibraryFound(true);
            info.setRuntimeLibraryPath(runtimePath);
        } else {
            info.addError("CUDA Runtime API library (" + runtimeLibName + ") was not found on system.");
        }

        if (driverPath != null) {
            verifyLibraryLinkage(driverPath, info);
        } else {
            info.setComputeCapable(false);
            info.setComputeDeviceType("CPU_ONLY");
            info.addError("GPU compute capability cannot be verified without CUDA Driver API library.");
        }

        return info;
    }

    private void verifyLibraryLinkage(String driverPath, CudaComputeInfo info) {
        try {
            Path path = Path.of(driverPath);
            if (Files.exists(path)) {
                try (Arena arena = Arena.ofConfined()) {
                    SymbolLookup lookup = SymbolLookup.libraryLookup(path, arena);
                    boolean cuInitFound = lookup.find("cuInit").isPresent();
                    boolean cuDriverVerFound = lookup.find("cuDriverGetVersion").isPresent();

                    if (cuInitFound || cuDriverVerFound) {
                        info.setComputeCapable(true);
                        info.setComputeDeviceType("CUDA_GPU_VERIFIED");
                    } else {
                        info.setComputeCapable(false);
                        info.setComputeDeviceType("CPU_FALLBACK");
                        info.addError("CUDA Driver API library loaded, but expected symbols (cuInit/cuDriverGetVersion) were absent.");
                    }
                }
            }
        } catch (Throwable t) {
            info.setComputeCapable(false);
            info.setComputeDeviceType("CPU_FALLBACK");
            info.addError("Java 21 FFM SymbolLookup failed to dynamically load CUDA library: " + t.getMessage());
        }
    }

    private String findSystemLibrary(String libName, boolean isWin) {
        String[] defaultLocations = isWin ? new String[]{
                "C:\\Windows\\System32\\" + libName,
                "C:\\Program Files\\NVIDIA GPU Computing Toolkit\\CUDA\\v12.0\\bin\\" + libName,
                "C:\\Program Files\\NVIDIA GPU Computing Toolkit\\CUDA\\v11.8\\bin\\" + libName
        } : new String[]{
                "/usr/lib/x86_64-linux-gnu/" + libName,
                "/usr/lib64/" + libName,
                "/usr/local/cuda/lib64/" + libName,
                "/usr/local/cuda/lib64/" + libName + ".1",
                "/usr/lib/x86_64-linux-gnu/" + libName + ".1"
        };

        for (String loc : defaultLocations) {
            File f = new File(loc);
            if (f.exists()) {
                return f.getAbsolutePath();
            }
        }

        String envVar = isWin ? "PATH" : "LD_LIBRARY_PATH";
        String paths = System.getenv(envVar);
        if (paths != null) {
            for (String dir : paths.split(File.pathSeparator)) {
                File f = new File(dir, libName);
                if (f.exists()) {
                    return f.getAbsolutePath();
                }
            }
        }

        return null;
    }

    private String findWindowsCudartFallback() {
        String cudaPath = System.getenv("CUDA_PATH");
        if (cudaPath != null) {
            File binDir = new File(cudaPath, "bin");
            if (binDir.exists() && binDir.isDirectory()) {
                File[] files = binDir.listFiles((dir, name) -> name.startsWith("cudart64_") && name.endsWith(".dll"));
                if (files != null && files.length > 0) {
                    return files[0].getAbsolutePath();
                }
            }
        }
        return null;
    }
}
