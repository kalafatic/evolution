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

    public DevelopSession createSession(DevelopTask task, String requestedModel) throws Exception {
        String sessionId = "develop-" + UUID.randomUUID().toString().substring(0, 8);
        File sessionWorkspaceDir = new File(baseWorkspacesDir, sessionId);

        if (!sessionWorkspaceDir.exists()) {
            sessionWorkspaceDir.mkdirs();
        }

        DevelopSession session = new DevelopSession(sessionId, task, sessionWorkspaceDir);
        sessions.put(sessionId, session);

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
        }
    }

    public void clearAllSessions() {
        List<String> ids = new ArrayList<>(sessions.keySet());
        for (String id : ids) {
            removeSession(id);
        }
    }
}
