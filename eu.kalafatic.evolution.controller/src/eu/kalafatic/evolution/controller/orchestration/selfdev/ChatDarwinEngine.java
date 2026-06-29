package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.util.ArrayList;
import java.util.List;

import eu.kalafatic.evolution.controller.agents.PromptIntentAnalyzer;
import eu.kalafatic.evolution.controller.kernel.EvolutionProfile;
import eu.kalafatic.evolution.controller.orchestration.FinalResponse;
import eu.kalafatic.evolution.controller.orchestration.FinalResponseAssembler;
import eu.kalafatic.evolution.controller.orchestration.IterationManager;
import eu.kalafatic.evolution.controller.orchestration.OrchestrationState;
import eu.kalafatic.evolution.controller.orchestration.OrchestratorResponse;
import eu.kalafatic.evolution.controller.orchestration.ResultType;
import eu.kalafatic.evolution.controller.orchestration.SystemState;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.TaskRequest;
import eu.kalafatic.evolution.controller.orchestration.cognitive.CapabilityType;
import eu.kalafatic.evolution.controller.orchestration.goal.GoalModel;
import eu.kalafatic.evolution.controller.trajectory.Trajectory;

/**
 * Chat-specific Darwin Engine implementation.
 * Handles conversational interactions with minimal evolution.
 * 
 * This engine bypasses most of the evolutionary machinery and
 * generates direct conversational responses via LLM.
 */
public class ChatDarwinEngine extends ADarwinEngine {

    public ChatDarwinEngine(TaskContext context, IterationMemoryService memoryService,
                            SystemStateSignalProvider stateProvider) {
        super(context, memoryService, stateProvider);
        context.log("[CHAT] ChatDarwinEngine initialized.");
    }

    @Override
    public String getMode() {
        return "CHAT";
    }

    @Override
    public CapabilityType getCapabilityType() {
        return CapabilityType.CHAT;
    }

    @Override
    protected OrchestratorResponse handleRouting(PromptIntentAnalyzer.IntentResult intent, 
                                                   String request, 
                                                   TaskRequest taskRequest,
                                                   IterationManager iterationManager) throws Exception {
        if (intent.isChat()) {
            context.log("[CHAT] Chat intent detected. Handling directly.");
            return handleChatRequest(request, taskRequest, iterationManager);
        }
        return null;
    }

    /**
     * Handles chat request directly without evolution.
     */
    protected OrchestratorResponse handleChatRequest(String request, 
                                                       TaskRequest taskRequest,
                                                       IterationManager iterationManager) throws Exception {
        context.log("[CHAT] Handling chat request directly.");
        
        OrchestrationState state = context.getOrchestrationState();
        state.getMetadata().put("isChatRequest", true);
        
        // Set CHAT profile
        EvolutionProfile chatProfile = EvolutionProfile.create(CapabilityType.CHAT, 1);
        context.getOrchestrationState().setExecutionProfile(chatProfile);
        
        // Generate chat response directly
        OrchestratorResponse response = new OrchestratorResponse();
        response.setResultType(ResultType.CHAT);
        
        String chatResponseText = generateChatResponse(request);
        response.setSummary(chatResponseText);
        state.getMetadata().put("chatResponse", chatResponseText);
        
        // Transition to DONE
        iterationManager.transition(SystemState.DONE, context);
        
        // Assemble final response
        FinalResponseAssembler assembler = new FinalResponseAssembler();
        FinalResponse finalResponse = assembler.assemble(context, chatResponseText, true, context.getStartTime());
        response.setFinalResponse(finalResponse);
        
        return response;
    }

    /**
     * Generates chat variants with minimal evolution.
     * This is the IMutationContract implementation.
     */
    @Override
    public List<BranchVariant> generateVariants(GoalModel goal, StateSnapshot snapshot, 
                                                 FailureMemory failureMemory, 
                                                 Trajectory trajectory, 
                                                 EvolutionaryPressureVector pressure) throws Exception {
        context.log("[CHAT] Generating chat variants for: " + goal.getPrimaryAction());
        
        String request = goal.getPrimaryAction();
        int expansionValue = getExpansionValue();
        int limit = expansionValue <= 5 ? 1 : 2;
        
        List<BranchVariant> variants = new ArrayList<>();
        
        for (int i = 0; i < limit; i++) {
            String strategy = i == 0 ? "Friendly Chat Response" : "Alternative Chat Response";
            String response = generateChatVariant(request, i);
            
            BranchVariant variant = createChatVariant(response, strategy);
            variant.setId("chat-variant-" + (i + 1) + "-" + System.currentTimeMillis());
            variant.setScore(0.95 - (i * 0.05));
            
            // Add to evolution tree with chat-specific node
            EvolutionTree tree = context.getKernelContext().getMemoryService().getEvolutionTree();
            EvolutionNode node = new EvolutionNode();
            node.setId(variant.getId());
            node.setStrategy(strategy);
            node.setStatus("CHAT");
            node.setIteration(context.getOrchestrationState().getIterationCount() + 1);
            tree.addNode(node);
            
            variants.add(variant);
        }
        
        context.log("[CHAT] Generated " + variants.size() + " chat variants.");
        return variants;
    }

    // ============================================================
    // PRIVATE HELPER METHODS
    // ============================================================

    /**
     * Generates a chat response variant via LLM.
     */
    private String generateChatVariant(String request, int index) {
        try {
            String systemInstruction = 
                "You are a friendly, helpful AI assistant. " +
                "RESPOND CONVERSATIONALLY. DO NOT generate code. " +
                "Just respond naturally as a helpful assistant. " +
                "Keep responses brief (1-2 sentences) and friendly.";
            
            String instruction = index == 0 ? 
                "Respond naturally. Be friendly and helpful." :
                "Provide a different but equally friendly response. Be helpful and encouraging.";
            
            String prompt = String.format(
                "%s\n\nUser said: \"%s\"\n\n%s",
                systemInstruction, request, instruction
            );
            
            return aiService.sendRequest(
                context.getOrchestrator(),
                prompt,
                context
            );
        } catch (Exception e) {
            context.log("[CHAT] Response generation failed: " + e.getMessage());
            return index == 0 ? 
                "Hello! How can I help you today?" :
                "I'm here to help! What would you like to know?";
        }
    }

    /**
     * Creates a chat branch variant from a response string.
     */
    private BranchVariant createChatVariant(String response, String strategy) {
        BranchVariant v = new BranchVariant();
        v.setId("chat-variant-" + System.currentTimeMillis());
        v.setBranchId(v.getId());
        v.setLineageId(context.getSessionId());
        v.setStrategy(strategy);
        v.setStrategyType("CHAT_RESPONSE");
        v.setActivationState(BranchVariant.ActivationState.ARCHIVED);
        v.setScore(0.95);
        v.setSuccess(true);
        v.setSurvivalArgument("LLM-generated conversational response");
        v.setTradeoffs("Minimal evolution, no code generation");
        v.setMutationTrace(response);
        v.setReasoningLevel(BranchVariant.ReasoningLevel.MINIMAL);
        v.setArchitectureEnabled(false);
        v.setImplementationEnabled(false);
        v.setSemanticAnchor("Conversational response to user input");
        v.setBranchName("chat/" + v.getId() + "-" + System.currentTimeMillis());
        
        BranchVariant.Action action = new BranchVariant.Action();
        action.setDomain("chat");
        action.setOperation("TALK");
        action.setTarget("conversation");
        action.setImplementation(response);
        action.setDescription("Chat response");
        v.getActions().add(action);
        
        return v;
    }

	@Override
	protected void initializeMode() throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void cleanupMode() throws Exception {
		// TODO Auto-generated method stub
		
	}
}