package eu.kalafatic.evolution.forge.trainer.device;

/**
 * Execution device for Forge training, with the truthful backend label used in
 * reporting. {@link #CUDA} is backed by the EVO-CUDA engine
 * ({@code eu.kalafatic.evolution.gpu.trainer.GpuTrainingEngine}); {@link #CPU} is
 * the canonical JVM Java trainer ({@code EvoLlmTrainer}).
 */
public enum TrainingDevice {
    CPU("CPU", "JVM Java CPU (EvoLlmTrainer)"),
    CUDA("CUDA", "EVO-CUDA");

    private final String label;
    private final String backendName;

    TrainingDevice(String label, String backendName) {
        this.label = label;
        this.backendName = backendName;
    }

    /** Short device label used in logs (e.g. {@code selectedDevice=CPU}). */
    public String label() {
        return label;
    }

    /** Human-readable backend implementation name. */
    public String backendName() {
        return backendName;
    }
}
