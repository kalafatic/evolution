package eu.kalafatic.evolution.controller.orchestration.selfdev;

import eu.kalafatic.evolution.controller.agents.BaseAiAgent;
import eu.kalafatic.evolution.controller.orchestration.SessionContainer;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import org.json.JSONArray;
import org.json.JSONObject;

public class IntentAnalyzer2 extends BaseAiAgent {
    
    public IntentAnalyzer2(String id, String type, SessionContainer container) {
		super(id, type, container);
	}

	public IntentProfile analyzeIntent(String userRequest, TaskContext context) throws Exception {
		instructions = buildIntentAnalysisPrompt(userRequest);
        String response = aiService.sendRequest(context.getOrchestrator(), instructions, context);
        
        return parseIntentResponse(response);
    }
    
    private String buildIntentAnalysisPrompt(String userRequest) {
        return """
            Analyze this user request and extract the true intent.
            
            USER REQUEST:
            """ + userRequest + """
            
            Answer these questions about what the user ACTUALLY wants:
            
            1. What is the primary goal? (one sentence)
            2. What is the complexity level? (SIMPLE, MEDIUM, COMPLEX)
            3. What domain is this in? (Java, Python, Web, API, etc.)
            4. What artifact should be produced? (single class, multiple classes, service, etc.)
            5. Does the user want a framework? (yes/no)
            6. What abstraction level? (IMPLEMENTATION, DESIGN, ARCHITECTURE)
            7. What are the key features needed? (list)
            8. What should be avoided? (list)
            
            Return as JSON:
            {
                "primaryGoal": "...",
                "complexity": "SIMPLE",
                "domain": "JAVA",
                "artifactType": "single class with main method",
                "requiresFramework": false,
                "abstractionLevel": "IMPLEMENTATION",
                "keyFeatures": ["feature1", "feature2"],
                "avoidances": ["spring", "microservices"],
                "userSkillLevel": "BEGINNER",  // INFERRED from phrasing
                "ambiguityScore": 0.2         // 0-1, how ambiguous is the request
            }
            """;
    }
    
    private IntentProfile parseIntentResponse(String response) {
        JSONObject obj = extractJson(response);
        IntentProfile profile = new IntentProfile();
        profile.primaryGoal = obj.optString("primaryGoal");
        profile.complexity = obj.optString("complexity");
        profile.domain = obj.optString("domain");
        profile.artifactType = obj.optString("artifactType");
        profile.requiresFramework = obj.optBoolean("requiresFramework");
        profile.abstractionLevel = obj.optString("abstractionLevel");
        profile.ambiguityScore = obj.optDouble("ambiguityScore", 0.5);
        
        JSONArray features = obj.optJSONArray("keyFeatures");
        if (features != null) {
            for (int i = 0; i < features.length(); i++) {
                profile.keyFeatures.add(features.getString(i));
            }
        }
        
        JSONArray avoidances = obj.optJSONArray("avoidances");
        if (avoidances != null) {
            for (int i = 0; i < avoidances.length(); i++) {
                profile.avoidances.add(avoidances.getString(i));
            }
        }
        
        profile.userSkillLevel = inferSkillLevel(response);
        
        return profile;
    }
    
    private String inferSkillLevel(String response) {
        String lower = response.toLowerCase();
        if (lower.contains("beginner") || lower.contains("simple") || lower.contains("basic")) {
            return "BEGINNER";
        }
        if (lower.contains("advanced") || lower.contains("complex") || lower.contains("enterprise")) {
            return "ADVANCED";
        }
        return "INTERMEDIATE";
    }

	@Override
	protected String getAgentInstructions() {
		return instructions;
	}
}