package eu.kalafatic.evolution.controller.orchestration.util;

public class ModelInfo {
    String modelName;
    boolean success;
    String error;
    
    long parameterCount;
    String parameterSizeDisplay;
    
    boolean isQuantized;
    String quantization;
    
    String modifiedAt;
    String modelFamily;
    String template;
    String system;
    String parameters;
    
    public boolean isSmall() {
        return parameterCount > 0 && parameterCount < 3_000_000_000L;
    }
    
    public boolean isMedium() {
        return parameterCount >= 3_000_000_000L && parameterCount < 20_000_000_000L;
    }
    
    public boolean isLarge() {
        return parameterCount >= 20_000_000_000L;
    }
    
    public boolean canHandleComplexJson() {
        // Gemma 2B can sometimes handle simple JSON, but not complex
        if (modelFamily.equals("gemma") && parameterCount < 7_000_000_000L) {
            return false;
        }
        return !isSmall() && parameterCount >= 7_000_000_000L;
    }
    
    public boolean canHandleCodeGeneration() {
        // Most models can generate code, but small ones make more mistakes
        if (isSmall()) {
            return modelFamily.equals("gemma") || 
                   modelFamily.equals("qwen") ||
                   modelFamily.equals("phi");
        }
        return true;
    }
    
    public boolean canHandleAbstractReasoning() {
        return isLarge() || (isMedium() && !isQuantized);
    }
    
    @Override
    public String toString() {
        return String.format("ModelInfo{name='%s', family='%s', parameters=%dB (%s), quantized=%s}", 
                modelName, modelFamily, parameterCount, parameterSizeDisplay, isQuantized);
    }

}
