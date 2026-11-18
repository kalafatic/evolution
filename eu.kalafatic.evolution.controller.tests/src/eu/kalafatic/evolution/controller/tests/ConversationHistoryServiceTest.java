package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import eu.kalafatic.evolution.controller.cognitive.ConversationHistoryService;
import eu.kalafatic.evolution.controller.cognitive.model.ConversationRecord;

/**
 * Durable conversation history: structured records (messages, tool calls,
 * tool results, decisions), search, replay across restart, compaction.
 */
public class ConversationHistoryServiceTest {

    private ConversationHistoryService service;

    @Before
    public void setUp() {
        service = ConversationHistoryService.getInstance();
    }

    @After
    public void tearDown() {
    }

    @Test
    public void appendMessageAndToolRecords() {
        String conversationId = "conv-test-" + System.nanoTime();
        ConversationRecord user = service.append(conversationId, "USER", null, null, "Please fix the build", null);
        assertNotNull(user);
        assertEquals(1, user.getSequence());

        ConversationRecord toolCall = service.appendToolCall(conversationId, "MavenTool", "mvn compile");
        assertEquals(2, toolCall.getSequence());
        assertEquals(ConversationRecord.ROLE_TOOL_CALL, toolCall.getRole());
        assertEquals("MavenTool", toolCall.getToolName());

        ConversationRecord toolResult = service.appendToolResult(conversationId, "MavenTool", "BUILD SUCCESS");
        assertEquals(3, toolResult.getSequence());

        ConversationRecord decision = service.appendDecision(conversationId, "ADR-42", "Use SQLite for cognitive data");
        assertEquals(ConversationRecord.ROLE_DECISION, decision.getRole());
    }

    @Test
    public void historySurvivesServiceRestart() {
        String conversationId = "conv-persist-" + System.nanoTime();
        service.append(conversationId, "USER", null, null, "first message", null);
        service.append(conversationId, "ASSISTANT", null, null, "second message", null);

        // Simulated restart: reset the singleton and read again from disk/DB.
        ConversationHistoryService.resetInstance();
        ConversationHistoryService reloaded = ConversationHistoryService.getInstance();
        List<ConversationRecord> records = reloaded.list(conversationId, 0, 100);
        assertEquals(2, records.size());
        assertEquals("first message", records.get(0).getContent());
        assertEquals("second message", records.get(1).getContent());
        assertEquals(1, records.get(0).getSequence());
        assertEquals(2, records.get(1).getSequence());
    }

    @Test
    public void searchFindsContent() {
        String conversationId = "conv-search-" + System.nanoTime();
        service.append(conversationId, "USER", null, null, "the unique searchable token", null);
        List<ConversationRecord> results = service.search("unique searchable", 10);
        assertTrue(results.stream().anyMatch(r -> r.getContent().contains("unique searchable")));
    }

    @Test
    public void compactionProducesBoundedSummary() {
        String conversationId = "conv-compact-" + System.nanoTime();
        StringBuilder large = new StringBuilder();
        for (int i = 0; i < 60; i++) {
            large.append("word").append(i).append(' ');
        }
        service.append(conversationId, "USER", null, null, large.toString(), null);
        service.append(conversationId, "ASSISTANT", null, null, "tail message", null);
        // The large record is ~104 tokens; a 150-token budget keeps it plus
        // the tail message while staying bounded.
        String summary = service.compact(conversationId, 150);
        assertNotNull(summary);
        assertTrue(summary.contains("tail message"));
        assertTrue("Summary must stay within the token budget",
                summary.length() / 4 <= 160);
    }
}
