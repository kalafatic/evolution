package eu.kalafatic.evolution.controller.orchestration;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Owns the lifecycle of SessionContainer instances.
 */
public class SessionManager {
    private static final SessionManager INSTANCE = new SessionManager();
    private final Map<String, SessionContainer> sessions = new ConcurrentHashMap<>();

    private SessionManager() {}

    public static SessionManager getInstance() {
        return INSTANCE;
    }

    public SessionContainer getOrCreateSession(String sessionId) {
        return sessions.computeIfAbsent(sessionId, SessionContext::new);
    }

    public SessionContainer getSession(String sessionId) {
        return sessions.get(sessionId);
    }

    public ServerOperation findOperation(String operationId) {
        if (operationId == null) return null;
        for (SessionContainer session : sessions.values()) {
            ServerOperation op = session.getOperation(operationId);
            if (op != null) return op;
        }
        return null;
    }

    public java.util.Collection<ServerOperation> getAllOperations() {
        java.util.List<ServerOperation> allOps = new java.util.ArrayList<>();
        for (SessionContainer session : sessions.values()) {
            allOps.addAll(session.getOperations());
        }
        return allOps;
    }

    public boolean cancelOperation(String operationId, String reason) {
        if (operationId == null) return false;
        for (SessionContainer session : sessions.values()) {
            if (session.cancelOperation(operationId, reason)) {
                return true;
            }
        }
        return false;
    }

    public void shutdownSession(String sessionId) {
        SessionContainer session = sessions.remove(sessionId);
        if (session != null) {
            session.shutdown();
        }
        eu.kalafatic.evolution.controller.orchestration.selfdev.DarwinEngineRegistry.unregister(sessionId);
    }

    public void shutdownAll() {
        sessions.keySet().forEach(sid -> {
            eu.kalafatic.evolution.controller.orchestration.selfdev.DarwinEngineRegistry.unregister(sid);
        });
        sessions.values().forEach(SessionContainer::shutdown);
        sessions.clear();
    }
}
