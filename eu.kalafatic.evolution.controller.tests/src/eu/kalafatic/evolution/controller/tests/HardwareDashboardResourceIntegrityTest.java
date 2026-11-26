package eu.kalafatic.evolution.controller.tests;

import org.eclipse.core.runtime.Platform;
import org.junit.Test;
import org.osgi.framework.Bundle;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Resource integrity checks for the Settings hardware dashboard: the HTML,
 * CSS and JS assets must actually ship inside the view bundle (build.properties
 * inclusion) and the JavaScript must expose the incremental-update entry
 * points the Java side pushes through the SWT Browser bridge.
 */
public class HardwareDashboardResourceIntegrityTest {

    private static String read(String path) throws Exception {
        Bundle b = Platform.getBundle("eu.kalafatic.evolution.view");
        assertNotNull("view bundle must be present", b);
        java.net.URL url = b.getEntry(path);
        assertNotNull("resource not found: " + path, url);
        try (InputStream in = url.openStream();
             BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            return r.lines().collect(Collectors.joining("\n"));
        }
    }

    @Test
    public void testDashboardAssetsShipInViewBundle() throws Exception {
        String html = read("hardware-dashboard.html");
        assertTrue("dashboard must load its stylesheet", html.contains("css/hardware-dashboard.css"));
        assertTrue("dashboard must load its script", html.contains("js/hardware-dashboard.js"));
        assertTrue("dashboard must render CPU utilization chart", html.contains("chart-cpu-util"));
        assertTrue("dashboard must render RAM chart", html.contains("chart-ram-util"));
        assertTrue("dashboard must render JVM heap chart", html.contains("chart-jvm-util"));
        assertTrue("dashboard must render a GPU container", html.contains("hw-gpu-container"));
        assertTrue("dashboard must expose scan/test/refresh actions", html.contains("hw-btn-scan")
                && html.contains("hw-btn-cuda") && html.contains("hw-btn-refresh"));
    }

    @Test
    public void testDashboardJsExposesIncrementalEntryPoints() throws Exception {
        String js = read("js/hardware-dashboard.js");
        assertTrue("JS must expose the sample push entry point", js.contains("window.evoHw"));
        assertTrue("JS must support incremental sample pushes", js.contains("push:"));
        assertTrue("JS must support scan results", js.contains("setScan"));
        assertTrue("JS must support status updates", js.contains("setStatus"));
        assertTrue("JS must render gaps for missing metrics (null values)", js.contains("p.v === null"));
        assertTrue("JS must keep bounded history", js.contains("HISTORY"));
        assertTrue("JS must bridge back to Java", js.contains("evoHwAction"));
        assertTrue("JS must signal readiness to Java", js.contains("evoHwReady"));
    }

    @Test
    public void testDashboardCssDefinesProfessionalTheme() throws Exception {
        String css = read("css/hardware-dashboard.css");
        assertTrue("CSS must define the dark engineering background", css.contains("--bg: #0e1420"));
        assertTrue("CSS must define metric cards", css.contains(".hw-card"));
        assertTrue("CSS must define chart boxes", css.contains(".hw-chart-box"));
        assertTrue("CSS must define status chips", css.contains(".hw-chip"));
        assertTrue("CSS must define status colors", css.contains("--success"));
    }
}
