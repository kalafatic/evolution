package eu.kalafatic.evolution.forge.model.llm;

import eu.kalafatic.evolution.forge.model.protocol.EvoDtype;

/**
 * Configuration model for model quantization and format conversion.
 */
public class QuantizationConfig {

    public enum TargetFormat {
        EVO,
        GGUF,
        ALL
    }

    public enum QuantizationScheme {
        F32(EvoDtype.F32, "Full Precision (32-bit Float)", 1.0),
        F16(EvoDtype.F16, "Half Precision (16-bit Float)", 0.5),
        BF16(EvoDtype.BF16, "BFloat16 Precision", 0.5),
        INT8(EvoDtype.INT8, "8-bit Integer Quantization", 0.25),
        INT4(EvoDtype.INT4, "4-bit Integer Quantization", 0.125),
        Q8_0(EvoDtype.INT8, "GGUF Q8_0 Quantization", 0.28),
        Q4_0(EvoDtype.INT4, "GGUF Q4_0 Quantization", 0.15),
        Q4_K_M(EvoDtype.INT4, "GGUF Q4_K_M Medium Quantization", 0.18);

        private final EvoDtype evoDtype;
        private final String displayName;
        private final double estimatedRatio;

        QuantizationScheme(EvoDtype evoDtype, String displayName, double estimatedRatio) {
            this.evoDtype = evoDtype;
            this.displayName = displayName;
            this.estimatedRatio = estimatedRatio;
        }

        public EvoDtype getEvoDtype() {
            return evoDtype;
        }

        public String getDisplayName() {
            return displayName;
        }

        public double getEstimatedRatio() {
            return estimatedRatio;
        }
    }

    public enum Preset {
        FULL_PRECISION,
        BALANCED,
        LOW_MEMORY,
        HIGH_SPEED
    }

    private boolean enabled = false;
    private TargetFormat targetFormat = TargetFormat.ALL;
    private QuantizationScheme scheme = QuantizationScheme.F16;
    private Preset preset = Preset.BALANCED;
    private String hardwareProfile = "AUTO";
    private boolean allowLossy = true;
    private double minQualityThreshold = 0.80;

    public QuantizationConfig() {}

    public QuantizationConfig(QuantizationScheme scheme) {
        this.enabled = true;
        this.scheme = scheme;
    }

    public static QuantizationConfig fromPreset(Preset preset) {
        QuantizationConfig cfg = new QuantizationConfig();
        cfg.preset = preset;
        switch (preset) {
            case FULL_PRECISION -> {
                cfg.enabled = false;
                cfg.scheme = QuantizationScheme.F32;
            }
            case BALANCED -> {
                cfg.enabled = true;
                cfg.scheme = QuantizationScheme.F16;
            }
            case LOW_MEMORY -> {
                cfg.enabled = true;
                cfg.scheme = QuantizationScheme.INT4;
            }
            case HIGH_SPEED -> {
                cfg.enabled = true;
                cfg.scheme = QuantizationScheme.INT8;
            }
        }
        return cfg;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public TargetFormat getTargetFormat() {
        return targetFormat;
    }

    public void setTargetFormat(TargetFormat targetFormat) {
        this.targetFormat = targetFormat;
    }

    public QuantizationScheme getScheme() {
        return scheme;
    }

    public void setScheme(QuantizationScheme scheme) {
        this.scheme = scheme;
    }

    public Preset getPreset() {
        return preset;
    }

    public void setPreset(Preset preset) {
        this.preset = preset;
    }

    public String getHardwareProfile() {
        return hardwareProfile;
    }

    public void setHardwareProfile(String hardwareProfile) {
        this.hardwareProfile = hardwareProfile;
    }

    public boolean isAllowLossy() {
        return allowLossy;
    }

    public void setAllowLossy(boolean allowLossy) {
        this.allowLossy = allowLossy;
    }

    public double getMinQualityThreshold() {
        return minQualityThreshold;
    }

    public void setMinQualityThreshold(double minQualityThreshold) {
        this.minQualityThreshold = minQualityThreshold;
    }
}
