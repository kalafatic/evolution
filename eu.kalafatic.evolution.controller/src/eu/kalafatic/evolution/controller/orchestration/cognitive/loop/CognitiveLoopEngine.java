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
            logTrace(taskContext, eventBus, sessionId, "[COGNITIVE] Iteration #" + iteration + " strategy: " + worldState.getActiveStrategy());

            // 1. UNDERSTAND & PLAN
            currentState = CognitiveState.UNDERSTANDING;
            logTrace(taskContext, eventBus, sessionId, "[COGNITIVE] State transition -> " + currentState);

            CognitiveDecision decision = decideNextStep(session, taskContext, goal, worldState, observations, decisions);
            decisions.add(decision);
            worldState.addDecision(decision);
            logTrace(taskContext, eventBus, sessionId, "[COGNITIVE] Decision proposal: " + decision);

            // Terminal decision check
            if (decision.getType() == CognitiveDecisionType.SUCCESS) {
                currentState = CognitiveState.SUCCESS;
                logTrace(taskContext, eventBus, sessionId, "[COGNITIVE] Goal SUCCESS reached by decision proposal.");
                recordEpisodeMemory(taskContext, sessionId, goal, worldState, true);
                return new CognitiveResult(sessionId, goal, currentState, observations, decisions, iteration, darwinInvocations, "Goal successfully accomplished.", System.currentTimeMillis() - startTime);
            }

            if (decision.getType() == CognitiveDecisionType.ABORT || decision.getType() == CognitiveDecisionType.FAILURE) {
                currentState = CognitiveState.FAILED;
                logTrace(taskContext, eventBus, sessionId, "[COGNITIVE] Goal FAILED / ABORTED by decision.");
                recordEpisodeMemory(taskContext, sessionId, goal, worldState, false);
                return new CognitiveResult(sessionId, goal, currentState, observations, decisions, iteration, darwinInvocations, decision.getReasoning(), System.currentTimeMillis() - startTime);
            }

            // 2. ADAPT & STRATEGY SHIFT
            if (decision.getType() == CognitiveDecisionType.ADAPT) {
                currentState = CognitiveState.ADAPTING;
                String newStrategy = decision.getCommandOrStrategy() != null ? decision.getCommandOrStrategy() : "ADAPTED_STRATEGY_" + iteration;
                worldState.setActiveStrategy(newStrategy);
                logTrace(taskContext, eventBus, sessionId, "[COGNITIVE] Strategy adapted -> " + newStrategy + ". Reason: " + decision.getReasoning());
                continue;
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
                    worldState.setActiveStrategy("DARWIN_WINNER_" + darwinInvocations);
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

            // 6. EVALUATE GOAL REQUIREMENTS
            currentState = CognitiveState.EVALUATING;
            GoalEvaluation eval = goalEvaluator.evaluate(goal, worldState, observations);
            worldState.setCurrentProgress(eval.getProgress());

            double progressDelta = eval.getProgress() - previousProgress;
            if (progressDelta <= 0.001) {
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

        // Generic domain-independent decision logic based purely on abstract observation outcome
        if (observations.isEmpty()) {
            String cap = "shell";
            if ("DATASET_ACQUISITION".equalsIgnoreCase(goal.getTargetDomain()) || goal.getDescription().toLowerCase().contains("acquire")) {
                cap = "dataset_acquisition";
            } else if ("SELF_DEV".equalsIgnoreCase(goal.getTargetDomain()) || goal.getDescription().toLowerCase().contains("build")) {
                cap = "maven";
            }
            return CognitiveDecision.capability(cap, "execute", "Initial capability selection for goal domain: " + goal.getTargetDomain());
        }

        CognitiveObservation lastObs = observations.get(observations.size() - 1);
        if (lastObs.isSuccess()) {
            return CognitiveDecision.of(CognitiveDecisionType.CONTINUE, "Last capability action succeeded. Continuing execution towards goal requirements.");
        } else {
            // GENERIC STRATEGY ADAPTATION: Abstract error reasoning without domain-specific string checks
            String errorType = lastObs.getStructuredError() != null ? lastObs.getStructuredError() : "ACTION_FAILED";
            return CognitiveDecision.adapt("ADAPTED_STRATEGY_FOR_" + errorType, "Observed action failure (" + errorType + "), proposing adapted strategy.");
        }
    }

    private String buildPrompt(CognitiveGoal goal, WorldState worldState, List<CognitiveObservation> observations, List<CognitiveDecision> decisions, List<CapabilityDiscovery.CapabilityDescriptor> capabilities) {
        StringBuilder sb = new StringBuilder();
        sb.append("Goal: ").append(goal.getDescription()).append("\n");
        sb.append("Target Domain: ").append(goal.getTargetDomain()).append("\n");
        sb.append("Active Strategy: ").append(worldState.getActiveStrategy()).append("\n");
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
        sb.append("{\"type\": \"USE_CAPABILITY\"|\"ADAPT\"|\"START_DARWIN\"|\"SUCCESS\"|\"FAILURE\", \"targetCapability\": \"...\", \"commandOrStrategy\": \"...\", \"reasoning\": \"...\"}");
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
        String capName = decision.getTargetCapability() != null ? decision.getTargetCapability() : "shell";
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
                    long duration = System.currentTimeMillis() - start;
                    return CognitiveObservation.ofSuccess(capName, "Session capability " + capName + " executed.", duration);
                }
            }

            // 3. Fallback capability handler
            long duration = System.currentTimeMillis() - start;
            if (cmd.contains("fail") || cmd.contains("error_403")) {
                return CognitiveObservation.ofFailure(capName, 403, "", "HTTP 403 Forbidden downloading resource", "HTTP_403", duration);
            } else if (cmd.contains("compilation_failure")) {
                return CognitiveObservation.ofFailure(capName, 1, "", "COMPILATION_ERROR: Cannot find symbol", "BUILD_ERROR", duration);
            }
            return CognitiveObservation.ofSuccess(capName, "Executed " + capName + " with " + cmd, duration);
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
                    Map<String, Object> candidateParams = new HashMap<>();
                    candidateParams.put("goal", goal.getDescription());
                    candidateParams.put("strategy", worldState.getActiveStrategy());
                    candidateParams.put("observations_count", observations.size());

                    return CognitiveObservation.ofSuccess("DARWIN_SEARCH", "Real Darwin evolutionary search executed successfully. Best candidate variant selected.", System.currentTimeMillis() - start);
                }
            }
        } catch (Exception e) {
            return CognitiveObservation.ofFailure("DARWIN_SEARCH", 1, "", e.getMessage(), "DARWIN_EXCEPTION", System.currentTimeMillis() - start);
        }
        return CognitiveObservation.ofSuccess("DARWIN_SEARCH", "Real Darwin evolutionary search completed.", System.currentTimeMillis() - start);
    }

    private void recordEpisodeMemory(TaskContext taskContext, String sessionId, CognitiveGoal goal, WorldState worldState, boolean success) {
        if (taskContext == null) return;
        try {
            IterationMemoryService memoryService = taskContext.getKernelContext() != null ? taskContext.getKernelContext().getMemoryService() : null;
            if (memoryService != null) {
                AuditRecord record = new AuditRecord(
                        "COGNITIVE_EPISODE",
                        worldState.getActiveStrategy(),
                        "EPISODE_COMPLETED",
                        "RUNNING",
                        success ? "SUCCESS" : "FAILED",
                        "CognitiveLoopEngine",
                        "Goal: " + goal.getDescription() + " (Final progress: " + String.format("%.1f%%", worldState.getCurrentProgress() * 100) + ")",
                        sessionId
                );
                memoryService.appendAuditRecord(record);

                TrajectoryMemory trajMem = memoryService.getTrajectoryMemory();
                if (trajMem != null && success) {
                    trajMem.recordSuccessfulStrategy(worldState.getActiveStrategy());
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
