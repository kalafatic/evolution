package eu.kalafatic.evolution.view.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import org.eclipse.swt.widgets.Display;

import eu.kalafatic.evolution.model.orchestration.ChatMessage;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;

/**
 * Lightweight Conversation Output Controller (Presentation Stabilization Layer).
 * Handles filtering, reordering, buffering, and terminal bubble protection.
 */
public class ConversationOutputController {
    private static final long STABILIZATION_BUFFER_MS = 300;

    private final Map<String, SessionBuffer> sessionBuffers = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final AtomicLong globalSequence = new AtomicLong(0);
    private final Consumer<ChatMessage> outputConsumer;

    public ConversationOutputController(Consumer<ChatMessage> outputConsumer) {
        this.outputConsumer = outputConsumer;
    }

    /**
     * Submit a message to the controller for stabilization and eventual display.
     */
    public void submitMessage(String sessionId, String turnId, String sender, String text,
                              String agentType, MessagePriority priority, boolean isTerminal) {

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
        msg.setIsTerminal(isTerminal || priority == MessagePriority.FINAL || priority == MessagePriority.USER_ACTION_REQUIRED);

        synchronized (buffer) {
            // Drop late noise if turn is already finalized
            if (buffer.isTurnFinalized(turnId) && msg.getPriority() < MessagePriority.USER_ACTION_REQUIRED.getLevel()) {
                System.out.println("[CONTROLLER] Dropping late message for finalized turn " + turnId + ": " + text);
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
        synchronized (buffer) {
            toEmit.addAll(buffer.consume(turnId));

            // Mark turn finalized if any terminal message is emitted
            for (ChatMessage msg : toEmit) {
                if (msg.isIsTerminal()) {
                    buffer.markFinalized(turnId);
                }
            }
        }

        if (!toEmit.isEmpty()) {
            // Ensure stable ordering by sequence number
            Collections.sort(toEmit, Comparator.comparingLong(ChatMessage::getSequenceNumber));

            Display.getDefault().asyncExec(() -> {
                for (ChatMessage msg : toEmit) {
                    outputConsumer.accept(msg);
                }
            });
        }
    }

    public void dispose() {
        scheduler.shutdown();
    }

    private static class SessionBuffer {
        private final Map<String, List<ChatMessage>> pendingByTurn = new ConcurrentHashMap<>();
        private final Map<String, Boolean> finalizedTurns = new ConcurrentHashMap<>();

        public void add(ChatMessage msg) {
            pendingByTurn.computeIfAbsent(msg.getTurnId(), k -> Collections.synchronizedList(new ArrayList<>())).add(msg);
        }

        public List<ChatMessage> consume(String turnId) {
            List<ChatMessage> pending = pendingByTurn.get(turnId);
            if (pending == null || pending.isEmpty()) return Collections.emptyList();

            List<ChatMessage> result = new ArrayList<>(pending);
            pending.clear();
            return result;
        }

        public void markFinalized(String turnId) {
            finalizedTurns.put(turnId, true);
        }

        public boolean isTurnFinalized(String turnId) {
            return finalizedTurns.getOrDefault(turnId, false);
        }
    }
}
