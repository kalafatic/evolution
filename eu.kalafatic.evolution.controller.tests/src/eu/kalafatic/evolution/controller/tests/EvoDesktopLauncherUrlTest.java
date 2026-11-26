package eu.kalafatic.evolution.controller.tests;

import eu.kalafatic.evolution.controller.orchestration.ServerEntry;
import eu.kalafatic.evolution.controller.orchestration.ServerState;
import eu.kalafatic.evolution.view.workbench.EvoDesktopLauncher;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests for the "Open in Web" URL resolution ({@link EvoDesktopLauncher#primaryWebUrl}):
 * the primary web interface URL must be built from the live server entry (configured
 * endpoint + actually allocated port) and must never be hardcoded.
 */
public class EvoDesktopLauncherUrlTest {

    @Test
    public void testUrlFollowsLiveServerEntryEndpoint() {
        // A server entry whose endpoint reflects a non-default allocated port.
        ServerEntry entry = new ServerEntry("HTTP_PRIMARY", "EVO Primary HTTP / REST Server",
                "HTTP", 28123, "127.0.0.1", "http://127.0.0.1:28123/", ServerState.RUNNING);
        assertEquals("http://127.0.0.1:28123/dashboard.html",
                EvoDesktopLauncher.primaryWebUrl(entry, 28123));
    }

    @Test
    public void testUrlUsesEntryEndpointWhenPortArgDiffers() {
        ServerEntry entry = new ServerEntry("HTTP_PRIMARY", "EVO Primary HTTP / REST Server",
                "HTTP", 28123, "127.0.0.1", "http://127.0.0.1:28123/", ServerState.RUNNING);
        // The entry endpoint is authoritative; the port argument must not
        // produce a conflicting URL.
        assertEquals("http://127.0.0.1:28123/dashboard.html",
                EvoDesktopLauncher.primaryWebUrl(entry, 28080));
    }

    @Test
    public void testUrlEndingWithoutSlashStaysUnchanged() {
        ServerEntry entry = new ServerEntry("HTTP_PRIMARY", "EVO Primary HTTP / REST Server",
                "HTTP", 28123, "127.0.0.1", "http://127.0.0.1:28123/dashboard.html",
                ServerState.RUNNING);
        assertEquals("http://127.0.0.1:28123/dashboard.html",
                EvoDesktopLauncher.primaryWebUrl(entry, 28123));
    }

    @Test
    public void testFallbackWithoutEntryUsesResolvedPort() {
        assertEquals("http://127.0.0.1:28080/dashboard.html",
                EvoDesktopLauncher.primaryWebUrl(null, 28080));
    }

    @Test
    public void testFallbackIgnoresBlankEntryEndpoint() {
        ServerEntry entry = new ServerEntry("HTTP_PRIMARY", "EVO Primary HTTP / REST Server",
                "HTTP", 28123, "127.0.0.1", "  ", ServerState.STOPPED);
        assertEquals("http://127.0.0.1:28123/dashboard.html",
                EvoDesktopLauncher.primaryWebUrl(entry, 28123));
    }
}
