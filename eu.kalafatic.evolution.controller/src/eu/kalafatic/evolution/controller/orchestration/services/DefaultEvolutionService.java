package eu.kalafatic.evolution.controller.orchestration.services;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Collectors;

import eu.kalafatic.evolution.controller.orchestration.IterationManager;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.OrchestrationState;
import eu.kalafatic.evolution.controller.orchestration.SystemState;
import eu.kalafatic.evolution.controller.orchestration.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.EvolutionPhaseMachine;
import eu.kalafatic.evolution.controller.orchestration.EvolutionStage;
import eu.kalafatic.evolution.controller.orchestration.EvolutionProgressPublisher;
import eu.kalafatic.evolution.controller.orchestration.OrchestratorResponse;
import eu.kalafatic.evolution.controller.orchestration.TaskRequest;
import eu.kalafatic.evolution.controller.orchestration.ResultType;
import eu.kalafatic.evolution.controller.orchestration.goal.GoalModel;
import eu.kalafatic.evolution.controller.orchestration.goal.SemanticEnvelope;
import eu.kalafatic.evolution.controller.orchestration.selfdev.IterationRecord;
import eu.kalafatic.evolution.controller.orchestration.selfdev.BranchVariant;
import eu.kalafatic.evolution.controller.orchestration.selfdev.AbstractionLevel;
import eu.kalafatic.evolution.controller.orchestration.intent.EvolutionAssessment;
import eu.kalafatic.evolution.controller.orchestration.intent.IntentExpansionResult;
import eu.kalafatic.evolution.controller.orchestration.intent.ClarificationPlanner;
import eu.kalafatic.evolution.controller.supervision.EvolutionDecision;
import eu.kalafatic.evolution.controller.trajectory.Trajectory;
import eu.kalafatic.evolution.controller.workflow.RuntimeEvent;
import eu.kalafatic.evolution.controller.workflow.RuntimeEventType;
import eu.kalafatic.evolution.model.orchestration.SelfDevDecision;
import eu.kalafatic.evolution.model.orchestration.EvaluationResult;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.PromptInstructions;
import eu.kalafatic.evolution.model.orchestration.ChatSession;
import eu.kalafatic.evolution.controller.orchestration.behavior.BehaviorTrait;
import eu.kalafatic.evolution.controller.orchestration.diagnostics.CausalNode;

public class DefaultEvolutionService implements EvolutionService {
    private final GenerationService generationService;
    private final WinnerService winnerService;

    public DefaultEvolutionService(GenerationService generationService, WinnerService winnerService) {
        this.generationService = generationService;
        this.winnerService = winnerService;
    }

    @Override
    public OrchestratorResponse orchestrate(TaskRequest taskRequest, IterationManager manager) throws Exception {
        return manager.getDarwinEngine().orchestrateEvolution(taskRequest, manager);
    }

    @Override
    public void evolve(String request, IterationManager manager, EvolutionAssessment initialAssessment) throws Exception {
        evolve(manager.getContext(), manager);
    }

    @Override
    public void evolve(TaskContext context, IterationManager iterationManager) throws Exception {
        // ADAPTIVE KERNEL: Ensure execution profile is initialized before access
        if (context.getExecutionProfile() == null) {
            eu.kalafatic.evolution.controller.kernel.EvolutionProfile profile_init =
                eu.kalafatic.evolution.controller.kernel.EvolutionIntensityCalculator.calculate(context, iterationManager.getActiveTrajectory(context), null);
            context.getOrchestrationState().setExecutionProfile(profile_init);
        }

        String request = context.getOrchestrationState().getRawInput();
        iterationManager.getSessionContainer().getEventBus().publish(new RuntimeEvent(RuntimeEventType.FLOW_STARTED, context.getSessionId(), "DarwinEngine", request));

        OrchestrationState state = context.getOrchestrationState();
        int safetyCounter = 0;
        int expansionValue = 5;
        if (context.getOrchestrator() != null && context.getOrchestrator().getAiChat() != null) {
            String sessionId = context.getSessionId();
            ChatSession chatSession = context.getOrchestrator().getAiChat().getSessions().stream()
                .filter(s -> s.getId().equals(sessionId)).findFirst().orElse(null);
            if (chatSession != null) {
                expansionValue = chatSession.getExpansion();
            }
        }

        int intensity_val = context.getExecutionProfile().getIntensity();
        int minIterations = 1;
        PromptInstructions instructions = (context.getOrchestrator() != null && context.getOrchestrator().getAiChat() != null) ?
                context.getOrchestrator().getAiChat().getPromptInstructions() : null;
        if (instructions != null) {
            minIterations = instructions.getPreferredMaxIterations();
        }

        if (minIterations < 2 && intensity_val >= 1 && context.getExecutionProfile().getCapability() != eu.kalafatic.evolution.controller.orchestration.cognitive.CapabilityType.CHAT) {
            minIterations = 2;
        }

        if (minIterations < 1) minIterations = 1;

        int maxIterationsLimit = 20;
        if (intensity_val == 1) maxIterationsLimit = 10;
        else if (intensity_val == 2) maxIterationsLimit = 15;
        else if (expansionValue <= 3) maxIterationsLimit = 25;
        else if (expansionValue >= 8) maxIterationsLimit = 100;
        maxIterationsLimit = Math.max(maxIterationsLimit, minIterations);

        EvaluationResult result = null;
        while (safetyCounter < maxIterationsLimit && !context.isPaused()) {
            state.setIterationCount(safetyCounter);
            if (safetyCounter > 0 && intensity_val >= 3) {
                iterationManager.refineTargetReality(request, context);
            }

            result = runDarwinIteration(context, iterationManager);
            safetyCounter++;

            Trajectory activeTrajectory = iterationManager.getActiveTrajectory(context);
            if (activeTrajectory != null && !iterationManager.isIntentExpansionPhase(context)) {
                boolean stabilized = iterationManager.getEvolutionaryTrajectoryEngine().evolve(activeTrajectory, context);
                if (stabilized) {
                    context.log("[DARWIN] [LOOP] Evolutionary equilibrium detected for trajectory " + activeTrajectory.getTrajectoryId() + ". Converging.");
                }
            }

            iterationManager.saveFullCheckpoint();

            if (result.getDecision() != SelfDevDecision.CONTINUE) {
                if (safetyCounter < minIterations) {
                    state.setCurrentPhase(EvolutionPhaseMachine.toLegacyString(EvolutionPhase.SELECTION_REFINEMENT));
                    if (context.getStateHolder().getState() == SystemState.DONE) {
                        iterationManager.transition(SystemState.INIT, context);
                    }
                } else {
                    iterationManager.getSessionContainer().getEventBus().publish(new RuntimeEvent(RuntimeEventType.FLOW_COMPLETED, context.getSessionId(), "DarwinEngine", result.getDecision().toString()));
                    break;
                }
            }

            String currentPhaseStr = state.getCurrentPhase();
            if (currentPhaseStr != null && (currentPhaseStr.contains("TERMINAL") || currentPhaseStr.contains("SATISFIED"))) {
                if (safetyCounter < minIterations) {
                    state.setCurrentPhase(EvolutionPhaseMachine.toLegacyString(EvolutionPhase.SELECTION_REFINEMENT));
                    if (context.getStateHolder().getState() == SystemState.DONE) {
                        iterationManager.transition(SystemState.INIT, context);
                    }
                } else {
                    break;
                }
            }
        }
    }

    @Override
    public EvaluationResult runDarwinIteration(TaskContext context, IterationManager iterationManager) throws Exception {
        SystemState currentState = context.getStateHolder().getState();
        if (currentState == SystemState.DONE || currentState == SystemState.FAILED) {
            iterationManager.transition(SystemState.INIT, context);
        }

        eu.kalafatic.evolution.controller.kernel.EvolutionProfile executionProfile =
            eu.kalafatic.evolution.controller.kernel.EvolutionIntensityCalculator.calculate(context, iterationManager.getActiveTrajectory(context), null);
        context.getOrchestrationState().setExecutionProfile(executionProfile);

        OrchestrationState state = context.getOrchestrationState();
        String goal = state.getRawInput();
        if (goal == null || goal.isEmpty()) {
            goal = context.getOrchestrator().getSelfDevSession() != null ? context.getOrchestrator().getSelfDevSession().getInitialRequest() : "Autonomous Improvement";
        }

        EvolutionPhaseMachine phaseMachine = new EvolutionPhaseMachine();
        EvolutionPhase phase = state.getCurrentPhase() != null ? EvolutionPhase.fromString(state.getCurrentPhase()) : phaseMachine.getInitialPhase();

        state.setCurrentPhase(EvolutionPhaseMachine.toLegacyString(phase));
        if (iterationManager.getCurrentIterationModel() != null) {
            iterationManager.getCurrentIterationModel().setPhase(state.getCurrentPhase());
        }

        Object goalModelObj = state.getMetadata().get("goalModel");
        GoalModel goalModel = eu.kalafatic.evolution.controller.parsers.JsonUtils.restoreFromMetadata(goalModelObj, GoalModel.class, "goalModel", context);
        if (goalModel != null && goalModel != goalModelObj) {
            state.getMetadata().put("goalModel", goalModel);
        }

        if (goalModel == null) {
            goalModel = iterationManager.getGoalUnderstandingEngine().understand(goal, context);
            state.getMetadata().put("goalModel", goalModel);
        }

        if (state.getLockedAbstractionLevel() == null) {
            AbstractionLevel lockedLevel = AbstractionLevel.DESIGN;
            String complexity = goalModel.getComplexity() != null ? goalModel.getComplexity().toUpperCase() : "MEDIUM";
            if ("SIMPLE".equals(complexity)) lockedLevel = AbstractionLevel.IMPLEMENTATION;
            else if ("HIGH".equals(complexity)) lockedLevel = AbstractionLevel.ARCHITECTURE;
            state.setLockedAbstractionLevel(lockedLevel);
        }

        Object envelopeObj = state.getMetadata().get("semanticEnvelope");
        SemanticEnvelope envelope = eu.kalafatic.evolution.controller.parsers.JsonUtils.restoreFromMetadata(envelopeObj, SemanticEnvelope.class, "semanticEnvelope", context);
        if (envelope != null && envelope != envelopeObj) {
            state.getMetadata().put("semanticEnvelope", envelope);
        }

        if (envelope == null) {
            envelope = iterationManager.getSemanticEnvelopeEngine().derive(goalModel, context);
            state.getMetadata().put("semanticEnvelope", envelope);
        }

        Trajectory activeTrajectory = iterationManager.getActiveTrajectory(context);
        int generation = activeTrajectory != null ? activeTrajectory.getGeneration() : 0;
        String lineage = activeTrajectory != null ? activeTrajectory.getTrajectoryId() : "alpha";

        EvolutionProgressPublisher.startIteration(context, state.getIterationCount() + 1, generation, lineage);
        EvolutionProgressPublisher.updateStage(context, EvolutionStage.ANALYSIS);

        if (phase == EvolutionPhase.TERMINAL_SUCCESS || phase == EvolutionPhase.TERMINAL_FAILURE || phase == EvolutionPhase.DESIGN_SATISFIED) {
            EvaluationResult res = OrchestrationFactory.eINSTANCE.createEvaluationResult();
            res.setSuccess(phase != EvolutionPhase.TERMINAL_FAILURE);
            res.setDecision(SelfDevDecision.STOP);
            return res;
        }

        if (phase == EvolutionPhase.INTENT_EXPANSION) {
            IntentExpansionResult expansion = iterationManager.getIntentExpansionEngine().expand(goal, context);
            state.getMetadata().put("intentExpansion", expansion);

            if (!iterationManager.handleIntentReview(context, expansion, goal)) {
                return iterationManager.failedResult();
            }

            ClarificationPlanner planner = iterationManager.getClarificationPlanner();
            ClarificationPlanner.Strategy strategy = planner.determineStrategy(expansion, context);

            boolean isStepMode = context.getOrchestrator().getAiChat() != null &&
                               context.getOrchestrator().getAiChat().getPromptInstructions() != null &&
                               context.getOrchestrator().getAiChat().getPromptInstructions().isStepMode();

            if (strategy == ClarificationPlanner.Strategy.CLARIFY_USER && !isStepMode) {
                if (context.isAutoApprove()) {
                    strategy = ClarificationPlanner.Strategy.AUTO_INFER;
                } else {
                    strategy = ClarificationPlanner.Strategy.BRANCH_PARALLEL;
                }
            }

            if (strategy == ClarificationPlanner.Strategy.CLARIFY_USER) {
                if (!iterationManager.handleClarification(context, planner, expansion, goal)) {
                    return iterationManager.failedResult();
                }
                EvaluationResult res = OrchestrationFactory.eINSTANCE.createEvaluationResult();
                res.setSuccess(true);
                res.setDecision(SelfDevDecision.CONTINUE);
                return res;
            }

            EvolutionPhase nextPhase = iterationManager.getEvolutionaryTrajectoryEngine().determineNextPhase(phase, null, context);
            state.setCurrentPhase(EvolutionPhaseMachine.toLegacyString(nextPhase));
            state.setIterationCount(state.getIterationCount() + 1);

            EvaluationResult res = OrchestrationFactory.eINSTANCE.createEvaluationResult();
            res.setSuccess(true);
            res.setDecision(SelfDevDecision.CONTINUE);
            return res;
        }

        iterationManager.checkStep(state.getCurrentPhase(), "BRANCH_GENERATION", "Spawning competing trajectories for: " + goal);
        List<BranchVariant> variants = generationService.generateProposals(context, goalModel, iterationManager);

        if (variants.isEmpty()) {
            return iterationManager.failedResult();
        }

        String manualId = null;
        if (state.getMetadata().containsKey("pendingControlCommand")) {
            String pendingCommand = (String) state.getMetadata().remove("pendingControlCommand");
            if (pendingCommand.toLowerCase().startsWith("select ") || pendingCommand.toLowerCase().startsWith("approve variant ")) {
                manualId = pendingCommand.toLowerCase().startsWith("select ") ? pendingCommand.substring(7).trim() : pendingCommand.substring(16).trim();
            } else if (pendingCommand.equalsIgnoreCase("approved") || pendingCommand.equalsIgnoreCase("proceed") || pendingCommand.equalsIgnoreCase("yes") || pendingCommand.equalsIgnoreCase("force solution")) {
                manualId = variants.stream()
                        .max((v1, v2) -> Double.compare(v1.getScore(), v2.getScore()))
                        .map(v -> v.getId())
                        .orElse(null);
            }
        }

        if (manualId == null && !context.isAutoApprove()) {
            if (executionProfile.requireUserSelection()) {
                manualId = winnerService.handleVariantSelection(context, variants, goal, iterationManager);
                if ("REGENERATE".equals(manualId)) {
                    return runDarwinIteration(context, iterationManager);
                }
                if (manualId == null || "STOP".equals(manualId) || "FAILED".equals(manualId)) {
                    EvaluationResult res = iterationManager.failedResult();
                    res.setDecision(SelfDevDecision.STOP);
                    return res;
                }
            } else {
                manualId = iterationManager.getSelectionEngine().selectWinnerAuto(variants);
            }
        }

        String iterId = iterationManager.getCurrentIterationModel() != null ? iterationManager.getCurrentIterationModel().getId() : "default";
        EvolutionDecision decision = winnerService.decide(iterId, variants, context, manualId, iterationManager);

        if (activeTrajectory != null) {
            decision.setPressure(iterationManager.getSessionContainer().getPressureEngine().analyze(activeTrajectory, context));
        }

        iterationManager.transition(SystemState.EXECUTING, context);
        EvaluationResult result = winnerService.processWinners(context, decision, variants, goalModel, iterationManager);
        iterationManager.transition(SystemState.VERIFYING, context);

        if (result.isSuccess()) {
            EvolutionPhase currentPhaseEnum = EvolutionPhase.fromString(state.getCurrentPhase());

            EvolutionPhase nextPhase = iterationManager.getEvolutionaryTrajectoryEngine().determineNextPhase(currentPhaseEnum, iterationManager.getActiveTrajectory(context), context);
            state.setIterationCount(state.getIterationCount() + 1);

            state.setCurrentPhase(EvolutionPhaseMachine.toLegacyString(nextPhase));

            if (nextPhase == EvolutionPhase.DESIGN_SATISFIED) {
                iterationManager.handleSatisfactionReview(context, iterationManager.getActiveTrajectory(context));
                nextPhase = EvolutionPhase.fromString(state.getCurrentPhase());
            }

            result.setDecision(phaseMachine.determineDecision(nextPhase));

            if (!iterationManager.handlePhaseConfirmation(context, state)) {
                result.setDecision(SelfDevDecision.STOP);
            }

            EvolutionProgressPublisher.completeIteration(context);
            iterationManager.transition(SystemState.DONE, context);
        } else {
            EvolutionProgressPublisher.completeIteration(context);
            iterationManager.transition(SystemState.FAILED, context);
        }

        return result;
    }
}
