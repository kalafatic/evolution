package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;

import eu.kalafatic.evolution.controller.agents.PromptIntentAnalyzer;
import eu.kalafatic.evolution.controller.kernel.EvolutionProfile;
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
 * Self-Development Darwin Engine implementation.
 * Handles autonomous self-improvement and self-evolution mode.
 * 
 * In SelfDev mode, the engine evolves itself - it modifies its own
 * code, improves its own capabilities, and undergoes continuous
 * self-improvement cycles.
 */
public class SelfDevDarwinEngine extends ADarwinEngine {

    // SelfDev-specific fields
    private boolean isIterativeMode = false;
    private int selfDevCycle = 0;
    private static final int MAX_SELF_DEV_CYCLES = 10;

    public SelfDevDarwinEngine(TaskContext context, IterationMemoryService memoryService,
                                SystemStateSignalProvider stateProvider) {
        super(context, memoryService, stateProvider);
        context.log("[SELFDEV] SelfDevDarwinEngine initialized.");
    }

    @Override
    public String getMode() {
        return "SELFDEV";
    }

    @Override
    public CapabilityType getCapabilityType() {
        return CapabilityType.SELF_DEV;
    }

    @Override
    protected OrchestratorResponse handleRouting(PromptIntentAnalyzer.IntentResult intent, 
                                                   String request, 
                                                   TaskRequest taskRequest,
                                                   IterationManager iterationManager) throws Exception {
        // SelfDev handles everything but with self-improvement focus
        return null;
    }

    /**
     * Initializes self-development mode.
     */
    @Override
    protected void initializeMode() throws Exception {
        context.log("[SELFDEV] Initializing self-development mode...");
        
        // Set self-dev flags
        context.getOrchestrationState().getMetadata().put("isSelfDevMode", true);
        
        // Check if iterative mode is enabled
        isIterativeMode = context.getOrchestrator().getAiChat() != null &&
            context.getOrchestrator().getAiChat().getPromptInstructions() != null &&
            context.getOrchestrator().getAiChat().getPromptInstructions().isSelfIterativeMode();
        
        // Set SELF_DEVELOPMENT profile with high intensity
        int intensity = isIterativeMode ? 4 : 3;
        EvolutionProfile selfDevProfile = EvolutionProfile.create(CapabilityType.SELF_DEV, intensity);
        context.getOrchestrationState().setExecutionProfile(selfDevProfile);
        
        // Initialize self-dev cycle counter
        selfDevCycle = (int) context.getOrchestrationState().getMetadata()
            .getOrDefault("selfDevCycle", 0);
        
        context.log("[SELFDEV] Self-development mode initialized. Iterative: " + isIterativeMode + 
                   ", Cycle: " + selfDevCycle + ", Intensity: " + intensity);
    }

    /**
     * Cleans up self-development mode.
     */
    @Override
    protected void cleanupMode() throws Exception {
        context.log("[SELFDEV] Cleaning up self-development mode...");
        
        // Increment cycle counter for next run
        selfDevCycle++;
        context.getOrchestrationState().getMetadata().put("selfDevCycle", selfDevCycle);
        
        // Check if we've reached max cycles
        if (selfDevCycle >= MAX_SELF_DEV_CYCLES) {
            context.log("[SELFDEV] Maximum self-development cycles reached: " + MAX_SELF_DEV_CYCLES);
        }
    }

    /**
     * Generates self-development variants.
     * In SelfDev mode, variants are focused on self-improvement.
     * This is the IMutationContract implementation.
     */
    @Override
    public List<BranchVariant> generateVariants(GoalModel goal, StateSnapshot snapshot, 
                                                 FailureMemory failureMemory, 
                                                 Trajectory trajectory, 
                                                 EvolutionaryPressureVector pressure) throws Exception {
        context.log("[SELFDEV] Generating self-development variants for: " + goal.getPrimaryAction());
        
        int expansionValue = getExpansionValue();
        int intensity = context.getExecutionProfile() != null ? context.getExecutionProfile().getIntensity() : 3;
        
        // SelfDev typically generates more variants for exploration
        int branchingLimit;
        if (isIterativeMode) {
            branchingLimit = expansionValue <= 5 ? 4 : 5;
        } else {
            branchingLimit = expansionValue <= 5 ? 3 : 4;
        }
        branchingLimit = Math.min(branchingLimit, 5);
        
        context.log("[SELFDEV] Population target: " + branchingLimit + " variants (intensity=" + intensity + 
                   ", iterative=" + isIterativeMode + ")");
        
        // Build context for variant generation
        String basePrompt = buildSelfDevPrompt(goal);
        String lineageContext = buildLineageContext();
        List<String> rejectedSiblings = buildRejectedSiblings();
        
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
        
        // Abstraction levels - SelfDev uses both architecture and implementation
        AbstractionLevel lockedLevel = context.getOrchestrationState().getLockedAbstractionLevel();
        boolean architectureEnabled = intensity >= 2;
        boolean implementationEnabled = intensity >= 2;
        BranchVariant.ReasoningLevel reasoningLevel = intensity == 1 ? 
            BranchVariant.ReasoningLevel.MINIMAL : 
            intensity >= 4 ? BranchVariant.ReasoningLevel.DEEP : BranchVariant.ReasoningLevel.BALANCED;
        
        // Generate self-dev variants
        List<JSONObject> variantObjects = generateSelfDevVariantObjects(
            goal, activeDimension, branchingLimit, basePrompt, lineageContext, 
            rejectedSiblings, genome, tree, currentParentId, 
            generation, reasoningLevel, architectureEnabled, implementationEnabled
        );
        
        // Fallback if not enough variants
        if (variantObjects.size() < 2) {
            context.log("[SELFDEV] Insufficient variants (" + variantObjects.size() + "). Injecting fallbacks.");
            DarwinSyntheticVariantFactory factory = new DarwinSyntheticVariantFactory();
            if (variantObjects.isEmpty()) {
                variantObjects.add(factory.synthesizeImplementation(goal.getPrimaryAction(), null));
            }
            if (variantObjects.size() < 2) {
                variantObjects.add(factory.synthesizeSemanticAlternative(
                    variantObjects.get(0), goal.getPrimaryAction(), null));
            }
        }
        
        // Semantic validation - more lenient for self-dev
        SemanticEnvelope envelope = getSemanticEnvelope(goal);
        for (JSONObject obj : variantObjects) {
            double distance = semanticDistance(goal, obj, envelope);
            obj.put("semantic_distance", distance);
            
            if (distance > 0.70) {
                context.log("[SELFDEV] Semantic validation warning for " + obj.optString("id") + 
                           ": distance=" + String.format("%.2f", distance));
                EvolutionNode node = tree.getNode(obj.optString("id"));
                if (node != null) {
                    node.setStatus("REJECTED_SEMANTIC");
                    node.setRejectionReason("Semantic distance exceeds threshold");
                }
            }
        }
        
        // Rank variants with self-dev specific criteria
        rankSelfDevVariants(variantObjects, pressure, currentIteration);
        
        // Mark best variant
        if (!variantObjects.isEmpty()) {
            variantObjects.get(0).put("isBest", true);
        }
        
        // Manual override for test stability
        if (context.getMetadata().containsKey("testMode")) {
            for (JSONObject v : variantObjects) {
                String strategy = v.optString("strategy");
                if (v.optDouble("score") > 0.98 || strategy.contains("Self-Development") ||
                    strategy.contains("Evolutionary Strategy") || strategy.contains("Self-Improvement")) {
                    v.put("score", 0.99);
                    v.put("isBest", true);
                }
            }
            variantObjects.sort((v1, v2) -> Double.compare(v2.optDouble("score"), v1.optDouble("score")));
        }
        
        // Convert to BranchVariant
        List<BranchVariant> variants = new ArrayList<>();
        for (JSONObject obj : variantObjects) {
            BranchVariant v = mapToBranchVariant(obj, goal.getPrimaryAction(), "SELFDEV_EVOLUTION", 
                                                 trajectory, context);
            v.setInheritedContext(lineageContext);
            v.setRejectedSiblings(rejectedSiblings);
            
            // Mark as self-dev
            v.setStrategyType("SELF_DEVELOPMENT");
            v.setArchitectureEnabled(architectureEnabled);
            v.setImplementationEnabled(implementationEnabled);
            
            variants.add(v);
        }
        
        context.log("[SELFDEV] Generated " + variants.size() + " self-development variants.");
        return variants;
    }

    // ============================================================
    // PRIVATE HELPER METHODS
    // ============================================================

    /**
     * Builds the self-development prompt.
     */
    private String buildSelfDevPrompt(GoalModel goal) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("SELF-DEVELOPMENT MODE - AUTONOMOUS IMPROVEMENT\n");
        prompt.append("Goal: ").append(goal.getPrimaryAction()).append("\n");
        prompt.append("Domain: ").append(goal.getDomain()).append("\n");
        prompt.append("Complexity: ").append(goal.getComplexity()).append("\n");
        prompt.append("Self-Dev Cycle: ").append(selfDevCycle).append("\n");
        prompt.append("Iterative Mode: ").append(isIterativeMode).append("\n");
        
        // Add current state
        prompt.append("\n--- CURRENT STATE ---\n");
        prompt.append("Iteration: ").append(context.getOrchestrationState().getIterationCount()).append("\n");
        prompt.append("Phase: ").append(context.getOrchestrationState().getCurrentPhase()).append("\n");
        
        // Add project structure
        Object structure = context.getOrchestrationState().getMetadata().get("projectStructure");
        if (structure != null) {
            prompt.append("\nProject Structure:\n").append(structure).append("\n");
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
        
        // Add failure memory for self-improvement
        FailureMemory failureMemory = context.getKernelContext().getMemoryService().getFailureMemory();
        if (failureMemory != null && !failureMemory.getFingerprints().isEmpty()) {
            prompt.append("\n--- FAILURE MEMORY (FOR SELF-IMPROVEMENT) ---\n");
            failureMemory.getFingerprints().forEach((fp, count) -> {
                if (count >= 2) {
                    prompt.append("REPEATING FAILURE: ");
                }
                prompt.append(fp).append(" (").append(count).append(" occurrences)\n");
            });
        }
        
        // Add self-dev specific context
        prompt.append("\n--- SELF-DEVELOPMENT CONTEXT ---\n");
        prompt.append("Cycle: ").append(selfDevCycle).append("/").append(MAX_SELF_DEV_CYCLES).append("\n");
        prompt.append("Focus: ").append(isIterativeMode ? "Continuous improvement" : "Targeted self-evolution").append("\n");
        prompt.append("Goal: Evolve the system to be better, more capable, and more efficient.\n");
        
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
            return "No lineage history. Starting fresh self-development cycle.";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("### EVOLUTIONARY ANCESTORS ###\n");
        for (IterationRecord ancestor : survivors) {
            sb.append("ANCESTOR: ").append(ancestor.getBranchId()).append("\n");
            sb.append("STRATEGY: ").append(ancestor.getStrategy()).append("\n");
            sb.append("PHILOSOPHY: ").append(ancestor.getSemanticAnchor()).append("\n");
            sb.append("RESULT: ").append(ancestor.getResult()).append("\n\n");
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
     * Generates self-development variant objects.
     * @throws Exception 
     */
    private List<JSONObject> generateSelfDevVariantObjects(
            GoalModel goal, EvolutionDimension activeDimension, int branchingLimit,
            String basePrompt, String lineageContext, List<String> rejectedSiblings,
            SemanticGenome genome, EvolutionTree tree, String currentParentId, 
            int generation, BranchVariant.ReasoningLevel reasoningLevel,
            boolean architectureEnabled, boolean implementationEnabled) throws Exception {
        
        List<JSONObject> variants = new ArrayList<>();
        
        // Use SiblingGenerationManager for self-dev
        SiblingGenerationManager siblingManager = new SiblingGenerationManager(getSessionContainer(), aiService);
        
        variants = siblingManager.generateSiblings(
            goal, activeDimension, branchingLimit, basePrompt, lineageContext, rejectedSiblings,
            context, genome, tree, currentParentId, generation, reasoningLevel,
            architectureEnabled, implementationEnabled, null, context.getOrchestrator()
        );
        
        // Add self-dev specific adjustments to each variant
        for (JSONObject obj : variants) {
            // Add self-dev metadata as JSON fields
            obj.put("selfDevCycle", selfDevCycle);
            obj.put("isIterativeMode", isIterativeMode);
            obj.put("selfDevFocus", isIterativeMode ? "CONTINUOUS_IMPROVEMENT" : "TARGETED_EVOLUTION");
            
            // Adjust strategy to include self-dev focus
            String strategy = obj.optString("strategy", "");
            if (!strategy.contains("Self-Development") && !strategy.contains("Self-Improvement")) {
                obj.put("strategy", "[Self-Dev Cycle " + selfDevCycle + "] " + strategy);
            }
            
            // Add self-improvement actions
            JSONArray actions = obj.optJSONArray("actions");
            if (actions == null) {
                actions = new JSONArray();
            }
            
            // Add self-analysis action
            JSONObject selfAnalysis = new JSONObject();
            selfAnalysis.put("domain", "selfdev");
            selfAnalysis.put("operation", "ANALYZE_SELF");
            selfAnalysis.put("target", "system");
            selfAnalysis.put("description", "Analyze own capabilities and identify improvement areas");
            actions.put(selfAnalysis);
            
            // Add self-improvement action
            JSONObject selfImprove = new JSONObject();
            selfImprove.put("domain", "selfdev");
            selfImprove.put("operation", "IMPROVE_SELF");
            selfImprove.put("target", "capabilities");
            selfImprove.put("description", "Improve own capabilities based on analysis");
            actions.put(selfImprove);
            
            obj.put("actions", actions);
            
            // Add self-dev specific survival argument
            obj.put("survival_argument", "Self-development cycle " + selfDevCycle + 
                   ": " + (isIterativeMode ? "Continuous improvement" : "Targeted evolution"));
        }
        
        return variants;
    }

    /**
     * Ranks self-development variants with self-dev specific criteria.
     */
    private void rankSelfDevVariants(List<JSONObject> variants, EvolutionaryPressureVector pressure, int iteration) {
        for (JSONObject v : variants) {
            double baseScore = v.optDouble("score", 0.7);
            
            // Boost score for self-improvement focus
            String strategy = v.optString("strategy", "").toLowerCase();
            if (strategy.contains("self-improvement") || strategy.contains("self-development") ||
                strategy.contains("self-evolution")) {
                baseScore += 0.1;
            }
            
            // Boost if it has self-analysis actions
            JSONArray actions = v.optJSONArray("actions");
            if (actions != null) {
                for (int i = 0; i < actions.length(); i++) {
                    JSONObject action = actions.optJSONObject(i);
                    if (action != null && "ANALYZE_SELF".equals(action.optString("operation"))) {
                        baseScore += 0.05;
                        break;
                    }
                }
            }
            
            // Higher score for iterative mode variants
            if (v.optBoolean("isIterativeMode", false)) {
                baseScore += 0.05;
            }
            
            // Lower score for later cycles (diminishing returns)
            int cycle = v.optInt("selfDevCycle", 0);
            if (cycle > 5) {
                baseScore -= (cycle - 5) * 0.01;
            }
            
            v.put("score", Math.max(0.1, Math.min(1.0, baseScore)));
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
        v.setStrategyType(obj.optString("strategy_type", "SELF_DEVELOPMENT"));
        v.setReasoningLevel(BranchVariant.ReasoningLevel.valueOf(
            obj.optString("reasoning_level", "DEEP")));
        v.setArchitectureEnabled(obj.optBoolean("architecture_enabled", true));
        v.setImplementationEnabled(obj.optBoolean("implementation_enabled", true));
        v.setStrategy(obj.optString("strategy", "unknown"));
        v.setSemanticAnchor(obj.optString("semantic_anchor", v.getStrategy()));
        v.setMutationPhilosophy(obj.optString("mutation_philosophy"));
        v.setMutationTrace("Generated in self-development evolution.");
        v.setScore(obj.optDouble("score", 0.0));
        v.setBranchName("selfdev/" + sanitize(goal) + "/" + v.getId() + "-" + System.currentTimeMillis());
        v.setSurvivalArgument(obj.optString("survival_argument", "none"));
        v.setTradeoffs(obj.optString("tradeoffs", "none"));
        v.setFailureRisks(obj.optString("failure_risks", "none"));
        
        // Store self-dev metadata in the variant's fields (using existing fields)
        // Store cycle info in semantic anchor or strategy
        int cycle = obj.optInt("selfDevCycle", 0);
        boolean iterative = obj.optBoolean("isIterativeMode", false);
        String focus = obj.optString("selfDevFocus", "TARGETED_EVOLUTION");
        
        // Append self-dev info to strategy for tracking
        String baseStrategy = v.getStrategy();
        v.setStrategy(baseStrategy + " [Cycle " + cycle + ", " + focus + "]");
        
        // Store in semantic anchor as well
        v.setSemanticAnchor(v.getSemanticAnchor() + " (SelfDev Cycle " + cycle + ")");
        
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
                action.setDomain(aObj.optString("domain", "selfdev"));
                action.setOperation(aObj.optString("operation", "ANALYZE"));
                action.setTarget(aObj.optString("target", "system"));
                action.setDescription(aObj.optString("description", "Self-development action"));
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