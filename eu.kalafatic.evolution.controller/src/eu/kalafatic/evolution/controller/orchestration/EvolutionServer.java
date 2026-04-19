package eu.kalafatic.evolution.controller.orchestration;

import fi.iki.elonen.NanoHTTPD;
import org.json.JSONObject;
import org.json.JSONArray;

import eu.kalafatic.evolution.model.orchestration.MonitoringData;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.ServerSession;
import eu.kalafatic.evolution.model.orchestration.SessionType;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import eu.kalafatic.evolution.controller.manager.OllamaService;
import eu.kalafatic.evolution.controller.manager.OllamaModel;
import eu.kalafatic.evolution.controller.tools.GitTool;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Embedded REST server for remote control.
 */
public class EvolutionServer extends NanoHTTPD {

    private final Map<String, ServerSession> activeSessions = new ConcurrentHashMap<>();

    public EvolutionServer(int port) {
        super(port);
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
            } else if (Method.GET.equals(method) && "/server/status".equals(uri)) {
                return handleGetServerStatus();
            } else if (Method.POST.equals(method) && "/server/session/ui".equals(uri)) {
                return handleRegisterUiSession(session);
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
            } else if (Method.GET.equals(method) && "/workspace/files".equals(uri)) {
                return handleListFiles(session);
            } else if (Method.POST.equals(method) && "/workspace/applyPatch".equals(uri)) {
                return handleApplyPatch(session);
            } else if (Method.GET.equals(method) && "/git/branches".equals(uri)) {
                return handleGetGitBranches(session);
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

        TaskResult result = OrchestratorServiceImpl.getInstance().execute(request);
        return newFixedLengthResponse(Response.Status.OK, "application/json",
            new JSONObject().put("id", result.getId()).put("status", result.getStatus().toString()).toString());
    }

    private Response handleGetTask(String id) {
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

    private Response handleGetGitBranches(IHTTPSession session) {
        String rootParam = session.getParms().get("root");
        File root = new File(rootParam != null ? rootParam : System.getProperty("user.dir"));

        if (!isPathSafe(root)) {
            return newFixedLengthResponse(Response.Status.FORBIDDEN, "application/json",
                new JSONObject().put("error", "Access denied: Root directory is outside allowed scope.").toString());
        }

        GitTool gitTool = new GitTool();
        List<String> branches = gitTool.getBranches(root);
        JSONArray array = new JSONArray(branches);

        return newFixedLengthResponse(Response.Status.OK, "application/json", array.toString());
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
