package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import eu.kalafatic.evolution.controller.orchestration.WebApplicationDescriptor;
import eu.kalafatic.evolution.view.editors.pages.browser.BrowserUrlUtils;

/**
 * Verifies robust URL normalization, loopback aliasing, runtime query stripping,
 * and registered web application matching in the EVO Browser.
 */
public class BrowserUrlUtilsTest {

    @Test
    public void testNormalizeUrlBasic() {
        assertEquals("http://localhost:28080/mcp.html", BrowserUrlUtils.normalizeUrl("http://localhost:28080/mcp.html"));
        assertEquals("http://localhost:28080/mcp.html", BrowserUrlUtils.normalizeUrl("http://127.0.0.1:28080/mcp.html"));
        assertEquals("http://localhost:28080/mcp.html", BrowserUrlUtils.normalizeUrl("http://0.0.0.0:28080/mcp.html"));
    }

    @Test
    public void testNormalizeUrlRuntimeSwtParam() {
        // Must strip ?runtime=SWT and &runtime=SWT
        assertEquals("http://localhost:28080/mcp.html", BrowserUrlUtils.normalizeUrl("http://localhost:28080/mcp.html?runtime=SWT"));
        assertEquals("http://localhost:28080/mcp.html", BrowserUrlUtils.normalizeUrl("http://127.0.0.1:28080/mcp.html?runtime=SWT"));
        assertEquals("http://localhost:28080/mcp.html?tab=1", BrowserUrlUtils.normalizeUrl("http://127.0.0.1:28080/mcp.html?tab=1&runtime=SWT"));
        assertEquals("http://localhost:28080/mcp.html?tab=1", BrowserUrlUtils.normalizeUrl("http://127.0.0.1:28080/mcp.html?runtime=SWT&tab=1"));
    }

    @Test
    public void testNormalizeTrailingSlashes() {
        // Root path "/" preserved
        assertEquals("http://localhost:28280/", BrowserUrlUtils.normalizeUrl("http://localhost:28280/"));
        assertEquals("http://localhost:28280/", BrowserUrlUtils.normalizeUrl("http://127.0.0.1:28280"));

        // Non-root trailing slashes stripped
        assertEquals("http://localhost:28080/develop", BrowserUrlUtils.normalizeUrl("http://localhost:28080/develop/"));
        assertEquals("http://localhost:28080/develop", BrowserUrlUtils.normalizeUrl("http://127.0.0.1:28080/develop"));
    }

    @Test
    public void testIsSameUrl() {
        assertTrue(BrowserUrlUtils.isSameUrl("http://localhost:28080/develop?runtime=SWT", "http://127.0.0.1:28080/develop"));
        assertTrue(BrowserUrlUtils.isSameUrl("http://127.0.0.1:28280/", "http://localhost:28280"));
        assertTrue(BrowserUrlUtils.isSameUrl("https://ollama.com", "https://ollama.com/"));
        assertFalse(BrowserUrlUtils.isSameUrl("http://localhost:28080/develop", "http://localhost:28080/chat"));
    }

    @Test
    public void testFindMatchingApp() {
        WebApplicationDescriptor chatApp = new WebApplicationDescriptor("chat", "AI Chat", "/experimental/chat?runtime=SWT", 28080, "PRIMARY");
        WebApplicationDescriptor mcpApp = new WebApplicationDescriptor("mcp", "MCP Studio", "/mcp.html?runtime=SWT", 28080, "PRIMARY");
        WebApplicationDescriptor mcpDocs = new WebApplicationDescriptor("mcp_docs", "MCP Docs", "/", 28280, "MCP");
        List<WebApplicationDescriptor> apps = Arrays.asList(chatApp, mcpApp, mcpDocs);

        // Exact match
        WebApplicationDescriptor found = BrowserUrlUtils.findMatchingApp("http://127.0.0.1:28080/mcp.html", apps);
        assertNotNull(found);
        assertEquals("mcp", found.getId());

        // Subpath match (e.g. hash or internal panel belonging to mcp.html)
        WebApplicationDescriptor foundSubpath = BrowserUrlUtils.findMatchingApp("http://localhost:28080/mcp.html?tool=echo", apps);
        assertNotNull(foundSubpath);
        assertEquals("mcp", foundSubpath.getId());

        // Port 28280 docs match
        WebApplicationDescriptor foundDocs = BrowserUrlUtils.findMatchingApp("http://127.0.0.1:28280/", apps);
        assertNotNull(foundDocs);
        assertEquals("mcp_docs", foundDocs.getId());

        // Unmatched external URL
        assertNull(BrowserUrlUtils.findMatchingApp("https://ollama.com", apps));
    }

    @Test
    public void testExtractPort() {
        assertEquals(28080, BrowserUrlUtils.extractPort("http://localhost:28080/mcp.html"));
        assertEquals(28280, BrowserUrlUtils.extractPort("http://127.0.0.1:28280/"));
        assertEquals(80, BrowserUrlUtils.extractPort("http://example.com/test"));
        assertEquals(443, BrowserUrlUtils.extractPort("https://ollama.com/library"));
    }
}
