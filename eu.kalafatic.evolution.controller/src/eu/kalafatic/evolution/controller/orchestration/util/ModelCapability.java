package eu.kalafatic.evolution.controller.orchestration.util;

public class ModelCapability {
    public String modelName;
    public String modelFamily;
    public String size; // UNKNOWN, SMALL, MEDIUM, LARGE, VERY_LARGE
    public long parameterCount;
    public String parameterDisplay;
    public boolean isQuantized;
    
    public boolean canHandleComplexJson;
    public boolean canHandleAbstractReasoning;
    public boolean canGenerateCode;
    public boolean needsSimplifiedPrompts;
    public String recommendedStrategy; // STEP_BY_STEP, SIMPLIFIED_JSON, FULL_JSON
    
    public boolean isSmall() {
        return "SMALL".equals(size) || "UNKNOWN".equals(size);
    }
    
    public boolean isMedium() {
        return "MEDIUM".equals(size);
    }
    
    public boolean isLarge() {
        return "LARGE".equals(size) || "VERY_LARGE".equals(size);
    }
    
    public boolean shouldUseStepByStep() {
        return "STEP_BY_STEP".equals(recommendedStrategy);
    }
    
    public boolean shouldUseSimplifiedJson() {
        return "SIMPLIFIED_JSON".equals(recommendedStrategy);
    }
    
    public boolean shouldUseFullJson() {
        return "FULL_JSON".equals(recommendedStrategy);
    }
    
    @Override
    public String toString() {
        return String.format("ModelCapability{name='%s', size='%s', params=%s, complexJson=%s, strategy='%s'}",
                modelName, size, parameterDisplay, canHandleComplexJson, recommendedStrategy);
    }
}