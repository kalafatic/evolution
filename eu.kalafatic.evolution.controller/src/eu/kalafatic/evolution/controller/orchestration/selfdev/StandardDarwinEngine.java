package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.util.List;

import eu.kalafatic.evolution.controller.orchestration.AiService;
import eu.kalafatic.evolution.controller.orchestration.IterationManager;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.goal.GoalModel;
import eu.kalafatic.evolution.model.orchestration.EvaluationResult;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.SelfDevDecision;

/**
 * STANDARD Darwin Engine - Handles code generation, implementation.
 * This is the original Darwin behavior.
 */
public class StandardDarwinEngine extends BaseDarwinEngine {
    
    private final AiService aiService;
    private final SiblingGenerationManager siblingManager;
    private final DarwinDiversityAnalyzer diversityAnalyzer;
    
    public StandardDarwinEngine(TaskContext context, IterationMemoryService memoryService, AiService aiService) {
        super(context, memoryService);
        this.aiService = aiService;
        this.siblingManager = new SiblingGenerationManager(context.getSessionContainer(), aiService);
        this.diversityAnalyzer = new DarwinDiversityAnalyzer();
    }
    
    @Override
    public EvaluationResult runIteration(GoalModel goal, IterationManager manager) throws Exception {
        context.log("[STANDARD_DARWIN] Running code generation iteration");
        
        // 1. Generate code variants
        List<BranchVariant> variants = generateVariants(goal, manager);
        
        if (variants.isEmpty()) {
            context.log("[STANDARD_DARWIN] No code variants generated.");
            return failedResult("No code variants generated");
        }
        
        // 2. Validate code variants (compilation, etc.)
        variants = validateVariants(variants, manager);
        
        if (variants.isEmpty()) {
            context.log("[STANDARD_DARWIN] No code variants passed validation.");
            return failedResult("No valid code variants after validation");
        }
        
        // 3. Select the best variant
        BranchVariant winner = selectBestVariant(variants);
        
        // 4. Execute (generate the code)
        return executeWinner(winner, manager);
    }
    
    @Override
	public List<BranchVariant> generateVariants(GoalModel goal, IterationManager manager) throws Exception {
        // Use existing SiblingGenerationManager for code generation
        // This is the existing code generation logic
        return manager.getDarwinEngine().generateProposals(context, goal, manager);
    }
    
    @Override
	public List<BranchVariant> validateVariants(List<BranchVariant> variants, IterationManager manager) {
        // Existing validation logic
        // Check for actions, compilation, etc.
        List<BranchVariant> valid = new java.util.ArrayList<>();
        for (BranchVariant v : variants) {
            if (v.getActions() != null && !v.getActions().isEmpty()) {
                valid.add(v);
            }
        }
        return valid;
    }
    
    @Override
	public EvaluationResult executeWinner(BranchVariant winner, IterationManager manager) throws Exception {
        context.log("[STANDARD_DARWIN] Executing winner code variant");
        
        // Apply the code changes
        boolean success = manager.executeTasksWithRetries(winner.getActions());
        
        EvaluationResult result = OrchestrationFactory.eINSTANCE.createEvaluationResult();
        result.setSuccess(success);
        result.setDecision(success ? SelfDevDecision.CONTINUE : SelfDevDecision.ROLLBACK);
        
        return result;
    }
    
    protected BranchVariant selectBestVariant(List<BranchVariant> variants) {
        return variants.stream()
            .max((v1, v2) -> Double.compare(v1.getScore(), v2.getScore()))
            .orElse(variants.get(0));
    }
    
    protected EvaluationResult failedResult(String reason) {
        EvaluationResult result = OrchestrationFactory.eINSTANCE.createEvaluationResult();
        result.setSuccess(false);
        result.setDecision(SelfDevDecision.ROLLBACK);
        result.getErrors().add(reason);
        return result;
    }
    
    @Override
    public String getMode() { return "STANDARD"; }
}