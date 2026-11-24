package eu.kalafatic.evolution.forge.trainer.device;

/**
 * Device-selection policy for Forge training.
 *
 * <p>Supports explicit CPU, explicit CUDA and automatic selection. Automatic
 * selection chooses CUDA only when the CUDA backend actually initializes
 * (a real CUDA device is present and the driver/NVRTC libraries load). It never
 * silently fabricates GPU support from configuration or device detection alone:
 * the selected device is a request, the actual device is what the backend reports
 * after a successful session is established.</p>
 */
public final class ForgeDeviceSelector {

    public enum Policy {
        /** Choose CUDA when available, otherwise CPU (with an explicit reason). */
        AUTO,
        /** Force the JVM Java CPU trainer. */
        CPU,
        /** Force the EVO-CUDA trainer; fall back to CPU with a reason if unavailable. */
        CUDA
    }

    /** Result of device selection: requested vs. selected vs. actual availability. */
    public static final class Selection {
        public final Policy policy;
        public final TrainingDevice requested;
        public final TrainingDevice selected;
        public final boolean cudaAvailable;
        public final String note;

        Selection(Policy policy, TrainingDevice requested, TrainingDevice selected,
                  boolean cudaAvailable, String note) {
            this.policy = policy;
            this.requested = requested;
            this.selected = selected;
            this.cudaAvailable = cudaAvailable;
            this.note = note;
        }

        public boolean useCuda() {
            return selected == TrainingDevice.CUDA;
        }

        @Override
        public String toString() {
            return "policy=" + policy + " requested=" + requested.label()
                    + " selected=" + selected.label()
                    + " cudaAvailable=" + cudaAvailable
                    + (note != null ? " note=" + note : "");
        }
    }

    private ForgeDeviceSelector() {
    }

    /** True when the CUDA backend initializes on this machine (real device + driver). */
    public static boolean isCudaAvailable() {
        // Reflective so the CPU-only path stays robust when the gpu bundle is absent
        // (mirrors EnrRuntime.detectGpu): no hard compile-time dependency on CudaDevice.
        try {
            Class<?> cls = Class.forName("eu.kalafatic.evolution.gpu.cuda.CudaDevice");
            Object r = cls.getMethod("isAvailable").invoke(null);
            return r instanceof Boolean && (Boolean) r;
        } catch (Throwable t) {
            return false;
        }
    }

    /** Resolves the policy from {@code evo.forge.device} (property or env), default AUTO. */
    public static Policy resolvePolicy() {
        String p = System.getProperty("evo.forge.device");
        if (p == null || p.isBlank()) {
            p = System.getenv("EVO_FORGE_DEVICE");
        }
        if (p != null && !p.isBlank()) {
            switch (p.trim().toUpperCase()) {
                case "CPU":
                    return Policy.CPU;
                case "CUDA":
                case "GPU":
                    return Policy.CUDA;
                case "AUTO":
                    return Policy.AUTO;
                default:
                    break;
            }
        }
        return Policy.AUTO;
    }

    /** Selects a device for {@code policy} (AUTO/CPU/CUDA). */
    public static Selection select(Policy policy) {
        return select(policy, policy == Policy.CPU ? TrainingDevice.CPU : TrainingDevice.CUDA);
    }

    public static Selection select(Policy policy, TrainingDevice explicitRequest) {
        boolean avail = isCudaAvailable();
        TrainingDevice requested = explicitRequest != null ? explicitRequest
                : (policy == Policy.CPU ? TrainingDevice.CPU : TrainingDevice.CUDA);

        if (policy == Policy.CPU) {
            return new Selection(policy, requested, TrainingDevice.CPU, avail, "Explicit CPU selection");
        }
        if (policy == Policy.CUDA) {
            if (avail) {
                return new Selection(policy, requested, TrainingDevice.CUDA, true, null);
            }
            return new Selection(policy, requested, TrainingDevice.CPU, false,
                    "Explicit CUDA requested but unavailable — CPU fallback");
        }
        // AUTO
        if (avail) {
            return new Selection(policy, requested, TrainingDevice.CUDA, true, null);
        }
        return new Selection(policy, requested, TrainingDevice.CPU, false,
                "CUDA unavailable — automatic CPU fallback");
    }
}
