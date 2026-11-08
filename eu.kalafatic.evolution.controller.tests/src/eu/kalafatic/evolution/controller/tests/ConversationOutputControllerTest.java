package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import eu.kalafatic.evolution.controller.orchestration.ConversationOutputController;
import eu.kalafatic.evolution.controller.orchestration.MessagePriority;
import eu.kalafatic.evolution.model.orchestration.ChatMessage;

public class ConversationOutputControllerTest {

    private ConversationOutputController controller;
    private String sessionId;

    @Before
    public void setUp() {
        controller = new ConversationOutputController();
        sessionId = "test-session-output-" + System.currentTimeMillis();
    }

    @After
    public void tearDown() {
        if (controller != null) {
            controller.clearSession(sessionId);
            controller.dispose();
        }
    }

    @Test
    public void testMessageSequenceOrderingAndDelivery() throws Exception {
        List<ChatMessage> received = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch latch = new CountDownLatch(3);

        controller.subscribe(msg -> {
            received.add(msg);
            latch.countDown();
        });

        String turnId = "turn-1";
        controller.submitMessage(sessionId, turnId, "User", "Hello", "user", MessagePriority.NORMAL, false);
        controller.submitMessage(sessionId, turnId, "Evo", "Thinking...", "thinking", MessagePriority.PROGRESS, false);
        controller.submitMessage(sessionId, turnId, "Evo", "Done", "response", MessagePriority.NORMAL, false);

        assertTrue("Expected 3 messages to be delivered within 1.5s", latch.await(1500, TimeUnit.MILLISECONDS));
        assertEquals(3, received.size());

        assertTrue("Sequence numbers must be monotonically increasing",
                received.get(0).getSequenceNumber() < received.get(1).getSequenceNumber());
        assertTrue("Sequence numbers must be monotonically increasing",
                received.get(1).getSequenceNumber() < received.get(2).getSequenceNumber());
    }

    @Test
    public void testGracePeriodAllowsTrailingMessages() throws Exception {
        List<ChatMessage> received = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch latch = new CountDownLatch(2);

        controller.subscribe(msg -> {
            received.add(msg);
            latch.countDown();
        });

        String turnId = "turn-terminal";
        // Final response emitted first
        controller.submitMessage(sessionId, turnId, "Evo", "Final answer", "final-response", MessagePriority.FINAL, true);

        // Immediate trailing summary emitted right after
        controller.submitMessage(sessionId, turnId, "Result Summary", "Success summary", "result-summary", MessagePriority.FINAL, true);

        assertTrue("Both terminal and summary messages should be delivered within grace window", latch.await(2000, TimeUnit.MILLISECONDS));
        assertEquals(2, received.size());
    }

    @Test
    public void testNormalPriorityMessagesPreservedAfterFinalization() throws Exception {
        CountDownLatch firstFlush = new CountDownLatch(1);
        controller.subscribe(msg -> firstFlush.countDown());

        String turnId = "turn-finalized";
        controller.submitMessage(sessionId, turnId, "Evo", "Finished", "final-response", MessagePriority.FINAL, true);

        assertTrue(firstFlush.await(1500, TimeUnit.MILLISECONDS));

        // Wait for finalization grace period to pass (1500ms + buffer)
        Thread.sleep(1700);

        List<ChatMessage> lateReceived = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch lateLatch = new CountDownLatch(1);
        controller.subscribe(msg -> {
            lateReceived.add(msg);
            lateLatch.countDown();
        });

        // Submit late NORMAL message (e.g. user approval or result metrics)
        controller.submitMessage(sessionId, turnId, "System", "Important Audit Event", "system", MessagePriority.NORMAL, false);

        assertTrue("Normal priority message should not be dropped even on finalized turn", lateLatch.await(1500, TimeUnit.MILLISECONDS));
        assertEquals(1, lateReceived.size());
        assertEquals("Important Audit Event", lateReceived.get(0).getText());
    }

    @Test
    public void testSessionHistoryAndCleanup() throws Exception {
        String turnId = "turn-hist";
        controller.submitMessage(sessionId, turnId, "User", "Ping", "user", MessagePriority.NORMAL, false);
        controller.submitMessage(sessionId, turnId, "Evo", "Pong", "response", MessagePriority.NORMAL, false);

        // Wait for buffer flush
        Thread.sleep(500);

        List<ChatMessage> history = controller.getSessionHistory(sessionId);
        assertNotNull(history);
        assertEquals(2, history.size());

        controller.clearSession(sessionId);
        List<ChatMessage> emptyHistory = controller.getSessionHistory(sessionId);
        assertTrue(emptyHistory.isEmpty());
    }

    @Test
    public void testDuplicateUserMessageInSameTurnDropped() throws Exception {
        String turnId = "turn-dup";
        controller.submitMessage(sessionId, turnId, "User [DARWIN]", "hello", "user", MessagePriority.NORMAL, false);
        controller.submitMessage(sessionId, turnId, "User", "hello", "user", MessagePriority.NORMAL, false);

        Thread.sleep(500);

        List<ChatMessage> history = controller.getSessionHistory(sessionId);
        assertEquals("Duplicate user message for the same turn should be dropped", 1, history.size());
        assertEquals("User [DARWIN]", history.get(0).getSender());
    }

    @Test
    public void testIdenticalUserMessagesInDifferentTurnsPreserved() throws Exception {
        String turn1 = "turn-1";
        String turn2 = "turn-2";

        controller.submitMessage(sessionId, turn1, "User", "hi", "user", MessagePriority.NORMAL, false);
        controller.submitMessage(sessionId, turn2, "User", "hi", "user", MessagePriority.NORMAL, false);

        Thread.sleep(500);

        List<ChatMessage> history = controller.getSessionHistory(sessionId);
        assertEquals("Identical user messages in separate turns should both be preserved", 2, history.size());
    }
}
