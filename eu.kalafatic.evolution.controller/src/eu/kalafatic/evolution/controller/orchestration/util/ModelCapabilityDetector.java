package eu.kalafatic.evolution.controller.orchestration.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ModelCapabilityDetector {
	
	private static final ModelCapabilityDetector INSTANCE = new ModelCapabilityDetector();
	
    private static final Map<String, ModelInfo> cache = new ConcurrentHashMap<>();
    private static final Map<String, ModelCapability> cacheMC = new ConcurrentHashMap<>();
	
    private final OllamaModelInfo ollamaFetcher = new OllamaModelInfo();
    
    public ModelInfo getModelCapability(String modelName) {
        // Check cache first
        if (cache.containsKey(modelName)) {
            return cache.get(modelName);
        }
        
        // Fetch from Ollama
        ModelInfo info = ollamaFetcher.getModelInfo(modelName);
        
        if (!info.success) {
            // Fallback: infer from model name
            info = inferFromName(modelName);
        }
        
        // Cache for future use
        cache.put(modelName, info);
        return info;
    }
    
    /**
     * Detects the capability of a model based on its name.
     * Uses caching to avoid repeated lookups.
     */
    public ModelCapability detect(String modelName) {
        // Check cache first
        if (cacheMC.containsKey(modelName)) {
            return cacheMC.get(modelName);
        }
        
        // Get model info from Ollama
        ModelInfo info = ollamaFetcher.getModelInfo(modelName);
        ModelCapability cap = toCapability(info);
        
        // Cache for future use
        cacheMC.put(modelName, cap);
        return cap;
    }
    
    private ModelCapability toCapability(ModelInfo info) {
        ModelCapability cap = new ModelCapability();
        cap.modelName = info.modelName;
        cap.modelFamily = info.modelFamily;
        cap.parameterCount = info.parameterCount;
        cap.parameterDisplay = info.parameterSizeDisplay;
        cap.isQuantized = info.isQuantized;
        
        // Determine capability based on parameter count
        if (info.parameterCount <= 0) {
            // Unknown model - assume small to be safe
            cap.size = "UNKNOWN";
            cap.canHandleComplexJson = false;
            cap.canHandleAbstractReasoning = false;
            cap.canGenerateCode = true;
            cap.needsSimplifiedPrompts = true;
            cap.recommendedStrategy = "STEP_BY_STEP";
        } else if (info.parameterCount < 3_000_000_000L) {
            // Small: gemma3:1b, llama3.2:1b, qwen:0.5b, phi:2.7b
            cap.size = "SMALL";
            cap.canHandleComplexJson = false;
            cap.canHandleAbstractReasoning = false;
            cap.canGenerateCode = true;
            cap.needsSimplifiedPrompts = true;
            cap.recommendedStrategy = "STEP_BY_STEP";
        } else if (info.parameterCount < 10_000_000_000L) {
            // Medium: gemma3:4b, llama3.2:3b, qwen:3b, gemma3:12b
            cap.size = "MEDIUM";
            cap.canHandleComplexJson = true;
            cap.canHandleAbstractReasoning = true;
            cap.canGenerateCode = true;
            cap.needsSimplifiedPrompts = false;
            cap.recommendedStrategy = "SIMPLIFIED_JSON";
        } else if (info.parameterCount < 30_000_000_000L) {
            // Large: llama3.1:8b, qwen:7b, gemma3:27b
            cap.size = "LARGE";
            cap.canHandleComplexJson = true;
            cap.canHandleAbstractReasoning = true;
            cap.canGenerateCode = true;
            cap.needsSimplifiedPrompts = false;
            cap.recommendedStrategy = "FULL_JSON";
        } else {
            // Very large: llama3:70b, qwen:72b
            cap.size = "VERY_LARGE";
            cap.canHandleComplexJson = true;
            cap.canHandleAbstractReasoning = true;
            cap.canGenerateCode = true;
            cap.needsSimplifiedPrompts = false;
            cap.recommendedStrategy = "FULL_JSON";
        }
        
        // Override for specific known issues
        if (info.modelFamily.equals("phi") && info.parameterCount < 4_000_000_000L) {
            cap.canHandleComplexJson = false;
            cap.recommendedStrategy = "STEP_BY_STEP";
        }
        
        return cap;
    }


    private ModelInfo inferFromName(String modelName) {
        ModelInfo info = new ModelInfo();
        info.modelName = modelName;
       // info.modelFamily = detectModelFamily(modelName);
        info.success = false;
        
        // Guess parameter count from name patterns
        if (modelName.matches(".*[0-9]+b.*")) {
            String num = modelName.replaceAll(".*?([0-9]+)b.*", "$1");
            long params = Long.parseLong(num) * 1_000_000_000L;
            info.parameterCount = params;
            info.parameterSizeDisplay = num + "B";
        } else if (modelName.contains("gemma") && !modelName.contains(":")) {
            // Default gemma without tag is 7B
            info.parameterCount = 7_000_000_000L;
            info.parameterSizeDisplay = "7B";
        } else if (modelName.contains("llama") && !modelName.contains(":")) {
            info.parameterCount = 7_000_000_000L;
            info.parameterSizeDisplay = "7B";
        } else {
            // Unknown - assume medium
            info.parameterCount = 7_000_000_000L;
            info.parameterSizeDisplay = "7B (assumed)";
        }
        
        return info;
    }

	public static ModelCapabilityDetector getInstance() {
		// TODO Auto-generated method stub
		return INSTANCE;
	}
}
