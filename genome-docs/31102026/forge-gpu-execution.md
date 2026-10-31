# EVO GPU Hardware Detection & Acceleration Subsystem

**Date:** October 31, 2026
**Document ID:** `genome-docs/31102026/forge-gpu-execution.md`

---

## 1. Hardware Detection & Foreign Function Interface (FFM)

Module `eu.kalafatic.evolution.gpu` executes isolated GPU device inspection:
- **CUDA Device Detection**: `NvidiaGpuDetector` executes `nvidia-smi` with timeouts.
- **FFM Native Discovery**: `CudaComputeVerifier` uses Java 21 Foreign Function & Memory API (`SymbolLookup`) to link native CUDA driver libraries (`libcuda.so` / `nvcuda.dll`) and runtime libraries (`libcudart.so` / `cudart.dll`).
- **Execution Truth**: Explicitly distinguishes `EXTERNAL_GPU_CUDA` acceleration from `CPU_HOST_FALLBACK` without fabricated GPU metrics.
