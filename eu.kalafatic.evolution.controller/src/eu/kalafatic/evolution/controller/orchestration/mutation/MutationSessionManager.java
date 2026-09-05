package eu.kalafatic.evolution.controller.orchestration.mutation;

import java.io.File;
import java.io.IOException;
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
import eu.kalafatic.evolution.controller.orchestration.selfdev.GitManager;
import eu.kalafatic.evolution.controller.tools.ShellTool;
import eu.kalafatic.evolution.model.orchestration.ChatSession;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

/**
 * Single control plane for Mutation interactive repository evolution sessions.
 */
public class MutationSessionManager {

    private static final MutationSessionManager INSTANCE = new MutationSessionManager();
    private final Map<String, MutationSession> sessions = new ConcurrentHashMap<>();
    private final File baseWorkspacesDir;

    private MutationSessionManager() {
        String userHome = System.getProperty("user.home");
        this.baseWorkspacesDir = new File(userHome, ".evo" + File.separator + "mutation-workspaces");
        if (!baseWorkspacesDir.exists()) {
            baseWorkspacesDir.mkdirs();
        }
    }

    public static MutationSessionManager getInstance() {
        return INSTANCE;
    }

    public File getBaseWorkspacesDir() {
        return baseWorkspacesDir;
    }

    public MutationSession createSession(String repoUrl, String branch, String requestedSessionId) throws Exception {
        return createSession(repoUrl, branch, requestedSessionId, null);
    }

    public MutationSession createSession(String repoUrl, String branch, String requestedSessionId, String requestedModel) throws Exception {
        String sessionId = (requestedSessionId != null && !requestedSessionId.trim().isEmpty())
                ? requestedSessionId.trim()
                : "mutation-" + UUID.randomUUID().toString().substring(0, 8);

        String activeBranch = (branch != null && !branch.trim().isEmpty()) ? branch.trim() : "main";
        File workspaceDir = new File(baseWorkspacesDir, sessionId);

        ShellTool shell = new ShellTool();

        if (!workspaceDir.exists()) {
            workspaceDir.mkdirs();
            if (repoUrl != null && !repoUrl.trim().isEmpty()) {
                String cleanUrl = repoUrl.trim();
                try {
                    // Attempt clone
                    shell.execute("git clone --branch " + activeBranch + " " + cleanUrl + " .", workspaceDir, null);
                } catch (Exception cloneError) {
                    // Try clone without branch if branch specific clone fails
                    try {
                        shell.execute("git clone " + cleanUrl + " .", workspaceDir, null);
                    } catch (Exception ex) {
                        // If cloning into existing directory or non-empty fails, init repository
                        GitManager gm = new GitManager(workspaceDir);
                        gm.ensureInitialCommit();
                    }
                }
            } else {
                GitManager gm = new GitManager(workspaceDir);
                gm.ensureInitialCommit();
            }
        }

        // Ensure Git repository state and branch
        GitManager gitManager = new GitManager(workspaceDir);
        gitManager.ensureInitialCommit();

        if (activeBranch != null && gitManager.isGitRepository()) {
            try {
                gitManager.createBranch(activeBranch);
            } catch (Exception ignored) {
            }
        }

        MutationSession mutationSession = new MutationSession(sessionId, repoUrl, activeBranch, workspaceDir);
        sessions.put(sessionId, mutationSession);

        // Register / sync with EVO core SessionManager and TaskContext
        SessionContainer container = SessionManager.getInstance().getOrCreateSession(sessionId);
        Orchestrator orch = OrchestratorServiceImpl.getInstance().getOrchestrator();

        if (container instanceof SessionContext) {
            SessionContext sc = (SessionContext) container;
            TaskContext tc = sc.getTaskContext();
            if (tc == null) {
                tc = new TaskContext(orch, workspaceDir);
                tc.setSessionId(sessionId);
                sc.setTaskContext(tc);
            } else {
                tc.setProjectRoot(workspaceDir);
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

        // Auto-create / configure ChatSession in Orchestrator model marked as REMOTE_CLIENT
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
                chatSession.setTargetPath(workspaceDir.getAbsolutePath());
                if (requestedModel != null && !requestedModel.trim().isEmpty()) {
                    chatSession.setLocalModel(requestedModel.trim());
                }
                chatSession.setIterativeMode(true);
                chatSession.setDarwinMode(true);
                chatSession.setAutoApprove(true);
                orch.getAiChat().getSessions().add(chatSession);
            } else {
                chatSession.setTargetType("REMOTE_CLIENT");
                chatSession.setTargetPath(workspaceDir.getAbsolutePath());
                if (requestedModel != null && !requestedModel.trim().isEmpty()) {
                    chatSession.setLocalModel(requestedModel.trim());
                }
            }
        }

        // Submit initial log message for logging and UI synchronization
        String turnId = sessionId + "__" + System.currentTimeMillis();
        ConversationOutputController.getInstance().submitMessage(
            sessionId,
            turnId,
            "Mutation Agent",
            "Remote client session initialized for " + (repoUrl != null && !repoUrl.isEmpty() ? repoUrl : workspaceDir.getName()) + " (Branch: " + activeBranch + ")",
            "ai",
            MessagePriority.NORMAL,
            false
        );

        return mutationSession;
    }

    public MutationSession getSession(String sessionId) {
        if (sessionId == null) return null;
        return sessions.get(sessionId);
    }

    public Collection<MutationSession> getAllSessions() {
        return new ArrayList<>(sessions.values());
    }

    public void removeSession(String sessionId) {
        if (sessionId == null) return;
        MutationSession session = sessions.remove(sessionId);
        if (session != null) {
            try {
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
