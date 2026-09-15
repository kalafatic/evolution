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

        currentState = CognitiveState.OBSERVING;
        logTrace(taskContext, eventBus, sessionId, "[COGNITIVE] State transition -> " + currentState);

        while (!budget.isExhausted(iteration, System.currentTimeMillis() - startTime, darwinInvocations, consecutiveNoProgress)) {
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

            CognitiveStrategy previousActive = worldState.getActiveStrategy();
            if (previousActive != null && !previousActive.isEquivalent(activeStrategy)) {
                consecutiveNoProgress = 0; // Reset consecutive no-progress counter when rotating to a new untried strategy candidate
            }

            worldState.setActiveStrategy(activeStrategy);
            logTrace(taskContext, eventBus, sessionId, "[COGNITIVE] Iteration #" + iteration + "\n[COGNITIVE] Strategy:\n    " + activeStrategy.getSignature());

            // Check for generic stagnation and record as evidence in WorldState
            if (worldState.detectStagnation(2)) {
                logTrace(taskContext, eventBus, sessionId, "[COGNITIVE] Generic stagnation detected (repeated failure signature). Recording STAGNATION_DETECTED in WorldState.");
                worldState.setFact("STAGNATION_DETECTED", Boolean.TRUE);
            }

            // 1. UNDERSTAND & PLAN
            currentState = CognitiveState.UNDERSTANDING;
            logTrace(taskContext, eventBus, sessionId, "[COGNITIVE] State transition -> " + currentState);

            CognitiveDecision decision = decideNextStep(session, taskContext, goal, worldState, observations, decisions, activeStrategy);
            decisions.add(decision);
            worldState.addDecision(decision);
            logTrace(taskContext, eventBus, sessionId, "[COGNITIVE] Decision proposal: " + decision);

            // Terminal failure/abort check
            if (decision.getType() == CognitiveDecisionType.ABORT || decision.getType() == CognitiveDecisionType.FAILURE) {
                currentState = CognitiveState.FAILED;
                logTrace(taskContext, eventBus, sessionId, "[COGNITIVE] Goal FAILED / ABORTED by decision.");
                recordEpisodeMemory(taskContext, sessionId, goal, worldState, false);
                return new CognitiveResult(sessionId, goal, currentState, observations, decisions, iteration, darwinInvocations, decision.getReasoning(), System.currentTimeMillis() - startTime);
            }

            // 2. ADAPT & STRATEGY SHIFT
            if (decision.getType() == CognitiveDecisionType.ADAPT) {
                currentState = CognitiveState.ADAPTING;
                String newStratId = decision.getCommandOrStrategy() != null ? decision.getCommandOrStrategy() : "ADAPTED_STRATEGY_" + iteration;
                CognitiveStrategy newStrat = new CognitiveStrategy(newStratId, decision.getReasoning(), Collections.emptyList(), decision.getParameters(), decision.getReasoning(), decision.getConfidence());

                if (!strategyPool.contains(newStrat)) {
                    strategyPool.add(newStrat);
                }
                worldState.setActiveStrategy(newStrat);
                logTrace(taskContext, eventBus, sessionId, "[COGNITIVE] Strategy adapted -> " + newStratId + ". Reason: " + decision.getReasoning() + "\n[COGNITIVE] Transition: EVALUATING -> PLANNING");
                continue; // Return directly to planning loop under the new strategy
            }

            // 3. REAL DARWIN ESCALATION
            if (decision.getType() == CognitiveDecisionType.START_DARWIN) {
                currentState = CognitiveState.DARWINING;
                logTrace(taskContext, eventBus, sessionId, "[COGNITIVE] State transition -> " + currentState + " (Executing real Darwin evolutionary search)");
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
                    logTrace(taskContext, eventBus, sessionId, "[COGNITIVE] Real Darwin search produced no winner. " + darwinObs.getStderr());
                }
                logTrace(taskContext, eventBus, sessionId, "[COGNITIVE] Transition: EVALUATING -> PLANNING");
                continue;
            }

            // 4. AUTHORITY & POLICY CHECK
            currentState = CognitiveState.PLANNING;
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
                logTrace(taskContext, eventBus, sessionId, "[COGNITIVE] Transition: EVALUATING -> PLANNING");
                continue;
            }

            // 5. ACT (USE CAPABILITY / TOOL)
            currentState = CognitiveState.ACTING;
            logTrace(taskContext, eventBus, sessionId, "[COGNITIVE] State transition -> " + currentState + " executing capability: " + decision.getTargetCapability());

            CognitiveObservation actionObs = executeCapabilityIdempotent(session, taskContext, decision, worldState, iteration);
            observations.add(actionObs);
            worldState.addObservation(actionObs);
            worldState.recordStrategyAttempt(activeStrategy, actionObs, iteration);

            CognitiveFailureType failureType = actionObs.getFailureType();
            long usableMB = actionObs.getUsableBytes() / (1024 * 1024);
            logTrace(taskContext, eventBus, sessionId, "[COGNITIVE] Result:\n    usable=" + usableMB + " MB\n    failure=" + failureType);

            // 6. EVALUATE GOAL REQUIREMENTS
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

            logTrace(taskContext, eventBus, sessionId, "[COGNITIVE] State transition -> " + currentState + " Eval Status: " + eval.getStatus() + ", Progress: " + String.format("%.1f%%", currentProgress * 100) + " (delta: " + String.format("%.1f%%", progressDelta * 100) + ")");

            if (eval.isAchieved()) {
                currentState = CognitiveState.SUCCESS;
                logTrace(taskContext, eventBus, sessionId, "[COGNITIVE] Goal SUCCESS verified by GoalEvaluator: " + eval.getEvidence());
                recordEpisodeMemory(taskContext, sessionId, goal, worldState, true);
                return new CognitiveResult(sessionId, goal, currentState, observations, decisions, iteration, darwinInvocations, "Goal successfully verified by GoalEvaluator.", System.currentTimeMillis() - startTime);
            }

            // Learning & Adaptation
            if (failureType.isFailure()) {
                if (failureType.isTransient() && worldState.getRetryCount(activeStrategy) <= budget.getMaxRetries()) {
                    logTrace(taskContext, eventBus, sessionId, "[COGNITIVE] Learning:\n    transient failure (" + failureType + "), retry allowed (#" + worldState.getRetryCount(activeStrategy) + "/" + budget.getMaxRetries() + ")");
                } else {
                    logTrace(taskContext, eventBus, sessionId, "[COGNITIVE] Learning:\n    strategy marked exhausted\n[COGNITIVE] Adaptation:\n    strategy " + activeStrategy.getSignature() + " excluded");
                }
            }

            int availableCandidates = countAvailableCandidates(worldState, strategyPool, budget);
            logTrace(taskContext, eventBus, sessionId, "[COGNITIVE] Candidate strategies:\n    " + availableCandidates + " available\n[COGNITIVE] Transition: EVALUATING -> PLANNING");
        }

        currentState = CognitiveState.FAILED;
        logTrace(taskContext, eventBus, sessionId, "[COGNITIVE] Control Loop budget exhausted or no progress remaining.");
        recordEpisodeMemory(taskContext, sessionId, goal, worldState, false);
        return new CognitiveResult(sessionId, goal, currentState, observations, decisions, iteration, darwinInvocations, "Budget exhausted or lack of progress.", System.currentTimeMillis() - startTime);
    }

    private List<CognitiveStrategy> initializeStrategyPool(CognitiveGoal goal, SessionContainer session, TaskContext context) {
        List<CognitiveStrategy> pool = new ArrayList<>();
        Map<String, Object> goalParams = goal != null ? goal.getParameters() : Map.of();
        Map<String, Object> meta = context != null ? context.getMetadata() : Map.of();

        String reqSource = (String) goalParams.getOrDefault("sourceType", meta.get("sourceType"));
        String reqRepo = (String) goalParams.getOrDefault("repository", meta.get("repository"));
        if (reqRepo == null) reqRepo = (String) goalParams.getOrDefault("domain", meta.get("domain"));
        String reqSplit = (String) goalParams.getOrDefault("split", meta.get("split"));

        if (reqSource != null || reqRepo != null) {
            String src = reqSource != null ? reqSource : "HUGGING_FACE";
            String repo = reqRepo != null ? reqRepo : "Salesforce/wikitext";
            String split = reqSplit != null ? reqSplit : "validation";
            pool.add(CognitiveStrategy.acquisitionStrategy("EXPLICIT_REQUESTED_STRATEGY", src, repo, split, goalParams));
        }

        // Dynamic Source Discovery integration using HuggingFaceSourceDiscovery
        String domain = goal != null ? goal.getTargetDomain() : "";
        String desc = goal != null ? goal.getDescription().toLowerCase() : "";
        if ("DATASET_ACQUISITION".equalsIgnoreCase(domain) || desc.contains("acquire") || desc.contains("dataset") || desc.contains("data")) {
            long targetBytes = 500L * 1024L * 1024L;
            Object tbObj = goalParams.get("targetUsableBytes");
            if (tbObj instanceof Number) targetBytes = ((Number) tbObj).longValue();

            TrainingDataPreferences prefs = TrainingDataPreferences.builder()
                    .minimumUsableBytes(targetBytes)
                    .targetUsableBytes(targetBytes)
                    .addDomain(reqRepo != null ? reqRepo : "text")
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
        if (taskContext != null) {
            try {
                AiService aiService = taskContext.getAiService();
                if (aiService != null) {
                    var availableCaps = capabilityDiscovery.discoverCapabilities(session);
                    String prompt = buildPrompt(goal, worldState, observations, decisions, availableCaps);
                    LlmResponse response = aiService.sendLlmRequest(taskContext.getOrchestrator(), prompt, 0.7f, null, taskContext, null);
                    if (response != null && response.getFinalContent() != null) {
                        CognitiveDecision parsed = parseLlmDecision(response.getFinalContent());
                        if (parsed != null && validateCapabilityExists(session, parsed.getTargetCapability())) {
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

        var availableCaps = capabilityDiscovery.discoverCapabilities(session);
        if (availableCaps.isEmpty()) {
            return CognitiveDecision.of(CognitiveDecisionType.FAILURE, "NO_ACTIONABLE_CAPABILITY: No capabilities or tools registered.");
        }

        CapabilityDiscovery.CapabilityDescriptor matchedCap = selectRelevantCapability(goal, worldState, availableCaps, observations);
        if (matchedCap == null) {
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

    private CognitiveDecision parseLlmDecision(String content) {
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
                CognitiveDecisionType type = CognitiveDecisionType.valueOf(typeStr.toUpperCase());
                String cap = json.optString("targetCapability", null);
                String cmd = json.optString("commandOrStrategy", null);
                String reasoning = json.optString("reasoning", "");
                return new CognitiveDecision(type, cap, cmd, reasoning, null);
            }
        } catch (Exception ignored) {}
        return null;
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
        actionKey += ":iter" + iteration + ":" + capName + ":" + stratSig;

        if (worldState.isActionAlreadyExecuted(actionKey)) {
            return CognitiveObservation.ofFailure(capName, 409, "", "Strategy action already executed in iteration " + iteration + ": " + actionKey, "DUPLICATE_ACTION_PREVENTED", 0);
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
            if (taskContext != null) {
                IterationMemoryService memoryService = taskContext.getKernelContext() != null ? taskContext.getKernelContext().getMemoryService() : null;
                File root = taskContext.getProjectRoot() != null ? taskContext.getProjectRoot() : new File(".");
                SystemStateSignalProvider stateProvider = new SystemStateSignalProvider(root, taskContext);

                IDarwinEngine darwin = DarwinEngineFactory.createEngine(PlatformType.ASSISTED_CODING, taskContext, memoryService, stateProvider);
                if (darwin != null) {
                    IterationManager manager = session != null ? session.getIterationManager() : null;
                    if (manager != null) {
                        EvaluationResult evalResult = darwin.runDarwinIteration(taskContext, manager);
                        long duration = System.currentTimeMillis() - start;
                        if (evalResult != null && evalResult.isSuccess()) {
                            String decisionStr = evalResult.getDecision() != null ? evalResult.getDecision().name() : "APPROVED";
                            return CognitiveObservation.ofSuccess("DARWIN_SEARCH", "DARWIN_WINNER_FOUND: Selected superior candidate variant (" + decisionStr + ")", duration);
                        } else {
                            return CognitiveObservation.ofFailure("DARWIN_SEARCH", 1, "", "DARWIN_NO_WINNER: Evolutionary iteration executed without promoting a winner.", "NO_WINNER", duration);
                        }
                    } else {
                        long duration = System.currentTimeMillis() - start;
                        return CognitiveObservation.ofFailure("DARWIN_SEARCH", 500, "", "DARWIN_UNAVAILABLE: IterationManager unavailable in session context.", "DARWIN_UNAVAILABLE", duration);
                    }
                }
            }
        } catch (Exception e) {
            return CognitiveObservation.ofFailure("DARWIN_SEARCH", 1, "", e.getMessage(), "DARWIN_EXCEPTION", System.currentTimeMillis() - start);
        }
        return CognitiveObservation.ofFailure("DARWIN_SEARCH", 1, "", "DARWIN_NO_WINNER: Evolutionary search completed without candidate promotion.", "NO_WINNER", System.currentTimeMillis() - start);
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
