package eu.kalafatic.evolution.controller.orchestration;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.Platform;
import org.json.JSONArray;
import org.json.JSONObject;
import org.osgi.framework.Bundle;

import eu.kalafatic.evolution.controller.manager.OllamaManager;
import eu.kalafatic.evolution.controller.manager.OllamaModel;
import eu.kalafatic.evolution.controller.manager.OllamaService;
import eu.kalafatic.evolution.controller.manager.ProjectModelManager;
import eu.kalafatic.evolution.controller.tools.GitTool;
import eu.kalafatic.evolution.creatic.api.CreaticAgent;
import eu.kalafatic.evolution.forge.controller.api.DatasetController;
import eu.kalafatic.evolution.forge.controller.api.ModelController;
import eu.kalafatic.evolution.forge.controller.api.SessionController;
import eu.kalafatic.evolution.forge.controller.api.SnapshotController;
import eu.kalafatic.evolution.forge.controller.api.TrainingController;
import eu.kalafatic.evolution.forge.controller.service.SelfEvoForgingService;
import eu.kalafatic.evolution.forge.controller.service.impl.SelfEvoForgingServiceImpl;
import eu.kalafatic.evolution.model.orchestration.ChatMessage;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.PromptInstructions;
import eu.kalafatic.evolution.model.orchestration.ServerSession;
import eu.kalafatic.evolution.model.orchestration.SessionType;
import eu.kalafatic.evolution.servers.controller.AuthController;
import eu.kalafatic.evolution.servers.database.DatabaseManager;
import eu.kalafatic.evolution.servers.repository.SessionRepository;
import eu.kalafatic.evolution.servers.repository.UserRepository;
import eu.kalafatic.evolution.servers.service.AuthService;
import fi.iki.elonen.NanoHTTPD;

/**
 * Embedded REST server for remote control.
 */
public class EvolutionServer extends NanoHTTPD {

    private final Map<String, ServerSession> activeSessions = new ConcurrentHashMap<>();
    private final java.util.Set<String> completedForgingRefreshes = ConcurrentHashMap.newKeySet();
    private final AuthService authService;
    private final AuthController authController;
    private final ArchitectureController architectureController;
    private final EvolutionDashboardController evolutionDashboardController;

    private SessionController sessionController;
    private ModelController modelController;
    private DatasetController datasetController;
    private TrainingController trainingController;
    private SnapshotController snapshotController;
    private SelfEvoForgingService selfEvoService = new SelfEvoForgingServiceImpl();

    public EvolutionServer(int port) {
        super(port);
        DatabaseManager dbManager = new DatabaseManager();
        UserRepository userRepository = new UserRepository(dbManager);
        SessionRepository sessionRepository = new SessionRepository(dbManager);
        this.authService = new AuthService(userRepository, sessionRepository);
        this.authController = new AuthController(authService);
        this.authController.setAuthEnabledProvider(() -> {
            Orchestrator orch = OrchestratorServiceImpl.getInstance().getOrchestrator();
            return orch != null && orch.getServerSettings() != null && orch.getServerSettings().isAuthenticate();
        });
        this.architectureController = new ArchitectureController();
        this.evolutionDashboardController = new EvolutionDashboardController();
    }

    public void setForgeControllers(SessionController sc, ModelController mc, DatasetController dc, TrainingController tc, SnapshotController snc) {
        this.sessionController = sc;
        this.modelController = mc;
        this.datasetController = dc;
        this.trainingController = tc;
        this.snapshotController = snc;
    }

    public void startServer() throws IOException {
        start(30000, false);
    }

    public void stopServer() {
        stop();
    }

    @Override
    public Response serve(IHTTPSession session) {
        Response response;
        if (Method.OPTIONS.equals(session.getMethod())) {
            response = newFixedLengthResponse(Response.Status.OK, NanoHTTPD.MIME_PLAINTEXT, "");
        } else {
            response = handleInternal(session);
        }
        addCorsHeaders(response);
        return response;
    }

    private void addCorsHeaders(Response res) {
        res.addHeader("Access-Control-Allow-Origin", "*");
        res.addHeader("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT, OPTIONS");
        res.addHeader("Access-Control-Allow-Headers", "Content-Type, Authorization, x-evo-runtime");
    }

    private Response handleInternal(IHTTPSession session) {
        String uri = session.getUri();
        Method method = session.getMethod();

        // 1. Authentication Routing
        if (uri.startsWith("/api/auth")) {
            return authController.handle(session);
        }

        // 2. Static Resources (Bypass authorization)
        if ("/login.html".equals(uri) || uri.startsWith("/css/") || uri.startsWith("/js/")) {
            return serveExternalResource(uri);
        }
        if (uri.endsWith("/auth-integration.js")) {
            return handleGetResource("/auth-integration.js", "application/javascript");
        }
        if (uri.contains("/forge-viz/")) {
            int idx = uri.indexOf("/forge-viz/");
            String fileName = uri.substring(idx + "/forge-viz/".length());
            return handleGetResource("forge-viz/" + fileName, "application/javascript");
        }
        if (uri.endsWith("/creatic.js")) {
            return handleGetResource("/creatic.js", "application/javascript");
        }
        if (uri.endsWith("/creatic.css")) {
            return handleGetResource("/creatic.css", "text/css");
        }
        if (uri.endsWith("/shared.css")) {
            return handleGetResource("/shared.css", "text/css");
        }
        if (uri.endsWith("/architecture.css")) {
            return handleGetResource("/architecture.css", "text/css");
        }
        if (uri.endsWith("/genome.js")) {
            return handleGetResource("/genome.js", "application/javascript");
        }
        if (uri.endsWith("/genome.css")) {
            return handleGetResource("/genome.css", "text/css");
        }

        // 3. Environment-Aware Authorization Check
        try {
            if (!isAuthorized(session)) {
                // For API calls, return 401.
                if (uri.startsWith("/server/") || uri.startsWith("/task") || uri.startsWith("/forge/") || uri.startsWith("/api/")) {
                    return newFixedLengthResponse(Response.Status.UNAUTHORIZED, "application/json",
                        new JSONObject().put("error", "Unauthorized").toString());
                }
            }
        } catch (Exception e) {
            System.err.println("Auth validation failure: " + e.getMessage());
            // If it's an HTML page request, return a 500 error page instead of redirecting to login
            if (Method.GET.equals(method) && (uri.endsWith(".html") || uri.startsWith("/experimental/"))) {
                return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_HTML,
                    "<html><body style='font-family:Tahoma;background:#ECE9D8;padding:50px;'><h1>Security Subsystem Busy</h1><p>The authentication database is currently locked. Please refresh the page in a few seconds.</p></body></html>");
            }
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json",
                new JSONObject().put("error", "Security subsystem busy").toString());
        }

        // 4. Authorized Static Resources (Dashboard)
        if ("/dashboard.html".equals(uri)) {
            return serveExternalResource(uri);
        }

        trackHttpSession(session);

        try {
            if (Method.GET.equals(method) && ("/".equals(uri) || "/index.html".equals(uri))) {
                return handleGetIndex();
            } else if (Method.GET.equals(method) && "/experimental/chat".equals(uri)) {
                updateSessionWorkflow(session, "CHAT");
                return handleGetChat();
            } else if (Method.GET.equals(method) && "/experimental/forge".equals(uri)) {
                updateSessionWorkflow(session, "FORGE");
                return handleGetForge();
            } else if (Method.GET.equals(method) && "/experimental/architecture".equals(uri)) {
                updateSessionWorkflow(session, "ARCHITECTURE");
                return handleGetArchitecture(session);
            } else if (Method.GET.equals(method) && "/experimental/evolution/tree".equals(uri)) {
                return handleGetEvolutionTreePage();
            } else if (Method.GET.equals(method) && "/evolution/tree".equals(uri)) {
                return handleGetEvolutionTreeJson(session);
            } else if (Method.GET.equals(method) && "/server/status".equals(uri)) {
                return handleGetServerStatus();
            } else if (Method.GET.equals(method) && "/server/system/state".equals(uri)) {
                return handleGetSystemState();
            } else if (Method.POST.equals(method) && "/server/session/ui".equals(uri)) {
                return handleRegisterUiSession(session);
            } else if (Method.GET.equals(method) && uri.startsWith("/server/conversation/")) {
                String sessionIdParam = uri.substring("/server/conversation/".length());
                return handleGetConversation(sessionIdParam);
            } else if (Method.GET.equals(method) && "/server/orchestrator/settings".equals(uri)) {
                return handleGetSettings();
            } else if (Method.POST.equals(method) && "/server/orchestrator/settings".equals(uri)) {
                return handleUpdateSettings(session);
            } else if (Method.POST.equals(method) && "/task".equals(uri)) {
                return handleCreateTask(session);
            } else if (Method.GET.equals(method) && uri.startsWith("/task/")) {
                String taskId = uri.substring(6);
                if (taskId.contains("/")) {
                    String[] parts = taskId.split("/");
                    if (parts.length == 2 && "approve".equals(parts[1])) {
                        return handleApproveTask(parts[0], session);
                    } else if (parts.length == 2 && "input".equals(parts[1])) {
                        return handleInputTask(parts[0], session);
                    }
                }
                return handleGetTask(taskId);
            } else if (Method.POST.equals(method) && uri.startsWith("/conversation/")) {
                String convId = uri.substring("/conversation/".length());
                if (convId.endsWith("/approve")) {
                    return handleApproveConversation(convId.substring(0, convId.length() - 8), session);
                } else if (convId.endsWith("/input")) {
                    return handleInputConversation(convId.substring(0, convId.length() - 6), session);
                } else if (convId.endsWith("/feedback")) {
                    return handleFeedbackConversation(convId.substring(0, convId.length() - 9), session);
                }
            } else if (Method.GET.equals(method) && "/workspace/files".equals(uri)) {
                return handleListFiles(session);
            } else if (Method.POST.equals(method) && "/workspace/applyPatch".equals(uri)) {
                return handleApplyPatch(session);
            } else if (Method.GET.equals(method) && "/git/branches".equals(uri)) {
                return handleGetGitBranches(session);
            } else if (Method.GET.equals(method) && "/forge/sessions".equals(uri)) {
                return handleGetForgeSessions();
            } else if (Method.POST.equals(method) && "/forge/session".equals(uri)) {
                return handleCreateForgeSession(session);
            } else if (Method.POST.equals(method) && "/forge/llm/test".equals(uri)) {
                return handleTestLlmModel(session);
            } else if (Method.POST.equals(method) && uri.startsWith("/forge/session/") && uri.endsWith("/generate-architecture")) {
                String id = uri.substring("/forge/session/".length(), uri.length() - "/generate-architecture".length());
                return handleGenerateArchitecture(id, session);
            } else if (Method.POST.equals(method) && "/forge/save-all".equals(uri)) {
                return handleForgeSaveAll();
            } else if (uri.startsWith("/forge/session/")) {
                String idPart = uri.substring("/forge/session/".length());
                String[] parts = idPart.split("/");
                String id = parts[0];

                if (parts.length > 1) {
                    String subAction = idPart.substring(id.length());
                    if (Method.GET.equals(method)) {
                        if (subAction.equals("/model")) return handleGetForgeModel(id);
                        if (subAction.equals("/structure")) return handleGetForgeStructure(id);
                        if (subAction.equals("/dataset/stats")) return handleGetDatasetStats(id);
                        if (subAction.startsWith("/dataset/sample/")) return handleGetDatasetSample(id, Integer.parseInt(parts[3]));
                        if (subAction.equals("/training/metrics")) return handleGetTrainingMetrics(id);
                        if (subAction.equals("/training/events")) return handleGetTrainingEvents(id);
                        if (subAction.startsWith("/snapshots/compare/active/")) return handleCompareSnapshots(id, parts[4]);
                        if (subAction.equals("/snapshots")) return handleGetForgeSnapshots(id);
                        if (subAction.equals("/forging/stats")) return handleGetForgingStats(id);
                        if (subAction.equals("/events")) return handleGetForgeEvents(id);
                        if (subAction.equals("/datasources")) return handleGetDatasources(id);
                    } else if (Method.POST.equals(method)) {
                        if (subAction.equals("/model")) return handleUpdateForgeModel(id, session);
                        if (subAction.equals("/clone")) return handleCloneForgeSession(id, session);
                        if (subAction.startsWith("/uistate/")) return handleUpdateUiState(id, parts[2], session);
                        if (subAction.equals("/forging/start")) return handleStartForging(id);
                        if (subAction.equals("/demo")) return handleRunForgeDemo(id);
                        if (subAction.equals("/generate-architecture")) return handleGenerateArchitecture(id, session);
                        if (subAction.equals("/open-folder")) return handleOpenFolder(id);
                        if (subAction.equals("/datasources")) return handleUpdateDatasources(id, session);
                    }
                } else {
                    if (Method.GET.equals(method)) return handleGetForgeSession(id);
                    if (Method.DELETE.equals(method)) return handleDeleteForgeSession(id);
                }
            } else if (Method.POST.equals(method) && "/creatic/analyze".equals(uri)) {
                return handleCreaticAnalyze(session);
            } else if (Method.POST.equals(method) && uri.startsWith("/forge/dataset/generate")) {
                return handleGenerateSyntheticDataset(session);
            }
        } catch (Exception e) {
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json",
                new JSONObject().put("error", e.getMessage()).toString());
        }

        return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Not Found");
    }

    private Response handleCreateTask(IHTTPSession session) throws IOException, ResponseException {
        Map<String, String> files = new HashMap<>();
        session.parseBody(files);
        String postData = files.get("postData");
        JSONObject json = new JSONObject(postData);

        TaskRequest request = new TaskRequest();
        request.setPrompt(json.getString("prompt"));
        request.setProjectRoot(new File(json.optString("projectRoot", System.getProperty("user.dir"))));

        if (json.has("model")) {
            request.getContext().put("model", json.getString("model"));
        }
        if (json.has("branch")) {
            request.getContext().put("branch", json.getString("branch"));
        }

        OrchestratorResponse response = OrchestratorServiceImpl.getInstance().handle(request);

        JSONObject jsonRes = new JSONObject();
        jsonRes.put("summary", response.getSummary());
        jsonRes.put("type", response.getResultType().toString());
        if (response.getContent() != null) jsonRes.put("content", response.getContent());

        return newFixedLengthResponse(Response.Status.OK, "application/json", jsonRes.toString());
    }

    private Response handleGetTask(String id) {
        // Legacy support
        TaskResult result = OrchestratorServiceImpl.getInstance().getTaskResult(id);
        if (result == null) {
            return newFixedLengthResponse(Response.Status.NOT_FOUND, "application/json",
                new JSONObject().put("error", "Task not found").toString());
        }

        JSONObject json = new JSONObject();
        json.put("id", result.getId());
        json.put("status", result.getStatus().toString());
        json.put("response", result.getResponse());
        json.put("logs", new JSONArray(result.getLogs()));
        json.put("fileChanges", new JSONArray(result.getFileChanges()));
        if (result.getError() != null) json.put("error", result.getError());
        if (result.getWaitingMessage() != null) json.put("waitingMessage", result.getWaitingMessage());

        return newFixedLengthResponse(Response.Status.OK, "application/json", json.toString());
    }

    private Response handleApproveTask(String id, IHTTPSession session) throws IOException, ResponseException {
        Map<String, String> files = new HashMap<>();
        session.parseBody(files);
        String postData = files.get("postData");
        JSONObject json = new JSONObject(postData != null ? postData : "{\"approved\": true}");

        OrchestratorServiceImpl.getInstance().provideApproval(id, json.optBoolean("approved", true));
        return newFixedLengthResponse(Response.Status.OK, "application/json", new JSONObject().put("status", "ok").toString());
    }

    private Response handleInputTask(String id, IHTTPSession session) throws IOException, ResponseException {
        Map<String, String> files = new HashMap<>();
        session.parseBody(files);
        String postData = files.get("postData");
        JSONObject json = new JSONObject(postData);

        OrchestratorServiceImpl.getInstance().provideInput(id, json.getString("input"));
        return newFixedLengthResponse(Response.Status.OK, "application/json", new JSONObject().put("status", "ok").toString());
    }

    private Response handleListFiles(IHTTPSession session) {
        String rootParam = session.getParms().get("root");
        File root = new File(rootParam != null ? rootParam : System.getProperty("user.dir"));

        // Security check
        if (!isPathSafe(root)) {
            return newFixedLengthResponse(Response.Status.FORBIDDEN, "application/json",
                new JSONObject().put("error", "Access denied: Root directory is outside allowed scope.").toString());
        }

        JSONArray array = new JSONArray();
        listFiles(root, root, array);

        return newFixedLengthResponse(Response.Status.OK, "application/json", array.toString());
    }

    private void listFiles(File root, File current, JSONArray array) {
        File[] files = current.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.getName().startsWith(".")) continue;
                String relativePath = root.toURI().relativize(f.toURI()).getPath();
                array.put(new JSONObject()
                    .put("name", f.getName())
                    .put("path", relativePath)
                    .put("isDirectory", f.isDirectory()));
                if (f.isDirectory()) {
                    listFiles(root, f, array);
                }
            }
        }
    }

    private Response handleApplyPatch(IHTTPSession session) throws IOException, ResponseException {
        Map<String, String> files = new HashMap<>();
        session.parseBody(files);
        String postData = files.get("postData");
        JSONObject json = new JSONObject(postData);

        String path = json.getString("path");
        String content = json.getString("content");
        File root = new File(json.optString("projectRoot", System.getProperty("user.dir")));

        if (!isPathSafe(root)) {
            return newFixedLengthResponse(Response.Status.FORBIDDEN, "application/json",
                new JSONObject().put("error", "Access denied: Project root is outside allowed scope.").toString());
        }

        File target = new File(root, path);
        if (!isChildOf(target, root)) {
            return newFixedLengthResponse(Response.Status.FORBIDDEN, "application/json",
                new JSONObject().put("error", "Access denied: Path is outside project root.").toString());
        }

        Files.writeString(target.toPath(), content);

        return newFixedLengthResponse(Response.Status.OK, "application/json",
            new JSONObject().put("status", "success").toString());
    }

    private Response handleOpenFolder(String sessionId) {
        try {
            File targetDir = null;
            File distDir = new File("dist");
            if (distDir.exists() && distDir.isDirectory()) {
                File[] matchingDirs = distDir.listFiles((dir, name) -> name.startsWith("forging-" + sessionId + "-"));
                if (matchingDirs != null && matchingDirs.length > 0) {
                    File latestDir = matchingDirs[0];
                    for (File d : matchingDirs) {
                        if (d.lastModified() > latestDir.lastModified()) {
                            latestDir = d;
                        }
                    }
                    targetDir = latestDir;
                }
            }
            if (targetDir == null) {
                targetDir = new File("dist/evo-" + sessionId);
            }
            if (!targetDir.exists()) {
                targetDir = new File("dist");
            }

            if (targetDir.exists() && java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop.getDesktop().open(targetDir);
                return newFixedLengthResponse(Response.Status.OK, "application/json", "{\"status\": \"ok\"}");
            }
            return newFixedLengthResponse(Response.Status.NOT_FOUND, "application/json", "{\"error\": \"Folder not found or Desktop not supported\"}");
        } catch (Exception e) {
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json", "{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    private boolean isPathSafe(File file) {
        try {
            String path = file.getCanonicalPath();
            String userDir = new File(System.getProperty("user.dir")).getCanonicalPath();
            String tmpDir = new File(System.getProperty("java.io.tmpdir")).getCanonicalPath();

            return path.startsWith(userDir) || path.startsWith(tmpDir);
        } catch (IOException e) {
            return false;
        }
    }

    private boolean isChildOf(File child, File parent) {
        try {
            String childPath = child.getCanonicalPath();
            String parentPath = parent.getCanonicalPath();
            return childPath.startsWith(parentPath);
        } catch (IOException e) {
            return false;
        }
    }

    private Response handleGetConversation(String sessionId) {
        List<ChatMessage> history = ConversationOutputController.getInstance().getSessionHistory(sessionId);
        JSONArray array = new JSONArray();
        for (ChatMessage msg : history) {
            array.put(new JSONObject()
                .put("sender", msg.getSender())
                .put("text", msg.getText())
                .put("agentType", msg.getAgentType())
                .put("timestamp", msg.getTimestamp())
                .put("priority", msg.getPriority())
                .put("sequenceNumber", msg.getSequenceNumber())
                .put("turnId", msg.getTurnId())
                .put("isTerminal", msg.isIsTerminal()));
        }
        return newFixedLengthResponse(Response.Status.OK, "application/json", array.toString());
    }

    private Response handleGetForgingStats(String sessionId) {
        eu.kalafatic.evolution.forge.controller.service.SelfEvoForgingService.ForgingStats stats = selfEvoService.getStats(sessionId);

        // Auto-refresh Ollama models when status transitions to COMPLETE
        if ("COMPLETE".equals(stats.status())) {
            if (completedForgingRefreshes.add(sessionId)) {
                try {
                    String ollamaUrl = "http://localhost:11434";
                    Orchestrator orch = OrchestratorServiceImpl.getInstance().getOrchestrator();
                    if (orch != null && orch.getOllama() != null) {
                        ollamaUrl = orch.getOllama().getUrl();
                    }
                    OllamaManager.getInstance().getService(ollamaUrl).refreshModels();
                } catch (Exception e) {
                    // Ignore any refresh errors during stats polling
                }
            }
        } else {
            completedForgingRefreshes.remove(sessionId);
        }

        JSONObject json = new JSONObject()
            .put("status", stats.status())
            .put("progress", stats.progress())
            .put("filesScanned", stats.filesScanned())
            .put("totalFiles", stats.totalFiles())
            .put("instructionsGenerated", stats.instructionsGenerated())
            .put("currentLoss", stats.currentLoss())
            .put("currentEpoch", stats.currentEpoch())
            .put("outputFolder", stats.outputFolder());
        return newFixedLengthResponse(Response.Status.OK, "application/json", json.toString());
    }

    private Response handleTestLlmModel(IHTTPSession session) {
        String modelName = session.getParms().get("model");
        if (modelName == null || modelName.isEmpty()) modelName = "evo";

        JSONObject report = new JSONObject();
        String ollamaUrl = "http://localhost:11434";
        Orchestrator orch = OrchestratorServiceImpl.getInstance().getOrchestrator();
        if (orch != null && orch.getOllama() != null) {
            ollamaUrl = orch.getOllama().getUrl();
        }

        OllamaService service = OllamaManager.getInstance().getService(ollamaUrl);
        boolean online = service.ping();
        report.put("ollama_online", online);
        report.put("version", online ? service.getVersion() : "N/A");

        if (!online) {
            report.put("status", "ERROR");
            report.put("error", "Ollama server is offline.");
            return newFixedLengthResponse(Response.Status.OK, "application/json", report.toString());
        }

        List<OllamaModel> models = service.loadModels();
        boolean found = false;
        for (OllamaModel m : models) {
            if (m.getName().equalsIgnoreCase(modelName) || m.getName().startsWith(modelName + ":")) {
                found = true;
                break;
            }
        }
        report.put("model_found", found);

        if (!found) {
            report.put("status", "ERROR");
            report.put("error", "Model '" + modelName + "' is not registered in Ollama.");
            return newFixedLengthResponse(Response.Status.OK, "application/json", report.toString());
        }

        // Test generation and measure latency
        long start = System.currentTimeMillis();
        try {
            service.setModel(modelName);
            String response = service.generate("Reply with exactly: OK");
            long latency = System.currentTimeMillis() - start;
            report.put("test_prompt_response", response.trim());
            report.put("latency_ms", latency);
            report.put("status", "SUCCESS");
        } catch (Exception e) {
            report.put("status", "ERROR");
            report.put("error", "Generation failed: " + e.getMessage());
        }

        return newFixedLengthResponse(Response.Status.OK, "application/json", report.toString());
    }

    private Response handleStartForging(String sessionId) {
        try {
            eu.kalafatic.evolution.model.orchestration.ForgeSession s = ForgeSessionManager.getInstance().findSession(sessionId);
            List<String> dataSources = new ArrayList<>();
            if (s != null) {
                String datasetBindings = s.getModelState().getDatasetBindings();
                if (datasetBindings != null && !datasetBindings.trim().isEmpty() && !datasetBindings.equals("[]")) {
                    try {
                        JSONArray arr = new JSONArray(datasetBindings);
                        for (int i = 0; i < arr.length(); i++) {
                            dataSources.add(arr.getString(i));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            if (dataSources.isEmpty()) {
                String codebase = ProjectModelManager.getCodebasePath();
                if (codebase != null) {
                    dataSources.add(codebase);
                } else {
                    dataSources.add("c:\\Users\\petrk\\git\\evolution");
                }
            }

            SessionContainer sessionCont = SessionManager.getInstance().getOrCreateSession(sessionId);
            final eu.kalafatic.evolution.controller.workflow.RuntimeEventBus bus = sessionCont.getEventBus();
            if (bus != null) {
                bus.publish(new eu.kalafatic.evolution.controller.workflow.RuntimeEvent(
                    eu.kalafatic.evolution.controller.workflow.RuntimeEventType.MODE_CHANGED,
                    sessionId, "EvolutionServer", "FORGE"
                ));
            }

            selfEvoService.startForging(sessionId, new File(".").toPath(), dataSources);

            if (bus != null) {
                new Thread(() -> {
                    String lastStatus = "";
                    while (true) {
                        try {
                            Thread.sleep(300);
                            eu.kalafatic.evolution.forge.controller.service.SelfEvoForgingService.ForgingStats stats = selfEvoService.getStats(sessionId);
                            String status = stats.status();
                            if (status == null) status = "IDLE";

                            if (!status.equals(lastStatus)) {
                                lastStatus = status;
                                eu.kalafatic.evolution.controller.workflow.RuntimeEventType type = switch (status) {
                                    case "STARTING" -> eu.kalafatic.evolution.controller.workflow.RuntimeEventType.FLOW_STARTED;
                                    case "SCANNING" -> eu.kalafatic.evolution.controller.workflow.RuntimeEventType.FORGE_SESSION_CREATED;
                                    case "ENHANCING" -> eu.kalafatic.evolution.controller.workflow.RuntimeEventType.FORGE_MODEL_CHANGED;
                                    case "TRAINING" -> eu.kalafatic.evolution.controller.workflow.RuntimeEventType.FORGE_TRAINING_STARTED;
                                    case "EXPORTING" -> eu.kalafatic.evolution.controller.workflow.RuntimeEventType.EXPORT_READY;
                                    case "EXPORT_GGUF" -> eu.kalafatic.evolution.controller.workflow.RuntimeEventType.DEPLOYMENT_STARTED;
                                    case "COMPLETE" -> eu.kalafatic.evolution.controller.workflow.RuntimeEventType.FLOW_COMPLETED;
                                    case "ERROR" -> eu.kalafatic.evolution.controller.workflow.RuntimeEventType.TASK_FAILED;
                                    default -> eu.kalafatic.evolution.controller.workflow.RuntimeEventType.VIEW_UPDATED;
                                };

                                eu.kalafatic.evolution.controller.workflow.RuntimeEvent ev = new eu.kalafatic.evolution.controller.workflow.RuntimeEvent(type, sessionId, "ForgeService", status)
                                    .withMetadata("status", status)
                                    .withMetadata("progress", stats.progress())
                                    .withMetadata("filesScanned", stats.filesScanned())
                                    .withMetadata("totalFiles", stats.totalFiles())
                                    .withMetadata("instructionsGenerated", stats.instructionsGenerated())
                                    .withMetadata("currentLoss", stats.currentLoss())
                                    .withMetadata("currentEpoch", stats.currentEpoch())
                                    .withMetadata("outputFolder", stats.outputFolder());

                                bus.publish(ev);
                            } else if ("TRAINING".equals(status) || "SCANNING".equals(status) || "ENHANCING".equals(status) || "EXPORTING".equals(status) || "EXPORT_GGUF".equals(status)) {
                                eu.kalafatic.evolution.controller.workflow.RuntimeEvent ev = new eu.kalafatic.evolution.controller.workflow.RuntimeEvent(eu.kalafatic.evolution.controller.workflow.RuntimeEventType.EVOLUTION_PROGRESS, sessionId, "ForgeService", status)
                                    .withMetadata("status", status)
                                    .withMetadata("progress", stats.progress())
                                    .withMetadata("filesScanned", stats.filesScanned())
                                    .withMetadata("totalFiles", stats.totalFiles())
                                    .withMetadata("instructionsGenerated", stats.instructionsGenerated())
                                    .withMetadata("currentLoss", stats.currentLoss())
                                    .withMetadata("currentEpoch", stats.currentEpoch())
                                    .withMetadata("outputFolder", stats.outputFolder());

                                bus.publish(ev);
                            }

                            if ("COMPLETE".equals(status) || "ERROR".equals(status) || "IDLE".equals(status)) {
                                break;
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            break;
                        }
                    }
                }, "ForgeStatsPoller-" + sessionId).start();
            }

            return newFixedLengthResponse(Response.Status.OK, "application/json", "{\"status\": \"started\"}");
        } catch (Exception e) {
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", e.getMessage());
        }
    }

    private Response handleGetDatasources(String id) {
        eu.kalafatic.evolution.model.orchestration.ForgeSession s = ForgeSessionManager.getInstance().findSession(id);
        if (s == null) return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Session not found");

        String datasetBindings = s.getModelState().getDatasetBindings();
        JSONArray arr;
        if (datasetBindings == null || datasetBindings.trim().isEmpty() || datasetBindings.equals("[]")) {
            arr = new JSONArray();
            String codebase = ProjectModelManager.getCodebasePath();
            if (codebase != null) {
                arr.put(codebase);
            } else {
                arr.put("c:\\Users\\petrk\\git\\evolution");
            }
        } else {
            try {
                arr = new JSONArray(datasetBindings);
            } catch (Exception e) {
                arr = new JSONArray();
                String codebase = ProjectModelManager.getCodebasePath();
                if (codebase != null) {
                    arr.put(codebase);
                } else {
                    arr.put("c:\\Users\\petrk\\git\\evolution");
                }
            }
        }
        return newFixedLengthResponse(Response.Status.OK, "application/json", arr.toString());
    }

    private Response handleUpdateDatasources(String id, IHTTPSession session) throws IOException, ResponseException {
        Map<String, String> files = new HashMap<>();
        session.parseBody(files);
        String postData = files.get("postData");

        eu.kalafatic.evolution.model.orchestration.ForgeSession s = ForgeSessionManager.getInstance().findSession(id);
        if (s == null) return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Session not found");

        s.getModelState().setDatasetBindings(postData);
        s.setLastModified(System.currentTimeMillis());

        try {
            Orchestrator orch = OrchestratorServiceImpl.getInstance().getOrchestrator();
            if (orch != null && orch.eResource() != null) {
                ProjectModelManager.getInstance().saveResource(orch.eResource());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return newFixedLengthResponse(Response.Status.OK, "application/json", "{\"status\": \"ok\"}");
    }

    private Response handleGetSettings() {
        Orchestrator orch = OrchestratorServiceImpl.getInstance().getOrchestrator();
        if (orch == null) {
            return newFixedLengthResponse(Response.Status.NOT_FOUND, "application/json", "{}");
        }

        JSONObject json = new JSONObject();
        json.put("aiMode", orch.getAiMode().getName());
        json.put("localModel", orch.getLocalModel());
        json.put("remoteModel", orch.getRemoteModel());
        json.put("darwinMode", orch.isDarwinMode());

        String userHome = System.getProperty("user.home");
        File ollamaHomeModels = new File(userHome, ".ollama/models");
        json.put("ollamaTargetDir", ollamaHomeModels.getAbsolutePath());

        String codebase = ProjectModelManager.getCodebasePath();
        File sourceModels = new File(codebase != null ? codebase : System.getProperty("user.dir"), "source/models");
        json.put("workspaceTargetDir", sourceModels.getAbsolutePath());

        if (orch.getServerSettings() != null) {
            json.put("authenticate", orch.getServerSettings().isAuthenticate());
        }

        if (orch.getAiChat() != null && orch.getAiChat().getPromptInstructions() != null) {
            PromptInstructions instr = orch.getAiChat().getPromptInstructions();
            json.put("iterativeMode", instr.isIterativeMode());
            json.put("selfIterativeMode", instr.isSelfIterativeMode());
            json.put("stepMode", instr.isStepMode());
            json.put("autoApprove", instr.isAutoApprove());
            json.put("gitAutomation", instr.isGitAutomation());
            json.put("maxIterations", instr.getPreferredMaxIterations());
        }

        return newFixedLengthResponse(Response.Status.OK, "application/json", json.toString());
    }

    private Response handleUpdateSettings(IHTTPSession session) throws IOException, ResponseException {
        Map<String, String> files = new HashMap<>();
        session.parseBody(files);
        String postData = files.get("postData");
        JSONObject json = new JSONObject(postData);

        Orchestrator orch = OrchestratorServiceImpl.getInstance().getOrchestrator();
        if (orch != null) {
            if (json.has("aiMode")) {
                for (eu.kalafatic.evolution.model.orchestration.AiMode mode : eu.kalafatic.evolution.model.orchestration.AiMode.values()) {
                    if (mode.getName().equalsIgnoreCase(json.getString("aiMode"))) {
                        orch.setAiMode(mode);
                        break;
                    }
                }
            }
            if (json.has("localModel")) orch.setLocalModel(json.getString("localModel"));
            if (json.has("remoteModel")) orch.setRemoteModel(json.getString("remoteModel"));
            if (json.has("darwinMode")) orch.setDarwinMode(json.getBoolean("darwinMode"));
            if (json.has("authenticate") && orch.getServerSettings() != null) {
                boolean auth = json.getBoolean("authenticate");
                orch.getServerSettings().setAuthenticate(auth);
                System.setProperty("evolution.api.authenticate", String.valueOf(auth));
            }

            if (orch.getAiChat() != null && orch.getAiChat().getPromptInstructions() != null) {
                PromptInstructions instr = orch.getAiChat().getPromptInstructions();
                if (json.has("iterativeMode")) instr.setIterativeMode(json.getBoolean("iterativeMode"));
                if (json.has("selfIterativeMode")) instr.setSelfIterativeMode(json.getBoolean("selfIterativeMode"));
                if (json.has("stepMode")) instr.setStepMode(json.getBoolean("stepMode"));
                if (json.has("autoApprove")) instr.setAutoApprove(json.getBoolean("autoApprove"));
                if (json.has("gitAutomation")) instr.setGitAutomation(json.getBoolean("gitAutomation"));
                if (json.has("maxIterations")) instr.setPreferredMaxIterations(json.getInt("maxIterations"));
            }
        }

        return newFixedLengthResponse(Response.Status.OK, "application/json", new JSONObject().put("status", "ok").toString());
    }

    private Response handleGetChat() {
        try (InputStream is = getClass().getResourceAsStream("chat.html")) {
            if (is == null) {
                return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "chat.html not found");
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String content = reader.lines().collect(Collectors.joining("\n"));
                return newFixedLengthResponse(Response.Status.OK, "text/html", content);
            }
        } catch (IOException e) {
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", e.getMessage());
        }
    }

    private Response handleGetEvolutionTreePage() {
        try (InputStream is = getClass().getResourceAsStream("evolution_tree.html")) {
            if (is == null) {
                return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "evolution_tree.html not found");
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String content = reader.lines().collect(Collectors.joining("\n"));
                return newFixedLengthResponse(Response.Status.OK, "text/html", content);
            }
        } catch (IOException e) {
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", e.getMessage());
        }
    }

    private Response handleGetEvolutionTreeJson(IHTTPSession session) {
        String sessionId = session.getParms().get("sessionId");
        if (sessionId == null) {
            Orchestrator orch = OrchestratorServiceImpl.getInstance().getOrchestrator();
            if (orch != null && orch.getSelfDevSession() != null) {
                sessionId = orch.getSelfDevSession().getId();
            }
        }

        if (sessionId == null) {
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, "application/json", "{\"error\": \"Missing sessionId\"}");
        }

        String json = evolutionDashboardController.getEvolutionTreeJson(sessionId);
        return newFixedLengthResponse(Response.Status.OK, "application/json", json);
    }

    private Response handleGetArchitecture(IHTTPSession session) {
        String path = session.getParms().get("path");
        String mode = session.getParms().get("mode");

        Orchestrator orch = OrchestratorServiceImpl.getInstance().getOrchestrator();
        String html = architectureController.renderArchitecture(orch, path, mode);

        if (html == null || "Error: Template not found".equals(html)) {
            return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Architecture template not found.");
        }

        return newFixedLengthResponse(Response.Status.OK, "text/html", html);
    }

    private Response handleGetForge() {
        try (InputStream is = getClass().getResourceAsStream("forge.html")) {
            if (is == null) {
                return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "forge.html not found");
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String content = reader.lines().collect(Collectors.joining("\n"));
                return newFixedLengthResponse(Response.Status.OK, "text/html", content);
            }
        } catch (IOException e) {
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", e.getMessage());
        }
    }

    private Response handleApproveConversation(String id, IHTTPSession session) throws IOException, ResponseException {
        Map<String, String> files = new HashMap<>();
        session.parseBody(files);
        String postData = files.get("postData");
        JSONObject json = new JSONObject(postData != null ? postData : "{\"approved\": true}");

        OrchestratorServiceImpl.getInstance().provideApproval(id, json.optBoolean("approved", true));
        return newFixedLengthResponse(Response.Status.OK, "application/json", new JSONObject().put("status", "ok").toString());
    }

    private Response handleInputConversation(String id, IHTTPSession session) throws IOException, ResponseException {
        Map<String, String> files = new HashMap<>();
        session.parseBody(files);
        String postData = files.get("postData");
        JSONObject json = new JSONObject(postData);

        OrchestratorServiceImpl.getInstance().provideInput(id, json.getString("input"));
        return newFixedLengthResponse(Response.Status.OK, "application/json", new JSONObject().put("status", "ok").toString());
    }

    private Response handleFeedbackConversation(String id, IHTTPSession session) throws IOException, ResponseException {
        Map<String, String> files = new HashMap<>();
        session.parseBody(files);
        String postData = files.get("postData");
        JSONObject json = new JSONObject(postData);

        // This is a bit tricky as submitFeedback on AiChatPage does more, but we can at least record it in the model
        Orchestrator orch = OrchestratorServiceImpl.getInstance().getOrchestrator();
        if (orch != null && orch.getSelfDevSession() != null) {
             int satisfaction = json.optInt("satisfaction", 5);
             String comments = json.optString("comments", "");
             // Basic recording - in a real scenario we would trigger the same logic as AiChatPage.submitFeedback
             // For now we'll just return OK as the "non-authoritative" web UI
        }

        return newFixedLengthResponse(Response.Status.OK, "application/json", new JSONObject().put("status", "ok").toString());
    }

    private boolean isAuthorized(IHTTPSession session) {
        // Check global authentication flag (default to bypass if not explicitly true)
        Orchestrator orch = OrchestratorServiceImpl.getInstance().getOrchestrator();
        boolean authEnabled = orch != null && orch.getServerSettings() != null && orch.getServerSettings().isAuthenticate();
        if (!authEnabled) {
            return true;
        }

        // SWT Browser bypass
        String runtimeHeader = session.getHeaders().get("x-evo-runtime");
        if (runtimeHeader == null) runtimeHeader = session.getHeaders().get("X-Evo-Runtime");
        String runtimeParam = session.getParms().get("runtime");
        if ("SWT".equalsIgnoreCase(runtimeHeader) || "SWT".equalsIgnoreCase(runtimeParam)) {
            return true;
        }

        // Validate session via AuthService
        String sessionId = getSessionIdFromRequest(session);

        if (sessionId != null) {
            // validateSession throws RuntimeException(SQLException) if DB is busy
            java.util.Optional<eu.kalafatic.evolution.servers.model.User> userOpt = authService.validateSession(sessionId);
            return userOpt.isPresent();
        }

        return false;
    }

    private String getSessionIdFromRequest(IHTTPSession session) {
        Map<String, String> headers = session.getHeaders();

        // 1. Authorization Header (Case-insensitive)
        String authHeader = headers.get("authorization");
        if (authHeader == null) authHeader = headers.get("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7).trim();
            if (!token.isEmpty() && !"undefined".equals(token) && !"null".equals(token)) {
                return token;
            }
        }

        // 2. Cookie (via NanoHTTPD CookieHandler)
        String sessionId = session.getCookies().read("sessionId");
        if (sessionId != null && !sessionId.isEmpty()) {
            return trimQuotes(sessionId);
        }

        // 3. Manual Cookie Header parsing (fallback)
        String cookieHeader = headers.get("cookie");
        if (cookieHeader == null) cookieHeader = headers.get("Cookie");

        if (cookieHeader != null) {
            String[] cookies = cookieHeader.split(";");
            for (String cookie : cookies) {
                String[] parts = cookie.trim().split("=");
                if (parts.length >= 2 && "sessionId".equalsIgnoreCase(parts[0])) {
                    return trimQuotes(parts[1]);
                }
            }
        }

        // 4. Query Parameter (fallback)
        String paramId = session.getParms().get("sessionId");
        return (paramId != null && !paramId.isEmpty()) ? paramId : null;
    }

    private String trimQuotes(String val) {
        if (val == null) return null;
        val = val.trim();
        if (val.startsWith("\"") && val.endsWith("\"") && val.length() >= 2) {
            return val.substring(1, val.length() - 1);
        }
        return val;
    }

    private Response serveExternalResource(String uri) {
        Bundle bundle = Platform.getBundle("eu.kalafatic.evolution.servers");
        if (bundle == null) return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Bundle not found");

        String path = "/eu/kalafatic/evolution/servers/web" + uri;
        try (InputStream is = bundle.getResource(path).openStream()) {
            String mimeType = getMimeType(uri);
            byte[] data = is.readAllBytes();
            return newFixedLengthResponse(Response.Status.OK, mimeType, new java.io.ByteArrayInputStream(data), data.length);
        } catch (Exception e) {
            return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Resource not found: " + uri);
        }
    }

    private String getMimeType(String uri) {
        if (uri.endsWith(".html")) return "text/html";
        if (uri.endsWith(".css")) return "text/css";
        if (uri.endsWith(".js")) return "application/javascript";
        if (uri.endsWith(".png")) return "image/png";
        if (uri.endsWith(".jpg") || uri.endsWith(".jpeg")) return "image/jpeg";
        if (uri.endsWith(".svg")) return "image/svg+xml";
        return "application/octet-stream";
    }

    private void updateSessionWorkflow(IHTTPSession session, String workflowType) {
        String sessionId = getSessionIdFromRequest(session);
        if (sessionId != null) {
            try {
                authService.updateWorkflowType(sessionId, workflowType);
            } catch (Exception e) {
                // Ignore update errors
            }
        }
    }

    private void trackHttpSession(IHTTPSession session) {
        String clientIp = session.getRemoteIpAddress();
        // Use IP as part of session tracking if no token is provided
        String sessionId = "http-" + clientIp;
        ServerSession s = activeSessions.get(sessionId);
        if (s == null) {
            s = OrchestrationFactory.eINSTANCE.createServerSession();
            s.setId(sessionId);
            s.setType(SessionType.HTTPD);
            s.setStartTime(System.currentTimeMillis());
            s.setClientIp(clientIp);
            activeSessions.put(sessionId, s);
        }
        s.setLastActivity(System.currentTimeMillis());
    }

    private Response handleRegisterUiSession(IHTTPSession session) throws IOException, ResponseException {
        Map<String, String> files = new HashMap<>();
        session.parseBody(files);
        String postData = files.get("postData");
        JSONObject json = new JSONObject(postData != null ? postData : "{}");

        String id = json.optString("id", "ui-" + UUID.randomUUID().toString());
        ServerSession s = OrchestrationFactory.eINSTANCE.createServerSession();
        s.setId(id);
        s.setType(SessionType.UI);
        s.setStartTime(System.currentTimeMillis());
        s.setLastActivity(System.currentTimeMillis());
        s.setClientIp(session.getRemoteIpAddress());
        activeSessions.put(id, s);

        return newFixedLengthResponse(Response.Status.OK, "application/json",
            new JSONObject().put("id", id).toString());
    }

    private Response handleGetIndex() {
        try (InputStream is = getClass().getResourceAsStream("index.html")) {
            if (is == null) {
                return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "index.html not found");
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String content = reader.lines().collect(Collectors.joining("\n"));
                return newFixedLengthResponse(Response.Status.OK, "text/html", content);
            }
        } catch (IOException e) {
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", e.getMessage());
        }
    }

    private Response handleGetServerStatus() {
        JSONObject status = new JSONObject();

        // Monitoring Data
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        double cpuLoad = osBean.getSystemLoadAverage(); // Simple load avg
        long freeMem = Runtime.getRuntime().freeMemory();
        long totalMem = Runtime.getRuntime().totalMemory();

        JSONObject monitoring = new JSONObject();
        monitoring.put("cpuUsage", cpuLoad);
        monitoring.put("memoryUsage", totalMem - freeMem);
        monitoring.put("totalMemory", totalMem);
        monitoring.put("timestamp", System.currentTimeMillis());
        status.put("monitoring", monitoring);

        // Ollama Status
        JSONObject ollamaStatus = new JSONObject();
        String ollamaUrl = "http://localhost:11434"; // Default

        // Try to get available models to avoid hardcoded default if it's missing
        OllamaService ollama = new OllamaService(ollamaUrl, null);
        boolean ollamaOnline = ollama.ping();
        ollamaStatus.put("online", ollamaOnline);
        ollamaStatus.put("url", ollamaUrl);
        ollamaStatus.put("version", ollamaOnline ? ollama.getVersion() : "N/A");

        JSONArray modelsArray = new JSONArray();
        if (ollamaOnline) {
            List<OllamaModel> models = ollama.loadModels();
            for (OllamaModel m : models) {
                modelsArray.put(new JSONObject().put("name", m.getName()).put("size", m.getSize()));
            }
        }
        ollamaStatus.put("models", modelsArray);
        status.put("ollama", ollamaStatus);

        // Sessions
        JSONArray sessions = new JSONArray();
        long now = System.currentTimeMillis();
        List<String> toRemove = new ArrayList<>();

        for (ServerSession s : activeSessions.values()) {
            // Cleanup sessions inactive for > 1 hour
            if (now - s.getLastActivity() > 3600000) {
                toRemove.add(s.getId());
                continue;
            }

            sessions.put(new JSONObject()
                .put("id", s.getId())
                .put("type", s.getType().getName())
                .put("startTime", s.getStartTime())
                .put("lastActivity", s.getLastActivity())
                .put("clientIp", s.getClientIp()));
        }

        for (String id : toRemove) activeSessions.remove(id);

        status.put("sessions", sessions);
        status.put("port", getListeningPort());

        return newFixedLengthResponse(Response.Status.OK, "application/json", status.toString());
    }

    private Response handleGetSystemState() {
        Orchestrator orch = OrchestratorServiceImpl.getInstance().getOrchestrator();
        if (orch == null) return newFixedLengthResponse(Response.Status.NOT_FOUND, "application/json", "{}");

        JSONObject root = new JSONObject();
        root.put("name", orch.getName());
        root.put("aiMode", orch.getAiMode().getName());

        JSONArray agents = new JSONArray();
        for (eu.kalafatic.evolution.model.orchestration.Agent a : orch.getAgents()) {
            agents.put(new JSONObject().put("id", a.getId()).put("type", a.getType()));
        }
        root.put("agents", agents);

        JSONArray tasks = new JSONArray();
        for (eu.kalafatic.evolution.model.orchestration.Task t : orch.getTasks()) {
            JSONObject taskObj = new JSONObject()
                .put("id", t.getId())
                .put("name", t.getName())
                .put("status", t.getStatus().toString());

            JSONArray next = new JSONArray();
            for (eu.kalafatic.evolution.model.orchestration.Task n : t.getNext()) next.put(n.getId());
            taskObj.put("next", next);
            tasks.put(taskObj);
        }
        root.put("tasks", tasks);

        JSONObject session = new JSONObject();
        if (orch.getSelfDevSession() != null) {
            session.put("status", orch.getSelfDevSession().getStatus().toString());
            JSONArray iterations = new JSONArray();
            for (eu.kalafatic.evolution.model.orchestration.Iteration i : orch.getSelfDevSession().getIterations()) {
                iterations.put(new JSONObject().put("id", i.getId()).put("phase", i.getPhase()));
            }
            session.put("iterations", iterations);
        } else {
            session.put("status", "ACTIVE");
        }

        // Add Cognitive State for UI
        String sessionId = orch.getId();
        SessionContainer container = SessionManager.getInstance().getSession(sessionId);
        if (container != null) {
            eu.kalafatic.evolution.controller.orchestration.cognitive.SessionCognitiveState cs = container.getCognitiveState();
            if (cs != null) {
                JSONObject cog = new JSONObject();
                cog.put("intent", cs.getCurrentIntent().name());
                cog.put("capability", cs.getCurrentCapability().name());
                cog.put("direction", cs.getCurrentDirection().name());
                cog.put("confidence", cs.getConfidence());

                JSONArray trajectory = new JSONArray();
                cs.getTrajectory().forEach(t -> trajectory.put(t.name()));
                cog.put("trajectory", trajectory);

                session.put("cognitiveState", cog);
            }
        }

        root.put("session", session);

        return newFixedLengthResponse(Response.Status.OK, "application/json", root.toString());
    }

    private Response handleGetForgeSessions() {
        JSONArray array = new JSONArray();
        for (eu.kalafatic.evolution.model.orchestration.ForgeSession s : ForgeSessionManager.getInstance().getSessions()) {
            JSONObject uiState = ForgeSessionManager.getInstance().getUiState(s.getSessionId());
            array.put(new JSONObject()
                .put("id", s.getSessionId())
                .put("name", s.getName())
                .put("modelType", s.getSelectedModelType())
                .put("status", s.getStatus().getName())
                .put("lastModified", s.getLastModified())
                .put("uiState", uiState));
        }
        return newFixedLengthResponse(Response.Status.OK, "application/json", array.toString());
    }

    private Response handleGetForgeSession(String id) {
        eu.kalafatic.evolution.model.orchestration.ForgeSession s = ForgeSessionManager.getInstance().findSession(id);
        if (s == null) return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Session not found");

        JSONObject json = new JSONObject()
            .put("id", s.getSessionId())
            .put("name", s.getName())
            .put("modelType", s.getSelectedModelType())
            .put("status", s.getStatus().getName())
            .put("createdAt", s.getCreatedAt())
            .put("lastModified", s.getLastModified())
            .put("uiState", ForgeSessionManager.getInstance().getUiState(id));

        return newFixedLengthResponse(Response.Status.OK, "application/json", json.toString());
    }

    private Response handleCreateForgeSession(IHTTPSession session) throws IOException, ResponseException {
        Map<String, String> files = new HashMap<>();
        session.parseBody(files);
        String postData = files.get("postData");
        JSONObject body = new JSONObject(postData);

        boolean isDemo = body.optBoolean("isDemo", false);
        eu.kalafatic.evolution.model.orchestration.ForgeSession s = ForgeSessionManager.getInstance().createSession(
            body.getString("name"), body.getString("modelType"), isDemo);

        return newFixedLengthResponse(Response.Status.OK, "application/json", new JSONObject().put("id", s.getSessionId()).toString());
    }

    private Response handleForgeSaveAll() {
        try {
            Orchestrator orch = OrchestratorServiceImpl.getInstance().getOrchestrator();
            if (orch != null && orch.eResource() != null) {
                ProjectModelManager.getInstance().saveResource(orch.eResource());
                return newFixedLengthResponse(Response.Status.OK, "application/json", "{\"status\": \"ok\"}");
            }
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json", "{\"error\": \"No active resource to save\"}");
        } catch (IOException e) {
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json", "{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    private Response handleDeleteForgeSession(String id) {
        boolean deleted = ForgeSessionManager.getInstance().deleteSession(id);
        if (deleted) return newFixedLengthResponse(Response.Status.OK, "application/json", "{\"status\": \"ok\"}");
        return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Session not found");
    }

    private Response handleGetForgeModel(String id) {
        eu.kalafatic.evolution.model.orchestration.ForgeSession s = ForgeSessionManager.getInstance().findSession(id);
        if (s == null) return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Session not found");

        return newFixedLengthResponse(Response.Status.OK, "application/json", s.getModelState().getModelGraph());
    }

    private Response handleUpdateForgeModel(String id, IHTTPSession session) throws IOException, ResponseException {
        Map<String, String> files = new HashMap<>();
        session.parseBody(files);
        String postData = files.get("postData");

        ForgeSessionManager.getInstance().updateModel(id, postData);
        return newFixedLengthResponse(Response.Status.OK, "application/json", "{\"status\": \"ok\"}");
    }

    private Response handleGenerateArchitecture(String id, IHTTPSession session) throws IOException, ResponseException {
        Map<String, String> files = new HashMap<>();
        session.parseBody(files);
        String postData = files.get("postData");
        JSONObject body = new JSONObject(postData);
        String modelType = body.getString("modelType");
        boolean isDemo = body.optBoolean("isDemo", false);

        eu.kalafatic.evolution.model.orchestration.ForgeSession s = ForgeSessionManager.getInstance().findSession(id);
        if (s == null) return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Session not found");

        if (isDemo) {
            ForgeSessionManager.getInstance().updateUiState(id, "isDemo", true);
        }

        ForgeSessionManager.getInstance().generateArchitecture(s, modelType);
        return newFixedLengthResponse(Response.Status.OK, "application/json", "{\"status\": \"ok\"}");
    }

    private Response handleGenerateSyntheticDataset(IHTTPSession session) throws IOException, ResponseException {
        Map<String, String> files = new HashMap<>();
        session.parseBody(files);
        String postData = files.get("postData");
        JSONObject body = new JSONObject(postData != null ? postData : "{}");
        String type = body.optString("type", "random");

        String result = ForgeSessionManager.getInstance().generateSyntheticDataset(type);
        return newFixedLengthResponse(Response.Status.OK, "application/json", result);
    }

    private Response handleCloneForgeSession(String id, IHTTPSession session) throws IOException, ResponseException {
        Map<String, String> files = new HashMap<>();
        session.parseBody(files);
        String postData = files.get("postData");
        JSONObject body = new JSONObject(postData);

        eu.kalafatic.evolution.model.orchestration.ForgeSession clone = ForgeSessionManager.getInstance().cloneSession(id, body.getString("name"));
        if (clone == null) return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Original session not found");

        return newFixedLengthResponse(Response.Status.OK, "application/json", new JSONObject().put("id", clone.getSessionId()).toString());
    }

    private Response handleGetForgeStructure(String sessionId) {
        if (modelController == null) return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "ModelController not available");
        return newFixedLengthResponse(Response.Status.OK, "application/json", modelController.getModelStructure(sessionId));
    }

    private Response handleGetDatasetStats(String sessionId) {
        if (datasetController == null) return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "DatasetController not available");
        JSONObject stats = new JSONObject();
        Map<String, Object> data = datasetController.getDatasetStatistics(sessionId);
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            stats.put(entry.getKey(), entry.getValue());
        }
        return newFixedLengthResponse(Response.Status.OK, "application/json", stats.toString());
    }

    private Response handleGetDatasetSample(String sessionId, int index) {
        if (datasetController == null) return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "DatasetController not available");
        JSONObject sample = new JSONObject();
        Map<String, Object> data = datasetController.getDatasetSample(sessionId, index);
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            sample.put(entry.getKey(), entry.getValue());
        }
        return newFixedLengthResponse(Response.Status.OK, "application/json", sample.toString());
    }

    private Response handleGetTrainingMetrics(String sessionId) {
        if (trainingController == null) return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "TrainingController not available");
        JSONObject metrics = new JSONObject();
        Map<String, Object> data = trainingController.getTrainingMetrics(sessionId);
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            metrics.put(entry.getKey(), entry.getValue());
        }
        return newFixedLengthResponse(Response.Status.OK, "application/json", metrics.toString());
    }

    private Response handleGetTrainingEvents(String sessionId) {
        if (trainingController == null) return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "TrainingController not available");
        JSONArray array = new JSONArray(trainingController.getRecentEvents(sessionId));
        return newFixedLengthResponse(Response.Status.OK, "application/json", array.toString());
    }

    private Response handleGetForgeSnapshots(String sessionId) {
        if (snapshotController == null) return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "SnapshotController not available");
        JSONArray array = new JSONArray();
        List<Map<String, Object>> snapshots = snapshotController.getSnapshots(sessionId);
        for (Map<String, Object> s : snapshots) {
            array.put(new JSONObject(s));
        }
        return newFixedLengthResponse(Response.Status.OK, "application/json", array.toString());
    }

    private Response handleCompareSnapshots(String sessionId, String snapshotId) {
        if (snapshotController == null) return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "SnapshotController not available");
        JSONObject diff = new JSONObject();
        Map<String, Object> data = snapshotController.compareSnapshots("active", snapshotId);
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            diff.put(entry.getKey(), entry.getValue());
        }
        return newFixedLengthResponse(Response.Status.OK, "application/json", diff.toString());
    }

    private Response handleUpdateUiState(String sessionId, String key, IHTTPSession session) throws IOException, ResponseException {
        Map<String, String> files = new HashMap<>();
        session.parseBody(files);
        String postData = files.get("postData");
        JSONObject body = new JSONObject(postData);
        Object value = body.get("value");

        ForgeSessionManager.getInstance().updateUiState(sessionId, key, value);
        return newFixedLengthResponse(Response.Status.OK, "application/json", "{\"status\": \"ok\"}");
    }

    private Response handleRunForgeDemo(String sessionId) {
        ForgeSessionManager.getInstance().runE2EDemo(sessionId);
        return newFixedLengthResponse(Response.Status.OK, "application/json", "{\"status\": \"started\"}");
    }

    private Response handleGetForgeEvents(String sessionId) {
        List<eu.kalafatic.evolution.controller.workflow.RuntimeEvent> events = ForgeSessionManager.getInstance().getRecentEvents(sessionId);
        JSONArray array = new JSONArray();
        for (eu.kalafatic.evolution.controller.workflow.RuntimeEvent e : events) {
            array.put(new JSONObject()
                .put("type", e.getType().name())
                .put("entityId", e.getEntityId())
                .put("action", e.getAction())
                .put("timestamp", e.getTimestamp()));
        }
        return newFixedLengthResponse(Response.Status.OK, "application/json", array.toString());
    }

    private Response handleGetGitBranches(IHTTPSession session) {
        String rootParam = session.getParms().get("root");
        File root = new File(rootParam != null ? rootParam : System.getProperty("user.dir"));

        if (!isPathSafe(root)) {
            return newFixedLengthResponse(Response.Status.FORBIDDEN, "application/json",
                new JSONObject().put("error", "Access denied: Root directory is outside allowed scope.").toString());
        }

        GitTool gitTool = new GitTool();
        List<String> branches = Collections.emptyList();
        try {
            branches = gitTool.getBranches(root);
        } catch (Exception e) {}
        JSONArray array = new JSONArray(branches);

        return newFixedLengthResponse(Response.Status.OK, "application/json", array.toString());
    }

    private Response handleCreaticAnalyze(IHTTPSession session) throws IOException, ResponseException {
        Map<String, String> files = new HashMap<>();
        session.parseBody(files);
        String postData = files.get("postData");
        JSONObject json = new JSONObject(postData != null ? postData : "{}");
        String pageId = json.optString("pageId", "general");

        Object response = CreaticAgent.getInstance().analyze(pageId);
        if (response == null) return newFixedLengthResponse(Response.Status.OK, "application/json", "{}");

        // Convert GuidanceResponse to JSON via Reflection to avoid compile-time dependency issues
        JSONObject res = new JSONObject();
        try {
            res.put("summary", (String) response.getClass().getMethod("getSummary").invoke(response));

            JSONArray actions = new JSONArray();
            List<?> actionsList = (List<?>) response.getClass().getMethod("getActions").invoke(response);
            for (Object a : actionsList) {
                actions.put(new JSONObject()
                    .put("label", a.getClass().getMethod("getLabel").invoke(a))
                    .put("actionId", a.getClass().getMethod("getActionId").invoke(a))
                    .put("description", a.getClass().getMethod("getDescription").invoke(a)));
            }
            res.put("actions", actions);

            JSONArray insights = new JSONArray();
            List<?> insightsList = (List<?>) response.getClass().getMethod("getInsights").invoke(response);
            for (Object i : insightsList) {
                insights.put(new JSONObject().put("text", i.getClass().getMethod("getText").invoke(i)));
            }
            res.put("insights", insights);

            JSONArray warnings = new JSONArray();
            List<?> warningsList = (List<?>) response.getClass().getMethod("getWarnings").invoke(response);
            for (Object w : warningsList) {
                warnings.put(new JSONObject().put("text", w.getClass().getMethod("getText").invoke(w)));
            }
            res.put("warnings", warnings);

            JSONArray tips = new JSONArray();
            List<?> tipsList = (List<?>) response.getClass().getMethod("getTips").invoke(response);
            for (Object t : tipsList) {
                tips.put(new JSONObject().put("text", t.getClass().getMethod("getText").invoke(t)));
            }
            res.put("tips", tips);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return newFixedLengthResponse(Response.Status.OK, "application/json", res.toString());
    }

    private Response handleGetResource(String name, String mimeType) {
        try (InputStream is = getClass().getResourceAsStream(name)) {
            if (is == null) {
                return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", name + " not found");
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String content = reader.lines().collect(Collectors.joining("\n"));
                return newFixedLengthResponse(Response.Status.OK, mimeType, content);
            }
        } catch (IOException e) {
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", e.getMessage());
        }
    }

    public static void main(String[] args) {
        int port = 48080;
        if (args.length > 0) port = Integer.parseInt(args[0]);

        EvolutionServer server = new EvolutionServer(port);
        try {
            server.startServer();
            System.out.println("Evolution Server started on port " + port);
        } catch (IOException e) {
            System.err.println("Could not start server:\n" + e);
        }
    }
}
