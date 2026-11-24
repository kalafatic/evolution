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
import static org.junit.Assert.assertFalse;

/**
 * CSS / visualization resource integrity checks for the Forge Page. Verifies the actual
 * shipped HTML/JS/CSS resources, not merely their existence: the theme tokens must be
 * concrete (no self-referential CSS variables), the shared interaction component must be
 * wired in, and the training loss chart must opt in to interactive zoom/pan.
 */
public class ForgePageResourceIntegrityTest {

    private static final String BASE = "eu/kalafatic/evolution/controller/orchestration/";

    private static String read(String path) throws Exception {
        Bundle b = Platform.getBundle("eu.kalafatic.evolution.controller");
        assertNotNull("controller bundle must be present", b);
        java.net.URL url = b.getEntry(path);
        assertNotNull("resource not found: " + path, url);
        try (InputStream in = url.openStream();
             BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            return r.lines().collect(Collectors.joining("\n"));
        }
    }

    @Test
    public void testThemeTokensAreConcreteNotSelfReferential() throws Exception {
        String html = read(BASE + "forge.html");
        // The previous defect declared e.g. `--evo-bg: var(--evo-bg)` (circular). There
        // must be no self-referential custom property of the form `--x: var(--x)`.
        assertFalse("forge.html must not contain self-referential CSS variables",
                html.matches("(?s).*--[a-z-]+:\\s*var\\(--\\1\\).*"));
        assertTrue("forge.html must define the light-theme background token",
                html.contains("--evo-bg: #f4f6f9"));
        assertTrue("forge.html must define a dark theme block",
                html.contains("[data-theme=\"dark\"]"));
        assertTrue("forge.html must define the primary accent token",
                html.contains("--evo-primary: #2f6bd8"));
    }

    @Test
    public void testSharedInteractionComponentIsWired() throws Exception {
        String html = read(BASE + "forge.html");
        assertTrue("forge.html must load the shared interaction component",
                html.contains("forge-viz/viz-interact.js"));
        String interact = read(BASE + "forge-viz/viz-interact.js");
        assertTrue("interaction component must expose EvoViz.interact",
                interact.contains("EvoViz.interact"));
        assertTrue("interaction component must expose EvoViz.initAll",
                interact.contains("EvoViz.initAll"));
    }

    @Test
    public void testTrainingLossChartOptsIntoInteraction() throws Exception {
        String training = read(BASE + "forge-viz/viz-training.js");
        assertTrue("training loss chart must opt into interactive zoom/pan",
                training.contains("viz-pane"));
    }

    @Test
    public void testInteractiveToolbarCssDefined() throws Exception {
        String html = read(BASE + "forge.html");
        assertTrue("forge.html must define .viz-toolbar styling", html.contains(".viz-toolbar"));
        assertTrue("forge.html must define .viz-zoom-level styling", html.contains(".viz-zoom-level"));
        assertTrue("forge.html must define .viz-pane styling", html.contains(".viz-pane"));
    }
}
