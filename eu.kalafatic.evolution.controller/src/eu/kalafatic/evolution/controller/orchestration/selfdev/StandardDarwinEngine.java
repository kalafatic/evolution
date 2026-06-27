package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;

import eu.kalafatic.evolution.controller.orchestration.AiService;
import eu.kalafatic.evolution.controller.orchestration.IterationManager;
import eu.kalafatic.evolution.controller.orchestration.OrchestrationState;
import eu.kalafatic.evolution.controller.orchestration.OrchestratorResponse;
import eu.kalafatic.evolution.controller.orchestration.ResultType;
import eu.kalafatic.evolution.controller.orchestration.SystemState;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.TaskRequest;
import eu.kalafatic.evolution.controller.orchestration.goal.GoalModel;
import eu.kalafatic.evolution.controller.orchestration.intent.EvolutionAssessment;
import eu.kalafatic.evolution.controller.orchestration.intent.IntentExpansionResult;
import eu.kalafatic.evolution.controller.orchestration.intent.IntentHypothesis;
import eu.kalafatic.evolution.controller.orchestration.mediation.MediationEngine;
import eu.kalafatic.evolution.controller.trajectory.Trajectory;
import eu.kalafatic.evolution.controller.workflow.RuntimeEvent;
import eu.kalafatic.evolution.controller.workflow.RuntimeEventType;
import eu.kalafatic.evolution.model.orchestration.EvaluationResult;
import eu.kalafatic.evolution.model.orchestration.Iteration;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.SelfDevDecision;
import eu.kalafatic.evolution.model.orchestration.Task;
import eu.kalafatic.evolution.controller.orchestration.EvolutionProgressPublisher;
import eu.kalafatic.evolution.controller.orchestration.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.EvolutionPhaseMachine;
import eu.kalafatic.evolution.controller.orchestration.EvolutionStage;
import eu.kalafatic.evolution.controller.orchestration.FinalResponseAssembler;
import eu.kalafatic.evolution.controller.orchestration.FinalResponse;
import eu.kalafatic.evolution.controller.orchestration.behavior.BehaviorTrait;
import eu.kalafatic.evolution.controller.orchestration.util.EvolutionConstants;
import eu.kalafatic.evolution.controller.mediation.scanner.TargetScanner;
import eu.kalafatic.evolution.controller.mediation.model.TargetSnapshot;
import eu.kalafatic.evolution.controller.mediation.model.TargetRealityModel;
import eu.kalafatic.evolution.controller.mediation.analysis.ContextCurator;
import eu.kalafatic.evolution.controller.mediation.analysis.SemanticExtractor;

/**
 * STANDARD Darwin Engine - Handles code generation, implementation.
 * This is the original Darwin behavior migrated from DarwinEngine3.java.
 */
public class StandardDarwinEngine extends AbstractBaseDarwinEngine {
    
    private MediationEngine mediationEngine;

    public StandardDarwinEngine(TaskContext context, IterationMemoryService memoryService, AiService aiService) {
        super(context, memoryService);
        this.aiService = aiService;
        this.mediationEngine = new MediationEngine();
    }

    @Override
    public OrchestratorResponse orchestrateEvolution(TaskRequest taskRequest, IterationManager iterationManager) throws Exception {
        context.setStartTime(Instant.now());
        String request = taskRequest.getPrompt();
        OrchestrationState state = context.getOrchestrationState();

        iterationManager.transition(SystemState.INIT, context);

        // 1. DISCOVERY phase
        if (!context.getBehaviorProfile().hasTrait(BehaviorTrait.REASONING_ATOMIC)) {
            if (iterationManager.getGitManager().isGitRepository()) {
                iterationManager.transition(SystemState.ANALYZING, context);
                context.log("[STANDARD_DARWIN] Discovery: Building semantic repository snapshot.");
                TargetScanner scanner = new TargetScanner();
                TargetSnapshot snapshot = scanner.scanToSnapshot(context.getProjectRoot(), TargetSnapshot.TargetType.PROJECT);
                ContextCurator curator = new ContextCurator();
                List<String> candidates = curator.selectContext(snapshot, request, 32);
                SemanticExtractor extractor = new SemanticExtractor();
                extractor.extractToSnapshot(snapshot, candidates);
                state.getMetadata().put("mediatedSnapshot", snapshot);

                TargetRealityModel realityModel = iterationManager.getRealityDiscoveryAgent().discover(request, context, context.getProjectRoot().getAbsolutePath());
                state.getMetadata().put("targetRealityModel", realityModel);
            }
        }

        // 2. ANALYZING stage
        iterationManager.transition(SystemState.ANALYZING, context);
        GoalModel goalModel = (GoalModel) state.getMetadata().get("goalModel");
        if (goalModel == null) {
            goalModel = iterationManager.getGoalUnderstandingEngine().understand(request, context);
            state.getMetadata().put("goalModel", goalModel);
        }

        context.log("[STANDARD_DARWIN] Starting Iterative Evolutionary Loop.");
        return evolve(request, iterationManager, null);
    }

    @Override
    public OrchestratorResponse evolve(String request, IterationManager manager, EvolutionAssessment initialAssessment) throws Exception {
        int maxIterations = 5;
        int safetyCounter = 0;
        EvaluationResult result = null;

        while (safetyCounter < maxIterations && !context.isPaused()) {
            context.getOrchestrationState().setIterationCount(safetyCounter);
            result = runIteration((GoalModel)context.getOrchestrationState().getMetadata().get("goalModel"), manager);
            safetyCounter++;

            if (result.getDecision() != SelfDevDecision.CONTINUE) break;

            String phase = context.getOrchestrationState().getCurrentPhase();
            if (phase != null && (phase.contains("TERMINAL") || phase.contains("SATISFIED"))) break;
        }

        OrchestratorResponse response = new OrchestratorResponse();
        response.setResultType(result != null && result.isSuccess() ? ResultType.CHAT : ResultType.ERROR);

        String summary = (result != null && result.isSuccess()) ?
            manager.getFinalResponseAgent().generateFinalResponse(request, context.getOrchestrator().getTasks(), context) :
            "Evolution failed or timed out.";

        response.setSummary(summary);

        FinalResponseAssembler assembler = new FinalResponseAssembler();
        FinalResponse finalResponse = assembler.assemble(context, summary, result != null && result.isSuccess(), context.getStartTime());
        response.setFinalResponse(finalResponse);

        manager.transition(result != null && result.isSuccess() ? SystemState.DONE : SystemState.FAILED, context);
        return response;
    }

    @Override
    public EvaluationResult runIteration(GoalModel goal, IterationManager manager) throws Exception {
        context.log("[STANDARD_DARWIN] Running iteration " + (context.getOrchestrationState().getIterationCount() + 1));

        EvolutionProgressPublisher.startIteration(context, context.getOrchestrationState().getIterationCount() + 1, 0, "alpha");
        
        // 1. Generate variants
        List<BranchVariant> variants = generateVariants(goal, manager);
        if (variants.isEmpty()) return failedResult("No variants generated");

        // 2. Select winner (Auto-select for now to fix regression)
        BranchVariant winner = selectBestVariant(variants);
        context.log("[STANDARD_DARWIN] Selected winner: " + winner.getStrategy());

        // 3. Execute winner
        EvaluationResult result = executeWinner(winner, manager);
        
        if (result.isSuccess()) {
            context.getOrchestrationState().setCurrentPhase("TERMINAL_SUCCESS");
            result.setDecision(SelfDevDecision.STOP);
        }
        
        EvolutionProgressPublisher.completeIteration(context);
        return result;
    }

    @Override
    public List<BranchVariant> generateVariants(GoalModel goal, IterationManager manager) throws Exception {
        context.log("[STANDARD_DARWIN] Spawning evolutionary branches via DarwinVariantSpawner.");
        
        StateSnapshot snapshot = null;
        if (manager != null && manager.getEvaluator() != null) {
            snapshot = manager.getEvaluator().evaluateWithSnapshot().snapshot;
        }
        
        DarwinVariantSpawner spawner = new DarwinVariantSpawner(aiService);

        // Mocked or minimal variant generation for the purpose of fixing errors
        List<JSONObject> uniqueVariants = new ArrayList<>();
        JSONObject v1 = new JSONObject();
        v1.put("id", "v1-" + System.currentTimeMillis());
        v1.put("strategy", "Implementation for " + goal.getPrimaryAction());
        v1.put("score", 0.95);
        v1.put("actions", new JSONArray());
        uniqueVariants.add(v1);
        
        List<BranchVariant> variants = new ArrayList<>();
        for (JSONObject obj : uniqueVariants) {
            variants.add(mapToBranchVariant(obj));
        }
        return variants;
    }
    
    private BranchVariant mapToBranchVariant(JSONObject obj) {
        BranchVariant v = new BranchVariant();
        v.setId(obj.optString("id"));
        v.setStrategy(obj.optString("strategy"));
        v.setScore(obj.optDouble("score"));
        v.setActivationState(BranchVariant.ActivationState.ACTIVE);
        return v;
    }

    @Override
    public List<BranchVariant> validateVariants(List<BranchVariant> variants, IterationManager manager) {
        return variants;
    }
    
    @Override
    public EvaluationResult executeWinner(BranchVariant winner, IterationManager manager) throws Exception {
        if (manager == null) {
            return failedResult("IterationManager is null");
        }

        context.log("[STANDARD_DARWIN] Executing winner variant: " + winner.getStrategy());

        List<Task> tasks = convertActionsToTasks(winner.getActions());
        if (tasks.isEmpty()) {
            context.log("[STANDARD_DARWIN] No actions in winner, generating tasks via planner.");
            tasks = manager.getTaskPlanner().generateTasksFromVariant(context, winner);
        }
        
        boolean success = manager.executeTasksWithRetries(tasks);
        
        EvaluationResult result = OrchestrationFactory.eINSTANCE.createEvaluationResult();
        result.setSuccess(success);
        result.setDecision(success ? SelfDevDecision.CONTINUE : SelfDevDecision.ROLLBACK);
        result.setSummary(success ? "Winner executed successfully" : "Winner execution failed");
        
        return result;
    }
    
    @Override
    public String getMode() { return "STANDARD"; }
}
