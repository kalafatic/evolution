package eu.kalafatic.evolution.controller.cognitive;

import java.util.UUID;

/**
 * Thin utility issuing canonical cognitive session ids (UUID v4) and
 * resolving legacy aliases. Not a session manager: the runtime registry
 * remains SessionManager and the durable registry is the cognitive_sessions
 * table managed by SessionContinuityService.
 */
public final class SessionIdService {

    private SessionIdService() {
    }

    /** Issues a new canonical session id. */
    public static String newSessionId() {
        return UUID.randomUUID().toString();
    }

    /** Issues a new canonical context id. */
    public static String newContextId() {
        return "ctx-" + UUID.randomUUID();
    }

    /** Issues a new canonical snapshot id. */
    public static String newSnapshotId() {
        return "snap-" + UUID.randomUUID();
    }

    /** Issues a new canonical conversation id. */
    public static String newConversationId() {
        return "conv-" + UUID.randomUUID();
    }

    /** True when the given id looks like a canonical UUID issued by this service. */
    public static boolean isCanonical(String sessionId) {
        if (sessionId == null) {
            return false;
        }
        try {
            UUID.fromString(sessionId);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
