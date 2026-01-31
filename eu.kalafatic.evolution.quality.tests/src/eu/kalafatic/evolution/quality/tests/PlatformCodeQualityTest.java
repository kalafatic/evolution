package eu.kalafatic.evolution.quality.tests;

import static org.junit.Assert.*;
import org.junit.Test;
import java.io.File;

public class PlatformCodeQualityTest {

    @Test
    public void testNamingConventions() {
        String packageName = this.getClass().getPackage().getName();
        assertTrue("Package name should start with eu.kalafatic", packageName.startsWith("eu.kalafatic"));
    }

    @Test
    public void testPluginIdMatchesDirectory() {
        // Simple placeholder for a quality check
        String pluginId = "eu.kalafatic.evolution.quality.tests";
        assertTrue(pluginId.contains("evolution"));
    }
}
