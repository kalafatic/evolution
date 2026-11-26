package eu.kalafatic.evolution.controller.tests;

import eu.kalafatic.evolution.view.util.ExternalBrowserLauncher;
import eu.kalafatic.evolution.view.util.ExternalBrowserLauncher.Outcome;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests for the shared external-browser launcher. Pure functions (URI
 * normalization, scheme classification, file-URI conversion, mail-handler
 * detection, credential scrubbing) are verified directly; rejection paths are
 * exercised headless (they never reach the OS or SWT dialogs — feedback falls
 * back to the log without a display).
 */
public class ExternalBrowserLauncherTest {

    // ---------------------------------------------------------------
    // Target normalization (backslash-escaped URLs)
    // ---------------------------------------------------------------
    @Test
    public void testBackslashEscapedUrlIsNormalized() {
        assertEquals("http://localhost:28080/dashboard.html",
                ExternalBrowserLauncher.normalizeTarget("http\\://localhost:28080/dashboard.html"));
        assertEquals("https://example.com/",
                ExternalBrowserLauncher.normalizeTarget("https\\://example.com/"));
    }

    @Test
    public void testNormalizationTrimsAndLeavesPlainValuesAlone() {
        assertEquals("http://host/", ExternalBrowserLauncher.normalizeTarget("  http://host/  "));
        assertEquals("C:\\Users\\x\\file.html",
                ExternalBrowserLauncher.normalizeTarget("C:\\Users\\x\\file.html"));
        assertNull(ExternalBrowserLauncher.normalizeTarget(null));
    }

    // ---------------------------------------------------------------
    // Scheme classification
    // ---------------------------------------------------------------
    @Test
    public void testSchemeClassification() {
        assertEquals("http", ExternalBrowserLauncher.schemeOf("http://x"));
        assertEquals("https", ExternalBrowserLauncher.schemeOf("https://x"));
        assertEquals("file", ExternalBrowserLauncher.schemeOf("file:///tmp/a.html"));
        assertEquals("mailto", ExternalBrowserLauncher.schemeOf("mailto:a@b.c"));
        assertEquals("data", ExternalBrowserLauncher.schemeOf("data:text/html,x"));
        assertEquals("about", ExternalBrowserLauncher.schemeOf("about:blank"));
        assertEquals("", ExternalBrowserLauncher.schemeOf("/tmp/a.html"));
        assertEquals("", ExternalBrowserLauncher.schemeOf(""));
        assertEquals("", ExternalBrowserLauncher.schemeOf(null));
    }

    // ---------------------------------------------------------------
    // Local HTML file → file URI
    // ---------------------------------------------------------------
    @Test
    public void testFileUriConversionWithSpacesAndUnicode() {
        String uri = ExternalBrowserLauncher.toFileUri(Path.of("/tmp", "evo forge", "diagram ä.html"));
        assertNotNull(uri);
        assertTrue("file URI must be ASCII-encoded (was: " + uri + ")",
                uri.startsWith("file:///") && !uri.contains(" ") && !uri.contains("ä"));
        assertTrue(uri.endsWith(".html"));
    }

    // ---------------------------------------------------------------
    // Mail-handler detection
    // ---------------------------------------------------------------
    @Test
    public void testMailHandlerDetection() {
        assertTrue(ExternalBrowserLauncher.isMailHandlerId("thunderbird_thunderbird.desktop"));
        assertTrue(ExternalBrowserLauncher.isMailHandlerId("org.gnome.Evolution.desktop"));
        assertTrue(ExternalBrowserLauncher.isMailHandlerId("org.kde.kmail.desktop"));
        assertTrue(ExternalBrowserLauncher.isMailHandlerId("mailspring.desktop"));
        assertFalse(ExternalBrowserLauncher.isMailHandlerId("firefox_firefox.desktop"));
        assertFalse(ExternalBrowserLauncher.isMailHandlerId("brave-browser.desktop"));
        assertFalse(ExternalBrowserLauncher.isMailHandlerId("google-chrome.desktop"));
        assertFalse(ExternalBrowserLauncher.isMailHandlerId(null));
        assertFalse(ExternalBrowserLauncher.isMailHandlerId(""));
    }

    // ---------------------------------------------------------------
    // Credential scrubbing for logs
    // ---------------------------------------------------------------
    @Test
    public void testScrubRemovesCredentialsQueryAndFragment() {
        String scrubbed = ExternalBrowserLauncher.scrubForLog(
                "https://user:secret@example.com:8443/path?token=abc123#frag");
        assertEquals("https://example.com:8443/path", scrubbed);
        assertFalse("credentials must never appear in logs", scrubbed.contains("secret"));
        assertFalse("tokens must never appear in logs", scrubbed.contains("abc123"));
        assertEquals("http://example.com/a",
                ExternalBrowserLauncher.scrubForLog("http://example.com/a"));
        assertEquals("/tmp/a b.html", ExternalBrowserLauncher.scrubForLog("/tmp/a b.html"));
    }

    // ---------------------------------------------------------------
    // Rejection paths (headless-safe: no OS launch, no display)
    // ---------------------------------------------------------------
    @Test
    public void testUnsupportedSchemesAreRejected() {
        assertEquals(Outcome.REJECTED, ExternalBrowserLauncher.launchExternal("test", "mailto:a@b.c"));
        assertEquals(Outcome.REJECTED, ExternalBrowserLauncher.launchExternal("test", "data:text/html,hello"));
        assertEquals(Outcome.REJECTED, ExternalBrowserLauncher.launchExternal("test", "about:blank"));
        assertEquals(Outcome.REJECTED, ExternalBrowserLauncher.launchExternal("test", "tel:+123456"));
        assertEquals(Outcome.REJECTED, ExternalBrowserLauncher.launchExternal("test", ""));
        assertEquals(Outcome.REJECTED, ExternalBrowserLauncher.launchExternal("test", null));
        assertEquals(Outcome.REJECTED, ExternalBrowserLauncher.launchExternal("test", "   "));
    }

    @Test
    public void testWebUrlLauncherRejectsNonHttp() {
        assertEquals(Outcome.REJECTED, ExternalBrowserLauncher.launchWebUrl("test", "mailto:a@b.c"));
        assertEquals(Outcome.REJECTED, ExternalBrowserLauncher.launchWebUrl("test", "file:///tmp/a.html"));
        assertEquals(Outcome.REJECTED, ExternalBrowserLauncher.launchWebUrl("test", "/tmp/a.html"));
        assertEquals(Outcome.REJECTED, ExternalBrowserLauncher.launchWebUrl("test", null));
    }

    @Test
    public void testHtmlFileLauncherValidatesInput() throws Exception {
        assertEquals(Outcome.REJECTED, ExternalBrowserLauncher.launchHtmlFile("test", null));
        assertEquals(Outcome.REJECTED,
                ExternalBrowserLauncher.launchHtmlFile("test", Path.of("/nonexistent", "x.html")));
        Path nonHtml = Files.createTempFile("evo-launcher-test", ".txt");
        try {
            Files.writeString(nonHtml, "not html");
            assertEquals(Outcome.REJECTED,
                    ExternalBrowserLauncher.launchHtmlFile("test", nonHtml));
        } finally {
            Files.deleteIfExists(nonHtml);
        }
    }

    @Test
    public void testWindowsDriveHtmlPathRecognition() {
        // Drive-letter paths must not be classified as an unsupported scheme.
        // (Verified indirectly: the scheme classifier yields a single letter,
        // and the launcher's dispatch treats that as a Windows HTML path.)
        assertEquals("c", ExternalBrowserLauncher.schemeOf("C:\\tmp\\diagram.html"));
    }
}
