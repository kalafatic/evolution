package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import eu.kalafatic.evolution.controller.orchestration.WebApplicationDescriptor;
import eu.kalafatic.evolution.view.editors.pages.browser.BrowserTabManager.ClosedTabState;
import eu.kalafatic.evolution.view.editors.pages.browser.BrowserUrlUtils;

/**
 * Validates tab deduplication invariants, URL comparison equivalence,
 * and closed tab state management in the EVO Browser.
 */
public class BrowserTabDeduplicationTest {

    @Test
    public void testClosedTabState() {
        ClosedTabState state = new ClosedTabState("http://localhost:28080/mcp.html", "MCP Studio", "mcp", true);
        assertEquals("http://localhost:28080/mcp.html", state.getUrl());
        assertEquals("MCP Studio", state.getTitle());
        assertEquals("mcp", state.getAppId());
        assertTrue(state.isPinned());
    }

    @Test
    public void testPort28280Deduplication() {
        WebApplicationDescriptor mcpDocs = new WebApplicationDescriptor("mcp_docs", "MCP Documentation", "/", 28280, "MCP");
        List<WebApplicationDescriptor> registered = Arrays.asList(mcpDocs);

        // All variations must resolve to the same web application
        WebApplicationDescriptor match1 = BrowserUrlUtils.findMatchingApp("http://localhost:28280/", registered);
        WebApplicationDescriptor match2 = BrowserUrlUtils.findMatchingApp("http://127.0.0.1:28280", registered);
        WebApplicationDescriptor match3 = BrowserUrlUtils.findMatchingApp("http://localhost:28280", registered);

        assertNotNull(match1);
        assertEquals("mcp_docs", match1.getId());
        assertEquals(match1, match2);
        assertEquals(match1, match3);

        assertTrue(BrowserUrlUtils.isSameUrl("http://localhost:28280/", "http://127.0.0.1:28280"));
    }

    @Test
    public void testPort28080AppsDistinction() {
        WebApplicationDescriptor chat = new WebApplicationDescriptor("chat", "AI Chat", "/experimental/chat?runtime=SWT", 28080, "PRIMARY");
        WebApplicationDescriptor mcp = new WebApplicationDescriptor("mcp", "MCP Studio", "/mcp.html?runtime=SWT", 28080, "PRIMARY");
        WebApplicationDescriptor develop = new WebApplicationDescriptor("develop", "Develop Agent", "/develop?runtime=SWT", 28080, "PRIMARY");
        List<WebApplicationDescriptor> registered = Arrays.asList(chat, mcp, develop);

        WebApplicationDescriptor mcpMatch = BrowserUrlUtils.findMatchingApp("http://127.0.0.1:28080/mcp.html", registered);
        WebApplicationDescriptor devMatch = BrowserUrlUtils.findMatchingApp("http://localhost:28080/develop", registered);
        WebApplicationDescriptor chatMatch = BrowserUrlUtils.findMatchingApp("http://127.0.0.1:28080/experimental/chat", registered);

        assertNotNull(mcpMatch);
        assertNotNull(devMatch);
        assertNotNull(chatMatch);

        assertEquals("mcp", mcpMatch.getId());
        assertEquals("develop", devMatch.getId());
        assertEquals("chat", chatMatch.getId());

        // Internal panel / anchor should still match the same app
        WebApplicationDescriptor panelMatch = BrowserUrlUtils.findMatchingApp("http://localhost:28080/mcp.html#tools", registered);
        assertNotNull(panelMatch);
        assertEquals("mcp", panelMatch.getId());
    }
}
