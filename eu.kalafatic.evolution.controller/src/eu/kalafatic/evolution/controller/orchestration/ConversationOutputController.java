package eu.kalafatic.evolution.controller.orchestration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import eu.kalafatic.evolution.model.orchestration.ChatMessage;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;

/**
 * Lightweight Conversation Output Controller (Presentation Stabilization Layer).
 * Handles filtering, reordering, buffering, and terminal bubble protection.
 * Headless-friendly and supports multiple subscribers.
 */
public class ConversationOutputController {
    private static final long STABILIZATION_BUFFER_MS = 300;

    private static ConversationOutputController instance;

    public static synchronized ConversationOutputController getInstance() {
        if (instance == null) {
            instance = new ConversationOutputController();
        }
        return instance;
    }

    private final Map<String, SessionBuffer> sessionBuffers = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final AtomicLong globalSequence = new AtomicLong(0);
    private final List<Consumer<ChatMessage>> subscribers = new CopyOnWriteArrayList<>();

    public ConversationOutputController() {
    }

    public void subscribe(Consumer<ChatMessage> subscriber) {
        subscribers.add(subscriber);
    }

    public void unsubscribe(Consumer<ChatMessage> subscriber) {
        subscribers.remove(subscriber);
    }

    /**
     * Submit a message to the controller for stabilization and eventual display.
     */
    public void submitMessage(String sessionId, String turnId, String sender, String text,
                              String agentType, MessagePriority priority, boolean isTerminal) {
        submitMessage(sessionId, turnId, sender, text, agentType, priority, isTerminal, null, null);
    }

    /**
     * Submit a message to the controller with explicit reasoning and responseKind attributes.
     */
    public void submitMessage(String sessionId, String turnId, String sender, String text,
                              String agentType, MessagePriority priority, boolean isTerminal,
                              String reasoning, String responseKind) {

        SessionBuffer buffer = sessionBuffers.computeIfAbsent(sessionId, k -> new SessionBuffer());

        // Sequence assignment
        long seq = globalSequence.incrementAndGet();

        // Create model object
        ChatMessage msg = OrchestrationFactory.eINSTANCE.createChatMessage();
        msg.setSender(sender);
        msg.setText(text);
        msg.setAgentType(agentType);
        msg.setTimestamp(java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")));
        msg.setPriority(priority.getLevel());
        msg.setSequenceNumber(seq);
        msg.setTurnId(turnId);
        msg.setIsTerminal(isTerminal || priority == MessagePriority.FINAL);
        msg.setReasoning(reasoning);
        msg.setResponseKind(responseKind);

        synchronized (buffer) {
            // Drop late noise if turn is already finalized and message is low priority (DEBUG / raw PROGRESS)
            if (buffer.isTurnFinalized(turnId) && msg.getPriority() < MessagePriority.NORMAL.getLevel()) {
                System.out.println(String.format(
                    "[CONTROLLER] Dropping late message [sessionId=%s, turnId=%s, seq=%d, sender=%s, priority=%s, reason=TURN_FINALIZED_LOW_PRIORITY]: %s",
                    sessionId, turnId, seq, sender, priority, text
                ));
                return;
            }

            boolean isUser = (agentType != null && agentType.toLowerCase().contains("user"))
                    || (sender != null && (sender.equalsIgnoreCase("you") || sender.toLowerCase().startsWith("user")));
            if (isUser && buffer.hasTurnUserMessage(turnId)) {
                System.out.println(String.format(
                    "[CONTROLLER] Dropping duplicate user message for turn [sessionId=%s, turnId=%s, sender=%s]: %s",
                    sessionId, turnId, sender, text
                ));
                return;
            }

            buffer.add(msg);

            // Schedule flush after small buffer window
            scheduler.schedule(() -> flushBuffer(sessionId, turnId), STABILIZATION_BUFFER_MS, TimeUnit.MILLISECONDS);
        }
    }

    private void flushBuffer(String sessionId, String turnId) {
        SessionBuffer buffer = sessionBuffers.get(sessionId);
        if (buffer == null) return;

        List<ChatMessage> toEmit = new ArrayList<>();
        boolean hasTerminal = false;
        synchronized (buffer) {
            toEmit.addAll(buffer.consume(turnId));

            for (ChatMessage msg : toEmit) {
                if (msg.isIsTerminal()) {
                    hasTerminal = true;
                }
            }
        }

        if (!toEmit.isEmpty()) {
            // Ensure stable ordering by sequence number
            Collections.sort(toEmit, Comparator.comparingLong(ChatMessage::getSequenceNumber));

            for (ChatMessage msg : toEmit) {
                for (Consumer<ChatMessage> subscriber : subscribers) {
                    try {
                        subscriber.accept(msg);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        if (hasTerminal) {
            // Allow a short grace period (1500ms) for trailing async diagnostics before finalizing the turn
            scheduler.schedule(() -> {
                SessionBuffer b = sessionBuffers.get(sessionId);
                if (b != null) {
                    b.markFinalized(turnId);
                }
            }, 1500, TimeUnit.MILLISECONDS);
        }
    }

    public void clearSession(String sessionId) {
        if (sessionId != null) {
            sessionBuffers.remove(sessionId);
        }
    }

    public List<ChatMessage> getSessionHistory(String sessionId) {
        SessionBuffer buffer = sessionBuffers.get(sessionId);
        if (buffer == null) return Collections.emptyList();

        List<ChatMessage> history = new ArrayList<>(buffer.getAllMessages());
        Collections.sort(history, Comparator.comparingLong(ChatMessage::getSequenceNumber));
        return history;
    }

    public void dispose() {
        scheduler.shutdown();
    }

    private static class SessionBuffer {
        private final Map<String, List<ChatMessage>> pendingByTurn = new ConcurrentHashMap<>();
        private final Map<String, List<ChatMessage>> historyByTurn = new ConcurrentHashMap<>();
        private final Map<String, Boolean> finalizedTurns = new ConcurrentHashMap<>();

        public void add(ChatMessage msg) {
            pendingByTurn.computeIfAbsent(msg.getTurnId(), k -> Collections.synchronizedList(new ArrayList<>())).add(msg);
            historyByTurn.computeIfAbsent(msg.getTurnId(), k -> Collections.synchronizedList(new ArrayList<>())).add(msg);
        }

        public boolean hasTurnUserMessage(String turnId) {
            if (turnId == null) return false;
            List<ChatMessage> turnHistory = historyByTurn.get(turnId);
            if (turnHistory != null) {
                synchronized (turnHistory) {
                    for (ChatMessage m : turnHistory) {
                        String at = m.getAgentType();
                        String snd = m.getSender();
                        if ((at != null && at.toLowerCase().contains("user"))
                                || (snd != null && (snd.equalsIgnoreCase("you") || snd.toLowerCase().startsWith("user")))) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }

        public List<ChatMessage> consume(String turnId) {
            List<ChatMessage> pending = pendingByTurn.get(turnId);
            if (pending == null || pending.isEmpty()) return Collections.emptyList();

            List<ChatMessage> result = new ArrayList<>(pending);
            pending.clear();
            return result;
        }

        public List<ChatMessage> getAllMessages() {
            List<ChatMessage> all = new ArrayList<>();
            for (List<ChatMessage> messages : historyByTurn.values()) {
                synchronized (messages) {
                    all.addAll(messages);
                }
            }
            return all;
        }

        public void markFinalized(String turnId) {
            finalizedTurns.put(turnId, true);
        }

        public boolean isTurnFinalized(String turnId) {
            return finalizedTurns.getOrDefault(turnId, false);
        }
    }
}
