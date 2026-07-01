package eu.kalafatic.evolution.controller.orchestration.util;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

public class OllamaModelInfo {
	
	  private double parameterCountBillions;
	    private int contextLength;
	    private String quantization;
    
    private static final String OLLAMA_HOST = "http://localhost:11434";
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    
    // Known model sizes for fallback
    private static final Map<String, Long> KNOWN_MODEL_SIZES = new HashMap<>();
    static {
        // Gemma family
        KNOWN_MODEL_SIZES.put("gemma3:1b", 1_000_000_000L);
        KNOWN_MODEL_SIZES.put("gemma3:2b", 2_000_000_000L);
        KNOWN_MODEL_SIZES.put("gemma3:4b", 4_000_000_000L);
        KNOWN_MODEL_SIZES.put("gemma3:12b", 12_000_000_000L);
        KNOWN_MODEL_SIZES.put("gemma3:27b", 27_000_000_000L);
        KNOWN_MODEL_SIZES.put("gemma:2b", 2_000_000_000L);
        KNOWN_MODEL_SIZES.put("gemma:7b", 7_000_000_000L);
        
        // Llama family
        KNOWN_MODEL_SIZES.put("llama3.2:1b", 1_000_000_000L);
        KNOWN_MODEL_SIZES.put("llama3.2:3b", 3_000_000_000L);
        KNOWN_MODEL_SIZES.put("llama3.1:8b", 8_000_000_000L);
        KNOWN_MODEL_SIZES.put("llama3:70b", 70_000_000_000L);
        KNOWN_MODEL_SIZES.put("llama3:8b", 8_000_000_000L);
        KNOWN_MODEL_SIZES.put("llama2:7b", 7_000_000_000L);
        KNOWN_MODEL_SIZES.put("llama2:13b", 13_000_000_000L);
        KNOWN_MODEL_SIZES.put("llama2:70b", 70_000_000_000L);
        
        // Qwen family
        KNOWN_MODEL_SIZES.put("qwen2.5:0.5b", 500_000_000L);
        KNOWN_MODEL_SIZES.put("qwen2.5:1.5b", 1_500_000_000L);
        KNOWN_MODEL_SIZES.put("qwen2.5:3b", 3_000_000_000L);
        KNOWN_MODEL_SIZES.put("qwen2.5:7b", 7_000_000_000L);
        KNOWN_MODEL_SIZES.put("qwen2.5:14b", 14_000_000_000L);
        KNOWN_MODEL_SIZES.put("qwen2.5:32b", 32_000_000_000L);
        KNOWN_MODEL_SIZES.put("qwen2.5:72b", 72_000_000_000L);
        
        // Phi family
        KNOWN_MODEL_SIZES.put("phi3:2.7b", 2_700_000_000L);
        KNOWN_MODEL_SIZES.put("phi3:mini", 3_800_000_000L);
        KNOWN_MODEL_SIZES.put("phi3:medium", 14_000_000_000L);
        
        // Mistral family
        KNOWN_MODEL_SIZES.put("mistral:7b", 7_000_000_000L);
        KNOWN_MODEL_SIZES.put("mistral:8x7b", 46_000_000_000L);
        KNOWN_MODEL_SIZES.put("mixtral:8x7b", 46_000_000_000L);
        
        // DeepSeek
        KNOWN_MODEL_SIZES.put("deepseek-coder:6.7b", 6_700_000_000L);
        KNOWN_MODEL_SIZES.put("deepseek-coder:33b", 33_000_000_000L);
    }
    

  
    
    public ModelInfo getModelInfo(String modelName) {
        ModelInfo info = new ModelInfo();
        info.modelName = modelName;
        
        try {
            String url = OLLAMA_HOST + "/api/show";
            String payload = "{\"name\": \"" + modelName + "\"}";
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, 
                    HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                JSONObject obj = new JSONObject(response.body());
                info.success = true;
                
                // 1. Try to get parameter_size from response first
                String paramSize = obj.optString("parameter_size", null);
                if (paramSize != null && !paramSize.isEmpty() && !paramSize.equals("0B")) {
                    info.parameterCount = parseParameterSize(paramSize);
                    info.parameterSizeDisplay = paramSize;
                } else {
                    // 2. Extract from modelfile (e.g., "FROM gemma3:1b")
                    String modelfile = obj.optString("modelfile", "");
                    info.parameterCount = extractParameterCountFromModelfile(modelfile);
                    info.parameterSizeDisplay = formatParameterSize(info.parameterCount);
                }
                
                // 3. If still not found, use known model sizes
                if (info.parameterCount <= 0) {
                    info.parameterCount = getKnownModelSize(modelName);
                    info.parameterSizeDisplay = formatParameterSize(info.parameterCount);
                }
                
                // 4. Detect quantization
                String quantization = obj.optString("quantization", "");
                if (!quantization.isEmpty()) {
                    info.isQuantized = quantization.contains("q") || quantization.contains("Q");
                    info.quantization = quantization;
                } else {
                    // Try to detect from modelfile
                    String modelfile = obj.optString("modelfile", "");
                    info.isQuantized = detectQuantization(modelfile);
                    info.quantization = extractQuantization(modelfile);
                }
                
                // 5. Other metadata
                info.modifiedAt = obj.optString("modified_at", "");
                info.modelFamily = detectModelFamily(modelName);
                info.template = obj.optString("template", "");
                info.system = obj.optString("system", "");
                info.parameters = obj.optString("parameters", "");
                
            } else {
                info.success = false;
                info.error = "HTTP " + response.statusCode();
            }
            
        } catch (Exception e) {
            info.success = false;
            info.error = e.getMessage();
        }
        
        // Final fallback
        if (info.parameterCount <= 0) {
            info.parameterCount = 1_000_000_000L; // Default to 1B
            info.parameterSizeDisplay = "1B (assumed)";
        }
        
        return info;
    }
    
    // ====== PRIVATE HELPER METHODS ======
    
    private long extractParameterCountFromModelfile(String modelfile) {
        if (modelfile == null || modelfile.isEmpty()) return 0;
        
        // Pattern: "FROM gemma3:1b" or "FROM model:7b"
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
            "(?i)FROM\\s+[a-zA-Z0-9_.-]+[:/]([0-9.]+[bB]?)"
        );
        java.util.regex.Matcher matcher = pattern.matcher(modelfile);
        
        if (matcher.find()) {
            String sizeStr = matcher.group(1);
            return parseParameterSize(sizeStr);
        }
        
        // Alternative: look for "gemma3:1b" anywhere in the text
        pattern = java.util.regex.Pattern.compile("([a-zA-Z0-9_.-]+[:/]?)([0-9.]+[bB])");
        matcher = pattern.matcher(modelfile);
        while (matcher.find()) {
            String sizeStr = matcher.group(2);
            long size = parseParameterSize(sizeStr);
            if (size > 0) return size;
        }
        
        return 0;
    }
    
    private long parseParameterSize(String paramSize) {
        if (paramSize == null || paramSize.isEmpty()) return 0;
        
        String cleaned = paramSize.trim().toLowerCase();
        cleaned = cleaned.replaceAll("[^0-9.]", "");
        
        if (cleaned.isEmpty()) return 0;
        
        try {
            double value = Double.parseDouble(cleaned);
            if (paramSize.toLowerCase().endsWith("b")) {
                return (long)(value * 1_000_000_000L);
            } else if (paramSize.toLowerCase().endsWith("m")) {
                return (long)(value * 1_000_000L);
            } else {
                return (long)(value * 1_000_000_000L);
            }
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    
    private String formatParameterSize(long bytes) {
        if (bytes <= 0) return "0B";
        if (bytes >= 1_000_000_000L) {
            long billions = bytes / 1_000_000_000L;
            long remainder = (bytes % 1_000_000_000L) / 100_000_000L;
            if (remainder > 0) {
                return billions + "." + remainder + "B";
            }
            return billions + "B";
        } else if (bytes >= 1_000_000L) {
            return (bytes / 1_000_000L) + "M";
        } else {
            return bytes + "B";
        }
    }
    
    private long getKnownModelSize(String modelName) {
        // Check exact match first
        if (KNOWN_MODEL_SIZES.containsKey(modelName)) {
            return KNOWN_MODEL_SIZES.get(modelName);
        }
        
        // Check partial matches
        String lower = modelName.toLowerCase();
        for (Map.Entry<String, Long> entry : KNOWN_MODEL_SIZES.entrySet()) {
            if (lower.contains(entry.getKey().toLowerCase())) {
                return entry.getValue();
            }
        }
        
        // Try to extract number from name (e.g., "gemma3:1b" -> 1)
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("[0-9]+(\\.[0-9]+)?[bB]");
        java.util.regex.Matcher matcher = pattern.matcher(lower);
        if (matcher.find()) {
            String sizeStr = matcher.group();
            return parseParameterSize(sizeStr);
        }
        
        return 0;
    }
    
    private boolean detectQuantization(String modelfile) {
        if (modelfile == null || modelfile.isEmpty()) return false;
        String lower = modelfile.toLowerCase();
        return lower.contains("q4") || lower.contains("q8") || 
               lower.contains("q5") || lower.contains("q6") ||
               lower.contains("qf") || lower.contains("q2") ||
               lower.contains("q3");
    }
    
    private String extractQuantization(String modelfile) {
        if (modelfile == null || modelfile.isEmpty()) return "";
        
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
            "(?i)(q[0-9]_[0-9a-zA-Z_]+)"
        );
        java.util.regex.Matcher matcher = pattern.matcher(modelfile);
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        pattern = java.util.regex.Pattern.compile("(?i)(q[0-9][0-9]?)");
        matcher = pattern.matcher(modelfile);
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        return "";
    }
    
    private String detectModelFamily(String modelName) {
        String lower = modelName.toLowerCase();
        if (lower.contains("gemma")) return "gemma";
        if (lower.contains("llama")) return "llama";
        if (lower.contains("qwen")) return "qwen";
        if (lower.contains("phi")) return "phi";
        if (lower.contains("mistral") || lower.contains("mixtral")) return "mistral";
        if (lower.contains("deepseek")) return "deepseek";
        if (lower.contains("codellama")) return "codellama";
        if (lower.contains("neural")) return "neural";
        return "unknown";
    }

	public double getParameterCountBillions() {
		return parameterCountBillions;
	}

	public void setParameterCountBillions(double parameterCountBillions) {
		this.parameterCountBillions = parameterCountBillions;
	}

	public int getContextLength() {
		return contextLength;
	}

	public void setContextLength(int contextLength) {
		this.contextLength = contextLength;
	}

	public String getQuantization() {
		return quantization;
	}

	public void setQuantization(String quantization) {
		this.quantization = quantization;
	}
}