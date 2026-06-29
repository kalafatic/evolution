package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;

import eu.kalafatic.evolution.controller.agents.PromptIntentAnalyzer;
import eu.kalafatic.evolution.controller.kernel.EvolutionProfile;
import eu.kalafatic.evolution.controller.mediation.model.MediationCandidate;
import eu.kalafatic.evolution.controller.mediation.model.MediationResult;
import eu.kalafatic.evolution.controller.orchestration.IterationManager;
import eu.kalafatic.evolution.controller.orchestration.OrchestrationState;
import eu.kalafatic.evolution.controller.orchestration.OrchestratorResponse;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.TaskRequest;
import eu.kalafatic.evolution.controller.orchestration.cognitive.CapabilityType;
import eu.kalafatic.evolution.controller.orchestration.goal.GoalModel;
import eu.kalafatic.evolution.controller.orchestration.goal.SemanticEnvelope;
import eu.kalafatic.evolution.controller.trajectory.Trajectory;

/**
 * Mediated Darwin Engine implementation.
 * Handles evolution in mediated mode where the engine acts as a cognitive
 * mediator rather than directly modifying source code.
 * 
 * In mediated mode, the engine discovers architecture, generates
 * understanding, and proposes changes without direct execution.
 */
public class MediatedDarwinEngine extends ADarwinEngine {

    public MediatedDarwinEngine(TaskContext context, IterationMemoryService memoryService,
                                SystemStateSignalProvider stateProvider) {
        super(context, memoryService, stateProvider);
        context.log("[MEDIATED] MediatedDarwinEngine initialized.");
    }

    @Override
    public String getMode() {
        return "MEDIATED";
    }

    @Override
    public CapabilityType getCapabilityType() {
        return CapabilityType.MEDIATED;
    }

    @Override
    protected OrchestratorResponse handleRouting(PromptIntentAnalyzer.IntentResult intent, 
                                                   String request, 
                                                   TaskRequest taskRequest,
                                                   IterationManager iterationManager) throws Exception {
        // Mediated engine handles everything, but with mediated behavior
        // It doesn't short-circuit chat like ChatEngine does
        return null;
    }

    /**
     * Initializes mediated mode - sets up the MEDIATED profile.
     */
    @Override
    protected void initializeMode() throws Exception {
        context.log("[MEDIATED] Initializing mediated mode...");
        
        // Ensure mediation flag is set
        context.getOrchestrationState().getMetadata().put("isMediatedMode", true);
        
        // Set MEDIATED profile
        EvolutionProfile mediatedProfile = EvolutionProfile.create(CapabilityType.MEDIATED, 2);
        context.getOrchestrationState().setExecutionProfile(mediatedProfile);
        
        context.log("[MEDIATED] Mediated mode initialized.");
    }

    /**
     * Cleans up mediated mode.
     */
    @Override
    protected void cleanupMode() throws Exception {
        context.log("[MEDIATED] Cleaning up mediated mode...");
        // Nothing specific needed for mediated cleanup
    }

    /**
     * Generates mediated variants.
     * In mediated mode, variants are cognitive/architectural rather than code changes.
     * This is the IMutationContract implementation.
     */
    @Override
    public List<BranchVariant> generateVariants(GoalModel goal, StateSnapshot snapshot, 
                                                 FailureMemory failureMemory, 
                                                 Trajectory trajectory, 
                                                 EvolutionaryPressureVector pressure) throws Exception {
        context.log("[MEDIATED] Generating mediated variants for: " + goal.getPrimaryAction());
        
        int expansionValue = getExpansionValue();
        int intensity = context.getExecutionProfile() != null ? context.getExecutionProfile().getIntensity() : 2;
        
        // Mediated mode typically generates fewer variants since it's cognitive
        int branchingLimit;
        if (intensity <= 1) {
            branchingLimit = 1;
        } else if (intensity <= 3) {
            branchingLimit = expansionValue <= 5 ? 2 : 3;
        } else {
            branchingLimit = expansionValue <= 5 ? 3 : 4;
        }
        branchingLimit = Math.min(branchingLimit, 4);
        
        context.log("[MEDIATED] Population target: " + branchingLimit + " variants (intensity=" + intensity + ")");
        
        // Build context for variant generation
        String basePrompt = buildMediatedPrompt(goal);
        String lineageContext = buildLineageContext();
        List<String> rejectedSiblings = buildRejectedSiblings();
        
        // Get mediation context from state
        MediationResult mediationResult = (MediationResult) context.getOrchestrationState()
            .getMetadata().get("mediationResult");
        
        // Get active dimension
        SemanticGenome genome = dimensionEngine.createGenome(goal, null, context);
        EvolutionDimension activeDimension = dimensionEngine.selectNextDimension(genome, context);
        context.getOrchestrationState().getMetadata().put("current_dimension", activeDimension.getId());
        
        // Get current parent
        EvolutionTree tree = context.getKernelContext().getMemoryService().getEvolutionTree();
        String currentParentId = tree.getCurrentWinnerId();
        if (currentParentId == null && tree.getRootId() != null) {
            currentParentId = tree.getRootId();
        }
        
        int currentIteration = context.getOrchestrationState().getIterationCount();
        int generation = trajectory != null ? trajectory.getGeneration() : 0;
        
        // Abstraction levels - mediated mode focuses on architecture
        AbstractionLevel lockedLevel = context.getOrchestrationState().getLockedAbstractionLevel();
        boolean architectureEnabled = true; // Mediated always enables architecture
        boolean implementationEnabled = intensity >= 3; // Only high intensity enables implementation
        BranchVariant.ReasoningLevel reasoningLevel = intensity == 1 ? 
            BranchVariant.ReasoningLevel.MINIMAL : 
            intensity == 4 ? BranchVariant.ReasoningLevel.DEEP : BranchVariant.ReasoningLevel.BALANCED;
        
        // Generate mediated variants
        List<JSONObject> variantObjects = generateMediatedVariantObjects(
            goal, activeDimension, branchingLimit, basePrompt, lineageContext, 
            rejectedSiblings, mediationResult, genome, tree, currentParentId, 
            generation, reasoningLevel, architectureEnabled, implementationEnabled
        );
        
        // Fallback if not enough variants
        if (variantObjects.size() < 1) {
            context.log("[MEDIATED] Insufficient variants. Injecting fallback.");
            variantObjects.add(createFallbackMediatedVariant(goal));
        }
        
        // Semantic validation
        SemanticEnvelope envelope = getSemanticEnvelope(goal);
        for (JSONObject obj : variantObjects) {
            double distance = semanticDistance(goal, obj, envelope);
            obj.put("semantic_distance", distance);
            
            if (distance > 0.70) {
                context.log("[MEDIATED] Semantic validation warning for " + obj.optString("id") + 
                           ": distance=" + String.format("%.2f", distance));
                EvolutionNode node = tree.getNode(obj.optString("id"));
                if (node != null) {
                    node.setStatus("REJECTED_SEMANTIC");
                    node.setRejectionReason("Semantic distance exceeds threshold");
                }
            }
        }
        
        // Rank variants
        rankMediatedVariants(variantObjects, pressure);
        
        // Mark best variant
        if (!variantObjects.isEmpty()) {
            variantObjects.get(0).put("isBest", true);
        }
        
        // Convert to BranchVariant
        List<BranchVariant> variants = new ArrayList<>();
        for (JSONObject obj : variantObjects) {
            BranchVariant v = mapToBranchVariant(obj, goal.getPrimaryAction(), "MEDIATED_EVOLUTION", 
                                                 trajectory, context);
            v.setInheritedContext(lineageContext);
            v.setRejectedSiblings(rejectedSiblings);
            
            // Mark as mediated
            v.setStrategyType("MEDIATED");
            v.setArchitectureEnabled(true);
            v.setImplementationEnabled(intensity >= 3);
            
            variants.add(v);
        }
        
        context.log("[MEDIATED] Generated " + variants.size() + " mediated variants.");
        return variants;
    }

    // ============================================================
    // PRIVATE HELPER METHODS
    // ============================================================

    /**
     * Builds the mediated prompt with architectural focus.
     */
    private String buildMediatedPrompt(GoalModel goal) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("MEDIATED MODE - ARCHITECTURAL EVOLUTION\n");
        prompt.append("Goal: ").append(goal.getPrimaryAction()).append("\n");
        prompt.append("Domain: ").append(goal.getDomain()).append("\n");
        prompt.append("Complexity: ").append(goal.getComplexity()).append("\n");
        
        // Add project structure
        Object structure = context.getOrchestrationState().getMetadata().get("projectStructure");
        if (structure != null) {
            prompt.append("\nProject Structure:\n").append(structure).append("\n");
        }
        
        // Add mediation insights
        MediationResult mediation = (MediationResult) context.getOrchestrationState()
            .getMetadata().get("mediationResult");
        if (mediation != null) {
            prompt.append("\n--- MEDIATION INSIGHTS ---\n");
            prompt.append("Hotspots: ").append(mediation.getHotspots().size()).append("\n");
            prompt.append("Candidates: ").append(mediation.getCandidates().size()).append("\n");
            if (mediation.getWinner() != null) {
                prompt.append("Winner: ").append(mediation.getWinner().getPrompt()).append("\n");
            }
        }
        
        // Add semantic envelope
        Object envelope = context.getOrchestrationState().getMetadata().get("semanticEnvelope");
        if (envelope instanceof SemanticEnvelope) {
            SemanticEnvelope env = (SemanticEnvelope) envelope;
            prompt.append("\nSemantic Envelope:\n");
            prompt.append("Core Intent: ").append(env.getCoreIntent()).append("\n");
            prompt.append("Mandatory Concepts: ").append(env.getMandatoryConcepts()).append("\n");
            prompt.append("Forbidden Regions: ").append(env.getForbiddenRegions()).append("\n");
        }
        
        // Add architectural facts
        Object realityModel = context.getOrchestrationState().getMetadata().get("targetRealityModel");
        if (realityModel instanceof eu.kalafatic.evolution.controller.mediation.model.TargetRealityModel) {
            eu.kalafatic.evolution.controller.mediation.model.TargetRealityModel model = 
                (eu.kalafatic.evolution.controller.mediation.model.TargetRealityModel) realityModel;
            prompt.append("\n--- TARGET REALITY ---\n");
            prompt.append("Domain: ").append(model.getDomain()).append("\n");
            prompt.append("Purpose: ").append(model.getPurpose()).append("\n");
            prompt.append("Architecture Summary: ").append(model.getArchitectureSummary()).append("\n");
            prompt.append("Objectives: ").append(model.getObjectives()).append("\n");
        }
        
        return prompt.toString();
    }

    /**
     * Builds lineage context from records.
     */
    private String buildLineageContext() {
        List<IterationRecord> survivors = memoryService.getRecords().stream()
            .filter(r -> "ACTIVE".equals(r.getActivationState()) || "KEPT".equals(r.getActivationState()))
            .collect(Collectors.toList());
        
        if (survivors.isEmpty()) {
            return "No lineage history.";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("### EVOLUTIONARY ANCESTORS ###\n");
        for (IterationRecord ancestor : survivors) {
            sb.append("ANCESTOR: ").append(ancestor.getBranchId()).append("\n");
            sb.append("STRATEGY: ").append(ancestor.getStrategy()).append("\n");
            sb.append("PHILOSOPHY: ").append(ancestor.getSemanticAnchor()).append("\n\n");
        }
        return sb.toString();
    }

    /**
     * Builds rejected siblings list.
     */
    private List<String> buildRejectedSiblings() {
        return memoryService.getRecords().stream()
            .filter(r -> !"ACTIVE".equals(r.getActivationState()) && !"KEPT".equals(r.getActivationState()))
            .map(r -> r.getStrategy() + " (Iteration " + r.getIteration() + ")")
            .distinct()
            .collect(Collectors.toList());
    }

    /**
     * Gets or creates the semantic envelope.
     */
    private SemanticEnvelope getSemanticEnvelope(GoalModel goal) {
        OrchestrationState state = context.getOrchestrationState();
        Object envelopeObj = state.getMetadata().get("semanticEnvelope");
        
        if (envelopeObj instanceof SemanticEnvelope) {
            return (SemanticEnvelope) envelopeObj;
        }
        
        SemanticEnvelope envelope = new SemanticEnvelope();
        envelope.setCoreIntent(goal.getPrimaryAction());
        state.getMetadata().put("semanticEnvelope", envelope);
        return envelope;
    }

    /**
     * Generates mediated variant objects.
     */
    private List<JSONObject> generateMediatedVariantObjects(
            GoalModel goal, EvolutionDimension activeDimension, int branchingLimit,
            String basePrompt, String lineageContext, List<String> rejectedSiblings,
            MediationResult mediationResult, SemanticGenome genome, EvolutionTree tree,
            String currentParentId, int generation, BranchVariant.ReasoningLevel reasoningLevel,
            boolean architectureEnabled, boolean implementationEnabled) {
        
        List<JSONObject> variants = new ArrayList<>();
        
        // Create variants based on mediation insights
        if (mediationResult != null && !mediationResult.getCandidates().isEmpty()) {
            // Use mediation candidates as seed for variants
            for (int i = 0; i < Math.min(branchingLimit, mediationResult.getCandidates().size()); i++) {
                MediationCandidate candidate = mediationResult.getCandidates().get(i);
                JSONObject variant = createMediatedVariantFromCandidate(
                    goal, candidate, i, generation, reasoningLevel);
                variants.add(variant);
            }
        }
        
        // If we still need more variants, generate synthetic ones
        while (variants.size() < Math.min(branchingLimit, 2)) {
            JSONObject variant = createSyntheticMediatedVariant(
                goal, activeDimension, variants.size(), generation, reasoningLevel);
            variants.add(variant);
        }
        
        return variants;
    }

    /**
     * Creates a mediated variant from a mediation candidate.
     */
    private JSONObject createMediatedVariantFromCandidate(
            GoalModel goal, MediationCandidate candidate, int index, 
            int generation, BranchVariant.ReasoningLevel reasoningLevel) {
        
        JSONObject obj = new JSONObject();
        obj.put("id", "mediated-variant-" + (index + 1) + "-" + System.currentTimeMillis());
        obj.put("strategy_type", "MEDIATED");
        obj.put("reasoning_level", reasoningLevel.name());
        obj.put("architecture_enabled", true);
        obj.put("implementation_enabled", false);
        obj.put("generation", generation);
        
        // Use candidate data
        String strategy = candidate.getArchitectureSummary() != null && !candidate.getArchitectureSummary().isEmpty()
            ? candidate.getArchitectureSummary() 
            : "Mediated strategy for: " + goal.getPrimaryAction();
        obj.put("strategy", strategy);
        obj.put("semantic_anchor", candidate.getPrompt() != null ? candidate.getPrompt() : strategy);
        obj.put("survival_argument", "Mediated cognitive evolution");
        obj.put("tradeoffs", "Focus on architecture over implementation");
        obj.put("failure_risks", "May lack implementation detail");
        obj.put("score", 0.85 - (index * 0.05));
        
        // Add selected files
        JSONArray filesArr = new JSONArray();
        if (candidate.getSelectedFiles() != null) {
            for (String file : candidate.getSelectedFiles()) {
                filesArr.put(file);
            }
        }
        obj.put("selected_files", filesArr);
        
        // Add actions (cognitive actions, not code modifications)
        JSONArray actionsArr = new JSONArray();
        JSONObject action1 = new JSONObject();
        action1.put("domain", "architecture");
        action1.put("operation", "ANALYZE");
        action1.put("target", "repository");
        action1.put("description", "Analyze repository structure and identify architectural patterns");
        actionsArr.put(action1);
        
        JSONObject action2 = new JSONObject();
        action2.put("domain", "architecture");
        action2.put("operation", "SUGGEST");
        action2.put("target", "architecture");
        action2.put("description", "Suggest architectural improvements based on mediation analysis");
        actionsArr.put(action2);
        
        obj.put("actions", actionsArr);
        
        // Add mediation candidate
        JSONObject medObj = new JSONObject();
        medObj.put("prompt", candidate.getPrompt());
        medObj.put("architecture_summary", candidate.getArchitectureSummary());
        medObj.put("dependencies", candidate.getDependencies());
        medObj.put("execution_instructions", candidate.getExecutionInstructions());
        medObj.put("evaluation", candidate.getEvaluation());
        
        JSONArray subArr = new JSONArray();
        if (candidate.getSubsystems() != null) {
            for (eu.kalafatic.evolution.controller.mediation.model.Subsystem sub : candidate.getSubsystems()) {
                JSONObject sObj = new JSONObject();
                sObj.put("id", sub.getId());
                sObj.put("name", sub.getName());
                sObj.put("purpose", sub.getPurpose());
                sObj.put("description", sub.getDescription());
                JSONArray bounds = new JSONArray();
                if (sub.getBoundaries() != null) {
                    for (String b : sub.getBoundaries()) bounds.put(b);
                }
                sObj.put("boundaries", bounds);
                JSONArray crit = new JSONArray();
                if (sub.getCriticalFiles() != null) {
                    for (String c : sub.getCriticalFiles()) crit.put(c);
                }
                sObj.put("critical_files", crit);
                JSONArray resp = new JSONArray();
                if (sub.getResponsibilities() != null) {
                    for (String r : sub.getResponsibilities()) resp.put(r);
                }
                sObj.put("responsibilities", resp);
                subArr.put(sObj);
            }
        }
        medObj.put("subsystems", subArr);
        
        JSONArray factArr = new JSONArray();
        if (candidate.getArchitecturalFacts() != null) {
            for (eu.kalafatic.evolution.controller.mediation.model.ArchitecturalFact fact : candidate.getArchitecturalFacts()) {
                JSONObject fObj = new JSONObject();
                fObj.put("id", fact.getId());
                fObj.put("subject", fact.getSubject());
                fObj.put("predicate", fact.getPredicate());
                fObj.put("description", fact.getDescription());
                fObj.put("confidence", fact.getConfidence());
                JSONArray ev = new JSONArray();
                if (fact.getEvidence() != null) {
                    for (String e : fact.getEvidence()) ev.put(e);
                }
                fObj.put("evidence", ev);
                factArr.put(fObj);
            }
        }
        medObj.put("architectural_facts", factArr);
        
        obj.put("mediation_candidate", medObj);
        
        return obj;
    }

    /**
     * Creates a synthetic mediated variant.
     */
    private JSONObject createSyntheticMediatedVariant(
            GoalModel goal, EvolutionDimension dimension, int index,
            int generation, BranchVariant.ReasoningLevel reasoningLevel) {
        
        JSONObject obj = new JSONObject();
        obj.put("id", "mediated-synthetic-" + (index + 1) + "-" + System.currentTimeMillis());
        obj.put("strategy_type", "MEDIATED");
        obj.put("reasoning_level", reasoningLevel.name());
        obj.put("architecture_enabled", true);
        obj.put("implementation_enabled", false);
        obj.put("generation", generation);
        
        String strategy = "Synthetic mediated strategy: " + goal.getPrimaryAction() + 
            " (Focus: " + (dimension != null ? dimension.getDescription() : "architecture") + ")";
        obj.put("strategy", strategy);
        obj.put("semantic_anchor", "Synthetic mediated anchor");
        obj.put("survival_argument", "Synthetic cognitive evolution");
        obj.put("tradeoffs", "May not fully reflect reality");
        obj.put("failure_risks", "Lacks empirical validation");
        obj.put("score", 0.75 - (index * 0.05));
        
        obj.put("selected_files", new JSONArray());
        
        // Actions
        JSONArray actionsArr = new JSONArray();
        JSONObject action1 = new JSONObject();
        action1.put("domain", "architecture");
        action1.put("operation", "ANALYZE");
        action1.put("target", "system");
        action1.put("description", "Analyze system architecture and propose evolution");
        actionsArr.put(action1);
        obj.put("actions", actionsArr);
        
        return obj;
    }

    /**
     * Creates a fallback mediated variant.
     */
    private JSONObject createFallbackMediatedVariant(GoalModel goal) {
        JSONObject obj = new JSONObject();
        obj.put("id", "mediated-fallback-" + System.currentTimeMillis());
        obj.put("strategy_type", "MEDIATED");
        obj.put("reasoning_level", "BALANCED");
        obj.put("architecture_enabled", true);
        obj.put("implementation_enabled", false);
        obj.put("generation", 0);
        obj.put("strategy", "Fallback mediated strategy: " + goal.getPrimaryAction());
        obj.put("semantic_anchor", "Fallback cognitive anchor");
        obj.put("survival_argument", "Fallback cognitive evolution");
        obj.put("tradeoffs", "Generic mediation");
        obj.put("failure_risks", "May not address specific concerns");
        obj.put("score", 0.7);
        obj.put("selected_files", new JSONArray());
        
        JSONArray actionsArr = new JSONArray();
        JSONObject action1 = new JSONObject();
        action1.put("domain", "architecture");
        action1.put("operation", "ANALYZE");
        action1.put("target", "system");
        action1.put("description", "Perform fallback mediated analysis");
        actionsArr.put(action1);
        obj.put("actions", actionsArr);
        
        return obj;
    }

    /**
     * Ranks mediated variants.
     */
    private void rankMediatedVariants(List<JSONObject> variants, EvolutionaryPressureVector pressure) {
        for (JSONObject v : variants) {
            double baseScore = v.optDouble("score", 0.7);
            
            // Boost score if it has mediation candidate data
            if (v.has("mediation_candidate") && v.getJSONObject("mediation_candidate").length() > 0) {
                baseScore += 0.1;
            }
            
            // Boost if it has architectural facts
            JSONObject medObj = v.optJSONObject("mediation_candidate");
            if (medObj != null && medObj.has("architectural_facts") && 
                medObj.getJSONArray("architectural_facts").length() > 0) {
                baseScore += 0.05;
            }
            
            v.put("score", Math.min(baseScore, 1.0));
        }
        
        // Sort by score descending
        variants.sort((v1, v2) -> Double.compare(v2.optDouble("score"), v1.optDouble("score")));
    }

    /**
     * Calculates semantic distance between goal and variant.
     */
    private double semanticDistance(GoalModel goal, JSONObject variant, SemanticEnvelope envelope) {
        String strategy = variant.optString("strategy", "").toLowerCase();
        String philosophy = variant.optString("semantic_anchor", "").toLowerCase();
        String primaryAction = goal.getPrimaryAction().toLowerCase();
        
        double distance = 0.0;
        
        // Exact match
        if (strategy.contains(primaryAction) || philosophy.contains(primaryAction)) {
            return 0.0;
        }
        
        // Keyword overlap
        String[] keywords = primaryAction.split(" ");
        int matches = 0;
        int significantKeywords = 0;
        for (String k : keywords) {
            if (k.length() <= 3) continue;
            significantKeywords++;
            if (strategy.contains(k) || philosophy.contains(k)) {
                matches++;
            }
        }
        double overlap = significantKeywords > 0 ? (double) matches / significantKeywords : 1.0;
        distance += (1.0 - overlap) * 0.6;
        
        return Math.min(1.0, distance);
    }

    /**
     * Maps JSON object to BranchVariant.
     */
    private BranchVariant mapToBranchVariant(JSONObject obj, String goal, String phase, 
                                              Trajectory trajectory, TaskContext context) {
        BranchVariant v = new BranchVariant();
        v.setId(obj.optString("id", "v-" + System.currentTimeMillis()));
        v.setBranchId(v.getId());
        v.setLineageId(context.getSessionId());
        v.setActivationState(BranchVariant.ActivationState.ARCHIVED);
        v.setStrategyType(obj.optString("strategy_type", "MEDIATED"));
        v.setReasoningLevel(BranchVariant.ReasoningLevel.valueOf(
            obj.optString("reasoning_level", "BALANCED")));
        v.setArchitectureEnabled(obj.optBoolean("architecture_enabled", true));
        v.setImplementationEnabled(obj.optBoolean("implementation_enabled", false));
        v.setStrategy(obj.optString("strategy", "unknown"));
        v.setSemanticAnchor(obj.optString("semantic_anchor", v.getStrategy()));
        v.setMutationPhilosophy(obj.optString("mutation_philosophy"));
        v.setMutationTrace("Generated in mediated evolution.");
        v.setScore(obj.optDouble("score", 0.0));
        v.setBranchName("mediated/" + sanitize(goal) + "/" + v.getId() + "-" + System.currentTimeMillis());
        v.setSurvivalArgument(obj.optString("survival_argument", "none"));
        v.setTradeoffs(obj.optString("tradeoffs", "none"));
        v.setFailureRisks(obj.optString("failure_risks", "none"));
        
        // Trajectory
        Trajectory t = new Trajectory(v.getId(), v.getStrategy());
        t.setFitnessScore(v.getScore());
        if (trajectory != null) {
            t.setParentTrajectoryId(trajectory.getTrajectoryId());
            trajectory.addChildTrajectoryId(t.getTrajectoryId());
        }
        v.setTrajectoryId(t.getTrajectoryId());
        memoryService.getTrajectoryMemory().recordTrajectory(t);
        
        // Actions
        JSONArray actionsArr = obj.optJSONArray("actions");
        if (actionsArr != null) {
            for (int i = 0; i < actionsArr.length(); i++) {
                JSONObject aObj = actionsArr.optJSONObject(i);
                if (aObj == null) continue;
                BranchVariant.Action action = new BranchVariant.Action();
                action.setDomain(aObj.optString("domain", "architecture"));
                action.setOperation(aObj.optString("operation", "ANALYZE"));
                action.setTarget(aObj.optString("target", "system"));
                action.setDescription(aObj.optString("description", "Cognitive mediation"));
                action.setImplementation(aObj.optString("implementation", ""));
                v.getActions().add(action);
            }
        }
        
        // Selected files
        JSONArray filesArr = obj.optJSONArray("selected_files");
        if (filesArr != null) {
            for (int i = 0; i < filesArr.length(); i++) {
                String s = filesArr.optString(i);
                if (s != null && !s.isEmpty()) {
                    v.getSelectedFiles().add(s);
                }
            }
        }
        
        // Mediation candidate
        JSONObject medObj = obj.optJSONObject("mediation_candidate");
        if (medObj != null) {
            MediationCandidate med = new MediationCandidate();
            med.setPrompt(medObj.optString("prompt"));
            med.setArchitectureSummary(medObj.optString("architecture_summary"));
            med.setDependencies(medObj.optString("dependencies"));
            med.setExecutionInstructions(medObj.optString("execution_instructions"));
            med.setEvaluation(medObj.optString("evaluation"));
            
            JSONArray subArr = medObj.optJSONArray("subsystems");
            if (subArr != null) {
                for (int i = 0; i < subArr.length(); i++) {
                    JSONObject sObj = subArr.optJSONObject(i);
                    if (sObj == null) continue;
                    eu.kalafatic.evolution.controller.mediation.model.Subsystem sub = 
                        new eu.kalafatic.evolution.controller.mediation.model.Subsystem();
                    sub.setId(sObj.optString("id"));
                    sub.setName(sObj.optString("name"));
                    sub.setPurpose(sObj.optString("purpose"));
                    sub.setDescription(sObj.optString("description"));
                    med.getSubsystems().add(sub);
                }
            }
            
            JSONArray factArr = medObj.optJSONArray("architectural_facts");
            if (factArr != null) {
                for (int i = 0; i < factArr.length(); i++) {
                    JSONObject fObj = factArr.optJSONObject(i);
                    if (fObj == null) continue;
                    eu.kalafatic.evolution.controller.mediation.model.ArchitecturalFact fact = 
                        new eu.kalafatic.evolution.controller.mediation.model.ArchitecturalFact();
                    fact.setId(fObj.optString("id"));
                    fact.setSubject(fObj.optString("subject"));
                    fact.setPredicate(fObj.optString("predicate"));
                    fact.setDescription(fObj.optString("description"));
                    fact.setConfidence(fObj.optDouble("confidence", 1.0));
                    med.getArchitecturalFacts().add(fact);
                }
            }
            
            v.setMediationCandidate(med);
        }
        
        return v;
    }

    private String sanitize(String s) {
        if (s == null || s.isEmpty()) return "unnamed";
        String sanitized = s.toLowerCase().replaceAll("[^a-z0-9]", "-").replaceAll("-+", "-");
        if (sanitized.startsWith("-")) sanitized = sanitized.substring(1);
        if (sanitized.endsWith("-")) sanitized = sanitized.substring(0, sanitized.length() - 1);
        if (sanitized.isEmpty()) return "unnamed";
        return sanitized.substring(0, Math.min(sanitized.length(), 30));
    }
}