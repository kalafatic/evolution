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

        CognitiveState currentState = CognitiveState.CREATED;
        logTrace(taskContext, eventBus, sessionId, "[COGNITIVE] Control Loop initialized for goal: " + goal.getDescription());

        int iteration = 0;
        int darwinInvocations = 0;
        int consecutiveNoProgress = 0;
        double previousProgress = 0.0;

        currentState = CognitiveState.OBSERVING;
        logTrace(taskContext, eventBus, sessionId, "[COGNITIVE] State transition -> " + currentState);

        while (!budget.isExhausted(iteration, System.currentTimeMillis() - startTime, darwinInvocations, consecutiveNoProgress)) {
            iteration++;
            logTrace(taskContext, eventBus, sessionId, "[COGNITIVE] Iteration #" + iteration + " strategy: " + worldState.getActiveStrategy().getIdentifier());

            // Check for generic stagnation
            if (worldState.detectStagnation(2)) {
                logTrace(taskContext, eventBus, sessionId, "[COGNITIVE] Generic stagnation detected (repeated failure signature). Forcing strategy adaptation.");
                CognitiveStrategy adapted = new CognitiveStrategy("ADAPTED_AFTER_STAGNATION_" + iteration, "Recover from stagnation", Collections.emptyList(), Collections.emptyMap(), "Stagnation recovery", 0.85);
                worldState.setActiveStrategy(adapted);
            }

            // 1. UNDERSTAND & PLAN
            currentState = CognitiveState.UNDERSTANDING;
            logTrace(taskContext, eventBus, sessionId, "[COGNITIVE] State transition -> " + currentState);

            CognitiveDecision decision = decideNextStep(session, taskContext, goal, worldState, observations, decisions);
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

            // 2. ADAPT & STRATEGY SHIFT (Correct semantics: update strategy and proceed to next planning iteration)
            if (decision.getType() == CognitiveDecisionType.ADAPT) {
                currentState = CognitiveState.ADAPTING;
                String newStratId = decision.getCommandOrStrategy() != null ? decision.getCommandOrStrategy() : "ADAPTED_STRATEGY_" + iteration;
                CognitiveStrategy newStrat = new CognitiveStrategy(newStratId, decision.getReasoning(), Collections.emptyList(), decision.getParameters(), decision.getReasoning(), decision.getConfidence());
                worldState.setActiveStrategy(newStrat);
                logTrace(taskContext, eventBus, sessionId, "[COGNITIVE] Strategy adapted -> " + newStratId + ". Reason: " + decision.getReasoning());
                continue; // Proceed to next planning phase under new strategy without executing an invented action
            }

            // 3. REAL DARWIN ESCALATION
            if (decision.getType() == CognitiveDecisionType.START_DARWIN || consecutiveNoProgress >= 2) {
                currentState = CognitiveState.DARWINING;
                logTrace(taskContext, eventBus, sessionId, "[COGNITIVE] State transition -> " + currentState + " (Executing real Darwin evolutionary search)");
                darwinInvocations++;

                CognitiveObservation darwinObs = executeRealDarwinSearch(session, taskContext, goal, worldState, observations);
                observations.add(darwinObs);
                worldState.addObservation(darwinObs);

                if (darwinObs.isSuccess()) {
                    logTrace(taskContext, eventBus, sessionId, "[COGNITIVE] Real Darwin search identified winning variant: " + darwinObs.getStdout());
                    CognitiveStrategy winnerStrat = new CognitiveStrategy("DARWIN_WINNER_" + darwinInvocations, "Execute Darwin winner variant", Collections.emptyList(), Collections.emptyMap(), darwinObs.getStdout(), 0.95);
                    worldState.setActiveStrategy(winnerStrat);
                    consecutiveNoProgress = 0;
                } else {
                    logTrace(taskContext, eventBus, sessionId, "[COGNITIVE] Real Darwin search produced no winner. " + darwinObs.getStderr());
                }
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
                continue;
            }

            // 5. ACT (USE CAPABILITY / TOOL)
            currentState = CognitiveState.ACTING;
            logTrace(taskContext, eventBus, sessionId, "[COGNITIVE] State transition -> " + currentState + " executing capability: " + decision.getTargetCapability());

            CognitiveObservation actionObs = executeCapability(session, taskContext, decision);
            observations.add(actionObs);
            worldState.addObservation(actionObs);

            // 6. EVALUATE GOAL REQUIREMENTS (Evaluator is the SOLE authority for SUCCESS termination)
            currentState = CognitiveState.EVALUATING;
            GoalEvaluation eval = goalEvaluator.evaluate(goal, worldState, observations);
            worldState.setCurrentProgress(eval.getProgress());

            double progressDelta = eval.getProgress() - previousProgress;
            if (progressDelta <= 0.001 && (actionObs == null || !actionObs.isSuccess())) {
                consecutiveNoProgress++;
            } else {
                consecutiveNoProgress = 0;
            }
            previousProgress = eval.getProgress();

            logTrace(taskContext, eventBus, sessionId, "[COGNITIVE] State transition -> " + currentState + " Eval Status: " + eval.getStatus() + ", Progress: " + String.format("%.1f%%", eval.getProgress() * 100) + " (delta: " + String.format("%.1f%%", progressDelta * 100) + ")");

            if (eval.isAchieved()) {
                currentState = CognitiveState.SUCCESS;
                logTrace(taskContext, eventBus, sessionId, "[COGNITIVE] Goal SUCCESS verified by GoalEvaluator: " + eval.getEvidence());
                recordEpisodeMemory(taskContext, sessionId, goal, worldState, true);
                return new CognitiveResult(sessionId, goal, currentState, observations, decisions, iteration, darwinInvocations, "Goal successfully verified by GoalEvaluator.", System.currentTimeMillis() - startTime);
            }
        }

        currentState = CognitiveState.FAILED;
        logTrace(taskContext, eventBus, sessionId, "[COGNITIVE] Control Loop budget exhausted or no progress remaining.");
        recordEpisodeMemory(taskContext, sessionId, goal, worldState, false);
        return new CognitiveResult(sessionId, goal, currentState, observations, decisions, iteration, darwinInvocations, "Budget exhausted or lack of progress.", System.currentTimeMillis() - startTime);
    }

    private CognitiveDecision decideNextStep(SessionContainer session, TaskContext taskContext, CognitiveGoal goal, WorldState worldState, List<CognitiveObservation> observations, List<CognitiveDecision> decisions) {
        if (taskContext != null) {
            try {
                AiService aiService = taskContext.getAiService();
                if (aiService != null) {
                    var availableCaps = capabilityDiscovery.discoverCapabilities(session);
                    String prompt = buildPrompt(goal, worldState, observations, decisions, availableCaps);
                    LlmResponse response = aiService.sendLlmRequest(taskContext.getOrchestrator(), prompt, 0.7f, null, taskContext, null);
                    if (response != null && response.getFinalContent() != null) {
                        CognitiveDecision parsed = parseLlmDecision(response.getFinalContent());
                        if (parsed != null) {
                            return parsed;
                        }
                    }
                }
            } catch (Exception e) {
                logTrace(taskContext, null, taskContext.getSessionId(), "[COGNITIVE] LLM reasoning fallback due to: " + e.getMessage());
            }
        }

        // Generic domain-independent capability selection without "shell" fallback
        var availableCaps = capabilityDiscovery.discoverCapabilities(session);
        if (availableCaps.isEmpty()) {
            return CognitiveDecision.of(CognitiveDecisionType.ABORT, "NO_CAPABILITY_AVAILABLE: No tools or session capabilities registered.");
        }

        if (observations.isEmpty()) {
            String selectedCap = availableCaps.get(0).getName();
            return CognitiveDecision.capability(selectedCap, "execute", "Generic initial capability selection from registry");
        }

        CognitiveObservation lastObs = observations.get(observations.size() - 1);
        if (lastObs.isSuccess()) {
            return CognitiveDecision.capability(lastObs.getActionName(), "continue", "Last step succeeded. Continuing capability execution.");
        } else {
            String errorType = lastObs.getStructuredError() != null ? lastObs.getStructuredError() : "ACTION_FAILED";
            // If we adapted in the previous step, select an alternative capability
            if (!decisions.isEmpty() && decisions.get(decisions.size() - 1).getType() == CognitiveDecisionType.ADAPT) {
                String altCap = availableCaps.size() > 1 ? availableCaps.get(1).getName() : availableCaps.get(0).getName();
                return CognitiveDecision.capability(altCap, "execute_alternative", "Executing alternative capability following strategy adaptation");
            }
            return CognitiveDecision.adapt("ADAPTED_STRATEGY_FOR_" + errorType, "Observed action failure (" + errorType + "), proposing adapted strategy.");
        }
    }

    private String buildPrompt(CognitiveGoal goal, WorldState worldState, List<CognitiveObservation> observations, List<CognitiveDecision> decisions, List<CapabilityDiscovery.CapabilityDescriptor> capabilities) {
        StringBuilder sb = new StringBuilder();
        sb.append("Goal: ").append(goal.getDescription()).append("\n");
        sb.append("Target Domain: ").append(goal.getTargetDomain()).append("\n");
        sb.append("Active Strategy: ").append(worldState.getActiveStrategy().getIdentifier()).append("\n");
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
            // 1. ToolFactory
            ITool tool = ToolFactory.getTool(capName);
            if (tool != null) {
                String out = tool.execute(cmd, workingDir, taskContext);
                long duration = System.currentTimeMillis() - start;
                return CognitiveObservation.ofSuccess(capName, out, duration);
            }

            // 2. Session Capability Registry
            if (session != null && session.getCapabilityRegistry() != null) {
                var cap = session.getCapabilityRegistry().getCapability(capName);
                if (cap != null) {
                    if (cap instanceof ITool) {
                        String out = ((ITool) cap).execute(cmd, workingDir, taskContext);
                        long duration = System.currentTimeMillis() - start;
                        return CognitiveObservation.ofSuccess(capName, out, duration);
                    } else {
                        long duration = System.currentTimeMillis() - start;
                        return CognitiveObservation.ofFailure(capName, 400, "", "Capability " + capName + " is not an executable ITool interface", "CAPABILITY_NOT_EXECUTABLE", duration);
                    }
                }
            }

            // 3. Return capability unavailable if tool or session capability is absent
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
    }
}
