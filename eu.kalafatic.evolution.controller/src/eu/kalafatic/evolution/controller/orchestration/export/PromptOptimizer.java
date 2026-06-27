package eu.kalafatic.evolution.controller.orchestration.export;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;

import eu.kalafatic.evolution.controller.agents.BaseAiAgent;
import eu.kalafatic.evolution.controller.orchestration.AiService;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.selfdev.IntentProfile;
import eu.kalafatic.evolution.controller.orchestration.selfdev.PromptStrategy;
import eu.kalafatic.evolution.controller.orchestration.SessionContainer;

public class PromptOptimizer extends BaseAiAgent {
    
    private AiService aiService;
    
    public PromptOptimizer(SessionContainer container) {
        super("PromptOptimizer", "PromptOptimizer", container);
    }
    
    @Override
    public void setAiService(AiService aiService) {
        super.setAiService(aiService);
        this.aiService = aiService;
    }
    
    public PromptStrategy optimizePrompt(IntentProfile intent, TaskContext context) {
        try {
            String prompt = buildOptimizationPrompt(intent);
            String response = aiService.sendRequest(context.getOrchestrator(), prompt, context);
            return parseOptimizationResponse(response, intent);
        } catch (Exception e) {
            context.log("[PROMPT_OPTIMIZER] Failed to optimize prompt: " + e.getMessage());
            return createDefaultStrategy(intent);
        }
    }
    
    private String buildOptimizationPrompt(IntentProfile intent) {
        return """
            Given this user intent, design the optimal prompt format for generating Java code.
            
            USER INTENT:
            Primary Goal: """ + intent.primaryGoal + """
            Complexity: """ + intent.complexity + """
            Domain: """ + intent.domain + """
            Artifact Type: """ + intent.artifactType + """
            Requires Framework: """ + intent.requiresFramework + """
            Abstraction Level: """ + intent.abstractionLevel + """
            Key Features: """ + String.join(", ", intent.keyFeatures) + """
            Avoidances: """ + String.join(", ", intent.avoidances) + """
            User Skill Level: """ + intent.userSkillLevel + """
            
            Based on this intent, determine:
            
            1. What prompt format will work best for this intent?
               Options: [STEP_BY_STEP, JSON_SCHEMA, SIMPLE_TEXT, CODE_ONLY]
            
            2. What fields should we ask the LLM to generate?
            
            3. What examples should we provide?
            
            4. What constraints should we enforce?
            
            5. How many siblings should we generate?
            
            6. What should be the prompt's "tone" (instructional, conversational, technical)?
            
            Return ONLY valid JSON, no explanation:
            {
                "format": "STEP_BY_STEP",
                "fields": ["class_name", "method_name", "implementation_code"],
                "examples": ["Example: Printer class with print method"],
                "constraints": ["No external libraries", "Single file"],
                "siblingCount": 3,
                "tone": "instructional",
                "promptTemplate": "Generate a Java class that {goal}.",
                "expectedOutputFormat": "Plain Java code",
                "validationRules": ["Must compile", "Must have main method"]
            }
            """;
    }
    
    private PromptStrategy parseOptimizationResponse(String response, IntentProfile intent) {
        JSONObject obj = extractJson(response);
        PromptStrategy strategy = new PromptStrategy();
        strategy.intent = intent;
        
        if (obj == null) {
            return createDefaultStrategy(intent);
        }
        
        strategy.format = obj.optString("format", "STEP_BY_STEP");
        strategy.siblingCount = obj.optInt("siblingCount", 3);
        strategy.tone = obj.optString("tone", "instructional");
        strategy.promptTemplate = obj.optString("promptTemplate", "Generate a Java class that {goal}.");
        strategy.expectedOutputFormat = obj.optString("expectedOutputFormat", "Java code");
        
        JSONArray fields = obj.optJSONArray("fields");
        if (fields != null) {
            for (int i = 0; i < fields.length(); i++) {
                strategy.fields.add(fields.getString(i));
            }
        } else {
            // Default fields
            strategy.fields.add("class_name");
            strategy.fields.add("implementation_code");
        }
        
        JSONArray examples = obj.optJSONArray("examples");
        if (examples != null) {
            for (int i = 0; i < examples.length(); i++) {
                strategy.examples.add(examples.getString(i));
            }
        } else {
            strategy.examples.add("Printer class with System.out.println");
        }
        
        JSONArray constraints = obj.optJSONArray("constraints");
        if (constraints != null) {
            for (int i = 0; i < constraints.length(); i++) {
                strategy.constraints.add(constraints.getString(i));
            }
        } else {
            strategy.constraints.add("No external libraries");
            strategy.constraints.add("Single file");
        }
        
        JSONArray validationRules = obj.optJSONArray("validationRules");
        if (validationRules != null) {
            for (int i = 0; i < validationRules.length(); i++) {
                strategy.validationRules.add(validationRules.getString(i));
            }
        } else {
            strategy.validationRules.add("Must compile");
            strategy.validationRules.add("Must have main method");
        }
        
        return strategy;
    }
    
    private PromptStrategy createDefaultStrategy(IntentProfile intent) {
        PromptStrategy strategy = new PromptStrategy();
        strategy.intent = intent;
        strategy.format = "STEP_BY_STEP";
        strategy.siblingCount = 3;
        strategy.tone = "instructional";
        strategy.promptTemplate = "Generate a Java class that prints text to the console.";
        strategy.expectedOutputFormat = "Java code";
        strategy.fields.add("class_name");
        strategy.fields.add("implementation_code");
        strategy.examples.add("Printer class with System.out.println");
        strategy.constraints.add("No external libraries");
        strategy.constraints.add("Single file");
        strategy.validationRules.add("Must compile");
        strategy.validationRules.add("Must have main method");
        return strategy;
    }
    
	@Override
	protected String getAgentInstructions() {
		// TODO Auto-generated method stub
		return null;
	}
}