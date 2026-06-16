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

import org.json.JSONArray;
import org.json.JSONObject;

import eu.kalafatic.evolution.forge.controller.api.DatasetController;
import eu.kalafatic.evolution.forge.controller.api.ModelController;
import eu.kalafatic.evolution.forge.controller.api.SessionController;
import eu.kalafatic.evolution.forge.controller.api.SnapshotController;
import eu.kalafatic.evolution.forge.controller.api.TrainingController;
import eu.kalafatic.evolution.controller.manager.OllamaModel;
import eu.kalafatic.evolution.controller.manager.OllamaService;
import eu.kalafatic.evolution.controller.tools.GitTool;
import eu.kalafatic.evolution.creatic.api.CreaticAgent;
import eu.kalafatic.evolution.model.orchestration.ChatMessage;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.PromptInstructions;
import eu.kalafatic.evolution.model.orchestration.ServerSession;
import eu.kalafatic.evolution.model.orchestration.SessionType;
import fi.iki.elonen.NanoHTTPD;

/**
 * Embedded REST server for remote control.
 */
public class EvolutionServer extends NanoHTTPD {

    private final Map<String, ServerSession> activeSessions = new ConcurrentHashMap<>();
    private SessionController sessionController;
    private ModelController modelController;
    private DatasetController datasetController;
    private TrainingController trainingController;
    private SnapshotController snapshotController;

    public EvolutionServer(int port) {
        super(port);
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
        String uri = session.getUri();
        Method method = session.getMethod();

        trackHttpSession(session);

        try {
            if (Method.GET.equals(method) && ("/".equals(uri) || "/index.html".equals(uri))) {
                return handleGetIndex();
            } else if (Method.GET.equals(method) && "/experimental/chat".equals(uri)) {
                return handleGetChat();
            } else if (Method.GET.equals(method) && "/experimental/forge".equals(uri)) {
                return handleGetForge();
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
            } else if (Method.GET.equals(method) && uri.startsWith("/forge/session/")) {
                String id = uri.substring("/forge/session/".length());
                if (id.endsWith("/model")) {
                    return handleGetForgeModel(id.substring(0, id.length() - 6));
                }
                return handleGetForgeSession(id);
            } else if (Method.POST.equals(method) && uri.startsWith("/forge/session/")) {
                String id = uri.substring("/forge/session/".length());
                if (id.endsWith("/model")) {
                    return handleUpdateForgeModel(id.substring(0, id.length() - 6), session);
                } else if (id.endsWith("/clone")) {
                    return handleCloneForgeSession(id.substring(0, id.length() - 6), session);
                }
            } else if (Method.POST.equals(method) && "/creatic/analyze".equals(uri)) {
                return handleCreaticAnalyze(session);
            } else if (Method.GET.equals(method) && "/creatic.js".equals(uri)) {
                return handleGetResource("creatic.js", "application/javascript");
            } else if (Method.GET.equals(method) && "/creatic.css".equals(uri)) {
                return handleGetResource("creatic.css", "text/css");
            } else if (Method.DELETE.equals(method) && uri.startsWith("/forge/session/")) {
                return handleDeleteForgeSession(uri.substring("/forge/session/".length()));
            } else if (Method.POST.equals(method) && uri.startsWith("/forge/dataset/generate")) {
                return handleGenerateSyntheticDataset(session);
            } else if (uri.startsWith("/forge/session/")) {
                String id = uri.substring("/forge/session/".length());
                if (id.contains("/structure")) {
                    return handleGetForgeStructure(id.split("/")[0]);
                } else if (id.contains("/dataset/stats")) {
                    return handleGetDatasetStats(id.split("/")[0]);
                } else if (id.contains("/dataset/sample/")) {
                    String[] parts = id.split("/");
                    return handleGetDatasetSample(parts[0], Integer.parseInt(parts[3]));
                } else if (id.contains("/training/metrics")) {
                    return handleGetTrainingMetrics(id.split("/")[0]);
                } else if (id.contains("/training/events")) {
                    return handleGetTrainingEvents(id.split("/")[0]);
                } else if (id.contains("/snapshots/compare/active/")) {
                    String[] parts = id.split("/");
                    return handleCompareSnapshots(parts[0], parts[4]);
                } else if (id.contains("/snapshots")) {
                    return handleGetForgeSnapshots(id.split("/")[0]);
                } else if (id.contains("/uistate/")) {
                    String[] parts = id.split("/");
                    return handleUpdateUiState(parts[0], parts[2], session);
                }
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

        if (orch.getSelfDevSession() != null) {
            JSONObject session = new JSONObject();
            session.put("status", orch.getSelfDevSession().getStatus().toString());
            JSONArray iterations = new JSONArray();
            for (eu.kalafatic.evolution.model.orchestration.Iteration i : orch.getSelfDevSession().getIterations()) {
                iterations.put(new JSONObject().put("id", i.getId()).put("phase", i.getPhase()));
            }
            session.put("iterations", iterations);

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
        }

        return newFixedLengthResponse(Response.Status.OK, "application/json", root.toString());
    }

    private Response handleGetForgeSessions() {
        JSONArray array = new JSONArray();
        for (eu.kalafatic.evolution.model.orchestration.ForgeSession s : ForgeSessionManager.getInstance().getSessions()) {
            array.put(new JSONObject()
                .put("id", s.getSessionId())
                .put("name", s.getName())
                .put("modelType", s.getSelectedModelType())
                .put("status", s.getStatus().getName())
                .put("lastModified", s.getLastModified()));
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

        eu.kalafatic.evolution.model.orchestration.ForgeSession s = ForgeSessionManager.getInstance().createSession(
            body.getString("name"), body.getString("modelType"));

        return newFixedLengthResponse(Response.Status.OK, "application/json", new JSONObject().put("id", s.getSessionId()).toString());
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
