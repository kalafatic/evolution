package eu.kalafatic.evolution.controller.orchestration.develop;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import eu.kalafatic.evolution.controller.orchestration.ConversationOutputController;
import eu.kalafatic.evolution.controller.orchestration.MessagePriority;
import eu.kalafatic.evolution.controller.orchestration.OrchestratorServiceImpl;
import eu.kalafatic.evolution.controller.orchestration.SessionContainer;
import eu.kalafatic.evolution.controller.orchestration.SessionContext;
import eu.kalafatic.evolution.controller.orchestration.SessionManager;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.model.orchestration.ChatSession;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

/**
 * Control plane for Develop session state, isolated workspaces, and EVO runtime synchronization.
 */
public class DevelopSessionManager {

    private static final DevelopSessionManager INSTANCE = new DevelopSessionManager();
    private final Map<String, DevelopSession> sessions = new ConcurrentHashMap<>();
    private final File baseWorkspacesDir;

    private DevelopSessionManager() {
        String userHome = System.getProperty("user.home");
        this.baseWorkspacesDir = new File(userHome, ".evo" + File.separator + "develop-workspaces");
        if (!baseWorkspacesDir.exists()) {
            baseWorkspacesDir.mkdirs();
        }
        recoverSessions();
    }

    public synchronized void recoverSessions() {
        if (!baseWorkspacesDir.exists() || !baseWorkspacesDir.isDirectory()) return;
        File[] sessionDirs = baseWorkspacesDir.listFiles(File::isDirectory);
        if (sessionDirs == null) return;

        for (File sessionDir : sessionDirs) {
            File stateFile = new File(sessionDir, "state.json");
            if (stateFile.exists()) {
                try {
                    String jsonText = java.nio.file.Files.readString(stateFile.toPath());
                    org.json.JSONObject obj = new org.json.JSONObject(jsonText);
                    String sessionId = obj.optString("sessionId", sessionDir.getName());

                    org.json.JSONObject taskObj = obj.optJSONObject("task");
                    if (taskObj != null) {
                        String title = taskObj.optString("title", "Autonomous Task");
                        String desc = taskObj.optString("description", "");
                        String repo = taskObj.optString("repository", "");
                        String branch = taskObj.optString("branch", "main");

                        DevelopPermissions perms = new DevelopPermissions();
                        org.json.JSONObject permObj = taskObj.optJSONObject("permissions");
                        if (permObj != null) {
                            perms.setRead(permObj.optBoolean("read", true));
                            perms.setWrite(permObj.optBoolean("write", true));
                            perms.setExecute(permObj.optBoolean("execute", true));
                            perms.setPush(permObj.optBoolean("push", false));
                        }

                        DevelopTask task = new DevelopTask(title, desc, repo, branch, perms);
                        // preserve original ID if present
                        String originalTaskId = taskObj.optString("id", null);
                        if (originalTaskId != null) {
                            try {
                                java.lang.reflect.Field idField = DevelopTask.class.getDeclaredField("id");
                                idField.setAccessible(true);
                                idField.set(task, originalTaskId);
                            } catch (Exception ignored) {}
                        }

                        String statusStr = taskObj.optString("status", "CREATED");
                        try {
                            task.setStatus(DevelopTaskStatus.valueOf(statusStr));
                        } catch (Exception ignored) {}

                        DevelopSession session = new DevelopSession(sessionId, task, sessionDir);
                        org.json.JSONArray logsArr = obj.optJSONArray("logs");
                        if (logsArr != null) {
                            List<String> logsList = new ArrayList<>();
                            for (int i = 0; i < logsArr.length(); i++) {
                                logsList.add(logsArr.getString(i));
                            }
                            session.loadLogs(logsList);
                        }

                        sessions.put(sessionId, session);
                        DevelopTaskManager.getInstance().registerTask(task);
                        DevelopTaskManager.getInstance().registerSessionForTask(task.getId(), session);
                    }
                } catch (Exception ignored) {
                }
            }
        }
    }

    public void persistSessionState(DevelopSession session) {
        if (session == null || session.getWorkspaceDir() == null) return;
        try {
            File stateFile = new File(session.getWorkspaceDir(), "state.json");
            org.json.JSONObject obj = new org.json.JSONObject();
            obj.put("sessionId", session.getSessionId());

            DevelopTask task = session.getTask();
            if (task != null) {
                org.json.JSONObject taskObj = new org.json.JSONObject();
                taskObj.put("id", task.getId());
                taskObj.put("title", task.getTitle());
                taskObj.put("description", task.getDescription());
                taskObj.put("repository", task.getRepository());
                taskObj.put("branch", task.getBranch());
                taskObj.put("status", task.getStatus() != null ? task.getStatus().name() : "CREATED");
                taskObj.put("createdAt", task.getCreatedAt());

                DevelopPermissions perms = task.getPermissions();
                if (perms != null) {
                    org.json.JSONObject permObj = new org.json.JSONObject();
                    permObj.put("read", perms.canRead());
                    permObj.put("write", perms.canWrite());
                    permObj.put("execute", perms.canExecute());
                    permObj.put("push", perms.canPush());
                    taskObj.put("permissions", permObj);
                }
                obj.put("task", taskObj);
            }

            org.json.JSONArray logsArr = new org.json.JSONArray(session.getLogs());
            obj.put("logs", logsArr);

            java.nio.file.Files.writeString(stateFile.toPath(), obj.toString(2));
        } catch (Exception ignored) {
        }
    }

    public static DevelopSessionManager getInstance() {
        return INSTANCE;
    }

    public File getBaseWorkspacesDir() {
        return baseWorkspacesDir;
    }

    public DevelopSession createSession(DevelopTask task) throws Exception {
        return createSession(task, null);
    }

    public DevelopSession createSessionFromArchive(DevelopTask task, byte[] archiveBytes, String requestedModel) throws Exception {
        DevelopSession session = createSession(task, requestedModel);
        if (archiveBytes != null && archiveBytes.length > 0) {
            SafeArchiveExtractor.extractArchive(archiveBytes, session.getRepositoryDir());
            session.addLog("Extracted project archive into workspace repository directory.");
        }
        return session;
    }

    public DevelopSession createSession(DevelopTask task, String requestedModel) throws Exception {
        String sessionId = "develop-" + UUID.randomUUID().toString().substring(0, 8);
        File sessionWorkspaceDir = new File(baseWorkspacesDir, sessionId);

        if (!sessionWorkspaceDir.exists()) {
            sessionWorkspaceDir.mkdirs();
        }

        DevelopSession session = new DevelopSession(sessionId, task, sessionWorkspaceDir);
        sessions.put(sessionId, session);
        persistSessionState(session);

        // Synchronize with EVO core SessionManager and TaskContext
        SessionContainer container = SessionManager.getInstance().getOrCreateSession(sessionId);
        Orchestrator orch = OrchestratorServiceImpl.getInstance().getOrchestrator();

        if (container instanceof SessionContext) {
            SessionContext sc = (SessionContext) container;
            TaskContext tc = sc.getTaskContext();
            if (tc == null) {
                tc = new TaskContext(orch, session.getRepositoryDir());
                tc.setSessionId(sessionId);
                sc.setTaskContext(tc);
            } else {
                tc.setProjectRoot(session.getRepositoryDir());
            }

            if (requestedModel != null && !requestedModel.trim().isEmpty()) {
                if (tc.getOrchestrator() != null) {
                    tc.getOrchestrator().setLocalModel(requestedModel.trim());
                    if (tc.getOrchestrator().getOllama() != null) {
                        tc.getOrchestrator().getOllama().setModel(requestedModel.trim());
                    }
                }
            }
        }

        // Configure ChatSession in Orchestrator EMF model marked as REMOTE_CLIENT
        if (orch != null) {
            if (orch.getAiChat() == null) {
                orch.setAiChat(OrchestrationFactory.eINSTANCE.createAiChat());
            }

            ChatSession chatSession = orch.getAiChat().getSessions().stream()
                    .filter(s -> sessionId.equals(s.getId()))
                    .findFirst()
                    .orElse(null);

            if (chatSession == null) {
                chatSession = OrchestrationFactory.eINSTANCE.createChatSession();
                chatSession.setId(sessionId);
                chatSession.setTargetType("REMOTE_CLIENT");
                chatSession.setTargetPath(session.getRepositoryDir().getAbsolutePath());
                if (requestedModel != null && !requestedModel.trim().isEmpty()) {
                    chatSession.setLocalModel(requestedModel.trim());
                }
                chatSession.setIterativeMode(true);
                chatSession.setDarwinMode(true);
                chatSession.setAutoApprove(true);
                orch.getAiChat().getSessions().add(chatSession);
            } else {
                chatSession.setTargetType("REMOTE_CLIENT");
                chatSession.setTargetPath(session.getRepositoryDir().getAbsolutePath());
                if (requestedModel != null && !requestedModel.trim().isEmpty()) {
                    chatSession.setLocalModel(requestedModel.trim());
                }
            }
        }

        String turnId = sessionId + "__" + System.currentTimeMillis();
        ConversationOutputController.getInstance().submitMessage(
                sessionId,
                turnId,
                "Develop Agent",
                "Develop session initialized for " + (task.getRepository() != null ? task.getRepository() : "Local Workspace") + " (Branch: " + task.getBranch() + ")",
                "ai",
                MessagePriority.NORMAL,
                false
        );

        return session;
    }

    public DevelopSession getSession(String sessionId) {
        if (sessionId == null) return null;
        return sessions.get(sessionId);
    }

    public Collection<DevelopSession> getAllSessions() {
        return new ArrayList<>(sessions.values());
    }

    public void removeSession(String sessionId) {
        if (sessionId == null) return;
        DevelopSession session = sessions.remove(sessionId);
        if (session != null) {
            try {
                session.cancel();
                SessionManager.getInstance().shutdownSession(sessionId);
            } catch (Exception ignored) {
            }
            try {
                File stateFile = new File(session.getWorkspaceDir(), "state.json");
                if (stateFile.exists()) {
                    stateFile.delete();
                }
            } catch (Exception ignored) {
            }
        }
    }

    public void clearAllSessions() {
        List<String> ids = new ArrayList<>(sessions.keySet());
        for (String id : ids) {
            removeSession(id);
        }
    }
}
