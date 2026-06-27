package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.util.ArrayList;
import java.util.List;

import eu.kalafatic.evolution.controller.orchestration.AiService;
import eu.kalafatic.evolution.controller.orchestration.IterationManager;
import eu.kalafatic.evolution.controller.orchestration.OrchestratorResponse;
import eu.kalafatic.evolution.controller.orchestration.ResultType;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.goal.GoalModel;
import eu.kalafatic.evolution.controller.orchestration.intent.EvolutionAssessment;
import eu.kalafatic.evolution.model.orchestration.EvaluationResult;

/**
 * CHAT Darwin Engine - Handles conversations, simple Q&A.
 * Does NOT generate code. Focuses on understanding and response.
 */
public class ChatDarwinEngine extends BaseDarwinEngine {
    
    private final AiService aiService;
    
    public ChatDarwinEngine(TaskContext context, IterationMemoryService memoryService, AiService aiService) {
        super(context, memoryService);
        this.aiService = aiService;
    }
    
    @Override
    public EvaluationResult runIteration(GoalModel goal, IterationManager manager) throws Exception {
        context.log("[CHAT_DARWIN] Running chat iteration for: " + goal.getPrimaryAction());
        
        // 1. Generate chat responses
        List<BranchVariant> variants = generateVariants(goal, manager);
        
        if (variants.isEmpty()) {
            context.log("[CHAT_DARWIN] No chat responses generated.");
            return failedResult("No chat responses generated");
        }
        
        // 2. Validate responses
        variants = validateVariants(variants, manager);
        
        if (variants.isEmpty()) {
            context.log("[CHAT_DARWIN] No chat responses passed validation.");
            return failedResult("No valid chat responses after validation");
        }
        
        // 3. Select the best response
        BranchVariant winner = selectBestVariant(variants);
        
        // 4. Execute (just return the response)
        return executeWinner(winner, manager);
    }
    
    @Override
    public List<BranchVariant> generateVariants(GoalModel goal, IterationManager manager) throws Exception {
        List<BranchVariant> variants = new ArrayList<>();
        
        String[] responseStyles = {
            "Provide a clear, concise answer.",
            "Provide a detailed, comprehensive answer.",
            "Provide a thoughtful, analytical answer."
        };
        
        for (int i = 0; i < responseStyles.length; i++) {
            BranchVariant variant = new BranchVariant();
            variant.setId("chat-variant-" + System.currentTimeMillis() + "-" + i);
            variant.setStrategy("Chat Response " + (i + 1));
            variant.setStrategyType("CHAT_RESPONSE");
            variant.setSemanticAnchor(responseStyles[i]);
            
            // Generate response using AI service
            String response = aiService.sendSimplePrompt(
                "User asked: " + goal.getPrimaryAction() + "\n\n" +
                "Respond with: " + responseStyles[i] + "\n" +
                "Keep it conversational and helpful."
            );
            
            variant.setChatResponse(response);
            variant.setScore(0.7 + (i * 0.05)); // Slightly different scores
            variants.add(variant);
        }
        
        return variants;
    }
    
    @Override
    public OrchestratorResponse evolve(String request, IterationManager manager, EvolutionAssessment initialAssessment) throws Exception {
        // Chat-specific evolution
        context.log("[CHAT_DARWIN] Starting chat evolution for: " + request);
        
        // Get or create goal model
        GoalModel goal = (GoalModel) context.getOrchestrationState().getMetadata().get("goalModel");
        if (goal == null) {
            goal = manager.getGoalUnderstandingEngine().understand(request, context);
            context.getOrchestrationState().getMetadata().put("goalModel", goal);
        }
        
        // Run the chat iteration
        EvaluationResult result = runIteration(goal, manager);
        
        OrchestratorResponse response = new OrchestratorResponse();
        response.setResultType(ResultType.CHAT);
        response.setSummary(result.isSuccess() ? "Chat evolution successful" : "Chat evolution failed");
        return response;
    }
    
    @Override
    public List<BranchVariant> validateVariants(List<BranchVariant> variants, IterationManager manager) {
        List<BranchVariant> valid = new ArrayList<>();
        for (BranchVariant v : variants) {
            if (v.getChatResponse() != null && !v.getChatResponse().isEmpty()) {
                valid.add(v);
            } else {
                context.log("[CHAT_DARWIN] Invalid chat variant: " + v.getId());
            }
        }
        return valid;
    }
    
    @Override
    public EvaluationResult executeWinner(BranchVariant winner, IterationManager manager) throws Exception {
        context.log("[CHAT_DARWIN] Executing winner chat response");
        
        // Store the response in context
        context.getOrchestrationState().getMetadata().put("chatResponse", winner.getChatResponse());
        context.getOrchestrationState().getMetadata().put("chatResponseId", winner.getId());
        
        return successResult("Chat response generated: " + winner.getStrategy());
    }
    
    @Override
    public String getMode() {
        return "CHAT";
    }
}