package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import eu.kalafatic.utils.hack.EclipseSplashHandler;

public class EclipseSplashHandlerTest {

    @Test
    public void testResolveVersionNonNull() {
        String version = EclipseSplashHandler.resolveVersion();
        assertNotNull("Version should not be null", version);
        assertFalse("Version should not be empty", version.trim().isEmpty());
    }

    @Test
    public void testEclipseSplashHandlerConstructor() {
        EclipseSplashHandler handler = new EclipseSplashHandler();
        assertNotNull("Splash handler version should not be null", handler.getVersion());
        assertFalse("Splash handler version should not be empty", handler.getVersion().trim().isEmpty());
    }
}
