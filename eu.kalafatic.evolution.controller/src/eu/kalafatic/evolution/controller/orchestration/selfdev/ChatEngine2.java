//package eu.kalafatic.evolution.controller.orchestration.selfdev;
//
//import java.util.ArrayList;
//import java.util.List;
//
//import eu.kalafatic.evolution.controller.orchestration.IterationManager;
//import eu.kalafatic.evolution.controller.orchestration.TaskContext;
//import eu.kalafatic.evolution.controller.orchestration.cognitive.CapabilityType;
//import eu.kalafatic.evolution.controller.orchestration.goal.GoalModel;
//import eu.kalafatic.evolution.model.orchestration.EvaluationResult;
//
///**
// * CHAT Darwin Engine - Handles conversational interactions.
// * 
// * Characteristics:
// * - Minimal evolution (1-2 branches)
// * - No Git worktree
// * - TALK actions (not WRITE)
// * - Fast, lightweight
// */
//public class ChatEngine2 extends AbstractDarwinEngine {
//
//    public ChatEngine2(TaskContext context, IterationMemoryService memoryService) {
//        super(context, memoryService);
//        setExecutionProfile(EvolutionProfile.create(CapabilityType.CHAT, 1));
//    }
//
//    @Override
//    public EvaluationResult runIteration(GoalModel goal, IterationManager manager) throws Exception {
//        context.log("[CHAT_ENGINE] Running chat iteration for: " + goal.getPrimaryAction());
//
//        // 1. Generate chat variants
//        List<BranchVariant> variants = generateVariants(goal, manager);
//
//        if (variants.isEmpty()) {
//            return failedResult("No chat responses generated");
//        }
//
//        // 2. Validate variants
//        variants = validateVariants(variants, manager);
//
//        if (variants.isEmpty()) {
//            return failedResult("No valid chat responses after validation");
//        }
//
//        // 3. Select the best response
//        BranchVariant winner = selectBestVariant(variants);
//
//        // 4. Execute (just return the response)
//        return executeWinner(winner, manager);
//    }
//
//    @Override
//    public List<BranchVariant> generateVariants(GoalModel goal, IterationManager manager) throws Exception {
//        List<BranchVariant> variants = new ArrayList<>();
//
//        String request = goal.getPrimaryAction();
//
//        // System instruction for chat responses
//        String systemInstruction =
//                "You are a friendly, helpful AI assistant specializing in software development. " +
//                        "RESPOND CONVERSATIONALLY. DO NOT generate code. DO NOT create files. " +
//                        "DO NOT mention Java classes or methods. Just respond naturally as a helpful assistant.";
//
//        // === Variant 1: Standard friendly response ===
//        String chatPrompt1 = String.format(
//                "%s\n\nUser said: \"%s\"\n\nRespond naturally. Be friendly and helpful.",
//                systemInstruction, request
//        );
//
//        String response1 = aiService.sendRequest(
//                context.getOrchestrator(),
//                chatPrompt1,
//                context
//        );
//
//        BranchVariant variant1 = createChatVariant(response1);
//        variant1.setId("chat-variant-1-" + System.currentTimeMillis());
//        variant1.setStrategy("Friendly Chat Response");
//        variant1.setScore(0.95);
//        variants.add(variant1);
//
//        // === Variant 2: Alternative response ===
//        String chatPrompt2 = String.format(
//                "%s\n\nUser said: \"%s\"\n\nProvide a different but equally friendly response. " +
//                        "Be helpful and encouraging.",
//                systemInstruction, request
//        );
//
//        String response2 = aiService.sendRequest(
//                context.getOrchestrator(),
//                chatPrompt2,
//                context
//        );
//
//        BranchVariant variant2 = createChatVariant(response2);
//        variant2.setId("chat-variant-2-" + System.currentTimeMillis());
//        variant2.setStrategy("Alternative Chat Response");
//        variant2.setScore(0.90);
//        variants.add(variant2);
//
//        context.log("[CHAT_ENGINE] Generated " + variants.size() + " conversational variants.");
//        return variants;
//    }
//
//    @Override
//    public List<BranchVariant> validateVariants(List<BranchVariant> variants, IterationManager manager) {
//        List<BranchVariant> valid = new ArrayList<>();
//        for (BranchVariant v : variants) {
//            if (v.getActions() != null && !v.getActions().isEmpty()) {
//                boolean hasTalkAction = v.getActions().stream()
//                        .anyMatch(a -> "TALK".equals(a.getOperation()));
//                if (hasTalkAction) {
//                    valid.add(v);
//                }
//            }
//        }
//        return valid;
//    }
//
//    @Override
//    public EvaluationResult executeWinner(BranchVariant winner, IterationManager manager) throws Exception {
//        context.log("[CHAT_ENGINE] Executing winner chat response");
//
//        // Extract the response
//        String response = winner.getActions().stream()
//                .filter(a -> "TALK".equals(a.getOperation()))
//                .map(a -> a.getImplementation())
//                .findFirst()
//                .orElse("Hello! How can I help you today?");
//
//        // Store response
//        context.getOrchestrationState().getMetadata().put("chatResponse", response);
//        context.getOrchestrationState().getMetadata().put("chatResponseId", winner.getId());
//
//        // Save lineage
//        saveLineage(winner, "SUCCESS", response);
//
//        return successResult();
//    }
//
//    @Override
//    public CapabilityType getCapabilityType() {
//        return CapabilityType.CHAT;
//    }
//
//    @Override
//    public String getMode() {
//        return "CHAT";
//    }
//
//    // ============================================================
//    // PRIVATE HELPERS
//    // ============================================================
//
//    private BranchVariant createChatVariant(String response) {
//        BranchVariant v = new BranchVariant();
//        v.setId("chat-variant-" + System.currentTimeMillis());
//        v.setBranchId(v.getId());
//        v.setLineageId(context.getSessionId());
//        v.setStrategy("Chat Response");
//        v.setStrategyType("CHAT_RESPONSE");
//        v.setActivationState(BranchVariant.ActivationState.ARCHIVED);
//        v.setScore(0.95);
//        v.setSuccess(true);
//        v.setSurvivalArgument("LLM-generated conversational response");
//        v.setTradeoffs("Minimal evolution, no code generation");
//        v.setMutationTrace(response);
//        v.setReasoningLevel(BranchVariant.ReasoningLevel.MINIMAL);
//        v.setArchitectureEnabled(false);
//        v.setImplementationEnabled(false);
//        v.setSemanticAnchor("Conversational response to user input");
//        v.setBranchName("chat/" + v.getId());
//
//        BranchVariant.Action action = new BranchVariant.Action();
//        action.setDomain("chat");
//        action.setOperation("TALK");
//        action.setTarget("conversation");
//        action.setImplementation(response);
//        action.setDescription("Chat response");
//        v.getActions().add(action);
//
//        return v;
//    }
//
//    private void saveLineage(BranchVariant variant, String result, String mutationTrace) {
//        IterationRecord record = new IterationRecord();
//        record.setIteration(context.getOrchestrationState().getIterationCount());
//        record.setGoal("Chat");
//        record.setStrategy(variant.getStrategy());
//        record.setBranchId(variant.getId());
//        record.setResult(result);
//        record.setActivationState("ACTIVE");
//        record.setMutationTrace(mutationTrace);
//        record.setTimestamp(System.currentTimeMillis());
//        context.getKernelContext().getMemoryService().saveRecord(record);
//        context.getKernelContext().getMemoryService().saveEvolutionTree();
//        context.getKernelContext().getMemoryService().flush();
//    }
//}