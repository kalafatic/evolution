package eu.kalafatic.evolution.gpu.cuda;

import java.lang.foreign.AddressLayout;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Genuine NVIDIA CUDA device backend using the Java 21 Foreign Function &amp; Memory
 * (FFM) API against the CUDA Driver API ({@code libcuda.so}) and NVRTC
 * ({@code libnvrtc.so}, the runtime CUDA-C&rarr;PTX compiler).
 *
 * <p>This is a real compute backend, not device detection: it initializes a CUDA
 * context, allocates device memory, compiles CUDA-C kernels to PTX at runtime via
 * NVRTC, loads the PTX with {@code cuModuleLoadData}, and launches kernels with
 * {@code cuLaunchKernel}. No {@code nvcc}, JCUDA or external toolchain is required.</p>
 *
 * <p>All state is confined to a single {@link Arena} so resources are released
 * deterministically when the device is closed. The class is a process-wide singleton
 * because a CUDA context is a heavyweight, per-process resource.</p>
 */
public final class CudaDevice implements AutoCloseable {

    private static final ValueLayout.OfInt I32 = ValueLayout.JAVA_INT;
    private static final ValueLayout.OfLong I64 = ValueLayout.JAVA_LONG;
    private static final AddressLayout ADDR = ValueLayout.ADDRESS;

    private final Linker linker = Linker.nativeLinker();
    private final Arena arena = Arena.ofShared();

    // Driver API method handles.
    private final MethodHandle cuInit, cuDeviceGet, cuDeviceGetName, cuDeviceGetAttribute,
            cuCtxCreate, cuCtxDestroy, cuCtxSetCurrent, cuMemAlloc, cuMemFree, cuMemcpyHtoD, cuMemcpyDtoH,
            cuModuleLoadData, cuModuleGetFunction, cuLaunchKernel, cuCtxSynchronize,
            cuMemGetInfo, cuMemGetInfo_v2, cuDeviceTotalMem_v2, cuGetErrorString;

    // NVRTC method handles.
    private final MethodHandle nvrtcCreateProgram, nvrtcCompileProgram, nvrtcGetPTXSize,
            nvrtcGetPTX, nvrtcGetProgramLogSize, nvrtcGetProgramLog, nvrtcDestroyProgram;

    private final String cudaLibPath;
    private final String nvrtcLibPath;
    private final int deviceOrdinal;
    private final String deviceName;
    private final long totalMemBytes;
    private final int computeCapabilityMajor;
    private final int computeCapabilityMinor;

    private MemorySegment context;          // CUcontext
    private boolean closed = false;
    private String lastError = "";
    private final ThreadLocal<Boolean> contextCurrent = ThreadLocal.withInitial(() -> false);

    // Execution-instrumentation counters (process-wide). These make real CUDA
    // activity independently observable: a diagnostic can read the deltas before
    // and after a training run and prove that kernels actually executed on the
    // device, instead of inferring GPU usage from device detection or config.
    private final java.util.concurrent.atomic.AtomicLong kernelLaunchCount =
            new java.util.concurrent.atomic.AtomicLong();
    private final java.util.concurrent.atomic.AtomicLong memcpyCount =
            new java.util.concurrent.atomic.AtomicLong();
    private final java.util.concurrent.atomic.AtomicLong memAllocCount =
            new java.util.concurrent.atomic.AtomicLong();
    private final java.util.concurrent.atomic.AtomicLong memFreeCount =
            new java.util.concurrent.atomic.AtomicLong();

    private static volatile CudaDevice instance;

    /** Returns the process-wide device, or {@code null} when CUDA is unavailable. */
    public static synchronized CudaDevice tryCreate() {
        if (instance == null) {
            try {
                instance = new CudaDevice(findLibrary("libcuda.so", new String[]{
                        "/usr/lib/x86_64-linux-gnu/libcuda.so.1",
                        "/usr/lib/x86_64-linux-gnu/libcuda.so",
                        "/usr/lib64/libcuda.so.1"}), 0);
            } catch (Throwable t) {
                instance = null;
            }
        }
        return instance;
    }

    public static synchronized CudaDevice instance() {
        return instance;
    }

    public static boolean isAvailable() {
        return tryCreate() != null;
    }

    public CudaDevice(String cudaLibPath, int deviceOrdinal) {
        this(cudaLibPath, findNvrtc(), deviceOrdinal);
    }

    public CudaDevice(String cudaLibPath, String nvrtcLibPath, int deviceOrdinal) {
        this.cudaLibPath = cudaLibPath;
        this.nvrtcLibPath = nvrtcLibPath;
        this.deviceOrdinal = deviceOrdinal;

        SymbolLookup cuda = SymbolLookup.libraryLookup(Path.of(cudaLibPath), arena);
        preloadNvrtcDependencies();
        SymbolLookup nvrtc = SymbolLookup.libraryLookup(Path.of(nvrtcLibPath), arena);

        cuInit = down(cuda, "cuInit", I32, I32);
        cuDeviceGet = down(cuda, "cuDeviceGet", I32, ADDR, I32);
        cuDeviceGetName = down(cuda, "cuDeviceGetName", I32, ADDR, I32, I32);
        cuDeviceGetAttribute = down(cuda, "cuDeviceGetAttribute", I32, ADDR, I32, I32);
        cuCtxCreate = down(cuda, "cuCtxCreate", I32, ADDR, I32, I32);
        cuCtxDestroy = down(cuda, "cuCtxDestroy", I32, ADDR);
        cuCtxSetCurrent = down(cuda, "cuCtxSetCurrent", I32, ADDR);
        cuMemAlloc = down(cuda, "cuMemAlloc", I32, ADDR, I64);
        cuMemFree = down(cuda, "cuMemFree", I32, I64);
        cuMemcpyHtoD = down(cuda, "cuMemcpyHtoD", I32, I64, ADDR, I64);
        cuMemcpyDtoH = down(cuda, "cuMemcpyDtoH", I32, ADDR, I64, I64);
        cuModuleLoadData = down(cuda, "cuModuleLoadData", I32, ADDR, ADDR);
        cuModuleGetFunction = down(cuda, "cuModuleGetFunction", I32, ADDR, ADDR, ADDR);
        cuLaunchKernel = down(cuda, "cuLaunchKernel", I32, ADDR, I32, I32, I32, I32, I32, I32, I32, ADDR, ADDR, ADDR);
        cuCtxSynchronize = down(cuda, "cuCtxSynchronize", I32);
        cuMemGetInfo = down(cuda, "cuMemGetInfo", I32, ADDR, ADDR);
        cuMemGetInfo_v2 = downOrNull(cuda, "cuMemGetInfo_v2", I32, I32, ADDR, ADDR, ADDR);
        cuDeviceTotalMem_v2 = downOrNull(cuda, "cuDeviceTotalMem_v2", I32, ADDR, I32);
        cuGetErrorString = down(cuda, "cuGetErrorString", I32, I32, ADDR);

        nvrtcCreateProgram = down(nvrtc, "nvrtcCreateProgram", I32, ADDR, ADDR, ADDR, I32, ADDR, ADDR);
        nvrtcCompileProgram = down(nvrtc, "nvrtcCompileProgram", I32, ADDR, I32, ADDR);
        nvrtcGetPTXSize = down(nvrtc, "nvrtcGetPTXSize", I32, ADDR, ADDR);
        nvrtcGetPTX = down(nvrtc, "nvrtcGetPTX", I32, ADDR, ADDR);
        nvrtcGetProgramLogSize = down(nvrtc, "nvrtcGetProgramLogSize", I32, ADDR, ADDR);
        nvrtcGetProgramLog = down(nvrtc, "nvrtcGetProgramLog", I32, ADDR, ADDR);
        nvrtcDestroyProgram = down(nvrtc, "nvrtcDestroyProgram", I32, ADDR);

        // Initialize the driver and create a context on the requested device.
        check((int) invoke(cuInit, 0), "cuInit");
        MemorySegment dev = arena.allocate(4);
        check((int) invoke(cuDeviceGet, dev, deviceOrdinal), "cuDeviceGet");
        int devOrdinal = dev.get(I32, 0);

        MemorySegment name = arena.allocate(256);
        check((int) invoke(cuDeviceGetName, name, 256, devOrdinal), "cuDeviceGetName");
        byte[] nameBytes = name.toArray(ValueLayout.JAVA_BYTE);
        int nl = 0; while (nl < nameBytes.length && nameBytes[nl] != 0) nl++;
        this.deviceName = new String(nameBytes, 0, nl, StandardCharsets.UTF_8);

        this.computeCapabilityMajor = attribute(devOrdinal, 75); // COMPUTE_CAPABILITY_MAJOR
        this.computeCapabilityMinor = attribute(devOrdinal, 76); // COMPUTE_CAPABILITY_MINOR

        this.context = arena.allocate(8);
        check((int) invoke(cuCtxCreate, context, 0, devOrdinal), "cuCtxCreate");

        // Total device memory. The legacy cuMemGetInfo / cuDeviceTotalMem return a
        // 32-bit-truncated value (0xFFFFFFFF = 4 GiB) on GPUs with >4 GiB of VRAM
        // (observed on RTX 5070 Ti, driver 595.99.02). The _v2 entry points report the
        // correct 64-bit size. Prefer _v2; fall back to the legacy query only when the
        // _v2 symbol is absent from the driver.
        this.totalMemBytes = queryTotalMemBytes(devOrdinal);
    }

    /**
     * Queries the true 64-bit total device memory. Prefers {@code cuDeviceTotalMem_v2}
     * (which returns the correct value on GPUs with &gt;4 GiB of VRAM) and falls back to
     * the legacy {@code cuMemGetInfo} total, which is known to return a 32-bit-truncated
     * {@code 0xFFFFFFFF} (4 GiB) on some Blackwell drivers.
     */
    private long queryTotalMemBytes(int devOrdinal) {
        if (cuDeviceTotalMem_v2 != null) {
            MemorySegment bytes = arena.allocate(8);
            try {
                if ((int) invoke(cuDeviceTotalMem_v2, bytes, devOrdinal) == 0) {
                    long total = bytes.get(I64, 0);
                    if (total > 0) {
                        return total;
                    }
                }
            } catch (Throwable ignored) {
                // fall back to the legacy query below
            }
        }
        MemorySegment free = arena.allocate(8);
        MemorySegment total = arena.allocate(8);
        check((int) invoke(cuMemGetInfo, free, total), "cuMemGetInfo");
        return total.get(I64, 0);
    }

    private MethodHandle down(SymbolLookup lib, String name, Object... layouts) {
        MemoryLayout[] l = new MemoryLayout[layouts.length];
        for (int i = 0; i < layouts.length; i++) l[i] = (MemoryLayout) layouts[i];
        return linker.downcallHandle(lib.find(name).orElseThrow(() -> new IllegalStateException("Missing symbol " + name)),
                FunctionDescriptor.of((MemoryLayout) layouts[0], java.util.Arrays.copyOfRange(l, 1, l.length)));
    }

    /** Like {@link #down} but returns {@code null} instead of throwing when the symbol is absent (older drivers). */
    private MethodHandle downOrNull(SymbolLookup lib, String name, Object... layouts) {
        var opt = lib.find(name);
        if (opt.isEmpty()) {
            return null;
        }
        MemoryLayout[] l = new MemoryLayout[layouts.length];
        for (int i = 0; i < layouts.length; i++) l[i] = (MemoryLayout) layouts[i];
        return linker.downcallHandle(opt.get(),
                FunctionDescriptor.of((MemoryLayout) layouts[0], java.util.Arrays.copyOfRange(l, 1, l.length)));
    }

    private int attribute(int dev, int attrib) {
        MemorySegment out = arena.allocate(4);
        int rc = (int) invoke(cuDeviceGetAttribute, out, attrib, dev);
        return rc == 0 ? out.get(I32, 0) : 0;
    }

    private Object invoke(MethodHandle mh, Object... args) {
        try {
            return mh.invokeWithArguments(args);
        } catch (Throwable t) {
            throw new RuntimeException("FFM invoke failed: " + t, t);
        }
    }

    private void check(int rc, String op) {
        if (rc != 0) {
            lastError = op + " failed: " + errorString(rc);
            throw new IllegalStateException(lastError);
        }
    }

    /**
     * A CUDA context is only current on the thread that made it current. The device is a
     * process-wide singleton, so before any driver-API call we make the context current on
     * the calling thread (once per thread).
     */
    private void ensureContext() {
        if (context == null || contextCurrent.get()) return;
        check((int) invoke(cuCtxSetCurrent, context.get(ADDR, 0)), "cuCtxSetCurrent");
        contextCurrent.set(true);
    }

    private String errorString(int code) {
        try {
            // cuGetErrorString(CUresult error, const char **pStr): the string is written to
            // *pStr (an out-parameter), and the function returns a CUresult. The previous
            // binding treated the int return value as the string pointer, which segfaults.
            MemorySegment pStr = arena.allocate(ADDR.byteSize());
            int rc = (int) invoke(cuGetErrorString, code, pStr);
            if (rc != 0) return "CUDA error " + code;
            MemorySegment strSeg = pStr.get(ADDR, 0);
            if (strSeg.address() == 0) return "CUDA error " + code;
            return strSeg.reinterpret(4096).getString(0);
        } catch (Throwable t) {
            return "CUDA error " + code;
        }
    }

    // ------------------------------------------------------------------
    // Public API
    // ------------------------------------------------------------------

    public String getDeviceName() { return deviceName; }
    public long getTotalMemBytes() { return totalMemBytes; }
    public int getComputeCapabilityMajor() { return computeCapabilityMajor; }
    public int getComputeCapabilityMinor() { return computeCapabilityMinor; }
    public String getLastError() { return lastError; }

    /** Total number of CUDA kernel launches performed through this device. */
    public long getKernelLaunchCount() { return kernelLaunchCount.get(); }

    /** Total number of host↔device memory copies (cuMemcpyHtoD / cuMemcpyDtoH). */
    public long getMemcpyCount() { return memcpyCount.get(); }

    /** Total number of device memory allocations (cuMemAlloc). */
    public long getMemAllocCount() { return memAllocCount.get(); }

    /** Total number of device memory releases (cuMemFree). */
    public long getMemFreeCount() { return memFreeCount.get(); }

    /** Free + total device memory in bytes (64-bit correct; prefers the _v2 API). */
    public long[] memGetInfo() {
        ensureContext();
        MemorySegment free = arena.allocate(8);
        MemorySegment total = arena.allocate(8);
        if (cuMemGetInfo_v2 != null) {
            MemorySegment largest = arena.allocate(8);
            try {
                if ((int) invoke(cuMemGetInfo_v2, deviceOrdinal, free, total, largest) == 0) {
                    long t = total.get(I64, 0);
                    if (t > 0) {
                        return new long[]{free.get(I64, 0), t};
                    }
                }
            } catch (Throwable ignored) {
                // fall back to the legacy query below
            }
        }
        check((int) invoke(cuMemGetInfo, free, total), "cuMemGetInfo");
        // Legacy cuMemGetInfo total is 32-bit-truncated on >4 GiB GPUs; correct it with
        // the reliably queried total when the legacy value looks truncated.
        long legacyTotal = total.get(I64, 0);
        long correctedTotal = (legacyTotal == 0xFFFFFFFFL && totalMemBytes > legacyTotal) ? totalMemBytes : legacyTotal;
        return new long[]{free.get(I64, 0), correctedTotal};
    }

    /** Allocates {@code bytes} of device memory, returns the device pointer. */
    public long alloc(long bytes) {
        if (bytes <= 0) {
            throw new IllegalArgumentException("Invalid CUDA allocation size: " + bytes
                    + " bytes (must be > 0)");
        }
        if (bytes > totalMemBytes) {
            // A single allocation larger than the device total can never succeed; fail
            // fast with an actionable diagnostic instead of a bare cuMemAlloc OOM.
            throw new IllegalStateException("cuMemAlloc size " + bytes + " bytes exceeds device total "
                    + totalMemBytes + " bytes (" + (totalMemBytes / (1024 * 1024)) + " MiB)");
        }
        ensureContext();
        MemorySegment ptr = arena.allocate(8);
        try {
            check((int) invoke(cuMemAlloc, ptr, bytes), "cuMemAlloc");
        } catch (IllegalStateException oom) {
            long[] mem = memGetInfo();
            throw new IllegalStateException("cuMemAlloc failed: " + oom.getMessage()
                    + " (requested=" + bytes + " bytes, free=" + mem[0] + " bytes, total="
                    + mem[1] + " bytes, device=" + deviceName + ")", oom);
        }
        memAllocCount.incrementAndGet();
        return ptr.get(I64, 0);
    }

    public void free(long devPtr) {
        if (devPtr == 0) return;
        ensureContext();
        invoke(cuMemFree, devPtr);
        memFreeCount.incrementAndGet();
    }

    public void memcpyHtoD(long devPtr, float[] host) {
        ensureContext();
        // Confined arena so the temporary host staging buffer is released immediately
        // after the transfer (the shared process arena would otherwise retain every
        // copy for the process lifetime and grow without bound across candidates).
        try (Arena confined = Arena.ofConfined()) {
            MemorySegment h = confined.allocate((long) host.length * 4);
            h.copyFrom(MemorySegment.ofArray(host));
            check((int) invoke(cuMemcpyHtoD, devPtr, h, (long) host.length * 4), "cuMemcpyHtoD");
        }
        memcpyCount.incrementAndGet();
    }

    public void memcpyHtoD(long devPtr, int[] host) {
        ensureContext();
        try (Arena confined = Arena.ofConfined()) {
            MemorySegment h = confined.allocate((long) host.length * 4);
            h.copyFrom(MemorySegment.ofArray(host));
            check((int) invoke(cuMemcpyHtoD, devPtr, h, (long) host.length * 4), "cuMemcpyHtoD");
        }
        memcpyCount.incrementAndGet();
    }

    public void memcpyDtoH(float[] host, long devPtr) {
        ensureContext();
        try (Arena confined = Arena.ofConfined()) {
            MemorySegment h = confined.allocate((long) host.length * 4);
            check((int) invoke(cuMemcpyDtoH, h, devPtr, (long) host.length * 4), "cuMemcpyDtoH");
            MemorySegment.ofArray(host).copyFrom(h);
        }
        memcpyCount.incrementAndGet();
    }

    public void synchronize() {
        ensureContext();
        check((int) invoke(cuCtxSynchronize), "cuCtxSynchronize");
    }

    /**
     * Compiles {@code cudaSource} (CUDA-C) to PTX via NVRTC and returns a loaded
     * {@link Module} with all its {@code __global__} kernels available.
     */
    public Module compile(String name, String cudaSource) {
        // Compile to PTX.
        MemorySegment src = arena.allocateFrom(cudaSource);
        MemorySegment prog = arena.allocate(8);
        MemorySegment progName = arena.allocateFrom(name);
        check((int) invoke(nvrtcCreateProgram, prog, src, progName, 0, MemorySegment.NULL, MemorySegment.NULL), "nvrtcCreateProgram");
        MemorySegment progH = prog.get(ADDR, 0);

        String arch = computeCapabilityMajor >= 12 ? "sm_" + computeCapabilityMajor + computeCapabilityMinor
                : (computeCapabilityMajor >= 9 ? "sm_" + computeCapabilityMajor + computeCapabilityMinor : "compute_" + computeCapabilityMajor + computeCapabilityMinor);
        // NVRTC accepts either sm_XX (cubin) or compute_XX (PTX, JIT'd). Prefer sm for the exact device.
        MemorySegment opt = arena.allocateFrom("--gpu-architecture=" + arch);
        MemorySegment optPtr = arena.allocate(8);
        optPtr.set(ADDR, 0, opt);
        int rc = (int) invoke(nvrtcCompileProgram, progH, 1, optPtr);
        if (rc != 0) {
            String log = programLog(progH);
            throw new IllegalStateException("NVRTC compile failed (rc=" + rc + ", arch=" + arch + "): " + log);
        }

        MemorySegment ptxSize = arena.allocate(8);
        check((int) invoke(nvrtcGetPTXSize, progH, ptxSize), "nvrtcGetPTXSize");
        long ps = ptxSize.get(I64, 0);
        MemorySegment ptx = arena.allocate(ps);
        check((int) invoke(nvrtcGetPTX, progH, ptx), "nvrtcGetPTX");
        byte[] ptxBytes = new byte[(int) ps];
        MemorySegment.ofArray(ptxBytes).copyFrom(ptx);

        // Load PTX into a module.
        ensureContext();
        MemorySegment module = arena.allocate(8);
        check((int) invoke(cuModuleLoadData, module, ptx), "cuModuleLoadData");
        MemorySegment moduleH = module.get(ADDR, 0);

        return new Module(this, moduleH);
    }

    private String programLog(MemorySegment progH) {
        try {
            MemorySegment logSize = arena.allocate(8);
            invoke(nvrtcGetProgramLogSize, progH, logSize);
            long ls = logSize.get(I64, 0);
            if (ls <= 1) return "(no log)";
            MemorySegment log = arena.allocate(ls);
            invoke(nvrtcGetProgramLog, progH, log);
            byte[] b = log.toArray(ValueLayout.JAVA_BYTE);
            return new String(b, 0, (int) ls - 1, StandardCharsets.UTF_8);
        } catch (Throwable t) {
            return "(unable to read log)";
        }
    }

    /** Launches a kernel with the given scalar / device-pointer arguments. */
    void launch(MemorySegment function, int gridX, int gridY, int gridZ,
                int blockX, int blockY, int blockZ, List<Object> args) {
        ensureContext();
        MemorySegment kparams = arena.allocate(Math.max(1, args.size()) * 8);
        int idx = 0;
        for (Object arg : args) {
            if (arg instanceof Long) {          // device pointer (float*/int*)
                MemorySegment slot = arena.allocate(8);
                slot.set(I64, 0, (Long) arg);
                kparams.set(ADDR, idx * 8, slot);
            } else if (arg instanceof Integer) { // int scalar (by value)
                MemorySegment slot = arena.allocate(4);
                slot.set(I32, 0, (Integer) arg);
                kparams.set(ADDR, idx * 8, slot);
            } else if (arg instanceof Float) {   // float scalar (by value)
                MemorySegment slot = arena.allocate(4);
                slot.set(ValueLayout.JAVA_FLOAT, 0, (Float) arg);
                kparams.set(ADDR, idx * 8, slot);
            } else {
                throw new IllegalArgumentException("Unsupported kernel arg type: " + arg.getClass());
            }
            idx++;
        }
        int rc = (int) invoke(cuLaunchKernel, function, gridX, gridY, gridZ, blockX, blockY, blockZ,
                0, MemorySegment.NULL, kparams, MemorySegment.NULL);
        check(rc, "cuLaunchKernel");
        kernelLaunchCount.incrementAndGet();
    }

    @Override
    public void close() {
        if (!closed) {
            closed = true;
            if (context != null) {
                try { invoke(cuCtxDestroy, context.get(ADDR, 0)); } catch (Throwable ignored) {}
            }
            try { arena.close(); } catch (Throwable ignored) {}
        }
    }

    /** A loaded CUDA module; provides typed kernel launchers. */
    public static final class Module {
        private final CudaDevice device;
        private final MemorySegment module;

        Module(CudaDevice device, MemorySegment module) {
            this.device = device;
            this.module = module;
        }

        public Kernel function(String name) {
            device.ensureContext();
            MemorySegment fn = device.arena.allocate(8);
            MemorySegment fnName = device.arena.allocateFrom(name);
            device.check((int) device.invoke(device.cuModuleGetFunction, fn, module, fnName), "cuModuleGetFunction");
            return new Kernel(device, fn.get(ADDR, 0), name);
        }
    }

    /** A resolved {@code __global__} kernel entry point. */
    public static final class Kernel {
        private final CudaDevice device;
        private final MemorySegment function;
        private final String name;

        Kernel(CudaDevice device, MemorySegment function, String name) {
            this.device = device;
            this.function = function;
            this.name = name;
        }

        public String getName() { return name; }

        /** Launches this kernel with a 1D grid of {@code n} threads and the given args. */
        public void launch(int n, List<Object> args) {
            int block = 256;
            int grid = (n + block - 1) / block;
            device.launch(function, grid, 1, 1, block, 1, 1, args);
        }

        public void launchGrid(int gridX, int blockX, List<Object> args) {
            device.launch(function, gridX, 1, 1, blockX, 1, 1, args);
        }
    }

    private static String findNvrtc() {
        return findLibrary("libnvrtc.so", new String[]{
                "/usr/local/lib/ollama/cuda_v13/libnvrtc.so.13",
                "/usr/local/lib/ollama/cuda_v13/libnvrtc.so",
                "/usr/local/lib/ollama/cuda_v12/libnvrtc.so",
                "/usr/local/cuda/lib64/libnvrtc.so"});
    }

    /**
     * NVRTC ({@code libnvrtc.so}) depends on {@code libnvfatbin}, {@code libnvJitLink}
     * and {@code libnvrtc-builtins}, which are only present in the Ollama CUDA vendor
     * directory (not on the default library path). Preload them in dependency order so
     * {@code dlopen} of the NVRTC library resolves cleanly without LD_LIBRARY_PATH.
     */
    private static void preloadNvrtcDependencies() {
        String[] deps = {
                "/usr/local/lib/ollama/cuda_v13/libnvfatbin.so.13.0.85",
                "/usr/local/lib/ollama/cuda_v13/libnvJitLink.so.13.0.88",
                "/usr/local/lib/ollama/cuda_v13/libnvrtc-builtins.so.13.0.88"
        };
        for (String d : deps) {
            try {
                if (Files.exists(Path.of(d))) {
                    System.load(d);
                }
            } catch (Throwable ignored) {
                // Best-effort: the caller may already have set LD_LIBRARY_PATH.
            }
        }
    }

    private static String findLibrary(String libName, String[] candidates) {
        for (String c : candidates) {
            if (Files.exists(Path.of(c))) return c;
        }
        return libName;
    }
}
