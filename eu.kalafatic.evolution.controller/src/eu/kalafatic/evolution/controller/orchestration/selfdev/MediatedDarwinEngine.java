package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.util.ArrayList;
import java.util.List;

import eu.kalafatic.evolution.controller.orchestration.OrchestratorResponse;
import eu.kalafatic.evolution.controller.mediation.model.Hotspot;
import eu.kalafatic.evolution.controller.mediation.model.MediationCandidate;
import eu.kalafatic.evolution.controller.mediation.model.MediationResult;
import eu.kalafatic.evolution.controller.orchestration.AiService;
import eu.kalafatic.evolution.controller.orchestration.IterationManager;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.goal.GoalModel;
import eu.kalafatic.evolution.controller.orchestration.mediation.MediationEngine;
import eu.kalafatic.evolution.model.orchestration.EvaluationResult;

/**
 * MEDIATED Darwin Engine - Handles LLM refinement package generation.
 * Does NOT generate code. Generates: prompt + selected files + context.
 */
public class MediatedDarwinEngine extends AbstractBaseDarwinEngine {
    
    private final AiService aiService;
    private final MediationEngine mediationEngine;
    
    public MediatedDarwinEngine(TaskContext context, IterationMemoryService memoryService, AiService aiService) {
        super(context, memoryService);
        this.aiService = aiService;
        this.mediationEngine = new MediationEngine();
    }
    
    @Override
    public OrchestratorResponse orchestrateEvolution(eu.kalafatic.evolution.controller.orchestration.TaskRequest taskRequest, IterationManager iterationManager) throws Exception {
        return evolve(taskRequest.getPrompt(), iterationManager, null);
    }

    @Override
    public OrchestratorResponse evolve(String request, IterationManager manager, eu.kalafatic.evolution.controller.orchestration.intent.EvolutionAssessment initialAssessment) throws Exception {
        // Get or create goal model
        GoalModel goal = (GoalModel) context.getOrchestrationState().getMetadata().get("goalModel");
        if (goal == null) {
            goal = manager.getGoalUnderstandingEngine().understand(request, context);
            context.getOrchestrationState().getMetadata().put("goalModel", goal);
        }

        // Run the iteration
        EvaluationResult result = runIteration(goal, manager);

        OrchestratorResponse response = new OrchestratorResponse();
        response.setResultType(eu.kalafatic.evolution.controller.orchestration.ResultType.CHAT);
        response.setSummary(result.isSuccess() ? "Mediated evolution successful" : "Mediated evolution failed");
        return response;
    }

    @Override
    public EvaluationResult runIteration(GoalModel goal, IterationManager manager) throws Exception {
        context.log("[MEDIATED_DARWIN] Running mediation iteration for: " + goal.getPrimaryAction());
        
        // 1. Run mediation to understand the repository
        MediationResult mediation = mediationEngine.mediate(context, goal.getPrimaryAction(), null);
        context.getOrchestrationState().getMetadata().put("mediationResult", mediation);
        
        // 2. Generate mediation candidates (prompt + files, NOT code)
        List<BranchVariant> variants = generateVariants(goal, manager);
        
        if (variants.isEmpty()) {
            context.log("[MEDIATED_DARWIN] No mediation candidates generated.");
            return failedResult("No mediation candidates generated");
        }
        
        // 3. Validate mediation candidates
        variants = validateVariants(variants, manager);
        
        if (variants.isEmpty()) {
            context.log("[MEDIATED_DARWIN] No mediation candidates passed validation.");
            return failedResult("No valid mediation candidates after validation");
        }
        
        // 4. Select the best mediation candidate
        BranchVariant winner = selectBestVariant(variants);
        context.log("[MEDIATED_DARWIN] Selected winner: " + winner.getId());
        
        // 5. Execute (generate the export package)
        return executeWinner(winner, manager);
    }
    
    @Override
    public List<BranchVariant> generateVariants(GoalModel goal, IterationManager manager) throws Exception {
        List<BranchVariant> variants = new ArrayList<>();
        
        // Get mediation result
        MediationResult mediation = (MediationResult) context.getOrchestrationState()
            .getMetadata().get("mediationResult");
        
        if (mediation == null) {
            context.log("[MEDIATED_DARWIN] No mediation result, running mediation...");
            mediation = mediationEngine.mediate(context, goal.getPrimaryAction(), null);
            context.getOrchestrationState().getMetadata().put("mediationResult", mediation);
        }
        
        // Generate 3 different mediation candidates
        for (int i = 0; i < 3; i++) {
            BranchVariant variant = createMediationVariant(mediation, i, goal);
            variants.add(variant);
        }
        
        return variants;
    }
    
    private BranchVariant createMediationVariant(MediationResult mediation, int index, GoalModel goal) {
        BranchVariant variant = new BranchVariant();
        variant.setId("mediated-variant-" + System.currentTimeMillis() + "-" + index);
        variant.setStrategy("Mediation Candidate " + (index + 1));
        variant.setStrategyType("MEDIATION_CANDIDATE");
        variant.setSemanticAnchor("Repository analysis and context extraction");
        variant.setReasoningLevel(BranchVariant.ReasoningLevel.DEEP);
        variant.setArchitectureEnabled(true);
        variant.setImplementationEnabled(false); // CRITICAL: No code generation
        
        // Build the mediation candidate
        MediationCandidate medCandidate = buildMediationCandidate(mediation, index, goal);
        variant.setMediationCandidate(medCandidate);
        
        // Empty actions - no code generation
        variant.setActions(new ArrayList<>());
        
        // Set score
        variant.setScore(0.7 + (index * 0.05));
        
        return variant;
    }
    
    private MediationCandidate buildMediationCandidate(MediationResult mediation, int index, GoalModel goal) {
        MediationCandidate candidate = new MediationCandidate();
        
        // Genome A: Optimized prompt for external LLM
        String[] prompts = {
            "Analyze the repository structure and identify the core components, their responsibilities, and dependencies. Focus on understanding the data flow and key architectural decisions.",
            "Examine the provided files to identify patterns, potential improvements, and areas of technical debt. Provide a structured analysis with actionable insights.",
            "Review the architecture of this system. Identify the main subsystems, their interactions, and any architectural smells. Suggest improvements for better maintainability."
        };
        candidate.setPrompt(prompts[index % prompts.length] + "\n\nGoal: " + goal.getPrimaryAction());
        
        // Genome B: Selected files
        List<String> files = getSelectedFiles(mediation, index);
        candidate.getSelectedFiles().addAll(files);
        
        // Architecture summary
        if (mediation.getModel() != null && mediation.getModel().getArchitectureSummary() != null) {
            candidate.setArchitectureSummary(mediation.getModel().getArchitectureSummary());
        } else {
            candidate.setArchitectureSummary("The system appears to be a Java-based application with standard Maven structure.");
        }
        
        // Subsystems
        if (mediation.getModel() != null && mediation.getModel().getSubsystems() != null) {
            candidate.getSubsystems().addAll(mediation.getModel().getSubsystems());
        }
        
        // Architectural facts
        if (mediation.getModel() != null && mediation.getModel().getArchitecturalFacts() != null) {
            candidate.getArchitecturalFacts().addAll(mediation.getModel().getArchitecturalFacts());
        }
        
        // Dependencies
        candidate.setDependencies("Java 8+, Maven, Standard Library");
        
        // Execution instructions
        candidate.setExecutionInstructions(
            "Analyze the provided files and propose improvements based on the architecture summary. " +
            "Focus on the identified hotspots and knowledge gaps."
        );
        
        // Self-evaluation
        candidate.setEvaluation("High-quality mediation candidate with " + files.size() + " files selected.");
        
        return candidate;
    }
    
    private List<String> getSelectedFiles(MediationResult mediation, int index) {
        List<String> files = new ArrayList<>();
        
        // Use hotspots from mediation
        if (mediation.getHotspots() != null && !mediation.getHotspots().isEmpty()) {
            for (Hotspot hotspot : mediation.getHotspots()) {
                if (hotspot.getFile() != null && !hotspot.getFile().isEmpty()) {
                    files.add(hotspot.getFile());
                }
            }
        }
        
        // If no hotspots, use default
        if (files.isEmpty()) {
            files.add("src/main/java/com/example/App.java");
            files.add("src/main/java/com/example/Config.java");
            files.add("pom.xml");
            files.add("README.md");
        }
        
        // Ensure we have 8-16 files (sweet spot for LLM context)
        while (files.size() < 8) {
            files.add("src/main/java/com/example/Component" + files.size() + ".java");
        }
        
        // Return first 16
        return files.subList(0, Math.min(files.size(), 16));
    }
    
    @Override
    public List<BranchVariant> validateVariants(List<BranchVariant> variants, IterationManager manager) {
        List<BranchVariant> valid = new ArrayList<>();
        for (BranchVariant v : variants) {
            MediationCandidate med = v.getMediationCandidate();
            if (med != null) {
                // Check required fields
                if (med.getPrompt() != null && !med.getPrompt().isEmpty() &&
                    med.getSelectedFiles() != null && !med.getSelectedFiles().isEmpty()) {
                    valid.add(v);
                } else {
                    context.log("[MEDIATED_DARWIN] Invalid mediation candidate: " + v.getId() + 
                               " - missing prompt or selected files");
                }
            }
        }
        return valid;
    }
    
    @Override
    public EvaluationResult executeWinner(BranchVariant winner, IterationManager manager) throws Exception {
        context.log("[MEDIATED_DARWIN] Executing winner mediation candidate");
        
        // Store the mediation candidate in context for export
        context.getOrchestrationState().getMetadata().put("winningMediationCandidate", 
            winner.getMediationCandidate());
        
        // Trigger export
        String exportResult = manager.performMediatedExportConvergence(
            context.getOrchestrationState().getRawInput(), context);
        
        context.log("[MEDIATED_DARWIN] Export result: " + exportResult);
        
        return successResult(exportResult);
    }
    
    @Override
    public String getMode() {
        return "MEDIATED";
    }
}