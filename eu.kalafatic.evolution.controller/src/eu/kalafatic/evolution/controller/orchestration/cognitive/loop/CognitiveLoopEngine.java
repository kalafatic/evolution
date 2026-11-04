package eu.kalafatic.evolution.controller.orchestration.cognitive.loop;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import eu.kalafatic.evolution.controller.kernel.AuthorityEngine;
import eu.kalafatic.evolution.controller.kernel.DefaultAuthorityEngine;
import eu.kalafatic.evolution.controller.log.Log;
import eu.kalafatic.evolution.controller.orchestration.AiService;
import eu.kalafatic.evolution.controller.orchestration.IterationManager;
import eu.kalafatic.evolution.controller.orchestration.PlatformType;
import eu.kalafatic.evolution.controller.orchestration.SessionContainer;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.llm.LlmResponse;
import eu.kalafatic.evolution.controller.orchestration.selfdev.BranchVariant;
import eu.kalafatic.evolution.controller.orchestration.selfdev.DarwinEngineFactory;
import eu.kalafatic.evolution.controller.orchestration.selfdev.IDarwinEngine;
import eu.kalafatic.evolution.controller.orchestration.selfdev.IterationMemoryService;
import eu.kalafatic.evolution.controller.orchestration.selfdev.SystemStateSignalProvider;
import eu.kalafatic.evolution.controller.orchestration.workspace.TrajectoryMemory;
import eu.kalafatic.evolution.controller.supervision.AuditRecord;
import eu.kalafatic.evolution.controller.supervision.AuthorityController;
import eu.kalafatic.evolution.controller.supervision.EvolutionDecision;
import eu.kalafatic.evolution.controller.tools.ITool;
import eu.kalafatic.evolution.controller.tools.ToolFactory;
import eu.kalafatic.evolution.controller.workflow.RuntimeEventBus;
import eu.kalafatic.evolution.forge.data.api.discovery.DataSourceCandidate;
import eu.kalafatic.evolution.forge.data.api.preference.TrainingDataPreferences;
import eu.kalafatic.evolution.forge.data.impl.discovery.HuggingFaceSourceDiscovery;
import eu.kalafatic.evolution.model.orchestration.EvaluationResult;

/**
 * Generic, domain-independent Control Loop Engine orchestrating existing EVO abstractions.
 * Operates strictly on abstract observations, goals, progress tracking, multi-dimensional budgets,
 * dynamic capability discovery, authority policy checks, and real Darwin evolutionary search escalation.
 */
public class CognitiveLoopEngine implements ICognitiveLoop {

    private final CognitiveBudget budget;
    private final AuthorityEngine authorityEngine;
    private final GoalEvaluator goalEvaluator;
    private final CapabilityDiscovery capabilityDiscovery;

    public CognitiveLoopEngine() {
        this(new CognitiveBudget(), new DefaultAuthorityEngine(new AuthorityController()));
    }

    public CognitiveLoopEngine(int maxIterations, AuthorityEngine authorityEngine) {
        this(new CognitiveBudget(maxIterations, 600000L, 3, 3), authorityEngine);
    }

    public CognitiveLoopEngine(CognitiveBudget budget, AuthorityEngine authorityEngine) {
        this.budget = budget != null ? budget : new CognitiveBudget();
        this.authorityEngine = authorityEngine != null ? authorityEngine : new DefaultAuthorityEngine(new AuthorityController());
        this.goalEvaluator = new GoalEvaluator();
        this.capabilityDiscovery = new CapabilityDiscovery();
    }

    @Override
    public CognitiveResult solve(SessionContainer session, TaskContext taskContext, CognitiveGoal goal) {
        long startTime = System.currentTimeMillis();
        String sessionId = session != null ? session.getSessionId() : (taskContext != null ? taskContext.getSessionId() : "CognitiveSession");
        RuntimeEventBus eventBus = session != null ? session.getEventBus() : null;

        WorldState worldState = new WorldState(sessionId);
        List<CognitiveObservation> observations = Collections.synchronizedList(new ArrayList<>());
        List<CognitiveDecision> decisions = Collections.synchronizedList(new ArrayList<>());

        List<CognitiveStrategy> strategyPool = initializeStrategyPool(goal, session, taskContext);

        CognitiveState currentState = CognitiveState.CREATED;
        logTrace(taskContext, eventBus, sessionId, "[COGNITIVE] Goal:\n    " + goal.getDescription());

        int iteration = 0;
        int darwinInvocations = 0;
        int consecutiveNoProgress = 0;
        double previousProgress = 0.0;

        int modelCalls = 0;
        int toolOperations = 0;

        // Stage 1: GOAL
        currentState = CognitiveState.GOAL;
        logTrace(taskContext, eventBus, sessionId, "[COGNITIVE] Lifecycle Stage -> GOAL");

        // Stage 2: UNDERSTAND (Query Genome experiences, past failures, patterns)
        currentState = CognitiveState.UNDERSTANDING;
        logTrace(taskContext, eventBus, sessionId, "[COGNITIVE] Lifecycle Stage -> UNDERSTANDING (Querying Genome previous experiences)");
        List<String> pastExperiences = retrieveGenomeExperiences(session, taskContext, goal);
        if (!pastExperiences.isEmpty()) {
            worldState.setFact("GENOME_EXPERIENCES", pastExperiences);
            logTrace(taskContext, eventBus, sessionId, "[COGNITIVE] Genome experiences retrieved: " + pastExperiences.size() + " records");
        }

        // Stage 3: OBSERVE initial state
        currentState = CognitiveState.OBSERVING;
        logTrace(taskContext, eventBus, sessionId, "[COGNITIVE] Lifecycle Stage -> OBSERVING");

        while (!budget.isExhausted(iteration, System.currentTimeMillis() - startTime, darwinInvocations, consecutiveNoProgress, modelCalls, toolOperations)) {
            // Check for explicit cancellation
            if (taskContext != null && taskContext.isCancelled()) {
                currentState = CognitiveState.CANCELLED;
                String msg = "Cognitive execution cancelled by user.";
                logTrace(taskContext, eventBus, sessionId, "[COGNITIVE] Goal CANCELLED: " + msg);
                recordEpisodeMemory(taskContext, sessionId, goal, worldState, false);
                return new CognitiveResult(sessionId, goal, currentState, observations, decisions, iteration, darwinInvocations, msg, System.currentTimeMillis() - startTime);
            }

            iteration++;

            // Select next viable untried strategy from strategy pool or active strategy
            CognitiveStrategy activeStrategy = selectNextViableStrategy(worldState, strategyPool, budget);
            if (activeStrategy == null) {
                currentState = CognitiveState.FAILED;
                String msg = "All viable strategies exhausted without satisfying goal requirements.";
                logTrace(taskContext, eventBus, sessionId, "[COGNITIVE] Goal FAILED: " + msg);
                recordEpisodeMemory(taskContext, sessionId, goal, worldState, false);
                return new CognitiveResult(sessionId, goal, currentState, observations, decisions, iteration, darwinInvocations, msg, System.currentTimeMillis() - startTime);
            }

            worldState.setActiveStrategy(activeStrategy);
            logTrace(taskContext, eventBus, sessionId, "[COGNITIVE] Iteration #" + iteration + "\n[COGNITIVE] Strategy:\n    " + activeStrategy.getSignature());

            // Check for generic stagnation and record as evidence in WorldState
            if (worldState.detectStagnation(2)) {
                logTrace(taskContext, eventBus, sessionId, "[COGNITIVE] Generic stagnation detected (repeated failure signature). Recording STAGNATION_DETECTED in WorldState.");
                worldState.setFact("STAGNATION_DETECTED", Boolean.TRUE);
            }

            // Stage 4: PLAN
            currentState = CognitiveState.PLANNING;
            logTrace(taskContext, eventBus, sessionId, "[COGNITIVE] Lifecycle Stage -> PLANNING");

            // Plan next step using available capabilities and history
            if (taskContext != null && taskContext.getAiService() != null) {
                modelCalls++;
            }
            CognitiveDecision decision = decideNextStep(session, taskContext, goal, worldState, observations, decisions, activeStrategy);
            decisions.add(decision);
            worldState.addDecision(decision);
            logTrace(taskContext, eventBus, sessionId, "[COGNITIVE] Decision proposal: " + decision);

            // Terminal failure/abort check
            if (decision.getType() == CognitiveDecisionType.ABORT || decision.getType() == CognitiveDecisionType.FAILURE) {
                currentState = CognitiveState.FAILED;
                logTrace(taskContext, eventBus, sessionId, "[COGNITIVE] Goal FAILED / ABORTED by decision: " + decision.getReasoning());
                recordEpisodeMemory(taskContext, sessionId, goal, worldState, false);
                return new CognitiveResult(sessionId, goal, currentState, observations, decisions, iteration, darwinInvocations, decision.getReasoning(), System.currentTimeMillis() - startTime);
            }

            // Stage 8 check inside loop: ADAPT & STRATEGY SHIFT
            if (decision.getType() == CognitiveDecisionType.ADAPT) {
                currentState = CognitiveState.ADAPTING;
                logTrace(taskContext, eventBus, sessionId, "[COGNITIVE] Lifecycle Stage -> ADAPTING");
                CognitiveStrategy adaptedStrat = adaptStrategy(worldState, decision, activeStrategy, strategyPool, iteration);
                if (!strategyPool.contains(adaptedStrat)) {
                    strategyPool.add(adaptedStrat);
                }
                worldState.setActiveStrategy(adaptedStrat);
                logTrace(taskContext, eventBus, sessionId, "[COGNITIVE] Strategy adapted -> " + adaptedStrat.getIdentifier() + ". Reason: " + decision.getReasoning());
                continue; // Return directly to planning loop under the new concrete strategy
            }

            // Stage 5 / Escalation: DARWIN ESCALATION
            if (decision.getType() == CognitiveDecisionType.START_DARWIN) {
                // Verify authority before Darwin execution
                if (!validateAuthority(taskContext, decision)) {
                    logTrace(taskContext, eventBus, sessionId, "[COGNITIVE] Darwin escalation rejected by Authority.");
                    CognitiveObservation rejectionObs = CognitiveObservation.ofFailure(
                            "DARWIN_SEARCH", 403, "", "Darwin escalation rejected by AuthorityController.", "AUTHORITY_DENIED", 0
                    );
                    observations.add(rejectionObs);
                    worldState.addObservation(rejectionObs);
                    consecutiveNoProgress++;
                    continue;
                }

                currentState = CognitiveState.DARWINING;
                logTrace(taskContext, eventBus, sessionId, "[COGNITIVE] State transition -> " + currentState + " (Executing Darwin evolutionary escalation)");
                darwinInvocations++;

                CognitiveObservation darwinObs = executeRealDarwinSearch(session, taskContext, goal, worldState, observations);
                observations.add(darwinObs);
                worldState.addObservation(darwinObs);

                if (darwinObs.isSuccess()) {
                    logTrace(taskContext, eventBus, sessionId, "[COGNITIVE] Real Darwin search identified winning variant: " + darwinObs.getStdout());
                    CognitiveStrategy winnerStrat = new CognitiveStrategy("DARWIN_WINNER_" + darwinInvocations, "Execute Darwin winner variant", Collections.emptyList(), Collections.emptyMap(), darwinObs.getStdout(), 0.95);
                    if (!strategyPool.contains(winnerStrat)) {
                        strategyPool.add(winnerStrat);
                    }
                    worldState.setActiveStrategy(winnerStrat);
                    consecutiveNoProgress = 0;
                } else {
                    logTrace(taskContext, eventBus, sessionId, "[COGNITIVE] Real Darwin search produced no winner: " + darwinObs.getStderr());
                }
                continue;
            }

            // Stage 5: SELECT CAPABILITY
            currentState = CognitiveState.SELECT_CAPABILITY;
            logTrace(taskContext, eventBus, sessionId, "[COGNITIVE] Lifecycle Stage -> SELECT_CAPABILITY: " + decision.getTargetCapability());

            // AUTHORITY & POLICY CHECK (Must validate before executing capability)
            if (!validateAuthority(taskContext, decision)) {
                logTrace(taskContext, eventBus, sessionId, "[COGNITIVE] Action rejected by Authority/Policy.");
                CognitiveObservation rejectionObs = CognitiveObservation.ofFailure(
                        decision.getTargetCapability() != null ? decision.getTargetCapability() : "Action",
                        403,
                        "",
                        "Action rejected by Authority/Policy rules.",
                        "AUTHORITY_DENIED",
                        0
                );
                observations.add(rejectionObs);
                worldState.addObservation(rejectionObs);
                consecutiveNoProgress++;
                continue;
            }

            // Stage 6: ACT (USE CAPABILITY / TOOL)
            currentState = CognitiveState.ACTING;
            logTrace(taskContext, eventBus, sessionId, "[COGNITIVE] Lifecycle Stage -> ACTING: executing " + decision.getTargetCapability());
            toolOperations++;

            CognitiveObservation actionObs = executeCapabilityIdempotent(session, taskContext, decision, worldState, iteration);
            observations.add(actionObs);
            worldState.addObservation(actionObs);
            worldState.recordStrategyAttempt(activeStrategy, actionObs, iteration);

            CognitiveFailureType failureType = actionObs.getFailureType();
            long usableMB = actionObs.getUsableBytes() / (1024 * 1024);
            logTrace(taskContext, eventBus, sessionId, "[COGNITIVE] Observation result: actionSuccess=" + actionObs.isSuccess() + ", usable=" + usableMB + " MB, failureType=" + failureType);

            // Stage 7: EVALUATE GOAL REQUIREMENTS
            currentState = CognitiveState.EVALUATING;
            GoalEvaluation eval = goalEvaluator.evaluate(goal, worldState, observations);
            double currentProgress = eval.getProgress();
            worldState.setCurrentProgress(currentProgress);

            double progressDelta = currentProgress - previousProgress;
            if (progressDelta <= 0.001) {
                consecutiveNoProgress++;
            } else {
                consecutiveNoProgress = 0;
            }
            previousProgress = currentProgress;

            logTrace(taskContext, eventBus, sessionId, "[COGNITIVE] Lifecycle Stage -> EVALUATING: Status=" + eval.getStatus() + ", Progress=" + String.format("%.1f%%", currentProgress * 100) + " (delta: " + String.format("%.1f%%", progressDelta * 100) + ", InfoGain: " + String.format("%.2f", worldState.getInformationGain()) + ")");

            // Stage 9: CONTINUE / COMPLETE / FAIL
            boolean hasSuccessfulObservation = observations.stream().anyMatch(CognitiveObservation::isSuccess);
            if (eval.isAchieved() && hasSuccessfulObservation && (currentProgress > 0.0 || !eval.getMissingRequirements().isEmpty())) {
                currentState = CognitiveState.SUCCESS;
                logTrace(taskContext, eventBus, sessionId, "[COGNITIVE] Goal COMPLETE / SUCCESS verified by GoalEvaluator: " + eval.getEvidence());
                recordEpisodeMemory(taskContext, sessionId, goal, worldState, true);
                return new CognitiveResult(sessionId, goal, currentState, observations, decisions, iteration, darwinInvocations, "Goal successfully verified by GoalEvaluator.", System.currentTimeMillis() - startTime);
            }

            // Learning & Concrete Adaptation trigger
            if (failureType.isFailure()) {
                if (failureType.isTransient() && worldState.getRetryCount(activeStrategy) <= budget.getMaxRetries()) {
                    logTrace(taskContext, eventBus, sessionId, "[COGNITIVE] Learning: transient failure (" + failureType + "), retry allowed (#" + worldState.getRetryCount(activeStrategy) + "/" + budget.getMaxRetries() + ")");
                } else {
                    logTrace(taskContext, eventBus, sessionId, "[COGNITIVE] Learning: strategy marked exhausted\n[COGNITIVE] Adaptation: concrete strategy " + activeStrategy.getSignature() + " excluded");
                    // Trigger concrete adaptation on the active strategy
                    CognitiveDecision adaptDec = CognitiveDecision.adapt("ADAPTED_" + activeStrategy.getIdentifier() + "_" + iteration, "Auto-adapt after failure: " + failureType);
                    CognitiveStrategy adapted = adaptStrategy(worldState, adaptDec, activeStrategy, strategyPool, iteration);
                    if (!strategyPool.contains(adapted)) {
                        strategyPool.add(adapted);
                    }
                }
            }

            int availableCandidates = countAvailableCandidates(worldState, strategyPool, budget);
            logTrace(taskContext, eventBus, sessionId, "[COGNITIVE] Decision -> CONTINUE: " + availableCandidates + " candidate strategies available");
        }

        // Distinguish timeout from budget / lack of progress
        boolean isTimeout = (System.currentTimeMillis() - startTime) >= budget.getMaxWallClockTimeMs();
        currentState = isTimeout ? CognitiveState.TIMEOUT : CognitiveState.FAILED;
        String finalMsg = isTimeout ? "Execution timed out: budget duration exceeded." : "Budget exhausted or lack of progress.";
        logTrace(taskContext, eventBus, sessionId, "[COGNITIVE] Control Loop ended: " + finalMsg);
        recordEpisodeMemory(taskContext, sessionId, goal, worldState, false);
        return new CognitiveResult(sessionId, goal, currentState, observations, decisions, iteration, darwinInvocations, finalMsg, System.currentTimeMillis() - startTime);
    }

    private List<CognitiveStrategy> initializeStrategyPool(CognitiveGoal goal, SessionContainer session, TaskContext context) {
        List<CognitiveStrategy> pool = new ArrayList<>();
        Map<String, Object> goalParams = goal != null ? goal.getParameters() : Map.of();
        Map<String, Object> meta = context != null ? context.getMetadata() : Map.of();

        String reqSource = (String) goalParams.getOrDefault("sourceType", meta.get("sourceType"));
        String reqRepo = (String) goalParams.getOrDefault("repository", meta.get("repository"));
        if (reqRepo == null) {
            String dom = (String) goalParams.getOrDefault("domain", meta.get("domain"));
            if (dom != null && dom.contains("/")) {
                reqRepo = dom.trim();
            }
        }
        String reqSplit = (String) goalParams.getOrDefault("split", meta.get("split"));

        boolean hasExplicitRepo = (reqRepo != null && !reqRepo.trim().isEmpty());

        if (reqSource != null || hasExplicitRepo) {
            String src = reqSource != null ? reqSource : "HUGGING_FACE";
            String repo = hasExplicitRepo ? reqRepo : "Salesforce/wikitext";
            String split = reqSplit != null ? reqSplit : "train";
            pool.add(CognitiveStrategy.acquisitionStrategy("EXPLICIT_REQUESTED_STRATEGY", src, repo, split, goalParams));
        }

        // Only run dynamic discovery if no explicit repository was requested
        if (!hasExplicitRepo) {
            String domain = goal != null ? goal.getTargetDomain() : "";
            String desc = goal != null ? goal.getDescription().toLowerCase() : "";
            if ("DATASET_ACQUISITION".equalsIgnoreCase(domain) || desc.contains("acquire") || desc.contains("dataset") || desc.contains("data")) {
                long targetBytes = 500L * 1024L * 1024L;
                Object tbObj = goalParams.get("targetUsableBytes");
                if (tbObj instanceof Number) targetBytes = ((Number) tbObj).longValue();

                TrainingDataPreferences prefs = TrainingDataPreferences.builder()
                        .minimumUsableBytes(targetBytes)
                        .targetUsableBytes(targetBytes)
                        .addDomain("text")
                        .build();

                HuggingFaceSourceDiscovery discovery = new HuggingFaceSourceDiscovery();
                List<DataSourceCandidate> candidates = discovery.discover(prefs);
                for (DataSourceCandidate cand : candidates) {
                    if (cand.getDatasetId() != null) {
                        String repoName = cand.getDatasetId();
                        CognitiveStrategy strat = CognitiveStrategy.acquisitionStrategy(
                                "DISCOVERED_STRAT_" + repoName.replaceAll("[^a-zA-Z0-9_]", "_"),
                                cand.getProvider(),
                                repoName,
                                "train",
                                goalParams
                        );
                        if (!pool.contains(strat)) {
                            pool.add(strat);
                        }
                    }
                }
            }
        }

        if (pool.isEmpty()) {
            pool.add(CognitiveStrategy.ofDefault("DEFAULT_BASELINE_STRATEGY"));
        }

        return pool;
    }

    private CognitiveStrategy selectNextViableStrategy(WorldState worldState, List<CognitiveStrategy> pool, CognitiveBudget budget) {
        CognitiveStrategy active = worldState.getActiveStrategy();
        if (active != null && !active.getIdentifier().startsWith("INITIAL_DEFAULT") && !worldState.isStrategyExhausted(active, budget.getMaxRetries())) {
            return active;
        }

        for (CognitiveStrategy strat : pool) {
            if (!worldState.isStrategyExhausted(strat, budget.getMaxRetries())) {
                return strat;
            }
        }
        return null;
    }

    private int countAvailableCandidates(WorldState worldState, List<CognitiveStrategy> pool, CognitiveBudget budget) {
        int count = 0;
        for (CognitiveStrategy strat : pool) {
            if (!worldState.isStrategyExhausted(strat, budget.getMaxRetries())) {
                count++;
            }
        }
        return count;
    }

    private CognitiveDecision decideNextStep(SessionContainer session, TaskContext taskContext, CognitiveGoal goal, WorldState worldState, List<CognitiveObservation> observations, List<CognitiveDecision> decisions, CognitiveStrategy activeStrategy) {
        var availableCaps = capabilityDiscovery.discoverCapabilities(session);

        if (taskContext != null) {
            try {
                AiService aiService = taskContext.getAiService();
                if (aiService != null) {
                    String prompt = buildPrompt(goal, worldState, observations, decisions, availableCaps);
                    LlmResponse response = aiService.sendLlmRequest(taskContext.getOrchestrator(), prompt, 0.7f, null, taskContext, null);
                    if (response != null && response.getFinalContent() != null) {
                        CognitiveDecision parsed = parseLlmDecision(response.getFinalContent(), availableCaps);
                        if (parsed != null && validateLlmDecision(parsed, availableCaps)) {
                            if (parsed.getCommandOrStrategy() != null && worldState.isStrategyExhausted(parsed.getCommandOrStrategy(), budget.getMaxRetries())) {
                                logTrace(taskContext, null, worldState.getSessionId(), "[COGNITIVE] Overriding LLM decision: Proposed strategy " + parsed.getCommandOrStrategy() + " is already exhausted.");
                            } else {
                                return parsed;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                logTrace(taskContext, null, taskContext.getSessionId(), "[COGNITIVE] LLM reasoning fallback due to: " + e.getMessage());
            }
        }

        if (availableCaps.isEmpty()) {
            return CognitiveDecision.of(CognitiveDecisionType.FAILURE, "NO_ACTIONABLE_CAPABILITY: No capabilities or tools registered.");
        }

        CapabilityDiscovery.CapabilityDescriptor matchedCap = selectRelevantCapability(goal, worldState, availableCaps, observations);
        if (matchedCap == null) {
            // If goal indicates synthesis or evolutionary search, propose Darwin escalation
            String goalText = (goal.getDescription() + " " + goal.getTargetDomain()).toLowerCase();
            if (goalText.contains("synthesize") || goalText.contains("evolve") || goalText.contains("darwin") || goalText.contains("optimize")) {
                return CognitiveDecision.darwin("DARWIN_ESCALATION", "Escalating to Darwin evolutionary search for synthesis goal");
            }
            return CognitiveDecision.of(CognitiveDecisionType.FAILURE, "NO_ACTIONABLE_CAPABILITY: No relevant capability matches goal requirements or strategy.");
        }

        String execCommand = buildExecutionCommand(activeStrategy, goal, worldState);
        return CognitiveDecision.capability(matchedCap.getName(), execCommand, "Executing strategy: " + activeStrategy.getSignature());
    }

    private String buildExecutionCommand(CognitiveStrategy strategy, CognitiveGoal goal, WorldState worldState) {
        Map<String, Object> params = new HashMap<>(strategy.getParameters());

        Object targetUsableObj = goal.getParameter("targetUsableBytes");
        if (targetUsableObj == null) targetUsableObj = goal.getParameter("targetMetric");

        if (targetUsableObj instanceof Number) {
            long targetBytes = ((Number) targetUsableObj).longValue();
            long accumulated = 0L;
            for (CognitiveObservation obs : worldState.getObservations()) {
                accumulated += obs.getUsableBytes();
            }
            long remaining = Math.max(0L, targetBytes - accumulated);
            params.put("targetUsableBytes", remaining);
        }

        JSONObject json = new JSONObject(params);
        return json.toString();
    }

    private CapabilityDiscovery.CapabilityDescriptor selectRelevantCapability(CognitiveGoal goal, WorldState worldState, List<CapabilityDiscovery.CapabilityDescriptor> caps, List<CognitiveObservation> obs) {
        String goalText = (goal.getDescription() + " " + goal.getTargetDomain()).toLowerCase();

        List<String> preferred = worldState.getActiveStrategy().getPreferredCapabilities();
        for (String pref : preferred) {
            for (var cap : caps) {
                if (cap.getName().equalsIgnoreCase(pref)) {
                    return cap;
                }
            }
        }

        for (var cap : caps) {
            String name = cap.getName().toLowerCase();
            if (goalText.contains(name) || (name.contains("dataset") && goalText.contains("acquire")) || (name.contains("maven") && goalText.contains("build"))) {
                return cap;
            }
        }

        return !caps.isEmpty() ? caps.get(0) : null;
    }

    private boolean validateCapabilityExists(SessionContainer session, String capName) {
        if (capName == null || capName.isEmpty()) return false;
        if (ToolFactory.getTool(capName) != null) return true;
        if (session != null && session.getCapabilityRegistry() != null && session.getCapabilityRegistry().getCapability(capName) != null) return true;
        return false;
    }

    private String buildPrompt(CognitiveGoal goal, WorldState worldState, List<CognitiveObservation> observations, List<CognitiveDecision> decisions, List<CapabilityDiscovery.CapabilityDescriptor> capabilities) {
        StringBuilder sb = new StringBuilder();
        sb.append("Goal: ").append(goal.getDescription()).append("\n");
        sb.append("Target Domain: ").append(goal.getTargetDomain()).append("\n");
        sb.append("Active Strategy: ").append(worldState.getActiveStrategy().getSignature()).append("\n");
        sb.append("Current Progress: ").append(String.format("%.1f%%", worldState.getCurrentProgress() * 100)).append("\n");
        sb.append("Available Capabilities:\n");
        for (var cap : capabilities) {
            sb.append(" - ").append(cap).append("\n");
        }
        sb.append("Recent Observations:\n");
        int start = Math.max(0, observations.size() - 5);
        for (int i = start; i < observations.size(); i++) {
            sb.append(" - ").append(observations.get(i)).append("\n");
        }
        sb.append("Produce a JSON decision response in format:\n");
        sb.append("{\"type\": \"USE_CAPABILITY\"|\"ADAPT\"|\"START_DARWIN\"|\"ABORT\", \"targetCapability\": \"...\", \"commandOrStrategy\": \"...\", \"reasoning\": \"...\"}");
        return sb.toString();
    }

    public boolean validateLlmDecision(CognitiveDecision decision, List<CapabilityDiscovery.CapabilityDescriptor> availableCaps) {
        if (decision == null || decision.getType() == null) {
            return false;
        }

        switch (decision.getType()) {
            case USE_CAPABILITY:
                if (decision.getTargetCapability() == null || decision.getTargetCapability().trim().isEmpty()) {
                    return false;
                }
                String target = decision.getTargetCapability().trim();
                boolean capExists = availableCaps.stream().anyMatch(c -> c.getName().equalsIgnoreCase(target));
                if (!capExists && ToolFactory.getTool(target) == null) {
                    return false;
                }
                return true;
            case ADAPT:
            case START_DARWIN:
            case ABORT:
            case FAILURE:
            case CONTINUE:
            case RETRY:
            case WAIT:
                return true;
            default:
                return false;
        }
    }

    private CognitiveDecision parseLlmDecision(String content, List<CapabilityDiscovery.CapabilityDescriptor> availableCaps) {
        try {
            String clean = content;
            if (clean.contains("```json")) {
                clean = clean.substring(clean.indexOf("```json") + 7);
                if (clean.contains("```")) {
                    clean = clean.substring(0, clean.indexOf("```"));
                }
            } else if (clean.contains("```")) {
                clean = clean.substring(clean.indexOf("```") + 3);
                if (clean.contains("```")) {
                    clean = clean.substring(0, clean.indexOf("```"));
                }
            }

            int start = clean.indexOf('{');
            int end = clean.lastIndexOf('}');
            if (start != -1 && end != -1 && end > start) {
                JSONObject json = new JSONObject(clean.substring(start, end + 1));
                String typeStr = json.optString("type", "USE_CAPABILITY");
                CognitiveDecisionType type;
                try {
                    type = CognitiveDecisionType.valueOf(typeStr.toUpperCase());
                } catch (IllegalArgumentException e) {
                    return null;
                }
                String cap = json.optString("targetCapability", null);
                String cmd = json.optString("commandOrStrategy", null);
                String reasoning = json.optString("reasoning", "");
                CognitiveDecision decision = new CognitiveDecision(type, cap, cmd, reasoning, null);
                if (validateLlmDecision(decision, availableCaps)) {
                    return decision;
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    public CognitiveStrategy adaptStrategy(WorldState worldState, CognitiveDecision decision, CognitiveStrategy activeStrategy, List<CognitiveStrategy> strategyPool, int iteration) {
        Map<String, Object> newParams = new HashMap<>(activeStrategy.getParameters());
        List<String> newPreferred = new ArrayList<>(activeStrategy.getPreferredCapabilities());

        if (decision.getParameters() != null && !decision.getParameters().isEmpty()) {
            newParams.putAll(decision.getParameters());
        }

        String targetCap = decision.getTargetCapability();
        if (targetCap != null && !targetCap.trim().isEmpty()) {
            if (!newPreferred.contains(targetCap)) {
                newPreferred.add(0, targetCap);
            }
        }

        // Adjust concrete execution parameters based on last failure type
        var lastObsList = worldState.getObservations();
        if (!lastObsList.isEmpty()) {
            var lastObs = lastObsList.get(lastObsList.size() - 1);
            CognitiveFailureType ft = lastObs.getFailureType();
            if (ft == CognitiveFailureType.NETWORK_FAILURE || "TIMEOUT".equalsIgnoreCase(lastObs.getStructuredError())) {
                long currentTimeout = ((Number) newParams.getOrDefault("timeoutMs", 5000L)).longValue();
                newParams.put("timeoutMs", currentTimeout * 2);
            } else if (ft == CognitiveFailureType.SOURCE_EMPTY || ft == CognitiveFailureType.SOURCE_EXHAUSTED) {
                // Switch split or fallback repository
                String currentSplit = (String) newParams.get("split");
                if ("validation".equalsIgnoreCase(currentSplit)) {
                    newParams.put("split", "train");
                } else if ("train".equalsIgnoreCase(currentSplit)) {
                    newParams.put("split", "test");
                }
                newParams.put("fallbackSourceUsed", Boolean.TRUE);
            }
        }

        String newStratId = decision.getCommandOrStrategy() != null && !decision.getCommandOrStrategy().trim().isEmpty()
                ? decision.getCommandOrStrategy()
                : activeStrategy.getIdentifier() + "_ADAPTED_" + iteration;

        String rationale = decision.getReasoning() != null && !decision.getReasoning().trim().isEmpty()
                ? decision.getReasoning()
                : "Adapted strategy due to feedback and world state";

        CognitiveStrategy adapted = new CognitiveStrategy(
                newStratId,
                activeStrategy.getObjective() + " (Adapted)",
                newPreferred,
                newParams,
                rationale,
                Math.max(0.7, activeStrategy.getConfidence() * 0.95)
        );

        return adapted;
    }

    private boolean validateAuthority(TaskContext context, CognitiveDecision decision) {
        if (decision.getType() == CognitiveDecisionType.ASK_FOR_AUTHORITY) {
            return false;
        }
        if (context == null) {
            return true;
        }

        List<BranchVariant> candidateVariants = Collections.emptyList();
        EvolutionDecision authDecision = authorityEngine.decide("COGNITIVE_ITERATION", candidateVariants, context, decision.getTargetCapability());
        if (authDecision != null && authDecision.getType() == AuthorityController.DecisionType.REJECT) {
            return false;
        }
        return true;
    }

    private CognitiveObservation executeCapabilityIdempotent(SessionContainer session, TaskContext taskContext, CognitiveDecision decision, WorldState worldState, int iteration) {
        String capName = decision.getTargetCapability();
        String stratSig = worldState.getActiveStrategy().getSignature();
        String actionKey = session != null ? session.getSessionId() : "session";
        actionKey += ":" + capName + ":" + stratSig;

        if (worldState.isActionAlreadyExecuted(actionKey)) {
            return CognitiveObservation.ofFailure(capName, 409, "", "Strategy action already executed in session: " + actionKey, "DUPLICATE_ACTION_PREVENTED", 0);
        }

        return executeCapability(session, taskContext, decision);
    }

    private CognitiveObservation executeCapability(SessionContainer session, TaskContext taskContext, CognitiveDecision decision) {
        long start = System.currentTimeMillis();
        String capName = decision.getTargetCapability();
        if (capName == null || capName.isEmpty()) {
            long duration = System.currentTimeMillis() - start;
            return CognitiveObservation.ofFailure("NONE", 400, "", "No target capability specified in decision", "CAPABILITY_UNSPECIFIED", duration);
        }

        String cmd = decision.getCommandOrStrategy() != null ? decision.getCommandOrStrategy() : "";
        File workingDir = taskContext != null && taskContext.getProjectRoot() != null ? taskContext.getProjectRoot() : new File(".");

        try {
            ITool tool = ToolFactory.getTool(capName);
            if (tool == null && session != null && session.getCapabilityRegistry() != null) {
                var cap = session.getCapabilityRegistry().getCapability(capName);
                if (cap instanceof ITool) {
                    tool = (ITool) cap;
                }
            }

            if (tool != null) {
                String out = tool.execute(cmd, workingDir, taskContext);
                long duration = System.currentTimeMillis() - start;

                Map<String, Object> meta = new HashMap<>();
                if (out != null && out.trim().startsWith("{")) {
                    try {
                        JSONObject jsonOut = new JSONObject(out);
                        for (Object keyObj : jsonOut.keySet()) {
                            String key = String.valueOf(keyObj);
                            meta.put(key, jsonOut.get(key));
                        }
                    } catch (Exception ignored) {}
                }

                if (taskContext != null) {
                    meta.putAll(taskContext.getMetadata());
                }

                boolean success = true;
                Object failTypeObj = meta.get("failureType");
                if (failTypeObj instanceof String) {
                    CognitiveFailureType ft = CognitiveFailureType.fromString((String) failTypeObj);
                    if (ft.isFailure()) {
                        success = false;
                    }
                } else if (meta.containsKey("acquisitionStatus")) {
                    String st = (String) meta.get("acquisitionStatus");
                    if ("INSUFFICIENT_SOURCE_DATA".equalsIgnoreCase(st) || "FAILED".equalsIgnoreCase(st)) {
                        success = false;
                    }
                }

                return new CognitiveObservation(capName, success, success ? 0 : 1, out, "", success ? null : (String) meta.getOrDefault("failureType", "ACTION_FAILED"), duration, meta);
            }

            long duration = System.currentTimeMillis() - start;
            return CognitiveObservation.ofFailure(capName, 404, "", "Capability or tool unavailable: " + capName, "CAPABILITY_UNAVAILABLE", duration);
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - start;
            return CognitiveObservation.ofFailure(capName, 1, "", e.getMessage(), "EXECUTION_EXCEPTION", duration);
        }
    }

    private CognitiveObservation executeRealDarwinSearch(SessionContainer session, TaskContext taskContext, CognitiveGoal goal, WorldState worldState, List<CognitiveObservation> observations) {
        long start = System.currentTimeMillis();
        try {
            if (taskContext == null || session == null) {
                long duration = System.currentTimeMillis() - start;
                return CognitiveObservation.ofFailure("DARWIN_SEARCH", 500, "", "DARWIN_UNAVAILABLE: TaskContext or SessionContainer is null.", "CONTEXT_MISSING", duration);
            }

            IterationManager manager = session.getIterationManager();
            if (manager == null) {
                try {
                    String desc = (goal != null && goal.getDescription() != null) ? goal.getDescription() : "Darwin Execution";
                    manager = eu.kalafatic.evolution.controller.orchestration.KernelFactory.create(desc, taskContext, session);
                    session.setIterationManager(manager);
                } catch (Exception e) {
                    logTrace(taskContext, null, session.getSessionId(), "[COGNITIVE] Failed to initialize IterationManager for session: " + e.getMessage());
                    long duration = System.currentTimeMillis() - start;
                    return CognitiveObservation.ofFailure("DARWIN_SEARCH", 500, "", "DARWIN_UNAVAILABLE: Could not initialize IterationManager: " + e.getMessage(), "INITIALIZATION_FAILED", duration);
                }
            }

            IDarwinEngine darwin = manager.getDarwinEngine();
            if (darwin == null) {
                long duration = System.currentTimeMillis() - start;
                return CognitiveObservation.ofFailure("DARWIN_SEARCH", 500, "", "DARWIN_UNAVAILABLE: Authoritative DarwinEngine not configured on IterationManager.", "ENGINE_MISSING", duration);
            }

            EvaluationResult evalResult = darwin.runDarwinIteration(taskContext, manager);
            long duration = System.currentTimeMillis() - start;
            if (evalResult != null && evalResult.isSuccess()) {
                String decisionStr = evalResult.getDecision() != null ? evalResult.getDecision().name() : "APPROVED";
                return CognitiveObservation.ofSuccess("DARWIN_SEARCH", "DARWIN_WINNER_FOUND: Selected superior candidate variant (" + decisionStr + ")", duration);
            } else {
                String errorMsg = (evalResult != null && !evalResult.getErrors().isEmpty())
                        ? String.join("; ", evalResult.getErrors())
                        : "Evolutionary iteration executed without promoting a winner.";
                return CognitiveObservation.ofFailure("DARWIN_SEARCH", 1, "", "DARWIN_NO_WINNER: " + errorMsg, "NO_WINNER", duration);
            }
        } catch (java.util.concurrent.TimeoutException e) {
            return CognitiveObservation.ofFailure("DARWIN_SEARCH", 408, "", "Darwin search timed out: " + e.getMessage(), "TIMEOUT", System.currentTimeMillis() - start);
        } catch (java.util.concurrent.CancellationException e) {
            return CognitiveObservation.ofFailure("DARWIN_SEARCH", 499, "", "Darwin search cancelled: " + e.getMessage(), "CANCELLED", System.currentTimeMillis() - start);
        } catch (Exception e) {
            return CognitiveObservation.ofFailure("DARWIN_SEARCH", 1, "", e.getMessage(), "DARWIN_EXCEPTION", System.currentTimeMillis() - start);
        }
    }

    public List<String> retrieveGenomeExperiences(SessionContainer session, TaskContext taskContext, CognitiveGoal goal) {
        List<String> experiences = new ArrayList<>();
        try {
            IterationMemoryService memoryService = null;
            if (taskContext != null && taskContext.getKernelContext() != null) {
                memoryService = taskContext.getKernelContext().getMemoryService();
            }
            if (memoryService == null && taskContext != null && taskContext.getProjectRoot() != null) {
                try {
                    memoryService = new IterationMemoryService(taskContext.getProjectRoot());
                } catch (Exception ignored) {}
            }

            if (memoryService != null) {
                String goalText = (goal != null && goal.getDescription() != null) ? goal.getDescription() : "";
                var successful = memoryService.findSuccessfulPatterns(goalText);
                for (var rec : successful) {
                    experiences.add("SUCCESSFUL_PATTERN: Strategy=" + rec.getStrategy() + ", Branch=" + rec.getBranch());
                }

                var failureMem = memoryService.getFailureMemory();
                if (failureMem != null && failureMem.getStrategyFailures() != null) {
                    for (var entry : failureMem.getStrategyFailures().entrySet()) {
                        experiences.add("KNOWN_FAILED_STRATEGY: " + entry.getKey() + " (failures=" + entry.getValue() + ")");
                    }
                }
                if (failureMem != null && failureMem.getFingerprints() != null) {
                    for (var entry : failureMem.getFingerprints().entrySet()) {
                        experiences.add("FAILURE_FINGERPRINT: " + entry.getKey() + " (count=" + entry.getValue() + ")");
                    }
                }
            }

            // Check SemanticGenome if stored in context metadata
            if (taskContext != null && taskContext.getOrchestrationState() != null) {
                Object genomeObj = taskContext.getOrchestrationState().getMetadata().get("semanticGenome");
                if (genomeObj instanceof eu.kalafatic.evolution.controller.orchestration.selfdev.SemanticGenome) {
                    var genome = (eu.kalafatic.evolution.controller.orchestration.selfdev.SemanticGenome) genomeObj;
                    for (var rej : genome.getRejectedMutations()) {
                        experiences.add("GENOME_REJECTED_MUTATION: " + rej.getStrategy() + " (Philosophy: " + rej.getPhilosophy() + ")");
                    }
                    for (var disc : genome.getDiscoveredMutations()) {
                        experiences.add("GENOME_ACCEPTED_MUTATION: " + disc.getStrategy());
                    }
                }
            }
        } catch (Exception ignored) {}
        return experiences;
    }

    private void recordEpisodeMemory(TaskContext taskContext, String sessionId, CognitiveGoal goal, WorldState worldState, boolean success) {
        if (taskContext == null) return;
        try {
            IterationMemoryService memoryService = taskContext.getKernelContext() != null ? taskContext.getKernelContext().getMemoryService() : null;
            if (memoryService != null) {
                AuditRecord record = new AuditRecord(
                        "COGNITIVE_EPISODE",
                        worldState.getActiveStrategy().getIdentifier(),
                        "EPISODE_COMPLETED",
                        "RUNNING",
                        success ? "SUCCESS" : "FAILED",
                        "CognitiveLoopEngine",
                        "Goal: " + goal.getDescription() + " (Final progress: " + String.format("%.1f%%", worldState.getCurrentProgress() * 100) + ", InfoGain: " + String.format("%.2f", worldState.getInformationGain()) + ")",
                        sessionId
                );
                memoryService.appendAuditRecord(record);

                TrajectoryMemory trajMem = memoryService.getTrajectoryMemory();
                if (trajMem != null && success) {
                    trajMem.recordSuccessfulStrategy(worldState.getActiveStrategy().getIdentifier());
                }

                if (!success && memoryService.getFailureMemory() != null) {
                    memoryService.getFailureMemory().recordStrategyFailure(worldState.getActiveStrategy().getSignature());
                }
            }
        } catch (Exception ignored) {}
    }

    private void logTrace(TaskContext taskContext, RuntimeEventBus eventBus, String sessionId, String message) {
        Log.log("[" + sessionId + "] " + message);
        if (taskContext != null) {
            taskContext.log(message);
        }
        if (eventBus != null) {
            eventBus.publish(new eu.kalafatic.evolution.controller.workflow.RuntimeEvent(
                eu.kalafatic.evolution.controller.workflow.RuntimeEventType.VIEW_UPDATED,
                sessionId,
                "CognitiveLoopEngine",
                message
            ));
        }
    }
}
