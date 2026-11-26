# Package Context — eu.kalafatic.evolution.view.monitor

## Purpose
The single authoritative hardware-monitoring subsystem for the EVO UI. One
sampling thread serves every consumer (Settings Hardware panel, Forge page,
AI Commander Forge panel) — there is no per-widget polling.

## Contents
- `HardwareMonitoringService` — centralized lifecycle (start/stop/interval),
  bounded metric history, listeners, "refresh now" and the one-shot hardware
  scan. Singleton; never touches SWT (headless-testable).
- `HardwareMetricsCollector` — single background sampling thread
  (evo-hardware-monitor) producing immutable `HardwareSnapshot`s.
- `HardwareSnapshot` — immutable multi-GPU sample; missing metrics are -1.
- `HardwareScanResult` — structured scan result (CPU/RAM/JVM/GPU/CUDA +
  timestamps + status + notes).
- `CpuMetricsReader`, `RamMetricsReader`, `GpuMetricsReader` — lightweight
  procfs/sysfs and nvidia-smi readers (multi-GPU).
- `HardwareDashboardBridge` — JSON payloads for the HTML dashboard
  (hardware-dashboard.html + js/hardware-dashboard.js).
- `HardwareMonitorGroup`, `SparklineChart` — legacy SWT live panel (Forge page).

## Architectural constraints
- CUDA detection is NOT duplicated here: the scan delegates to
  `CudaDiagnosticService.probeCudaCapability()` in the controller bundle.
- All UI updates marshal through `Display.asyncExec`; sampling never runs on
  the SWT thread.
- No third-party monitoring library (OSHI) — readers use stdlib + procfs +
  nvidia-smi, matching the existing EVO dependency policy.
