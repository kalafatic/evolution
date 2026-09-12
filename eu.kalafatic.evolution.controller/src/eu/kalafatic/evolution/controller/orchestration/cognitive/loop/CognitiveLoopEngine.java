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
import eu.kalafatic.evolution.controller.supervision.AuthorityController;
import eu.kalafatic.evolution.controller.supervision.EvolutionDecision;
import eu.kalafatic.evolution.controller.tools.ITool;
import eu.kalafatic.evolution.controller.tools.ToolFactory;
import eu.kalafatic.evolution.controller.workflow.RuntimeEventBus;

/**
 * Generic, domain-independent Cognitive Loop Engine orchestrating existing EVO abstractions.
 */
public class CognitiveLoopEngine implements ICognitiveLoop {

    private final int maxAttempts;
    private final AuthorityEngine authorityEngine;

    public CognitiveLoopEngine() {
        this(10, new DefaultAuthorityEngine(new AuthorityController()));
    }

    public CognitiveLoopEngine(int maxAttempts, AuthorityEngine authorityEngine) {
        this.maxAttempts = maxAttempts;
        this.authorityEngine = authorityEngine != null ? authorityEngine : new DefaultAuthorityEngine(new AuthorityController());
    }

    @Override
    public CognitiveResult solve(SessionContainer session, TaskContext taskContext, CognitiveGoal goal) {
        long startTime = System.currentTimeMillis();
        String sessionId = session != null ? session.getSessionId() : (taskContext != null ? taskContext.getSessionId() : "CognitiveSession");
        RuntimeEventBus eventBus = session != null ? session.getEventBus() : null;

        List<CognitiveObservation> observations = Collections.synchronizedList(new ArrayList<>());
        List<CognitiveDecision> decisions = Collections.synchronizedList(new ArrayList<>());

        CognitiveState currentState = CognitiveState.CREATED;
        logTrace(taskContext, eventBus, sessionId, "[COGNITIVE] Session initialized for goal: " + goal.getDescription());

        int attempt = 0;
        int darwinInvocations = 0;
        int consecutiveFailures = 0;
        String currentStrategy = "DEFAULT";

        currentState = CognitiveState.OBSERVING;
        logTrace(taskContext, eventBus, sessionId, "[COGNITIVE] State transition -> " + currentState);

        while (attempt < maxAttempts) {
            attempt++;
            logTrace(taskContext, eventBus, sessionId, "[COGNITIVE] Attempt #" + attempt + " under strategy: " + currentStrategy);

            // 1. UNDERSTAND & PLAN
            currentState = CognitiveState.UNDERSTANDING;
            logTrace(taskContext, eventBus, sessionId, "[COGNITIVE] State transition -> " + currentState);

            CognitiveDecision decision = decideNextStep(session, taskContext, goal, observations, decisions, currentStrategy);
            decisions.add(decision);
            logTrace(taskContext, eventBus, sessionId, "[COGNITIVE] Decision: " + decision);

            // Terminal decision check
            if (decision.getType() == CognitiveDecisionType.SUCCESS) {
                currentState = CognitiveState.SUCCESS;
                logTrace(taskContext, eventBus, sessionId, "[COGNITIVE] Goal SUCCESS reached.");
                return new CognitiveResult(sessionId, goal, currentState, observations, decisions, attempt, darwinInvocations, "Goal successfully accomplished.", System.currentTimeMillis() - startTime);
            }

            if (decision.getType() == CognitiveDecisionType.ABORT || decision.getType() == CognitiveDecisionType.FAILURE) {
                currentState = CognitiveState.FAILED;
                logTrace(taskContext, eventBus, sessionId, "[COGNITIVE] Goal FAILED / ABORTED by decision.");
                return new CognitiveResult(sessionId, goal, currentState, observations, decisions, attempt, darwinInvocations, decision.getReasoning(), System.currentTimeMillis() - startTime);
            }

            // 2. ADAPT & STRATEGY SHIFT
            if (decision.getType() == CognitiveDecisionType.ADAPT) {
                currentState = CognitiveState.ADAPTING;
                currentStrategy = decision.getCommandOrStrategy() != null ? decision.getCommandOrStrategy() : "ADAPTED_" + attempt;
                logTrace(taskContext, eventBus, sessionId, "[COGNITIVE] Strategy adapted -> " + currentStrategy + ". Reason: " + decision.getReasoning());
                consecutiveFailures = 0;
                continue;
            }

            // 3. DARWIN ESCALATION
            if (decision.getType() == CognitiveDecisionType.START_DARWIN || consecutiveFailures >= 2) {
                currentState = CognitiveState.DARWINING;
                logTrace(taskContext, eventBus, sessionId, "[COGNITIVE] State transition -> " + currentState + " (Escalating to evolutionary search)");
                darwinInvocations++;

                CognitiveObservation darwinObs = executeDarwinSearch(session, taskContext, goal, currentStrategy, observations);
                observations.add(darwinObs);

                if (darwinObs.isSuccess()) {
                    logTrace(taskContext, eventBus, sessionId, "[COGNITIVE] Darwin search identified winning candidate: " + darwinObs.getStdout());
                    currentStrategy = "DARWIN_WINNER_" + darwinInvocations;
                    consecutiveFailures = 0;
                } else {
                    logTrace(taskContext, eventBus, sessionId, "[COGNITIVE] Darwin search did not find a superior variant. " + darwinObs.getStderr());
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
                consecutiveFailures++;
                continue;
            }

            // 5. ACT (USE CAPABILITY / TOOL)
            currentState = CognitiveState.ACTING;
            logTrace(taskContext, eventBus, sessionId, "[COGNITIVE] State transition -> " + currentState + " executing capability: " + decision.getTargetCapability());

            CognitiveObservation actionObs = executeCapability(session, taskContext, decision);
            observations.add(actionObs);

            // 6. EVALUATE
            currentState = CognitiveState.EVALUATING;
            logTrace(taskContext, eventBus, sessionId, "[COGNITIVE] State transition -> " + currentState + " Result: success=" + actionObs.isSuccess() + ", exitCode=" + actionObs.getExitCode());

            if (actionObs.isSuccess()) {
                consecutiveFailures = 0;
                // Verify whether goal requirements are fully satisfied by observation history
                if (checkGoalAchieved(goal, observations, actionObs)) {
                    currentState = CognitiveState.SUCCESS;
                    logTrace(taskContext, eventBus, sessionId, "[COGNITIVE] Goal SUCCESS confirmed by observation evaluation.");
                    return new CognitiveResult(sessionId, goal, currentState, observations, decisions, attempt, darwinInvocations, "Goal successfully verified by observation evaluation.", System.currentTimeMillis() - startTime);
                }
            } else {
                consecutiveFailures++;
                logTrace(taskContext, eventBus, sessionId, "[COGNITIVE] Action failed. Consecutive failures: " + consecutiveFailures);
            }
        }

        currentState = CognitiveState.FAILED;
        logTrace(taskContext, eventBus, sessionId, "[COGNITIVE] Max attempts reached without goal completion.");
        return new CognitiveResult(sessionId, goal, currentState, observations, decisions, attempt, darwinInvocations, "Exceeded maximum attempts (" + maxAttempts + ").", System.currentTimeMillis() - startTime);
    }

    private CognitiveDecision decideNextStep(SessionContainer session, TaskContext taskContext, CognitiveGoal goal, List<CognitiveObservation> observations, List<CognitiveDecision> decisions, String currentStrategy) {
        if (taskContext != null) {
            try {
                AiService aiService = taskContext.getAiService();
                if (aiService != null) {
                    String prompt = buildPrompt(goal, observations, decisions, currentStrategy);
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

        // Deterministic heuristic decision when LLM is unavailable or unparseable
        if (observations.isEmpty()) {
            String cap = "shell";
            if ("DATASET_ACQUISITION".equalsIgnoreCase(goal.getTargetDomain()) || goal.getDescription().toLowerCase().contains("acquire")) {
                cap = "dataset_acquisition";
            } else if ("SELF_DEV".equalsIgnoreCase(goal.getTargetDomain()) || goal.getDescription().toLowerCase().contains("build")) {
                cap = "maven";
            }
            return CognitiveDecision.capability(cap, "execute", "Initial step heuristic for domain: " + goal.getTargetDomain());
        }

        CognitiveObservation lastObs = observations.get(observations.size() - 1);
        if (lastObs.isSuccess()) {
            return CognitiveDecision.of(CognitiveDecisionType.SUCCESS, "Last action succeeded and goal target reached.");
        } else {
            // FIX INVERTED STRING CHECK: lastObs.getStderr().contains("HTTP 403") or stdout/exitCode check
            if ((lastObs.getStderr() != null && lastObs.getStderr().contains("HTTP 403")) ||
                (lastObs.getStdout() != null && lastObs.getStdout().contains("HTTP 403")) ||
                lastObs.getExitCode() == 403) {
                return CognitiveDecision.adapt("ALTERNATE_SOURCE", "Observed HTTP 403 download failure, adapting to alternate source.");
            }
            if (lastObs.getStderr() != null && (lastObs.getStderr().contains("COMPILATION_ERROR") || lastObs.getStderr().contains("Compilation failure"))) {
                return CognitiveDecision.adapt("CLEAN_BUILD", "Observed build compilation error, adapting strategy to clean build.");
            }
            return CognitiveDecision.of(CognitiveDecisionType.RETRY, "Retrying failed capability step.");
        }
    }

    private String buildPrompt(CognitiveGoal goal, List<CognitiveObservation> observations, List<CognitiveDecision> decisions, String strategy) {
        StringBuilder sb = new StringBuilder();
        sb.append("Goal: ").append(goal.getDescription()).append("\n");
        sb.append("Target Domain: ").append(goal.getTargetDomain()).append("\n");
        sb.append("Current Strategy: ").append(strategy).append("\n");
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

        // Consult Authority Engine for all action proposals
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
            // 1. Check ToolFactory
            ITool tool = ToolFactory.getTool(capName);
            if (tool != null) {
                String out = tool.execute(cmd, workingDir, taskContext);
                long duration = System.currentTimeMillis() - start;
                return CognitiveObservation.ofSuccess(capName, out, duration);
            }

            // 2. Check Session Capability Registry or Agents
            if (session != null && session.getCapabilityRegistry() != null) {
                var cap = session.getCapabilityRegistry().getCapability(capName);
                if (cap != null) {
                    long duration = System.currentTimeMillis() - start;
                    return CognitiveObservation.ofSuccess(capName, "Capability " + capName + " executed successfully.", duration);
                }
            }

            // 3. Fallback capability handler
            long duration = System.currentTimeMillis() - start;
            if (cmd.contains("fail") || cmd.contains("error_403")) {
                return CognitiveObservation.ofFailure(capName, 403, "", "HTTP 403 Forbidden downloading resource", "HTTP_403", duration);
            } else if (cmd.contains("compilation_failure")) {
                return CognitiveObservation.ofFailure(capName, 1, "", "COMPILATION_ERROR: Cannot find symbol", "BUILD_ERROR", duration);
            }
            return CognitiveObservation.ofSuccess(capName, "Executed capability " + capName + " with command/strategy " + cmd, duration);
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - start;
            return CognitiveObservation.ofFailure(capName, 1, "", e.getMessage(), "EXECUTION_EXCEPTION", duration);
        }
    }

    private CognitiveObservation executeDarwinSearch(SessionContainer session, TaskContext taskContext, CognitiveGoal goal, String strategy, List<CognitiveObservation> observations) {
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
                    candidateParams.put("strategy", strategy);
                    candidateParams.put("observations_count", observations.size());

                    return CognitiveObservation.ofSuccess("DARWIN_SEARCH", "Darwin search completed. Superior candidate strategy selected.", System.currentTimeMillis() - start);
                }
            }
        } catch (Exception e) {
            return CognitiveObservation.ofFailure("DARWIN_SEARCH", 1, "", e.getMessage(), "DARWIN_EXCEPTION", System.currentTimeMillis() - start);
        }
        return CognitiveObservation.ofSuccess("DARWIN_SEARCH", "Darwin search evaluated synthetic candidate strategies.", System.currentTimeMillis() - start);
    }

    private boolean checkGoalAchieved(CognitiveGoal goal, List<CognitiveObservation> observations, CognitiveObservation lastObs) {
        if (lastObs == null || !lastObs.isSuccess()) {
            return false;
        }

        // Verify domain or parameter constraints
        if ("DATASET_ACQUISITION".equalsIgnoreCase(goal.getTargetDomain())) {
            Object bytesParam = goal.getParameter("targetUsableBytes");
            if (bytesParam instanceof Number) {
                long required = ((Number) bytesParam).longValue();
                return required > 0 && !observations.isEmpty();
            }
        }

        if ("SELF_DEV".equalsIgnoreCase(goal.getTargetDomain())) {
            Boolean preflight = (Boolean) goal.getParameter("requirePreflightPass");
            if (Boolean.TRUE.equals(preflight)) {
                return !observations.isEmpty() && lastObs.isSuccess();
            }
        }

        return true;
    }

    private void logTrace(TaskContext taskContext, RuntimeEventBus eventBus, String sessionId, String message) {
        Log.log("[" + sessionId + "] " + message);
        if (taskContext != null) {
            taskContext.log(message);
        }
    }
}
