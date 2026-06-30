//package eu.kalafatic.evolution.controller.orchestration.selfdev;
//
//import java.util.ArrayList;
//import java.util.List;
//
//import eu.kalafatic.evolution.controller.orchestration.IterationManager;
//import eu.kalafatic.evolution.controller.orchestration.TaskContext;
//import eu.kalafatic.evolution.controller.orchestration.cognitive.CapabilityType;
//import eu.kalafatic.evolution.controller.orchestration.goal.GoalModel;
//import eu.kalafatic.evolution.controller.orchestration.mediation.MediationResult;
//import eu.kalafatic.evolution.model.orchestration.EvaluationResult;
//
///**
// * MEDIATED Darwin Engine - LLM refinement package generation.
// * 
// * Characteristics:
// * - No code generation
// * - Generates prompt + selected files
// * - Uses MediationEngine for context
// * - Creates MediationCandidate
// * - Exports ZIP package
// */
//public class MediatedEngine extends AbstractDarwinEngine {
//
//    public MediatedEngine(TaskContext context, IterationMemoryService memoryService) {
//        super(context, memoryService);
//        setExecutionProfile(EvolutionProfile.create(CapabilityType.ARCHITECTURE, 3));
//    }
//
//    @Override
//    public EvaluationResult runIteration(GoalModel goal, IterationManager manager) throws Exception {
//        context.log("[MEDIATED_ENGINE] Running mediation iteration for: " + goal.getPrimaryAction());
//
//        // 1. Run mediation to understand the repository
//        MediationResult mediation = mediationEngine.mediate(context, goal.getPrimaryAction(), null);
//        context.getOrchestrationState().getMetadata().put("mediationResult", mediation);
//
//        // 2. Generate mediation candidates (prompt + files, NOT code)
//        List<BranchVariant> variants = generateVariants(goal, manager);
//
//        if (variants.isEmpty()) {
//            return failedResult("No mediation candidates generated");
//        }
//
//        // 3. Validate mediation candidates
//        variants = validateVariants(variants, manager);
//
//        if (variants.isEmpty()) {
//            return failedResult("No valid mediation candidates after validation");
//        }
//
//        // 4. Select the best mediation candidate
//        BranchVariant winner = selectBestVariant(variants);
//        context.log("[MEDIATED_ENGINE] Selected winner: " + winner.getId());
//
//        // 5. Execute (generate the export package)
//        return executeWinner(winner, manager);
//    }
//
//    @Override
//    public List<BranchVariant> generateVariants(GoalModel goal, IterationManager manager) throws Exception {
//        List<BranchVariant> variants = new ArrayList<>();
//
//        // Get mediation result
//        MediationResult mediation = (MediationResult) context.getOrchestrationState()
//                .getMetadata().get("mediationResult");
//
//        if (mediation == null) {
//            mediation = mediationEngine.mediate(context, goal.getPrimaryAction(), null);
//            context.getOrchestrationState().getMetadata().put("mediationResult", mediation);
//        }
//
//        // Generate 3 different mediation candidates
//        for (int i = 0; i < 3; i++) {
//            BranchVariant variant = createMediationVariant(mediation, i, goal);
//            variants.add(variant);
//        }
//
//        return variants;
//    }
//
//    @Override
//    public List<BranchVariant> validateVariants(List<BranchVariant> variants, IterationManager manager) {
//        List<BranchVariant> valid = new ArrayList<>();
//        for (BranchVariant v : variants) {
//            if (v.getMediationCandidate() != null &&
//                    v.getMediationCandidate().getPrompt() != null &&
//                    !v.getMediationCandidate().getPrompt().isEmpty() &&
//                    v.getMediationCandidate().getSelectedFiles() != null &&
//                    !v.getMediationCandidate().getSelectedFiles().isEmpty()) {
//                valid.add(v);
//            }
//        }
//        return valid;
//    }
//
//    @Override
//    public EvaluationResult executeWinner(BranchVariant winner, IterationManager manager) throws Exception {
//        context.log("[MEDIATED_ENGINE] Executing winner mediation candidate");
//
//        // Store the mediation candidate for export
//        context.getOrchestrationState().getMetadata().put("winningMediationCandidate",
//                winner.getMediationCandidate());
//
//        // Trigger export
//        String exportResult = manager.performMediatedExportConvergence(
//                context.getOrchestrationState().getRawInput(), context);
//
//        context.log("[MEDIATED_ENGINE] Export result: " + exportResult);
//        context.getOrchestrationState().getMetadata().put("exportResult", exportResult);
//
//        return successResult();
//    }
//
//    @Override
//    public CapabilityType getCapabilityType() {
//        return CapabilityType.ARCHITECTURE;
//    }
//
//    @Override
//    public String getMode() {
//        return "MEDIATED";
//    }
//
//    // ============================================================
//    // PRIVATE HELPERS
//    // ============================================================
//
//    private BranchVariant createMediationVariant(MediationResult mediation, int index, GoalModel goal) {
//        BranchVariant variant = new BranchVariant();
//        variant.setId("mediated-variant-" + System.currentTimeMillis() + "-" + index);
//        variant.setStrategy("Mediation Candidate " + (index + 1));
//        variant.setStrategyType("MEDIATION_CANDIDATE");
//        variant.setSemanticAnchor("Repository analysis and context extraction");
//        variant.setReasoningLevel(BranchVariant.ReasoningLevel.DEEP);
//        variant.setArchitectureEnabled(true);
//        variant.setImplementationEnabled(false); // No code generation!
//
//        // Build mediation candidate
//        MediationCandidate candidate = buildMediationCandidate(mediation, index, goal);
//        variant.setMediationCandidate(candidate);
//
//        // Empty actions - no code generation!
//        variant.setActions(new ArrayList<>());
//
//        variant.setScore(0.7 + (index * 0.05));
//
//        return variant;
//    }
//
//    private MediationCandidate buildMediationCandidate(MediationResult mediation, int index, GoalModel goal) {
//        MediationCandidate candidate = new MediationCandidate();
//
//        String[] prompts = {
//                "Analyze the repository structure and identify the core components, their responsibilities, and dependencies.",
//                "Examine the provided files to identify patterns, potential improvements, and areas of technical debt.",
//                "Review the architecture of this system. Identify the main subsystems, their interactions, and any architectural smells."
//        };
//
//        candidate.setPrompt(prompts[index % prompts.length] + "\n\nGoal: " + goal.getPrimaryAction());
//
//        // Select files from mediation
//        List<String> files = getSelectedFiles(mediation, index);
//        candidate.getSelectedFiles().addAll(files);
//
//        if (mediation.getModel() != null) {
//            candidate.setArchitectureSummary(mediation.getModel().getArchitectureSummary());
//            candidate.getSubsystems().addAll(mediation.getModel().getSubsystems());
//            candidate.getArchitecturalFacts().addAll(mediation.getModel().getArchitecturalFacts());
//        }
//
//        candidate.setDependencies("Java 8+, Maven");
//        candidate.setExecutionInstructions(
//                "Analyze the provided files and propose improvements based on the architecture summary.");
//        candidate.setEvaluation("High-quality mediation candidate with " + files.size() + " files selected.");
//
//        return candidate;
//    }
//
//    private List<String> getSelectedFiles(MediationResult mediation, int index) {
//        List<String> files = new ArrayList<>();
//
//        if (mediation.getHotspots() != null && !mediation.getHotspots().isEmpty()) {
//            for (Hotspot hotspot : mediation.getHotspots()) {
//                if (hotspot.getFile() != null && !hotspot.getFile().isEmpty()) {
//                    files.add(hotspot.getFile());
//                }
//            }
//        }
//
//        // Ensure we have 8-16 files
//        while (files.size() < 8) {
//            files.add("src/main/java/com/example/Component" + files.size() + ".java");
//        }
//
//        return files.subList(0, Math.min(files.size(), 16));
//    }
//}