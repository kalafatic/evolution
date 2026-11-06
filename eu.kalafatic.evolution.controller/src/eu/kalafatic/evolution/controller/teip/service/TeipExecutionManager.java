package eu.kalafatic.evolution.controller.teip.service;

import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

import eu.kalafatic.evolution.controller.agents.AgentFactory;
import eu.kalafatic.evolution.controller.agents.IAgent;
import eu.kalafatic.evolution.controller.manager.OllamaManager;
import eu.kalafatic.evolution.controller.manager.OllamaModel;
import eu.kalafatic.evolution.controller.manager.OllamaService;
import eu.kalafatic.evolution.controller.orchestration.KernelFacade;
import eu.kalafatic.evolution.controller.orchestration.OrchestratorResponse;
import eu.kalafatic.evolution.controller.orchestration.PlatformType;
import eu.kalafatic.evolution.controller.orchestration.ResultType;
import eu.kalafatic.evolution.controller.orchestration.ServerOperation;
import eu.kalafatic.evolution.controller.orchestration.ServerOperationState;
import eu.kalafatic.evolution.controller.orchestration.SessionContainer;
import eu.kalafatic.evolution.controller.orchestration.SessionContext;
import eu.kalafatic.evolution.controller.orchestration.SessionManager;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.TaskRequest;
import eu.kalafatic.evolution.controller.teip.dto.TeipAcceptedResponse;
import eu.kalafatic.evolution.controller.teip.dto.TeipCapabilityDescriptor;
import eu.kalafatic.evolution.controller.teip.dto.TeipError;
import eu.kalafatic.evolution.controller.teip.dto.TeipEvent;
import eu.kalafatic.evolution.controller.teip.dto.TeipExecutionRequest;
import eu.kalafatic.evolution.controller.teip.dto.TeipExecutionResult;
import eu.kalafatic.evolution.controller.teip.dto.TeipExecutionStatus;
import eu.kalafatic.evolution.controller.teip.dto.TeipExecutionStatusResponse;
import eu.kalafatic.evolution.controller.teip.dto.TeipHealthResponse;
import eu.kalafatic.evolution.controller.teip.store.TeipEventStore;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.utils.log.Log;

/**
 * Authoritative lifecycle coordinator for TEIP 1.0 executions.
 * Manages mappings between external TEIP executions and internal EVO SessionContainers,
 * ServerOperations, idempotency caches, and dynamic capability queries.
 */
public class TeipExecutionManager {

    private static final TeipExecutionManager INSTANCE = new TeipExecutionManager();

    // Idempotency cache: idempotencyKey -> TeipAcceptedResponse
    private final Map<String, TeipAcceptedResponse> idempotencyStore = new ConcurrentHashMap<>();
    // Execution metadata: executionId -> TeipExecutionRequest
    private final Map<String, TeipExecutionRequest> executionRequests = new ConcurrentHashMap<>();
    // Execution mapping: executionId -> internal EVO sessionId
    private final Map<String, String> executionToSessionMap = new ConcurrentHashMap<>();
    // Execution results: executionId -> TeipExecutionResult
    private final Map<String, TeipExecutionResult> executionResults = new ConcurrentHashMap<>();
    // Terminal status cache for completed/failed/cancelled executions
    private final Map<String, TeipExecutionStatusResponse> statusCache = new ConcurrentHashMap<>();

    private volatile boolean teipEnabled = true;

    private TeipExecutionManager() {}

    public static TeipExecutionManager getInstance() {
        return INSTANCE;
    }

    public boolean isTeipEnabled() {
        return teipEnabled;
    }

    public void setTeipEnabled(boolean teipEnabled) {
        this.teipEnabled = teipEnabled;
    }

    public TeipHealthResponse checkHealth() {
        TeipHealthResponse resp = new TeipHealthResponse();
        if (!teipEnabled) {
            resp.setStatus("DISABLED");
            resp.setReady(false);
            resp.getDetails().put("reason", "TEIP integration is disabled by configuration");
            return resp;
        }

        resp.setStatus("UP");
        resp.setReady(true);
        resp.getDetails().put("activeExecutions", executionToSessionMap.size());
        resp.getDetails().put("eventStoreSize", TeipEventStore.getInstance().getAllEvents("").size());
        return resp;
    }

    public TeipCapabilityDescriptor getCapabilities() {
        TeipCapabilityDescriptor desc = new TeipCapabilityDescriptor();

        // 1. Dynamic Platform Modes
        for (PlatformType pt : PlatformType.values()) {
            desc.getSupportedModes().add(pt.name());
        }

        // 2. Dynamic Agents via AgentFactory
        try {
            SessionContainer dummySession = SessionManager.getInstance().getOrCreateSession("teip-capabilities-probe");
            List<IAgent> agents = AgentFactory.createIsolatedAgents(dummySession);
            for (IAgent agent : agents) {
                Map<String, Object> agentMap = new HashMap<>();
                agentMap.put("name", agent.getClass().getSimpleName());
                agentMap.put("description", "Registered EVO reasoning and execution agent");
                desc.getAgents().add(agentMap);
            }
        } catch (Throwable t) {
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("name", "JavaDevAgent");
            fallback.put("description", "Java development agent");
            desc.getAgents().add(fallback);
        }

        // 3. Dynamic Models from OllamaService
        try {
            OllamaService ollama = OllamaManager.getInstance().getService("http://localhost:11434");
            List<OllamaModel> models = ollama.loadModels();
            if (models != null) {
                for (OllamaModel m : models) {
                    Map<String, Object> mMap = new HashMap<>();
                    mMap.put("id", m.getName());
                    mMap.put("available", true);
                    mMap.put("provider", "Ollama");
                    mMap.put("size", m.getSize());
                    desc.getModels().add(mMap);
                }
            }
        } catch (Throwable ignored) {}

        // Fallback default model if none installed/running
        if (desc.getModels().isEmpty()) {
            Map<String, Object> mMap = new HashMap<>();
            mMap.put("id", "llama3.2:3b");
            mMap.put("available", true);
            mMap.put("provider", "Ollama");
            desc.getModels().add(mMap);
        }

        // 4. Tools from ToolRegistry
        try {
            for (String toolName : List.of("list_files", "read_file", "edit_file", "run_tests", "git_status")) {
                Map<String, Object> tMap = new HashMap<>();
                tMap.put("name", toolName);
                tMap.put("scope", "tryst:" + toolName);
                desc.getTools().add(tMap);
            }
        } catch (Throwable ignored) {}

        return desc;
    }

    public synchronized TeipAcceptedResponse createExecution(TeipExecutionRequest request) {
        if (!teipEnabled) {
            throw new IllegalStateException("TEIP integration is currently disabled");
        }

        // Validate required fields
        if (request.getInstruction() == null || request.getInstruction().trim().isEmpty()) {
            throw new IllegalArgumentException("Execution instruction cannot be empty");
        }

        // Idempotency check
        String idempotencyKey = request.getIdempotencyKey();
        if (idempotencyKey != null && !idempotencyKey.trim().isEmpty()) {
            TeipAcceptedResponse cached = idempotencyStore.get(idempotencyKey.trim());
            if (cached != null) {
                Log.log("[TEIP] Idempotency match for key=" + idempotencyKey + ", returning executionId=" + cached.getExecutionId());
                return cached;
            }
        }

        // Allocate stable opaque executionId and EVO sessionId
        String executionId = "exec-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String evoSessionId = "evo-teip-" + executionId;

        executionRequests.put(executionId, request);
        executionToSessionMap.put(executionId, evoSessionId);

        TeipAcceptedResponse accepted = new TeipAcceptedResponse(executionId, request.getCorrelationId());
        if (idempotencyKey != null && !idempotencyKey.trim().isEmpty()) {
            idempotencyStore.put(idempotencyKey.trim(), accepted);
        }

        // Initialize status response
        TeipExecutionStatusResponse statusResp = new TeipExecutionStatusResponse();
        statusResp.setExecutionId(executionId);
        statusResp.setCorrelationId(request.getCorrelationId());
        statusResp.setStatus(TeipExecutionStatus.QUEUED);
        statusResp.setCreatedAt(Instant.now().toString());
        statusResp.setStatusMessage("Queued for execution");
        statusCache.put(executionId, statusResp);

        // Record initial event in event store
        Map<String, Object> initData = new HashMap<>();
        initData.put("capability", request.getCapability());
        initData.put("mode", request.getMode());
        initData.put("caller", request.getCaller());
        TeipEventStore.getInstance().appendEvent(executionId, "EXECUTION_QUEUED", request.getCorrelationId(), initData);

        // Bind and dispatch to EVO SessionContainer and ServerOperation
        dispatchExecution(executionId, evoSessionId, request);

        return accepted;
    }

    private void dispatchExecution(String executionId, String evoSessionId, TeipExecutionRequest request) {
        SessionContainer sessionContainer = SessionManager.getInstance().getOrCreateSession(evoSessionId);

        String owner = "TRYST";
        if (request.getCaller() != null && request.getCaller().containsKey("operator")) {
            owner = String.valueOf(request.getCaller().get("operator"));
        }

        ServerOperation op = new ServerOperation("op-" + executionId, evoSessionId, owner, "TEIP_EXECUTION");
        op.transitionTo(ServerOperationState.QUEUED, "Queued TEIP execution: " + request.getInstruction());
        sessionContainer.registerOperation(op);

        Future<?> future = sessionContainer.getExecutorService().submit(() -> {
            try {
                op.setWorkerThread(Thread.currentThread());
                op.transitionTo(ServerOperationState.RUNNING, "Starting execution");
                op.setProgress(0.1, "Initializing environment");

                TeipExecutionStatusResponse curStatus = statusCache.get(executionId);
                if (curStatus != null) {
                    curStatus.setStatus(TeipExecutionStatus.RUNNING);
                    curStatus.setStartedAt(Instant.now().toString());
                    curStatus.setProgress(0.1);
                    curStatus.setStatusMessage("Running instruction");
                }

                TeipEventStore.getInstance().appendEvent(executionId, "EXECUTION_STARTED", request.getCorrelationId(), Map.of("mode", String.valueOf(request.getMode())));

                // Build TaskRequest for OrchestratorServiceImpl
                TaskRequest taskReq = new TaskRequest();
                taskReq.setPrompt(request.getInstruction());
                taskReq.setProjectRoot(new File(System.getProperty("user.dir")));
                taskReq.getContext().put("sessionId", evoSessionId);
                taskReq.getContext().put("executionId", executionId);
                taskReq.getContext().put("grantedToolScopes", request.getGrantedToolScopes());

                if (request.getParameters() != null && request.getParameters().containsKey("model")) {
                    taskReq.getContext().put("model", String.valueOf(request.getParameters().get("model")));
                }

                // Execute via KernelFacade
                Orchestrator orchModel = OrchestrationFactory.eINSTANCE.createOrchestrator();
                orchModel.setId(evoSessionId);
                orchModel.setAiChat(OrchestrationFactory.eINSTANCE.createAiChat());
                orchModel.getAiChat().setPromptInstructions(OrchestrationFactory.eINSTANCE.createPromptInstructions());
                orchModel.setOllama(OrchestrationFactory.eINSTANCE.createOllama());
                orchModel.getOllama().setUrl("http://localhost:11434");
                orchModel.getOllama().setModel("llama3.2:3b");

                TaskContext context = new TaskContext(orchModel, taskReq.getProjectRoot());
                context.setSessionId(evoSessionId);
                if (sessionContainer instanceof SessionContext) {
                    ((SessionContext) sessionContainer).setTaskContext(context);
                }

                context.addLogListener(log -> {
                    op.setProgress(Math.min(0.9, op.getProgress() + 0.05), log);
                    TeipEventStore.getInstance().appendEvent(executionId, "EXECUTION_LOG", request.getCorrelationId(), Map.of("log", log));
                });

                KernelFacade kernel = new KernelFacade();
                OrchestratorResponse orchResp = kernel.handle(taskReq, context);

                if (orchResp.getResultType() == ResultType.ERROR) {
                    throw new RuntimeException(orchResp.getSummary());
                }

                // Finalize success
                op.setProgress(1.0, "Execution completed successfully");
                op.complete(orchResp.getSummary(), Collections.emptyMap());

                if (curStatus != null) {
                    curStatus.setStatus(TeipExecutionStatus.COMPLETED);
                    curStatus.setProgress(1.0);
                    curStatus.setStatusMessage("Completed successfully");
                    curStatus.setCompletedAt(Instant.now().toString());
                }

                TeipExecutionResult res = new TeipExecutionResult(executionId, request.getCorrelationId(), TeipExecutionStatus.COMPLETED, orchResp.getSummary());
                executionResults.put(executionId, res);

                TeipEventStore.getInstance().appendEvent(executionId, "EXECUTION_COMPLETED", request.getCorrelationId(), Map.of("summary", orchResp.getSummary()));

            } catch (Throwable t) {
                boolean cancelled = op.isCancellationRequested() || Thread.currentThread().isInterrupted();
                TeipExecutionStatus finalState = cancelled ? TeipExecutionStatus.CANCELLED : TeipExecutionStatus.FAILED;

                op.fail(t.getMessage(), t);
                TeipExecutionStatusResponse curStatus = statusCache.get(executionId);
                if (curStatus != null) {
                    curStatus.setStatus(finalState);
                    curStatus.setStatusMessage(t.getMessage());
                    curStatus.setCompletedAt(Instant.now().toString());
                }

                TeipExecutionResult res = new TeipExecutionResult(executionId, request.getCorrelationId(), finalState, t.getMessage());
                executionResults.put(executionId, res);

                TeipEventStore.getInstance().appendEvent(
                    executionId,
                    cancelled ? "EXECUTION_CANCELLED" : "EXECUTION_FAILED",
                    request.getCorrelationId(),
                    Map.of("error", t.getMessage() != null ? t.getMessage() : "Execution error")
                );
            }
        });

        op.setExecutionFuture(future);
    }

    public TeipExecutionStatusResponse getStatus(String executionId) {
        if (executionId == null) return null;

        String sessionId = executionToSessionMap.get(executionId);
        TeipExecutionStatusResponse cached = statusCache.get(executionId);
        if (cached == null) return null;

        if (sessionId != null) {
            SessionContainer container = SessionManager.getInstance().getSession(sessionId);
            if (container != null) {
                ServerOperation op = container.getOperation("op-" + executionId);
                if (op != null) {
                    cached.setProgress(op.getProgress());
                    cached.setStatusMessage(op.getStatusMessage());
                    if (op.getState() == ServerOperationState.COMPLETED) {
                        cached.setStatus(TeipExecutionStatus.COMPLETED);
                    } else if (op.getState() == ServerOperationState.FAILED) {
                        cached.setStatus(TeipExecutionStatus.FAILED);
                    } else if (op.getState() == ServerOperationState.CANCELLED) {
                        cached.setStatus(TeipExecutionStatus.CANCELLED);
                    } else if (op.getState() == ServerOperationState.RUNNING) {
                        cached.setStatus(TeipExecutionStatus.RUNNING);
                    }
                }
            }
        }

        return cached;
    }

    public TeipExecutionResult getResult(String executionId) {
        if (executionId == null) return null;
        return executionResults.get(executionId);
    }

    public synchronized boolean cancelExecution(String executionId, String reason) {
        if (executionId == null) return false;

        TeipExecutionStatusResponse status = statusCache.get(executionId);
        if (status == null) return false;

        // Idempotent cancellation: if already terminal, return true
        if (status.getStatus().isTerminal()) {
            return true;
        }

        String sessionId = executionToSessionMap.get(executionId);
        if (sessionId != null) {
            SessionManager.getInstance().cancelOperation("op-" + executionId, reason);
        }

        status.setStatus(TeipExecutionStatus.CANCELLED);
        status.setStatusMessage("Cancelled: " + (reason != null ? reason : "Operator request"));
        status.setCompletedAt(Instant.now().toString());

        TeipExecutionResult res = new TeipExecutionResult(executionId, status.getCorrelationId(), TeipExecutionStatus.CANCELLED, "Execution cancelled: " + reason);
        executionResults.put(executionId, res);

        TeipEventStore.getInstance().appendEvent(
            executionId,
            "EXECUTION_CANCELLED",
            status.getCorrelationId(),
            Map.of("reason", reason != null ? reason : "Operator request")
        );

        return true;
    }

    public List<TeipExecutionStatusResponse> listAllExecutions() {
        return new ArrayList<>(statusCache.values());
    }

    public void clearAll() {
        idempotencyStore.clear();
        executionRequests.clear();
        executionToSessionMap.clear();
        executionResults.clear();
        statusCache.clear();
        TeipEventStore.getInstance().reset();
    }
}
