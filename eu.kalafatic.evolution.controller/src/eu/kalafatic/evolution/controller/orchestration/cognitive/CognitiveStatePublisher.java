package eu.kalafatic.evolution.controller.orchestration.cognitive;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.json.JSONObject;
import eu.kalafatic.evolution.controller.orchestration.ConversationOutputController;
import eu.kalafatic.evolution.controller.orchestration.MessagePriority;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.workflow.RuntimeEvent;
import eu.kalafatic.evolution.controller.workflow.RuntimeEventType;

/**
 * Responsible for broadcasting cognitive state changes to the UI and other subscribers.
 * Implements change filtering to avoid event spam.
 */
public class CognitiveStatePublisher {
    private static final Map<String, SessionCognitiveSnapshot> lastSnapshots = new java.util.LinkedHashMap<>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, SessionCognitiveSnapshot> eldest) {
            return size() > 100;
        }
    };
    private static final double CONFIDENCE_THRESHOLD = 0.05;

    public synchronized void publish(TaskContext context, SessionCognitiveState state) {
        String sessionId = null;
        if (context != null) {
            sessionId = context.getSessionId();
        } else if (state != null) {
            sessionId = state.getSessionId();
        }

        if (sessionId == null) return;

        SessionCognitiveSnapshot current = new SessionCognitiveSnapshot(state);
        SessionCognitiveSnapshot last = lastSnapshots.get(sessionId);

        if (shouldPublish(current, last)) {
            lastSnapshots.put(sessionId, current);
            broadcast(context, current);
        }
    }

    private boolean shouldPublish(SessionCognitiveSnapshot current, SessionCognitiveSnapshot last) {
        if (last == null) return true;

        // Emit on any discrete state change
        if (current.getCapability() != last.getCapability()) return true;
        if (current.getIntent() != last.getIntent()) return true;
        if (current.getDirection() != last.getDirection()) return true;
        if (current.getDepth() != last.getDepth()) return true;

        // Emit on significant confidence or metric shift
        if (Math.abs(current.getConfidence() - last.getConfidence()) > CONFIDENCE_THRESHOLD) return true;
        if (Math.abs(current.getVelocity() - last.getVelocity()) > 0.5) return true;

        return false;
    }

    private void broadcast(TaskContext context, SessionCognitiveSnapshot snapshot) {
        String sessionId = snapshot.getSessionId();
        if (sessionId == null && context != null) sessionId = context.getSessionId();
        if (sessionId == null) return;

        JSONObject payload = snapshot.toJSON();

        // 1. Internal Event Bus
        RuntimeEvent event = new RuntimeEvent(
                RuntimeEventType.COGNITIVE_STATE_CHANGED,
                sessionId,
                "CognitiveStateEngine",
                payload.toString()
        );

        eu.kalafatic.evolution.controller.workflow.RuntimeEventBus bus = null;
        if (context != null && context.getKernelContext() != null) {
            bus = context.getKernelContext().getEventBus();
        } else {
            eu.kalafatic.evolution.controller.orchestration.SessionContainer session =
                eu.kalafatic.evolution.controller.orchestration.SessionManager.getInstance().getSession(sessionId);
            if (session != null) bus = session.getEventBus();
        }

        if (bus != null) {
            bus.publish(event);
        }

        // 2. Chat UI Bridge
        // Use a consistent turnId to allow UI to update the same sidebar block if needed,
        // although renderer.js currently handles this as a specialized non-rendered message.
        ConversationOutputController.getInstance().submitMessage(
                sessionId,
                sessionId + "_cognitive_state",
                "Cognitive Engine",
                payload.toString(),
                "cognitive-state-changed",
                MessagePriority.PROGRESS,
                false
        );
    }
}
