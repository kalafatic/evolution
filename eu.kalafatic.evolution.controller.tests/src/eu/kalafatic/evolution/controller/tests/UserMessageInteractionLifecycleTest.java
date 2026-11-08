package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import eu.kalafatic.evolution.controller.orchestration.ConversationOutputController;
import eu.kalafatic.evolution.controller.orchestration.MessagePriority;
import eu.kalafatic.evolution.controller.orchestration.OrchestratorServiceImpl;
import eu.kalafatic.evolution.controller.orchestration.SessionContainer;
import eu.kalafatic.evolution.controller.orchestration.SessionContext;
import eu.kalafatic.evolution.controller.orchestration.SessionManager;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.TaskRequest;
import eu.kalafatic.evolution.model.orchestration.ChatMessage;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;

/**
 * Verifies message publication ownership, turn identity preservation,
 * and safeguard against duplicate user message bubbles between UI and Orchestrator.
 */
public class UserMessageInteractionLifecycleTest {

    private ConversationOutputController controller;
    private String sessionIdA;
    private String sessionIdB;

    @Before
    public void setUp() {
        controller = ConversationOutputController.getInstance();
        sessionIdA = "test-interaction-a-" + System.currentTimeMillis();
        sessionIdB = "test-interaction-b-" + System.currentTimeMillis();
    }

    @After
    public void tearDown() {
        if (controller != null) {
            controller.clearSession(sessionIdA);
            controller.clearSession(sessionIdB);
        }
        try {
            SessionManager.getInstance().shutdownSession(sessionIdA);
        } catch (Exception ignored) {}
        try {
            SessionManager.getInstance().shutdownSession(sessionIdB);
        } catch (Exception ignored) {}
    }

    @Test
    public void testUiSubmissionPublishesExactlyOnceAndOrchestratorDoesNotRepublish() throws Exception {
        List<ChatMessage> received = Collections.synchronizedList(new ArrayList<>());
        Consumer<ChatMessage> subscriber = received::add;
        controller.subscribe(subscriber);

        try {
            String turnId = sessionIdA + "__" + System.currentTimeMillis();
            String prompt = "hello world from ui";

            // Pre-cancel context so background executor immediately short-circuits without workspace operations
            SessionContainer session = SessionManager.getInstance().getOrCreateSession(sessionIdA);
            TaskContext ctx = new TaskContext(OrchestrationFactory.eINSTANCE.createOrchestrator(), new File("."));
            ctx.setCancelled(true);
            if (session instanceof SessionContext) {
                ((SessionContext) session).setTaskContext(ctx);
            }

            // 1. UI publishes initial user message
            controller.submitMessage(sessionIdA, turnId, "User [DARWIN]", prompt, "user", MessagePriority.NORMAL, false);

            // 2. Submit to OrchestratorService with turnId and userMessageHandled = true
            TaskRequest request = new TaskRequest(prompt, new File("."));
            request.getContext().put("turnId", turnId);
            request.getContext().put("userMessageHandled", Boolean.TRUE);

            OrchestratorServiceImpl.getInstance().submit(sessionIdA, request);

            // Wait for stabilization flush
            Thread.sleep(500);

            // Check that only ONE user message exists in history
            List<ChatMessage> history = controller.getSessionHistory(sessionIdA);
            long userMessageCount = history.stream()
                    .filter(m -> m.getAgentType() != null && m.getAgentType().contains("user"))
                    .count();

            assertEquals("UI submission must result in exactly 1 user message published", 1, userMessageCount);
            ChatMessage userMsg = history.stream()
                    .filter(m -> m.getAgentType() != null && m.getAgentType().contains("user"))
                    .findFirst()
                    .orElse(null);
            assertNotNull(userMsg);
            assertEquals("User [DARWIN]", userMsg.getSender());
            assertEquals(prompt, userMsg.getText());
            assertEquals(turnId, userMsg.getTurnId());
        } finally {
            controller.unsubscribe(subscriber);
        }
    }

    @Test
    public void testRestHeadlessSubmissionPublishesUserMessageExactlyOnce() throws Exception {
        List<ChatMessage> received = Collections.synchronizedList(new ArrayList<>());
        Consumer<ChatMessage> subscriber = received::add;
        controller.subscribe(subscriber);

        try {
            String prompt = "hello world from rest";

            // Pre-cancel context so background executor immediately short-circuits without workspace operations
            SessionContainer session = SessionManager.getInstance().getOrCreateSession(sessionIdA);
            TaskContext ctx = new TaskContext(OrchestrationFactory.eINSTANCE.createOrchestrator(), new File("."));
            ctx.setCancelled(true);
            if (session instanceof SessionContext) {
                ((SessionContext) session).setTaskContext(ctx);
            }

            // REST submission: no turnId and userMessageHandled is false/absent
            TaskRequest request = new TaskRequest(prompt, new File("."));

            OrchestratorServiceImpl.getInstance().submit(sessionIdA, request);

            // Wait for stabilization flush
            Thread.sleep(500);

            List<ChatMessage> history = controller.getSessionHistory(sessionIdA);
            long userMessageCount = history.stream()
                    .filter(m -> m.getAgentType() != null && m.getAgentType().contains("user"))
                    .count();

            assertEquals("REST submission must publish initial user message exactly once", 1, userMessageCount);
            ChatMessage userMsg = history.stream()
                    .filter(m -> m.getAgentType() != null && m.getAgentType().contains("user"))
                    .findFirst()
                    .orElse(null);
            assertNotNull(userMsg);
            assertEquals("User", userMsg.getSender());
            assertEquals(prompt, userMsg.getText());
            assertNotNull(userMsg.getTurnId());
            assertTrue("Turn ID must start with sessionId", userMsg.getTurnId().startsWith(sessionIdA));
        } finally {
            controller.unsubscribe(subscriber);
        }
    }

    @Test
    public void testConsecutiveLegitimateMessagesWithIdenticalTextAcrossTurnsPreserved() throws Exception {
        List<ChatMessage> received = Collections.synchronizedList(new ArrayList<>());
        Consumer<ChatMessage> subscriber = received::add;
        controller.subscribe(subscriber);

        try {
            String prompt = "hi";
            String turn1 = sessionIdA + "__turn1";
            String turn2 = sessionIdA + "__turn2";

            controller.submitMessage(sessionIdA, turn1, "User", prompt, "user", MessagePriority.NORMAL, false);
            controller.submitMessage(sessionIdA, turn1, "Evo", "Hello!", "response", MessagePriority.NORMAL, false);

            controller.submitMessage(sessionIdA, turn2, "User", prompt, "user", MessagePriority.NORMAL, false);
            controller.submitMessage(sessionIdA, turn2, "Evo", "Hello again!", "response", MessagePriority.NORMAL, false);

            Thread.sleep(500);

            List<ChatMessage> history = controller.getSessionHistory(sessionIdA);
            long userMessageCount = history.stream()
                    .filter(m -> m.getAgentType() != null && m.getAgentType().contains("user"))
                    .count();

            assertEquals("Two legitimate user messages with identical text across distinct turns must both be preserved", 2, userMessageCount);
        } finally {
            controller.unsubscribe(subscriber);
        }
    }

    @Test
    public void testDuplicateUserMessageInSameTurnDroppedByControllerSafeguard() throws Exception {
        List<ChatMessage> received = Collections.synchronizedList(new ArrayList<>());
        Consumer<ChatMessage> subscriber = received::add;
        controller.subscribe(subscriber);

        try {
            String turnId = sessionIdA + "__single_turn";

            // Submit initial user message for turn
            controller.submitMessage(sessionIdA, turnId, "User [DARWIN]", "test prompt", "user", MessagePriority.NORMAL, false);

            // Attempt to submit duplicate user message for same turn
            controller.submitMessage(sessionIdA, turnId, "User", "test prompt", "user", MessagePriority.NORMAL, false);

            Thread.sleep(500);

            List<ChatMessage> history = controller.getSessionHistory(sessionIdA);
            long userMessageCount = history.stream()
                    .filter(m -> m.getAgentType() != null && m.getAgentType().contains("user"))
                    .count();

            assertEquals("Duplicate user message for the same turn must be dropped by safeguard", 1, userMessageCount);
            assertEquals("User [DARWIN]", history.get(0).getSender());
        } finally {
            controller.unsubscribe(subscriber);
        }
    }

    @Test
    public void testSessionIsolationPreservedForIdenticalTurnIds() throws Exception {
        String turnId = "common_turn";
        controller.submitMessage(sessionIdA, turnId, "User", "message A", "user", MessagePriority.NORMAL, false);
        controller.submitMessage(sessionIdB, turnId, "User", "message B", "user", MessagePriority.NORMAL, false);

        Thread.sleep(500);

        List<ChatMessage> historyA = controller.getSessionHistory(sessionIdA);
        List<ChatMessage> historyB = controller.getSessionHistory(sessionIdB);

        assertEquals(1, historyA.size());
        assertEquals(1, historyB.size());
        assertEquals("message A", historyA.get(0).getText());
        assertEquals("message B", historyB.get(0).getText());
    }
}
