package eu.kalafatic.evolution.controller.orchestration.selfdev;

import org.json.JSONArray;
import org.json.JSONObject;

import eu.kalafatic.evolution.controller.agents.BaseAiAgent;
import eu.kalafatic.evolution.controller.orchestration.SessionContainer;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;

public class PromptOptimizer extends BaseAiAgent {
	
    
    public PromptOptimizer(SessionContainer container) {
		super("PromptOptimizer", "PromptOptimizer", container);
	}

	public PromptStrategy optimizePrompt(IntentProfile intent, TaskContext context) throws Exception {
		instructions = buildOptimizationPrompt(intent);
        String response = aiService.sendRequest(context.getOrchestrator(), instructions, context);
        return parseOptimizationResponse(response, intent);
    }
	
	@Override
	protected String getAgentInstructions() {
		return instructions;
	}
    
    private String buildOptimizationPrompt(IntentProfile intent) {
        return """
            Given this user intent, design the optimal prompt format for resolving the goal.
            
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
            
            Return as JSON:
            {
                "format": "STEP_BY_STEP",
                "fields": ["field1", "field2"],
                "examples": ["Example 1", "Example 2"],
                "constraints": ["Constraint 1", "Constraint 2"],
                "siblingCount": 3,
                "tone": "instructional/conversational/technical",
                "promptTemplate": "Instructions for {goal}...",
                "expectedOutputFormat": "[description of format]",
                "validationRules": ["Rule 1", "Rule 2"]
            }
            """;
    }
        
    private PromptStrategy parseOptimizationResponse(String response, IntentProfile intent) {
        JSONObject obj = extractJson(response);
        PromptStrategy strategy = new PromptStrategy();
        strategy.intent = intent;
        strategy.format = obj.optString("format");
        strategy.siblingCount = obj.optInt("siblingCount", 3);
        strategy.tone = obj.optString("tone", "instructional");
        strategy.promptTemplate = obj.optString("promptTemplate");
        strategy.expectedOutputFormat = obj.optString("expectedOutputFormat");
        
        JSONArray fields = obj.optJSONArray("fields");
        if (fields != null) {
            for (int i = 0; i < fields.length(); i++) {
                strategy.fields.add(fields.getString(i));
            }
        }
        
        JSONArray examples = obj.optJSONArray("examples");
        if (examples != null) {
            for (int i = 0; i < examples.length(); i++) {
                strategy.examples.add(examples.getString(i));
            }
        }
        
        JSONArray constraints = obj.optJSONArray("constraints");
        if (constraints != null) {
            for (int i = 0; i < constraints.length(); i++) {
                strategy.constraints.add(constraints.getString(i));
            }
        }
        
        JSONArray validationRules = obj.optJSONArray("validationRules");
        if (validationRules != null) {
            for (int i = 0; i < validationRules.length(); i++) {
                strategy.validationRules.add(validationRules.getString(i));
            }
        }
        
        return strategy;
    }

	
}