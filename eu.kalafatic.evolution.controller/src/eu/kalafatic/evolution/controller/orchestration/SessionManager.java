package eu.kalafatic.evolution.controller.orchestration;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

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

    public void shutdownSession(String sessionId) {
        SessionContainer session = sessions.remove(sessionId);
        if (session != null) {
            session.shutdown();
        }
    }

    public void shutdownAll() {
        sessions.values().forEach(SessionContainer::shutdown);
        sessions.clear();
    }
}
