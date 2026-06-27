package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import eu.kalafatic.evolution.controller.agents.BaseAiAgent;
import eu.kalafatic.evolution.controller.orchestration.AiService;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.SessionContainer;

public class IntentAnalyzer extends BaseAiAgent {
    
    private AiService aiService;
    
    public IntentAnalyzer(SessionContainer container) {
        super("IntentAnalyzer", "IntentAnalyzer", container);
    }
    
    @Override
    public void setAiService(AiService aiService) {
        super.setAiService(aiService);
        this.aiService = aiService;
    }
    
    @Override
    protected String getAgentInstructions() {
        return "You are an Intent Analyzer. Your task is to understand what the user actually wants.";
    }
    
    public IntentProfile analyzeIntent(String userRequest, TaskContext context) {
        try {
            String prompt = buildIntentAnalysisPrompt(userRequest);
            String response = aiService.sendRequest(context.getOrchestrator(), prompt, context);
            return parseIntentResponse(response, userRequest, context);
        } catch (Exception e) {
            if (context != null) {
                context.log("[INTENT_ANALYZER] Failed to analyze intent: " + e.getMessage());
            }
            return createDefaultProfile(userRequest);
        }
    }
    
    private String buildIntentAnalysisPrompt(String userRequest) {
        return """
            Analyze this user request and extract the true intent.
            
            USER REQUEST:
            """ + userRequest + """
            
            Answer these questions about what the user ACTUALLY wants:
            
            1. What is the primary goal? (one sentence)
            2. What is the complexity level? (SIMPLE, MEDIUM, COMPLEX)
            3. What domain is this in? (JAVA, PYTHON, WEB, API, etc.)
            4. What artifact should be produced? (single class, multiple classes, service, etc.)
            5. Does the user want a framework? (yes/no)
            6. What abstraction level? (IMPLEMENTATION, DESIGN, ARCHITECTURE)
            7. What are the key features needed? (list)
            8. What should be avoided? (list)
            
            Return ONLY valid JSON, no explanation:
            {
                "primaryGoal": "Create a Java class that prints text",
                "complexity": "SIMPLE",
                "domain": "JAVA",
                "artifactType": "single class with main method",
                "requiresFramework": false,
                "abstractionLevel": "IMPLEMENTATION",
                "keyFeatures": ["print text", "console output"],
                "avoidances": ["spring", "microservices", "external libraries"],
                "ambiguityScore": 0.2
            }
            """;
    }
    
    private IntentProfile parseIntentResponse(String response, String userRequest, TaskContext context) {
        // Use parent's extractJson method
        JSONObject obj = extractJson(response);
        
        if (obj == null) {
            if (context != null) {
                context.log("[INTENT_ANALYZER] Failed to parse JSON from response");
            }
            return createDefaultProfile(userRequest);
        }
        
        IntentProfile profile = new IntentProfile();
        profile.primaryGoal = obj.optString("primaryGoal", "Create Java class that prints text");
        profile.complexity = obj.optString("complexity", "SIMPLE");
        profile.domain = obj.optString("domain", "JAVA");
        profile.artifactType = obj.optString("artifactType", "single class with main method");
        profile.requiresFramework = obj.optBoolean("requiresFramework", false);
        profile.abstractionLevel = obj.optString("abstractionLevel", "IMPLEMENTATION");
        profile.ambiguityScore = obj.optDouble("ambiguityScore", 0.5);
        
        JSONArray features = obj.optJSONArray("keyFeatures");
        if (features != null) {
            for (int i = 0; i < features.length(); i++) {
                profile.keyFeatures.add(features.getString(i));
            }
        } else {
            profile.keyFeatures.add("print text");
            profile.keyFeatures.add("console output");
        }
        
        JSONArray avoidances = obj.optJSONArray("avoidances");
        if (avoidances != null) {
            for (int i = 0; i < avoidances.length(); i++) {
                profile.avoidances.add(avoidances.getString(i));
            }
        } else {
            profile.avoidances.add("external frameworks");
            profile.avoidances.add("complex dependencies");
        }
        
        profile.userSkillLevel = inferSkillLevel(response);
        
        return profile;
    }
    
    private IntentProfile createDefaultProfile(String userRequest) {
        IntentProfile profile = new IntentProfile();
        profile.primaryGoal = userRequest != null && !userRequest.isEmpty() ? userRequest : "Create Java class";
        profile.complexity = "SIMPLE";
        profile.domain = "JAVA";
        profile.artifactType = "single class with main method";
        profile.requiresFramework = false;
        profile.abstractionLevel = "IMPLEMENTATION";
        profile.ambiguityScore = 0.5;
        profile.keyFeatures.add("print text");
        profile.keyFeatures.add("console output");
        profile.avoidances.add("external frameworks");
        profile.avoidances.add("complex dependencies");
        profile.userSkillLevel = "BEGINNER";
        return profile;
    }
    
    private String inferSkillLevel(String response) {
        if (response == null) return "BEGINNER";
        String lower = response.toLowerCase();
        if (lower.contains("beginner") || lower.contains("simple") || lower.contains("basic")) {
            return "BEGINNER";
        }
        if (lower.contains("advanced") || lower.contains("complex") || lower.contains("enterprise")) {
            return "ADVANCED";
        }
        return "INTERMEDIATE";
    }
}