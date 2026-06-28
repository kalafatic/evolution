package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;

import eu.kalafatic.evolution.controller.orchestration.AiService;
import eu.kalafatic.evolution.controller.orchestration.EvolutionProgressPublisher;
import eu.kalafatic.evolution.controller.orchestration.SessionContainer;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.goal.GoalModel;
import eu.kalafatic.evolution.controller.orchestration.util.ModelCapability;
import eu.kalafatic.evolution.controller.orchestration.util.ModelCapabilityDetector;

public class DynamicSiblingGenerator {
    
    private final SessionContainer container;
    private final AiService aiService;
    private final IntentAnalyzer intentAnalyzer;
    private final PromptOptimizer promptOptimizer;
    private final ModelCapabilityDetector capabilityDetector;
    
    public DynamicSiblingGenerator(SessionContainer container, AiService aiService) {
        this.container = container;
        this.aiService = aiService;
        
        // Initialize the analyzers with the container
        this.intentAnalyzer = new IntentAnalyzer(container);
        this.intentAnalyzer.setAiService(aiService);
        
        this.promptOptimizer = new PromptOptimizer( container);
        this.promptOptimizer.setAiService(aiService);
        
        this.capabilityDetector = new ModelCapabilityDetector();
    }
    
    public List<JSONObject> generateSiblings(
            String userRequest, 
            GoalModel goal,
            TaskContext context,
            EvolutionDimension activeDimension) throws Exception {
        
        // STEP 1: Analyze intent
        context.log("[DYNAMIC] Analyzing user intent...");
        IntentProfile intent = intentAnalyzer.analyzeIntent(userRequest, context);
        context.log("[DYNAMIC] Intent: " + intent.primaryGoal + " | Complexity: " + intent.complexity);
        
        // STEP 2: Optimize prompt based on intent
        context.log("[DYNAMIC] Optimizing prompt for intent...");
        PromptStrategy strategy = promptOptimizer.optimizePrompt(intent, context);
        context.log("[DYNAMIC] Using format: " + strategy.format + " | Siblings: " + strategy.siblingCount);
        
        // STEP 3: Detect model capability
        String modelName = getModelName(context);
        ModelCapability capability = capabilityDetector.getModelCapability(modelName);
        context.log("[DYNAMIC] Model: " + modelName + " | Capability: " + capability.size);
        
        // STEP 4: Generate siblings using the optimized strategy
        List<JSONObject> variants = new ArrayList<>();
        
        for (int i = 0; i < strategy.siblingCount; i++) {
            String prompt = buildDynamicPrompt(strategy, i, context);
            context.log("[DYNAMIC] Generating sibling " + (i+1) + "/" + strategy.siblingCount);
            
            JSONObject variant = generateSingleVariant(prompt, strategy, i, context);
            if (variant != null && validateVariant(variant, strategy)) {
                variants.add(variant);
            }
        }
        
        // Ensure we have at least one variant
        if (variants.isEmpty()) {
            context.log("[DYNAMIC] No valid variants generated. Creating fallback.");
            JSONObject fallback = createFallbackVariant(intent, context);
            variants.add(fallback);
        }
        
        return variants;
    }
    
    /**
     * Gets the model name from the context.
     */
    private String getModelName(TaskContext context) {
        if (context != null && context.getOrchestrator() != null) {
            var orchestrator = context.getOrchestrator();
            // Try Ollama first
            if (orchestrator.getOllama() != null) {
                String model = orchestrator.getOllama().getModel();
                if (model != null && !model.isEmpty()) {
                    return model;
                }
            }
            // Try provider
//            if (orchestrator.getProvider() != null) {
//                String model = orchestrator.getProvider().getModel();
//                if (model != null && !model.isEmpty()) {
//                    return model;
//                }
//            }
//            // Try AI service
//            if (orchestrator.getAiService() != null) {
//                String model = orchestrator.getAiService().getModel();
//                if (model != null && !model.isEmpty()) {
//                    return model;
//                }
//            }
        }
        // Fallback: check system property or environment
        String modelFromProperty = System.getProperty("ollama.model");
        if (modelFromProperty != null && !modelFromProperty.isEmpty()) {
            return modelFromProperty;
        }
        return "unknown";
    }
    
    private String buildDynamicPrompt(PromptStrategy strategy, int siblingIndex, TaskContext context) {
        StringBuilder prompt = new StringBuilder();
        
        // Base instruction
        prompt.append("Generate a Java solution for: ").append(strategy.intent.primaryGoal).append("\n\n");
        
        // Tone
        if ("instructional".equals(strategy.tone)) {
            prompt.append("Follow these instructions carefully:\n");
        } else if ("conversational".equals(strategy.tone)) {
            prompt.append("Let's create a Java class together.\n");
        }
        
        // Format-specific prompt construction
        switch (strategy.format) {
            case "STEP_BY_STEP":
                prompt.append(buildStepByStepPrompt(strategy, siblingIndex));
                break;
            case "JSON_SCHEMA":
                prompt.append(buildJsonSchemaPrompt(strategy, siblingIndex));
                break;
            case "SIMPLE_TEXT":
                prompt.append(buildSimpleTextPrompt(strategy, siblingIndex));
                break;
            case "CODE_ONLY":
                prompt.append(buildCodeOnlyPrompt(strategy, siblingIndex));
                break;
            default:
                prompt.append(buildSimpleTextPrompt(strategy, siblingIndex));
        }
        
        // Add examples
        if (!strategy.examples.isEmpty()) {
            prompt.append("\n\nExamples:\n");
            for (String example : strategy.examples) {
                prompt.append("- ").append(example).append("\n");
            }
        }
        
        // Add constraints
        if (!strategy.constraints.isEmpty()) {
            prompt.append("\n\nConstraints:\n");
            for (String constraint : strategy.constraints) {
                prompt.append("- ").append(constraint).append("\n");
            }
        }
        
        // Add validation expectations
        if (!strategy.validationRules.isEmpty()) {
            prompt.append("\n\nYour solution must:\n");
            for (String rule : strategy.validationRules) {
                prompt.append("- ").append(rule).append("\n");
            }
        }
        
        // Sibling variation hint
        if (siblingIndex > 0) {
            prompt.append("\n\nMake this solution DIFFERENT from the previous one.\n");
            prompt.append("Focus on a different approach or style.\n");
        }
        
        return prompt.toString();
    }
    
    private String buildStepByStepPrompt(PromptStrategy strategy, int index) {
        StringBuilder sb = new StringBuilder();
        sb.append("Step 1: What should the class be named?\n");
        sb.append("Step 2: What should the main method do?\n");
        sb.append("Step 3: Write the complete Java code.\n\n");
        sb.append("Return your answer in this format:\n");
        sb.append("CLASS_NAME: [name]\n");
        sb.append("METHOD: [description]\n");
        sb.append("CODE:\n");
        sb.append("```java\n");
        sb.append("[code here]\n");
        sb.append("```\n");
        return sb.toString();
    }
    
    private String buildJsonSchemaPrompt(PromptStrategy strategy, int index) {
        return """
            Return a JSON object with this structure:
            
            {
                "class_name": "Name of the class",
                "fields": ["field1", "field2"],
                "methods": [
                    {
                        "name": "methodName",
                        "returnType": "void",
                        "parameters": ["String text"],
                        "body": "System.out.println(text);"
                    }
                ],
                "main_method": {
                    "body": "Printer printer = new Printer(); printer.print(\"Hello\");"
                }
            }
            """;
    }
    
    private String buildSimpleTextPrompt(PromptStrategy strategy, int index) {
        return """
            Write a complete Java class that prints text to the console.
            
            Include:
            1. The class definition
            2. A method that prints text
            3. A main method that demonstrates it
            
            Return ONLY the Java code, no explanation.
            """;
    }
    
    private String buildCodeOnlyPrompt(PromptStrategy strategy, int index) {
        return """
            Write a Java class that prints text.
            Only output the code, no markdown, no explanation.
            """;
    }
    
    private JSONObject generateSingleVariant(String prompt, PromptStrategy strategy, int index, TaskContext context) {
        String variantId = "variant-" + System.currentTimeMillis() + "-" + index;
        String variantStrategy = strategy.intent.primaryGoal + " - Variant " + (index + 1);

        try {
            EvolutionProgressPublisher.updateBranchStatus(context, variantId, variantStrategy, "analyzing", null);
            String response = aiService.sendRequest(context.getOrchestrator(), prompt, context);
            
            // Parse based on format
            JSONObject variant = new JSONObject();
            variant.put("id", variantId);
            variant.put("strategy", variantStrategy);
            
            // Extract code based on format
            String code = extractCode(response, strategy.format);
            variant.put("implementation", code);
            
            // Extract class name
            String className = extractClassName(code);
            variant.put("class_name", className);
            
            // Build action
            JSONObject action = new JSONObject();
            action.put("operation", "WRITE");
            action.put("target", "src/main/java/com/example/" + className + ".java");
            action.put("implementation", code);
            JSONArray actions = new JSONArray();
            actions.put(action);
            variant.put("actions", actions);
            
            // Fill required fields
            variant.put("strategy_type", "PROBABLE_SURVIVOR");
            variant.put("semantic_anchor", strategy.intent.primaryGoal);
            variant.put("survival_argument", "Generated for: " + strategy.intent.primaryGoal);
            variant.put("tradeoffs", "Simple implementation");
            variant.put("reasoning_level", "MINIMAL");
            variant.put("architecture_enabled", false);
            variant.put("implementation_enabled", true);
            
            // Engineering dimensions
            JSONObject dims = new JSONObject();
            dims.put("active_dimension", "IMPLEMENTATION");
            dims.put("active_dimension_description", "Define the core functionality");
            dims.put("execution_model", "atomic");
            dims.put("abstraction_depth", "low");
            variant.put("engineering_dimensions", dims);
            
            // Projected steps
            JSONArray steps = new JSONArray();
            steps.put("Write Java class: " + className);
            steps.put("Add main method");
            variant.put("projected_steps", steps);
            
            EvolutionProgressPublisher.updateBranchStatus(context, variantId, variantStrategy, "planned", null);
            return variant;
            
        } catch (Exception e) {
            context.log("[DYNAMIC] Failed to generate variant: " + e.getMessage());
            EvolutionProgressPublisher.updateBranchStatus(context, variantId, variantStrategy, "failed", null);
            return null;
        }
    }
    
    private String extractCode(String response, String format) {
        // Try to extract from code blocks first
        Pattern codeBlockPattern = Pattern.compile(
            "```(?:java)?\\s*\\n(.*?)\\n```", 
            Pattern.DOTALL
        );
        Matcher matcher = codeBlockPattern.matcher(response);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        
        // If it's CODE_ONLY format, return as-is
        if ("CODE_ONLY".equals(format)) {
            return response.trim();
        }
        
        // Try to extract from STEP_BY_STEP format
        if ("STEP_BY_STEP".equals(format)) {
            Pattern stepPattern = Pattern.compile(
                "CODE:\\s*\\n?```(?:java)?\\s*\\n(.*?)\\n```", 
                Pattern.DOTALL
            );
            matcher = stepPattern.matcher(response);
            if (matcher.find()) {
                return matcher.group(1).trim();
            }
        }
        
        // Try to extract from JSON
        try {
            JSONObject obj = new JSONObject(response);
            return obj.optString("code", obj.optString("implementation", response));
        } catch (Exception e) {
            // Return raw response
            return response;
        }
    }
    
    private String extractClassName(String code) {
        // Look for "public class ClassName"
        Pattern pattern = Pattern.compile(
            "public\\s+class\\s+(\\w+)"
        );
        Matcher matcher = pattern.matcher(code);
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        // Look for "class ClassName"
        pattern = Pattern.compile("class\\s+(\\w+)");
        matcher = pattern.matcher(code);
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        return "DefaultPrinter";
    }
    
    private boolean validateVariant(JSONObject variant, PromptStrategy strategy) {
        // Check required fields
        if (!variant.has("implementation") || variant.optString("implementation").isEmpty()) {
            return false;
        }
        
        String code = variant.optString("implementation");
        
        // Check for class definition
        if (!code.contains("class")) {
            return false;
        }
        
        // Check for main method if required
        if (strategy.validationRules.stream().anyMatch(r -> r.contains("main"))) {
            if (!code.contains("main")) {
                return false;
            }
        }
        
        // Check for System.out.println if required (for print tasks)
        if (strategy.intent.primaryGoal.toLowerCase().contains("print") || 
            strategy.intent.primaryGoal.toLowerCase().contains("text")) {
            if (!code.contains("System.out.println") && !code.contains("System.out.print")) {
                // Allow fallback, but log warning
                return true; // Don't reject, just warn
            }
        }
        
        return true;
    }
    
    private JSONObject createFallbackVariant(IntentProfile intent, TaskContext context) {
        JSONObject variant = new JSONObject();
        String className = "Printer";
        String code = "public class Printer {\n" +
                      "    public void print(String text) {\n" +
                      "        System.out.println(text);\n" +
                      "    }\n" +
                      "    \n" +
                      "    public static void main(String[] args) {\n" +
                      "        Printer printer = new Printer();\n" +
                      "        printer.print(\"Hello, World!\");\n" +
                      "    }\n" +
                      "}";
        
        variant.put("id", "variant-fallback-" + System.currentTimeMillis());
        variant.put("strategy", "Fallback: " + intent.primaryGoal);
        variant.put("implementation", code);
        variant.put("class_name", className);
        
        JSONObject action = new JSONObject();
        action.put("operation", "WRITE");
        action.put("target", "src/main/java/com/example/" + className + ".java");
        action.put("implementation", code);
        JSONArray actions = new JSONArray();
        actions.put(action);
        variant.put("actions", actions);
        
        variant.put("strategy_type", "PROBABLE_SURVIVOR");
        variant.put("semantic_anchor", "Fallback implementation");
        variant.put("survival_argument", "Generated as fallback");
        variant.put("tradeoffs", "Simple implementation");
        
        JSONObject dims = new JSONObject();
        dims.put("active_dimension", "IMPLEMENTATION");
        variant.put("engineering_dimensions", dims);
        
        return variant;
    }
}