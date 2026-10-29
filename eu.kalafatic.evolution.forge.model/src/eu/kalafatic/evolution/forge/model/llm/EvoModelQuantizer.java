package eu.kalafatic.evolution.forge.model.llm;

import eu.kalafatic.evolution.forge.model.protocol.EvoDtype;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Quantization engine for EVO Native Model Artifacts (.evo) and GGUF conversions.
 */
public class EvoModelQuantizer {

    public static class QuantizationResult {
        private final EvoModelArtifact quantizedArtifact;
        private final long originalSize;
        private final long quantizedSize;
        private final double compressionRatio;
        private final String schemeName;

        public QuantizationResult(EvoModelArtifact quantizedArtifact, long originalSize, long quantizedSize, String schemeName) {
            this.quantizedArtifact = quantizedArtifact;
            this.originalSize = originalSize;
            this.quantizedSize = quantizedSize;
            this.compressionRatio = originalSize > 0 ? (double) quantizedSize / originalSize : 1.0;
            this.schemeName = schemeName;
        }

        public EvoModelArtifact getQuantizedArtifact() {
            return quantizedArtifact;
        }

        public long getOriginalSize() {
            return originalSize;
        }

        public long getQuantizedSize() {
            return quantizedSize;
        }

        public double getCompressionRatio() {
            return compressionRatio;
        }

        public String getSchemeName() {
            return schemeName;
        }
    }

    /**
     * Quantizes an EvoModelArtifact according to the provided QuantizationConfig.
     */
    public QuantizationResult quantizeArtifact(EvoModelArtifact sourceArtifact, QuantizationConfig config) {
        if (sourceArtifact == null) {
            throw new IllegalArgumentException("Source artifact cannot be null");
        }
        if (config == null) {
            config = new QuantizationConfig(QuantizationConfig.QuantizationScheme.F16);
        }

        QuantizationConfig.QuantizationScheme scheme = config.getScheme();
        EvoDtype targetDtype = scheme.getEvoDtype();

        EvoModelArtifact quantized = new EvoModelArtifact();

        // Copy model architecture and tokenizer descriptors
        String newName = sourceArtifact.getModelName() + "-quant-" + scheme.name().toLowerCase();
        quantized.setModelName(newName);

        EvoLlmModel sourceModel = sourceArtifact.createModel();
        quantized.initializeFromModel(newName, sourceModel, sourceArtifact.getTokenizerVocab());

        // Copy metadata and add quantization provenance
        Map<String, String> meta = quantized.getMetadata();
        meta.putAll(sourceArtifact.getMetadata());
        meta.put("quantized", "true");
        meta.put("quantization_scheme", scheme.name());
        meta.put("quantization_dtype", targetDtype.name());
        meta.put("source_model_name", sourceArtifact.getModelName());
        meta.put("source_content_hash", sourceArtifact.getModelContentHash() != null ? sourceArtifact.getModelContentHash() : "");
        meta.put("quantized_timestamp", String.valueOf(System.currentTimeMillis()));

        List<float[]> sourceWeightData = sourceArtifact.getWeightData();
        List<float[]> quantizedWeightData = new ArrayList<>();
        List<EvoDtype> weightDtypes = new ArrayList<>();

        long totalOriginalElements = 0;

        for (int i = 0; i < sourceWeightData.size(); i++) {
            float[] origData = sourceWeightData.get(i);
            totalOriginalElements += origData.length;

            float[] qData = new float[origData.length];
            float scale = calculatePerTensorScale(origData, targetDtype);

            for (int j = 0; j < origData.length; j++) {
                qData[j] = quantizeValue(origData[j], targetDtype, scale);
            }

            quantizedWeightData.add(qData);
            weightDtypes.add(targetDtype);

            String weightName = sourceArtifact.getWeightNames().size() > i ? sourceArtifact.getWeightNames().get(i) : "weight_" + i;
            meta.put("tensor_scale_" + weightName, String.valueOf(scale));
        }

        // Replace weight data in quantized artifact
        quantized.getWeightData().clear();
        quantized.getWeightData().addAll(quantizedWeightData);

        quantized.recalculateManifestAndHash();

        long originalBytes = totalOriginalElements * Float.BYTES;
        long quantizedBytes = (long) (totalOriginalElements * targetDtype.getElementBytes());

        meta.put("original_weight_bytes", String.valueOf(originalBytes));
        meta.put("quantized_weight_bytes", String.valueOf(quantizedBytes));
        meta.put("compression_ratio", String.format("%.4f", (double) quantizedBytes / originalBytes));

        return new QuantizationResult(quantized, originalBytes, quantizedBytes, scheme.name());
    }

    private float calculatePerTensorScale(float[] data, EvoDtype targetDtype) {
        float maxAbs = 0.0f;
        for (float v : data) {
            float abs = Math.abs(v);
            if (abs > maxAbs) {
                maxAbs = abs;
            }
        }
        if (maxAbs == 0.0f) return 1.0f;

        switch (targetDtype) {
            case INT8 -> {
                return maxAbs / 127.0f;
            }
            case INT4 -> {
                return maxAbs / 7.0f;
            }
            default -> {
                return 1.0f;
            }
        }
    }

    private float quantizeValue(float val, EvoDtype targetDtype, float scale) {
        if (Float.isNaN(val) || Float.isInfinite(val)) {
            return 0.0f;
        }

        switch (targetDtype) {
            case F16 -> {
                // Truncate float32 precision to 16-bit half float precision
                short f16Bits = floatToFloat16(val);
                return float16ToFloat(f16Bits);
            }
            case BF16 -> {
                // Truncate lower 16 bits of float32 mantissa
                int bits = Float.floatToIntBits(val);
                int bf16Bits = (bits & 0xFFFF0000);
                return Float.intBitsToFloat(bf16Bits);
            }
            case INT8 -> {
                int q = Math.round(val / scale);
                q = Math.max(-127, Math.min(127, q));
                return q * scale;
            }
            case INT4 -> {
                int q = Math.round(val / scale);
                q = Math.max(-7, Math.min(7, q));
                return q * scale;
            }
            case F32 -> {
                return val;
            }
            default -> {
                return val;
            }
        }
    }

    private static short floatToFloat16(float val) {
        int bits = Float.floatToIntBits(val);
        int sign = (bits >>> 16) & 0x8000;
        int exp = ((bits >>> 23) & 0xFF) - 127 + 15;
        int mant = bits & 0x007FFFFF;

        if (exp <= 0) {
            return (short) sign;
        } else if (exp >= 31) {
            return (short) (sign | 0x7C00);
        } else {
            return (short) (sign | (exp << 10) | (mant >>> 13));
        }
    }

    private static float float16ToFloat(short hbits) {
        int mant = hbits & 0x03FF;
        int exp = hbits & 0x7C00;
        if (exp == 0x7C00) {
            exp = 0x8f;
        } else if (exp != 0) {
            exp = (exp >> 10) + 127 - 15;
        } else if (mant != 0) {
            exp = 127 - 14;
            while ((mant & 0x0400) == 0) {
                mant <<= 1;
                exp--;
            }
            mant &= 0x03FF;
        } else {
            exp = 0;
        }
        int bits = ((hbits & 0x8000) << 16) | (exp << 23) | (mant << 13);
        return Float.intBitsToFloat(bits);
    }
}
