package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import eu.kalafatic.evolution.controller.orchestration.WebApplicationDescriptor;
import eu.kalafatic.evolution.view.editors.pages.BrowserPage;
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

    @Test
    public void testAllEvolutionWebPagesBecomeDefaultTabs() {
        // Regression guard: an earlier revision of BrowserPage.initializeTabs only
        // opened a four-entry whitelist (chat/dashboard/develop/mcp) and silently
        // dropped Forge, Architecture, Mutation, Administration and MCP
        // Documentation tabs. Every registered EVO web application must be a
        // default tab regardless of whether its port is reachable yet.
        List<WebApplicationDescriptor> registered = Arrays.asList(
                new WebApplicationDescriptor("chat", "AI Chat", "/experimental/chat?runtime=SWT", 28080, "PRIMARY"),
                new WebApplicationDescriptor("dashboard", "Intelligence Portal", "/dashboard.html?runtime=SWT", 28080, "PRIMARY"),
                new WebApplicationDescriptor("develop", "Develop Agent", "/develop?runtime=SWT", 28080, "PRIMARY"),
                new WebApplicationDescriptor("mcp", "MCP Studio", "/mcp.html?runtime=SWT", 28080, "PRIMARY"),
                new WebApplicationDescriptor("forge", "Forge Studio", "/experimental/forge?runtime=SWT", 28080, "PRIMARY"),
                new WebApplicationDescriptor("architecture", "Architecture", "/experimental/architecture?runtime=SWT", 28080, "PRIMARY"),
                new WebApplicationDescriptor("mutation", "Mutation", "/mutation?runtime=SWT", 28080, "PRIMARY"),
                new WebApplicationDescriptor("admin", "Administration", "/admin.html?runtime=SWT", 28080, "PRIMARY"),
                new WebApplicationDescriptor("mcp_docs", "MCP Documentation", "/", 28280, "MCP"));

        List<WebApplicationDescriptor> defaultTabs = BrowserPage.resolveDefaultTabApplications(registered);

        assertEquals("every registered EVO web page must be a default tab", 9, defaultTabs.size());
        List<String> ids = defaultTabs.stream().map(WebApplicationDescriptor::getId).toList();
        for (String expected : Arrays.asList("chat", "dashboard", "develop", "mcp",
                "forge", "architecture", "mutation", "admin", "mcp_docs")) {
            assertTrue("missing default tab: " + expected, ids.contains(expected));
        }
        // Null-safe: no apps registered → no tabs.
        assertTrue(BrowserPage.resolveDefaultTabApplications(null).isEmpty());
    }

    @Test
    public void testServerPortSeriesPagesResolveToTheirPorts() {
        // Platform +20000 allocation standard: 8080 → 28080, +100 per EVO service.
        // Every HTTP server port owns a default browser tab; the TCP control port
        // (28580) has no web page.
        List<WebApplicationDescriptor> registered = Arrays.asList(
                new WebApplicationDescriptor("primary_home", "EVO Home", "/", 28080, "HTTP_PRIMARY"),
                new WebApplicationDescriptor("portal", "Portal Server", "/dashboard.html", 28180, "HTTP_SECONDARY"),
                new WebApplicationDescriptor("mcp_docs", "MCP Documentation", "/", 28280, "MCP"),
                new WebApplicationDescriptor("develop_endpoint", "Develop Endpoint", "/develop?runtime=SWT", 28380, "DEVELOP"),
                new WebApplicationDescriptor("supervisor_api", "Supervisor REST API", "/health", 28480, "SUPERVISOR_API"),
                new WebApplicationDescriptor("auxiliary", "Auxiliary Service", "/", 28680, "AUXILIARY"),
                new WebApplicationDescriptor("enr_runtime", "ENR Neural Runtime", "/status", 28780, "ENR"));

        WebApplicationDescriptor home = BrowserUrlUtils.findMatchingApp(
                "http://localhost:28080/", registered);
        assertNotNull(home);
        assertEquals("primary_home", home.getId());

        WebApplicationDescriptor portal = BrowserUrlUtils.findMatchingApp(
                "http://localhost:28180/dashboard.html", registered);
        assertNotNull(portal);
        assertEquals("portal", portal.getId());

        WebApplicationDescriptor mcp = BrowserUrlUtils.findMatchingApp(
                "http://localhost:28280/", registered);
        assertNotNull(mcp);
        assertEquals("mcp_docs", mcp.getId());

        WebApplicationDescriptor develop = BrowserUrlUtils.findMatchingApp(
                "http://localhost:28380/develop", registered);
        assertNotNull(develop);
        assertEquals("develop_endpoint", develop.getId());

        WebApplicationDescriptor supervisor = BrowserUrlUtils.findMatchingApp(
                "http://localhost:28480/health", registered);
        assertNotNull(supervisor);
        assertEquals("supervisor_api", supervisor.getId());

        WebApplicationDescriptor enr = BrowserUrlUtils.findMatchingApp(
                "http://localhost:28780/status", registered);
        assertNotNull(enr);
        assertEquals("enr_runtime", enr.getId());
    }
}
