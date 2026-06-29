package eu.kalafatic.evolution.controller.orchestration.util;

public final class LlmCapabilityUtil {

    private LlmCapabilityUtil() {
    }

    public static double calculateCapability(OllamaModelInfo info) {

        if (info == null) {
            return 0.5;
        }

        double capability = 0.20;

        // Parameter count (largest influence)
        double billions = info.getParameterCountBillions();
        capability += Math.min(0.45, billions / 70.0 * 0.45);

        // Context window
        capability += Math.min(0.15, info.getContextLength() / 131072.0 * 0.15);

        // Quantization
        switch (info.getQuantization().toUpperCase()) {
            case "Q2_K":
                capability += 0.02;
                break;
            case "Q3_K_M":
                capability += 0.04;
                break;
            case "Q4_K_M":
                capability += 0.06;
                break;
            case "Q5_K_M":
                capability += 0.08;
                break;
            case "Q6_K":
                capability += 0.10;
                break;
            case "Q8_0":
                capability += 0.12;
                break;
            case "F16":
            case "FP16":
                capability += 0.15;
                break;
        }

        return Math.min(1.0, capability);
    }
}