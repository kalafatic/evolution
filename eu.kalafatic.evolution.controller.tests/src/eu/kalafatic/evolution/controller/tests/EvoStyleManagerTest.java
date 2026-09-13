package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import eu.kalafatic.evolution.controller.ui.EvoStyleManager;
import eu.kalafatic.evolution.controller.ui.IEvoStyleManager;

/**
 * Unit tests for the Unified EVO HTML Style Manager.
 */
public class EvoStyleManagerTest {

    private IEvoStyleManager styleManager;

    @Before
    public void setUp() {
        styleManager = EvoStyleManager.getInstance();
    }

    @Test
    public void testInstanceNotNull() {
        assertNotNull("EvoStyleManager instance should not be null", styleManager);
    }

    @Test
    public void testThemeCssTokens() {
        String themeCss = styleManager.getThemeCss();
        assertNotNull("Theme CSS should not be null", themeCss);
        assertTrue("Theme CSS must define :root", themeCss.contains(":root"));
        assertTrue("Theme CSS must define --evo-bg", themeCss.contains("--evo-bg"));
        assertTrue("Theme CSS must define --evo-surface", themeCss.contains("--evo-surface"));
        assertTrue("Theme CSS must define --evo-border", themeCss.contains("--evo-border"));
        assertTrue("Theme CSS must define --evo-text", themeCss.contains("--evo-text"));
        assertTrue("Theme CSS must define --evo-primary", themeCss.contains("--evo-primary"));
        assertTrue("Theme CSS must define --evo-radius", themeCss.contains("--evo-radius"));
    }

    @Test
    public void testBaseCss() {
        String baseCss = styleManager.getBaseCss();
        assertNotNull("Base CSS should not be null", baseCss);
        assertTrue("Base CSS must contain html, body rules", baseCss.contains("html, body"));
        assertTrue("Base CSS must contain scrollbar rules", baseCss.contains("::-webkit-scrollbar"));
    }

    @Test
    public void testComponentCss() {
        String componentCss = styleManager.getComponentCss();
        assertNotNull("Component CSS should not be null", componentCss);
        assertTrue("Component CSS must define .btn", componentCss.contains(".btn"));
        assertTrue("Component CSS must define .btn-primary", componentCss.contains(".btn-primary"));
        assertTrue("Component CSS must define inputs and selects", componentCss.contains("input[type=\"text\"]"));
        assertTrue("Component CSS must define .evo-panel", componentCss.contains(".evo-panel"));
        assertTrue("Component CSS must define .evo-table", componentCss.contains(".evo-table"));
    }

    @Test
    public void testMasterCss() {
        String masterCss = styleManager.getEvoStyleCss();
        assertNotNull("Master CSS should not be null", masterCss);
        assertTrue("Master CSS must include theme tokens", masterCss.contains("--evo-primary"));
        assertTrue("Master CSS must include base rules", masterCss.contains("html, body"));
        assertTrue("Master CSS must include components", masterCss.contains(".btn-primary"));
    }

    @Test
    public void testPageCssResolution() {
        assertNotNull("Architecture CSS should not be null", styleManager.getPageCss("architecture"));
        assertNotNull("Chat CSS should not be null", styleManager.getPageCss("chat"));
        assertNotNull("Forge CSS should not be null", styleManager.getPageCss("forge"));
        assertNotNull("Develop CSS should not be null", styleManager.getPageCss("develop"));
    }

    @Test
    public void testStyleInjection() {
        String template = "<html><head><title>Test</title>{{EVO_STYLE_CSS}}</head><body></body></html>";
        String injected = styleManager.injectEvoStyle(template);
        assertTrue("Injected template should contain theme tokens", injected.contains("--evo-primary"));
        assertFalse("Placeholder {{EVO_STYLE_CSS}} should be replaced", injected.contains("{{EVO_STYLE_CSS}}"));

        String templateHead = "<html><head><title>Test</title></head><body></body></html>";
        String injectedHead = styleManager.injectEvoStyle(templateHead);
        assertTrue("Injected head should contain style block", injectedHead.contains("<style>"));
        assertTrue("Injected head should contain --evo-primary", injectedHead.contains("--evo-primary"));
    }
}
