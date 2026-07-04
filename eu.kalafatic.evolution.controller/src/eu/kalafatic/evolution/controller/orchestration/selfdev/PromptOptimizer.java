package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import eu.kalafatic.evolution.controller.agents.BaseAiAgent;
import eu.kalafatic.evolution.controller.orchestration.SessionContainer;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.util.ModeRecognizer;

public class PromptOptimizer extends BaseAiAgent {
	
    
    public PromptOptimizer(SessionContainer container) {
		super("PromptOptimizer", "PromptOptimizer", container);
	}

	public PromptStrategy optimizePrompt(IntentProfile intent, TaskContext context) throws Exception {
	    PromptStrategy strategy = new PromptStrategy();
	    strategy.intent = intent;
	    
	    // Check if mediated mode
	    boolean isMediated = ModeRecognizer.isMediatedMode(context);
	    
	    if (isMediated) {
	        strategy.format = "MEDIATED";
	        strategy.tone = "analytical";
	        strategy.constraints = Arrays.asList(
	            "NO code generation",
	            "Focus on architectural analysis",
	            "Identify 8-16 critical files",
	            "Provide optimized prompt for external LLM"
	        );
	        strategy.validationRules = Arrays.asList(
	            "Must include ARCHITECTURE_SUMMARY",
	            "Must include CRITICAL_FILES with 8-16 files",
	            "Must include OPTIMIZED_PROMPT"
	        );
	        return strategy;
	    }
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
            
            5. What should be the prompt's "tone" (instructional, conversational, technical)?
            
            Return as JSON:
            {
                "format": "STEP_BY_STEP",
                "fields": ["field1", "field2"],
                "examples": ["Example 1", "Example 2"],
                "constraints": ["Constraint 1", "Constraint 2"],
                "tone": "instructional/conversational/technical",
                "promptTemplate": "Instructions for {goal}...",
                "expectedOutputFormat": "[description of format]",
                "validationRules": ["Rule 1", "Rule 2"]
            }
            """;
    }
        
    private PromptStrategy parseOptimizationResponse(String response, IntentProfile intent) {    	
    	 if (response == null || response.isEmpty()) {
             return createDefaultStrategy(intent);
         }
         
         JSONObject obj = extractJson(response);
         
         if (obj == null) {
             return createDefaultStrategy(intent);
         }
       
        PromptStrategy strategy = new PromptStrategy();
        strategy.intent = intent;
        strategy.format = obj.optString("format");
        strategy.tone = obj.optString("tone", "instructional");
        strategy.promptTemplate = obj.optString("promptTemplate");
        strategy.expectedOutputFormat = obj.optString("expectedOutputFormat");
        
        JSONArray fields = obj.optJSONArray("fields");
        if (fields != null) {
            for (int i = 0; i < fields.length(); i++) {
                strategy.fields.add(fields.optString(i));
            }
        }
        
        JSONArray examples = obj.optJSONArray("examples");
        if (examples != null) {
            for (int i = 0; i < examples.length(); i++) {
                strategy.examples.add(examples.optString(i));
            }
        }
        
        JSONArray constraints = obj.optJSONArray("constraints");
        if (constraints != null) {
            for (int i = 0; i < constraints.length(); i++) {
                strategy.constraints.add(constraints.optString(i));
            }
        }
        
        JSONArray validationRules = obj.optJSONArray("validationRules");
        if (validationRules != null) {
            for (int i = 0; i < validationRules.length(); i++) {
                strategy.validationRules.add(validationRules.optString(i));
            }
        }
        
        return strategy;
    }

	
    /**
     * Creates a default strategy based on the intent.
     * This is the key method - it uses the intent to generate a sensible default.
     */
    private PromptStrategy createDefaultStrategy(IntentProfile intent) {
        PromptStrategy strategy = new PromptStrategy();
        strategy.intent = intent;
        
        // Derive defaults from intent
        strategy.format = determineDefaultFormat(intent);
        strategy.siblingCount = determineDefaultSiblingCount(intent);
        strategy.tone = determineDefaultTone(intent);
        strategy.promptTemplate = determineDefaultPromptTemplate(intent);
        strategy.expectedOutputFormat = "Java code";
        strategy.fields.addAll(determineDefaultFields(intent));
        strategy.examples.addAll(determineDefaultExamples(intent));
        strategy.constraints.addAll(determineDefaultConstraints(intent));
        strategy.validationRules.addAll(determineDefaultValidationRules(intent));
        
        return strategy;
    }
    
    // ============================================================
    // INTENT-BASED DEFAULT DETERMINATION
    // ============================================================
    
    private String determineDefaultFormat(IntentProfile intent) {
        // Use STEP_BY_STEP for simple tasks with clear steps
        if ("SIMPLE".equals(intent.complexity)) {
            return "STEP_BY_STEP";
        }
        // Use CODE_ONLY for experienced users
        if ("ADVANCED".equals(intent.userSkillLevel)) {
            return "CODE_ONLY";
        }
        // Use JSON_SCHEMA for complex/architecture tasks
        if ("ARCHITECTURE".equals(intent.abstractionLevel) || "COMPLEX".equals(intent.complexity)) {
            return "JSON_SCHEMA";
        }
        return "STEP_BY_STEP";
    }
    
    private int determineDefaultSiblingCount(IntentProfile intent) {
        // More siblings for complex tasks
        if ("COMPLEX".equals(intent.complexity)) {
            return 5;
        }
        if ("HIGH".equals(intent.complexity)) {
            return 4;
        }
        // Fewer for simple tasks
        if ("SIMPLE".equals(intent.complexity)) {
            return 2;
        }
        return 3;
    }
    
    private String determineDefaultTone(IntentProfile intent) {
        String primaryGoal = intent.primaryGoal != null ? intent.primaryGoal : "";
        String domain = intent.domain != null ? intent.domain : "";
        
        if (primaryGoal.toLowerCase().contains("explain") || 
            primaryGoal.toLowerCase().contains("describe") ||
            primaryGoal.toLowerCase().contains("help")) {
            return "conversational";
        }
        if (domain.contains("JAVA") || domain.contains("PYTHON") || domain.contains("CODE")) {
            return "instructional";
        }
        return "instructional";
    }
    
    private String determineDefaultPromptTemplate(IntentProfile intent) {
        // Build template based on intent
        StringBuilder template = new StringBuilder();
        template.append("Generate a ");
        template.append(intent.domain.toLowerCase());
        template.append(" ");
        
        if (intent.artifactType != null && !intent.artifactType.isEmpty()) {
            template.append(intent.artifactType);
        } else {
            template.append("class");
        }
        
        template.append(" that ");
        template.append(intent.primaryGoal.toLowerCase());
        template.append(".");
        
        return template.toString();
    }
    
    private List<String> determineDefaultFields(IntentProfile intent) {
        List<String> fields = new ArrayList<>();
        fields.add("implementation_code");
        
        if (!"CODE_ONLY".equals(determineDefaultFormat(intent))) {
            fields.add("class_name");
            if (intent.keyFeatures != null && intent.keyFeatures.size() > 1) {
                fields.add("method_name");
            }
        }
        
        return fields;
    }
    
    private List<String> determineDefaultExamples(IntentProfile intent) {
        List<String> examples = new ArrayList<>();
        
        // Generate example based on domain
        if (intent.domain.contains("JAVA")) {
            examples.add("Example: Printer class with print(String text) method using System.out.println");
        } else if (intent.domain.contains("PYTHON")) {
            examples.add("Example: Printer class with print(text) method using print()");
        } else {
            examples.add("Example: Simple implementation with clear naming");
        }
        
        return examples;
    }
    
    private List<String> determineDefaultConstraints(IntentProfile intent) {
        List<String> constraints = new ArrayList<>();
        String domain = intent.domain != null ? intent.domain : "";
        
        if (domain.contains("JAVA")) {
            constraints.add("Must be valid Java syntax");
            constraints.add("Follow Java naming conventions");
        }
        
        // Safe boolean check - requiresFramework is primitive boolean, so it's never null
        if (!intent.requiresFramework) {
            constraints.add("No external libraries or frameworks");
        }
        
        if (intent.avoidances != null && !intent.avoidances.isEmpty()) {
            for (String avoidance : intent.avoidances) {
                if (avoidance != null && !avoidance.isEmpty()) {
                    constraints.add("Avoid: " + avoidance);
                }
            }
        }
        
        constraints.add("Single file implementation");
        return constraints;
    }
    
    private List<String> determineDefaultValidationRules(IntentProfile intent) {
        List<String> rules = new ArrayList<>();
        rules.add("Must compile");
        
        if (intent.domain.contains("JAVA")) {
            rules.add("Must have a main method");
        }
        
        return rules;
    }
}